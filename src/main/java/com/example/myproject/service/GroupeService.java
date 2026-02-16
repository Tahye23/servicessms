package com.example.myproject.service;

import com.example.myproject.domain.Configuration;
import com.example.myproject.domain.Contact;
import com.example.myproject.domain.Groupe;
import com.example.myproject.domain.Groupedecontact;
import com.example.myproject.domain.User;
import com.example.myproject.repository.*;
import com.example.myproject.security.SecurityUtils;
import jakarta.persistence.EntityNotFoundException;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class GroupeService {

    private static final Logger log = LoggerFactory.getLogger(GroupeService.class);
    private final GroupeRepository groupeRepository;
    private final ContactRepository contactRepository;
    private final GroupedecontactRepository groupedecontactRepository;
    private final WhatsAppApiService whatsAppApiService;
    private final ConfigurationRepository configurationRepository;
    private final UserRepository userRepository;

    public GroupeService(
        GroupeRepository groupeRepository,
        ContactRepository contactRepository,
        GroupedecontactRepository groupedecontactRepository,
        WhatsAppApiService whatsAppApiService,
        ConfigurationRepository configurationRepository,
        UserRepository userRepository
    ) {
        this.groupeRepository = groupeRepository;
        this.contactRepository = contactRepository;
        this.groupedecontactRepository = groupedecontactRepository;
        this.whatsAppApiService = whatsAppApiService;
        this.configurationRepository = configurationRepository;
        this.userRepository = userRepository;
    }

    /**
     * Ajoute une liste de contacts à un groupe via la table de liaison Groupedecontact.
     * @param groupeId Id du groupe
     * @param contactIds Liste des IDs contacts à ajouter
     */
    public void addContactsToGroup(Long groupeId, List<Long> contactIds) {
        Groupe groupe = groupeRepository
            .findById(groupeId)
            .orElseThrow(() -> new EntityNotFoundException("Groupe non trouvé : " + groupeId));

        List<Contact> contacts = contactRepository.findAllById(contactIds);

        for (Contact contact : contacts) {
            // Vérifier si la liaison existe déjà pour éviter doublons
            boolean alreadyLinked = groupedecontactRepository.existsByCgrgroupeAndContact(groupe, contact);
            if (!alreadyLinked) {
                Groupedecontact linkage = new Groupedecontact();
                linkage.setCgrgroupe(groupe);
                linkage.setContact(contact);
                groupedecontactRepository.save(linkage);
            }
        }
    }

    public Map<String, Object> verifyWhatsAppContactsAndCreateGroup(Long groupeId) {
        // Récupérer le groupe source
        Groupe sourceGroupe = groupeRepository
            .findById(groupeId)
            .orElseThrow(() -> new EntityNotFoundException("Groupe non trouvé : " + groupeId));

        // Déterminer l'utilisateur effectif
        String effectiveUserLogin = determineEffectiveUserLogin(sourceGroupe.getUser_id());

        // Récupérer la configuration WhatsApp de l'utilisateur
        Optional<Configuration> configOpt = configurationRepository.findOneByUserLogin(effectiveUserLogin);
        if (configOpt.isEmpty()) {
            throw new IllegalStateException("Configuration WhatsApp non trouvée pour l'utilisateur : " + effectiveUserLogin);
        }

        Configuration config = configOpt.get();
        if (!config.isVerified() || !Boolean.TRUE.equals(config.getValid())) {
            throw new IllegalStateException("Configuration WhatsApp invalide ou non vérifiée pour l'utilisateur : " + effectiveUserLogin);
        }

        // Récupérer tous les contacts du groupe
        List<Contact> contacts = groupedecontactRepository.findContactsByGroupeId(groupeId);

        // Collecter tous les numéros pour vérification
        List<String> phoneNumbers = contacts
            .stream()
            .map(Contact::getContelephone)
            .filter(Objects::nonNull)
            .filter(phone -> !phone.trim().isEmpty())
            .toList();

        log.info(
            "Vérification de {} numéros WhatsApp pour le groupe {} avec la configuration de {}",
            phoneNumbers.size(),
            groupeId,
            effectiveUserLogin
        );

        // Vérifier tous les numéros avec la configuration utilisateur
        Map<String, Boolean> whatsappResults = whatsAppApiService.verifyMultipleNumbers(phoneNumbers, config);

        List<Contact> whatsappContacts = new ArrayList<>();
        List<Contact> invalidContacts = new ArrayList<>();

        // Classer les contacts selon les résultats
        for (Contact contact : contacts) {
            Boolean isValid = whatsappResults.get(contact.getContelephone());
            if (Boolean.TRUE.equals(isValid)) {
                whatsappContacts.add(contact);
            } else {
                invalidContacts.add(contact);
            }
        }

        // Créer le groupe WhatsApp si des contacts valides
        Groupe whatsappGroupe = null;
        if (!whatsappContacts.isEmpty()) {
            whatsappGroupe = new Groupe();
            whatsappGroupe.setGrotitre(sourceGroupe.getGrotitre() + " - WhatsApp");
            whatsappGroupe.setUser_id(sourceGroupe.getUser_id());
            whatsappGroupe.setGroupType("whatsapp");
            whatsappGroupe = groupeRepository.save(whatsappGroupe);

            // Ajouter les contacts valides au nouveau groupe
            for (Contact contact : whatsappContacts) {
                Groupedecontact linkage = new Groupedecontact();
                linkage.setCgrgroupe(whatsappGroupe);
                linkage.setContact(contact);
                groupedecontactRepository.save(linkage);
            }

            log.info("Groupe WhatsApp créé avec {} contacts valides : {}", whatsappContacts.size(), whatsappGroupe.getGrotitre());
        }

        // Retourner les résultats
        Map<String, Object> result = new HashMap<>();
        result.put("totalContacts", contacts.size());
        result.put("validWhatsappContacts", whatsappContacts.size());
        result.put("invalidContacts", invalidContacts.size());
        result.put("whatsappGroupId", whatsappGroupe != null ? whatsappGroupe.getId() : null);
        result.put("whatsappGroupName", whatsappGroupe != null ? whatsappGroupe.getGrotitre() : null);
        result.put("configurationUser", effectiveUserLogin);

        return result;
    }

    /**
     * Détermine l'utilisateur effectif (même logique que dans getAllGroupes)
     */
    private String determineEffectiveUserLogin(String userId) {
        if (userId == null) return null;

        boolean isUser =
            SecurityUtils.hasCurrentUserAnyOfAuthorities("ROLE_USER") && !SecurityUtils.hasCurrentUserAnyOfAuthorities("ROLE_ADMIN");
        if (!isUser) {
            return userId;
        }

        return userRepository.findOneByLogin(userId).map(User::getExpediteur).filter(Objects::nonNull).orElse(userId);
    }
}
