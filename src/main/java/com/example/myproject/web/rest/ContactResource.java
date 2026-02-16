package com.example.myproject.web.rest;

import com.example.myproject.domain.*;
import com.example.myproject.repository.*;
import com.example.myproject.security.SecurityUtils;
import com.example.myproject.service.ContactCleaningService;
import com.example.myproject.service.dto.AdvancedFiltersPayload;
import com.example.myproject.service.dto.ProgressStatus;
import com.example.myproject.service.dto.ProgressTracker;
import com.example.myproject.service.helper.PhoneNumberHelper;
import com.example.myproject.web.rest.dto.*;
import com.example.myproject.web.rest.errors.BadRequestAlertException;
import com.example.myproject.web.rest.errors.ResourceNotFoundException;
import com.example.myproject.web.rest.errors.UnauthorizedException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.exceptions.CsvException; // Assurez-vous que cette bibliothèque est incluse
import com.opencsv.exceptions.CsvException;
import jakarta.servlet.http.HttpServletResponse;
import java.io.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.List;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.PaginationUtil;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link Contact}.
 */
@RestController
@RequestMapping("/api/contacts")
@Transactional
public class ContactResource {

    private final Logger log = LoggerFactory.getLogger(ContactResource.class);

    private static final String ENTITY_NAME = "contact";
    private static final int BATCH_SIZE = 1000; // Taille de batch pour traitement en mémoire
    private static final String ERROR_HEADER = "LineNumber,Error,LineContent";
    private static final String CSV_DELIMITER = "[;]";
    private final SmsRepository smsRepository;
    private final GroupedecontactRepository groupedecontactRepository;

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final ImportHistoryRepository importHistoryRepository;
    private final UserRepository userRepository;
    private final ContactRepository contactRepository;
    private final GroupeRepository groupeRepository;
    private final ContactCleaningService contactCleaningService;
    private final ProgressTracker progressTracker;

    @Autowired
    private ObjectMapper objectMapper;

    public ContactResource(
        SmsRepository smsRepository,
        GroupedecontactRepository groupedecontactRepository,
        ImportHistoryRepository importHistoryRepository,
        UserRepository userRepository,
        ContactRepository contactRepository,
        GroupeRepository groupeRepository,
        ContactCleaningService contactCleaningService,
        ProgressTracker progressTracker
    ) {
        this.smsRepository = smsRepository;
        this.groupedecontactRepository = groupedecontactRepository;
        this.importHistoryRepository = importHistoryRepository;
        this.userRepository = userRepository;
        this.contactRepository = contactRepository;
        this.groupeRepository = groupeRepository;
        this.contactCleaningService = contactCleaningService;
        this.progressTracker = progressTracker;
    }

    /**
     * {@code POST  /contacts} : Create a new contact.
     *
     * @param contactDTO the contact to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new contact, or with status {@code 400 (Bad Request)} if the contact has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<Contact> createContact(@RequestBody ContactDTO contactDTO) throws URISyntaxException {
        log.debug("REST request to save Contact : {}", contactDTO);

        if (contactDTO.getId() != null) {
            throw new BadRequestAlertException("A new contact cannot already have an ID", ENTITY_NAME, "idexists");
        }

        // Récupérer le login de l'utilisateur connecté
        String userLogin = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new IllegalStateException("L'utilisateur n'est pas connecté"));
        String currentUserLogin = determineEffectiveUserLogin(userLogin);
        String rawPhone = contactDTO.getContelephone();
        if (!PhoneNumberHelper.isValidPhoneNumber(rawPhone)) {
            throw new BadRequestAlertException("Le numéro de téléphone fourni est invalide.", ENTITY_NAME, "invalidPhone");
        }

        String normalized = PhoneNumberHelper.normalizePhoneNumber(rawPhone);
        // Vérifier si le contact existe déjà pour cet utilisateur
        boolean contactExists = contactRepository.existsByContelephoneAndUserLogin(normalized, currentUserLogin);
        if (contactExists) {
            throw new BadRequestAlertException("Un contact avec ce numéro existe déjà pour cet utilisateur", ENTITY_NAME, "contactexists");
        }

        // Mapper le DTO vers l'entité Contact
        Contact contact = new Contact();
        contact.setConnom(contactDTO.getConnom());
        contact.setConprenom(contactDTO.getConprenom());
        contact.setContelephone(normalized);
        contact.setStatuttraitement(contactDTO.getStatuttraitement());
        contact.setUser_login(currentUserLogin);
        contact.setCustomFields(contactDTO.getCustomFields());

        // Associations de groupes
        if (contactDTO.getGroupes() != null && !contactDTO.getGroupes().isEmpty()) {
            Set<Groupedecontact> associations = new HashSet<>();
            for (Groupe groupe : contactDTO.getGroupes()) {
                Groupedecontact association = new Groupedecontact();
                association.setCgrgroupe(groupe);
                association.setContact(contact);
                associations.add(association);
            }
            contact.setGroupedecontacts(associations);
        }

        // Sauvegarde en base
        contact = contactRepository.save(contact);

        return ResponseEntity.created(new URI("/api/contacts/" + contact.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, contact.getId().toString()))
            .body(contact);
    }

    // Modification à apporter à votre contrôleur backend pour supporter la pagination des messages

    @GetMapping("/{contactId}/messages")
    public ResponseEntity<List<Sms>> getMessagesForContact(
        @PathVariable Long contactId,
        @RequestParam(value = "page", defaultValue = "0") int page,
        @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        Contact contact = contactRepository
            .findById(contactId)
            .orElseThrow(() -> new ResourceNotFoundException("Contact introuvable avec id " + contactId));

        String phone = contact.getContelephone();

        // Création d'un objet Pageable pour la pagination
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "sendDate"));

        // Récupération des messages paginés
        Page<Sms> messagesPage = smsRepository.findBySenderOrReceiverOrderBySendDateAsc(phone, phone, pageable);

        // Ajout de l'en-tête X-Total-Count
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Total-Count", String.valueOf(messagesPage.getTotalElements()));

        return ResponseEntity.ok().headers(headers).body(messagesPage.getContent());
    }

    @GetMapping("/import-history")
    public ResponseEntity<Page<ImportHistory>> getImportHistory(
        @org.springdoc.core.annotations.ParameterObject Pageable pageable,
        @RequestParam(value = "search", required = false) String search
    ) {
        log.debug("REST request to get a page of ImportHistory with search: {}", search);
        Page<ImportHistory> page;

        if (SecurityUtils.hasCurrentUserAnyOfAuthorities("ROLE_ADMIN")) {
            if (search != null && !search.trim().isEmpty()) {
                page = importHistoryRepository.searchImportHistory(search.trim(), pageable);
            } else {
                page = importHistoryRepository.findAll(pageable);
            }
        } else {
            Optional<String> currentUserLoginOpt = SecurityUtils.getCurrentUserLogin();
            if (currentUserLoginOpt.isPresent()) {
                String currentUserLogin = currentUserLoginOpt.orElseThrow(
                    () -> new IllegalStateException("L'utilisateur n'est pas authentifié")
                );
                if (search != null && !search.trim().isEmpty()) {
                    page = importHistoryRepository.findByUserLoginAndSearch(currentUserLogin, search.trim(), pageable);
                } else {
                    page = importHistoryRepository.findByUserLogin(currentUserLogin, pageable);
                }
            } else {
                page = Page.empty(pageable);
            }
        }

        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page);
    }

    public List<Contact> importContact(MultipartFile file) throws IOException {
        List<Contact> contacts = new ArrayList<>();
        if (file.isEmpty()) {
            return contacts;
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            //List<Contact> contacts = new ArrayList<>();
            Set<String> uniquePhoneNumbers = new HashSet<>();
            // Skip the header line
            reader.readLine();
            while ((line = reader.readLine()) != null) {
                String[] fields = line.split(",");
                Contact contact = new Contact();

                //contact.setContelephone(fields[2]);
                //String phoneNumber = fields[2];
                log.info("Numéro de téléphone brut : '{}'", fields[2]);
                String Name = fields[0].replaceAll("[^\\d]", "").trim();
                String Prenom = fields[1].replaceAll("[^\\d]", "").trim();
                String phoneNumber = fields[2].replaceAll("[^\\d]", "").trim();

                contact.setConnom(Name);
                contact.setConprenom(Prenom);
                contact.setContelephone(phoneNumber);
                contacts.add(contact);
                // Set other fields as needed

            }

            return contacts;
            // return contactRepository.saveAll(contacts);
        }
    }

    /**
     * {@code PUT  /contacts/:id} : Updates an existing contact.
     *
     * @param file the contact to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated contact,
     * or with status {@code 400 (Bad Request)} if the contact is not valid,
     * or with status {@code 500 (Internal Server Error)} if the contact couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */

    @PostMapping("/pushfile/idop")
    public ResponseEntity<Void> uploadCSV(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            List<Contact> contacts = new ArrayList<>();
            Set<String> uniquePhoneNumbers = new HashSet<>();
            // Skip the header line
            reader.readLine();
            while ((line = reader.readLine()) != null) {
                String[] fields = line.split(",");
                Contact contact = new Contact();

                //contact.setContelephone(fields[2]);
                //String phoneNumber = fields[2];
                log.info("Numéro de téléphone brut : '{}'", fields[2]);
                //String Name = fields[0].replaceAll("[^\\d]", "").trim();
                //String Prenom =fields[1].replaceAll("[^\\d]", "").trim();
                String phoneNumber = fields[2].replaceAll("[^\\d]", "").trim();
                if (PhoneNumberHelper.isValidPhoneNumber(phoneNumber)) {
                    if (uniquePhoneNumbers.add(phoneNumber)) {
                        contact.setConnom(fields[0]);
                        contact.setConprenom(fields[1]);
                        contact.setContelephone(phoneNumber);
                        contacts.add(contact);
                    }
                } else {
                    log.error("Le numéro de téléphone '{}' est invalide.", phoneNumber);
                }
                // Set other fields as needed

            }

            contactRepository.saveAll(contacts);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Contact> updateContact(@PathVariable("id") final Long id, @RequestBody ContactDTO contactDTO)
        throws URISyntaxException {
        log.debug("REST request to update Contact : {}, {}", id, contactDTO);

        if (contactDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, contactDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }
        if (!contactRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        // Récupérer le login de l'utilisateur connecté
        Optional<String> currentUserLoginOpt = SecurityUtils.getCurrentUserLogin();
        if (!currentUserLoginOpt.isPresent()) {
            throw new IllegalStateException("L'utilisateur n'est pas connecté");
        }
        String currentUserLogin = currentUserLoginOpt.orElseThrow(() -> new IllegalStateException("L'utilisateur n'est pas authentifié"));

        if (!PhoneNumberHelper.isValidPhoneNumber(contactDTO.getContelephone())) {
            throw new BadRequestAlertException(
                "\"Le numéro de téléphone fourni est invalide. Veuillez vérifier sa saisie. ",
                ENTITY_NAME,
                "contactInvalid"
            );
        }
        String normalizedPhone = PhoneNumberHelper.normalizePhoneNumber(contactDTO.getContelephone());

        // Mapper le DTO vers l'entité existante
        Contact contactToUpdate = contactRepository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Le contact avec l'ID " + id + " n'a pas été trouvé"));
        if (!Objects.equals(contactToUpdate.getContelephone(), contactDTO.getContelephone())) {
            boolean contactExists = contactRepository.existsByContelephoneAndUserLogin(normalizedPhone, currentUserLogin);
            if (contactExists) {
                throw new BadRequestAlertException(
                    "Un contact avec ce numéro existe déjà pour cet utilisateur",
                    ENTITY_NAME,
                    "contactexists"
                );
            }
        }

        contactToUpdate.setConnom(contactDTO.getConnom());
        contactToUpdate.setConprenom(contactDTO.getConprenom());
        contactToUpdate.setContelephone(normalizedPhone);
        contactToUpdate.setStatuttraitement(contactDTO.getStatuttraitement());
        contactToUpdate.setUser_login(currentUserLogin);
        contactToUpdate.setCustomFields(contactDTO.getCustomFields());

        // Réinitialiser les associations actuelles
        contactToUpdate.getGroupedecontacts().clear();
        if (contactDTO.getGroupes() != null && !contactDTO.getGroupes().isEmpty()) {
            for (Groupe groupe : contactDTO.getGroupes()) {
                Groupedecontact association = new Groupedecontact();
                association.setCgrgroupe(groupe);
                association.setContact(contactToUpdate);
                contactToUpdate.getGroupedecontacts().add(association);
            }
        }

        contactToUpdate = contactRepository.save(contactToUpdate);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, contactToUpdate.getId().toString()))
            .body(contactToUpdate);
    }

    /**
     * {@code PATCH  /contacts/:id} : Partial updates given fields of an existing contact, field will ignore if it is null
     *
     * @param id the id of the contact to save.
     * @param contact the contact to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated contact,
     * or with status {@code 400 (Bad Request)} if the contact is not valid,
     * or with status {@code 404 (Not Found)} if the contact is not found,
     * or with status {@code 500 (Internal Server Error)} if the contact couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<Contact> partialUpdateContact(
        @PathVariable(value = "id", required = false) final Long id,
        @RequestBody Contact contact
    ) throws URISyntaxException {
        log.debug("REST request to partial update Contact partially : {}, {}", id, contact);
        if (contact.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, contact.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!contactRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<Contact> result = contactRepository
            .findById(contact.getId())
            .map(existingContact -> {
                if (contact.getConid() != null) {
                    existingContact.setConid(contact.getConid());
                }
                if (contact.getConnom() != null) {
                    existingContact.setConnom(contact.getConnom());
                }
                if (contact.getConprenom() != null) {
                    existingContact.setConprenom(contact.getConprenom());
                }
                if (contact.getContelephone() != null) {
                    existingContact.setContelephone(contact.getContelephone());
                }
                if (contact.getStatuttraitement() != null) {
                    existingContact.setStatuttraitement(contact.getStatuttraitement());
                }

                return existingContact;
            })
            .map(contactRepository::save);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, contact.getId().toString())
        );
    }

    /**
     * {@code GET  /contacts} : get all the contacts.
     *
     * @param pageable the pagination information.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of contacts in body.
     */
    @GetMapping("")
    public ResponseEntity<List<Contact>> getAllContacts(
        @ParameterObject Pageable pageable,
        @RequestParam(value = "search", required = false) String search
    ) {
        log.debug("REST request to get a page of Contacts with search: {}", search);

        try {
            String currentUserLogin = getCurrentUserLoginOrThrow();
            String effectiveUserLogin = determineEffectiveUserLogin(currentUserLogin);
            boolean isAdmin = SecurityUtils.hasCurrentUserAnyOfAuthorities("ROLE_ADMIN");

            Page<Contact> page = getContactsPage(search, effectiveUserLogin, isAdmin, pageable);

            HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);

            return ResponseEntity.ok().headers(headers).body(page.getContent());
        } catch (IllegalStateException e) {
            log.warn("Utilisateur non authentifié lors de la récupération des contacts");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des contacts", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // /api/contacts/filter?page=0&size=20&sort=id,desc
    @PostMapping("/filter")
    public ResponseEntity<List<Contact>> filterContacts(@RequestBody AdvancedFiltersPayload f, @ParameterObject Pageable pageable) {
        try {
            String currentUserLogin = getCurrentUserLoginOrThrow();
            String effectiveUserLogin = determineEffectiveUserLogin(currentUserLogin);
            boolean isAdmin = SecurityUtils.hasCurrentUserAnyOfAuthorities("ROLE_ADMIN");
            String userLoginParam = isAdmin ? null : effectiveUserLogin;

            norm(f);
            String nomLike = notBlank(f.getNom()) ? toLike(f.getNom(), f.getNomFilterType()) : null;
            String prenomLike = notBlank(f.getPrenom()) ? toLike(f.getPrenom(), f.getPrenomFilterType()) : null;
            String telephoneLike = notBlank(f.getTelephone()) ? toLike(f.getTelephone(), f.getTelephoneFilterType()) : null;

            String searchLike = notBlank(f.getSearch()) ? "%" + f.getSearch().trim() + "%" : null;

            log.info("FilterContacts - nom={}, prenom={}, tel={}, search={}", nomLike, prenomLike, telephoneLike, searchLike);

            Page<Contact> page = contactRepository.findAllWithAdvancedFilters_like(
                userLoginParam,
                nomLike,
                prenomLike,
                telephoneLike,
                searchLike,
                f.getStatut(),
                f.getHasWhatsapp(),
                f.getMinSmsSent(),
                f.getMaxSmsSent(),
                f.getMinWhatsappSent(),
                f.getMaxWhatsappSent(),
                f.getHasReceivedMessages(),
                pageable
            );

            HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);

            return ResponseEntity.ok().headers(headers).body(page.getContent());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (Exception e) {
            log.error("Erreur filterContacts", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /* Helpers */
    private void norm(AdvancedFiltersPayload f) {
        f.setSearch(trimToNull(f.getSearch()));
        f.setNom(trimToNull(f.getNom()));
        f.setPrenom(trimToNull(f.getPrenom()));
        f.setTelephone(trimToNull(f.getTelephone()));
        f.setSmsStatus(trimToNull(f.getSmsStatus()));
        f.setDeliveryStatus(trimToNull(f.getDeliveryStatus()));
        f.setLastErrorContains(trimToNull(f.getLastErrorContains()));
        f.setNomFilterType(defaultType(f.getNomFilterType()));
        f.setPrenomFilterType(defaultType(f.getPrenomFilterType()));
        f.setTelephoneFilterType(defaultType(f.getTelephoneFilterType()));
    }

    private String trimToNull(String s) {
        return (s == null || s.trim().isEmpty()) ? null : s.trim();
    }

    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private String defaultType(String t) {
        return notBlank(t) ? t : "contains"; // valeurs: contains | startsWith | endsWith | exact
    }

    private String toLike(String value, String type) {
        if (!notBlank(value)) return null;
        String v = value.trim();
        switch (type == null ? "contains" : type) {
            case "startsWith":
                return v + "%";
            case "endsWith":
                return "%" + v;
            case "exact":
                return v; // ILIKE 'abc' ⇔ égal (sans %)
            default:
                return "%" + v + "%";
        }
    }

    // Méthodes extraites pour améliorer la lisibilité et la testabilité

    private String getCurrentUserLoginOrThrow() {
        return SecurityUtils.getCurrentUserLogin().orElseThrow(() -> new IllegalStateException("L'utilisateur n'est pas authentifié"));
    }

    private String determineEffectiveUserLogin(String currentUserLogin) {
        boolean isUser =
            SecurityUtils.hasCurrentUserAnyOfAuthorities("ROLE_USER") && !SecurityUtils.hasCurrentUserAnyOfAuthorities("ROLE_ADMIN");

        if (!isUser) {
            return currentUserLogin;
        }

        return userRepository.findOneByLogin(currentUserLogin).map(User::getExpediteur).filter(Objects::nonNull).orElse(currentUserLogin);
    }

    private Page<Contact> getContactsPage(String search, String userLogin, boolean isAdmin, Pageable pageable) {
        boolean hasSearch = search != null && !search.trim().isEmpty();
        String trimmedSearch = hasSearch ? search.trim() : null;

        if (isAdmin) {
            return hasSearch ? contactRepository.searchContacts(trimmedSearch, pageable) : contactRepository.findAll(pageable);
        } else {
            return hasSearch
                ? contactRepository.findByUserLoginAndSearch(userLogin, trimmedSearch, pageable)
                : contactRepository.findByUserLogin(userLogin, pageable);
        }
    }

    @GetMapping("/groupe/{groupeId}")
    public ResponseEntity<List<Contact>> getContactsByGroupeId(@PathVariable Long groupeId) {
        List<Contact> contacts = contactRepository.findByGroupeId(groupeId);
        if (!contacts.isEmpty()) {
            return ResponseEntity.ok().body(contacts);
        }
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(Collections.emptyList());
    }

    /**
     * {@code GET  /contacts/:id} : get the "id" contact.
     *
     * @param id the id of the contact to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the contact, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Contact> getContact(@PathVariable("id") Long id) {
        log.debug("REST request to get Contact : {}", id);
        Optional<Contact> contact = contactRepository.findById(id);
        return ResponseUtil.wrapOrNotFound(contact);
    }

    @GetMapping("/valides")
    public ResponseEntity<List<Contact>> getValidContacts() {
        // Utiliser la méthode findByStatuttraitement pour récupérer les contacts valides
        List<Contact> validContacts = contactRepository.findByStatuttraitement(1);

        // Retourner la réponse avec la liste des contacts valides
        return ResponseEntity.ok(validContacts);
    }

    @GetMapping("/dublants")
    public ResponseEntity<List<Contact>> getdublantContacts() {
        // Utiliser la méthode findByStatuttraitement pour récupérer les contacts valides
        List<Contact> doublantContacts = contactRepository.findByStatuttraitement(3);

        // Retourner la réponse avec la liste des contacts valides
        return ResponseEntity.ok(doublantContacts);
    }

    @GetMapping("/allContacts")
    public ResponseEntity<List<Contact>> getContacts() {
        List<Contact> contactList = this.contactRepository.findAll();
        return ResponseEntity.ok().body(contactList);
    }

    /**
     * {@code DELETE  /contacts/:id} : delete the "id" contact.
     *
     * @param id the id of the contact to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteContact(@PathVariable("id") Long id) {
        log.debug("REST request to delete Contact : {}", id);
        contactRepository.deleteById(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }

    @PostMapping("/clean")
    public ResponseEntity<DuplicateContactsResponse> cleanContacts(
        @RequestParam("file") MultipartFile file,
        @RequestParam("groupId") Long groupId,
        @RequestParam(value = "insert", defaultValue = "true") boolean insert
    ) {
        Optional<String> loginOpt = SecurityUtils.getCurrentUserLogin();
        if (!loginOpt.isPresent()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String login = loginOpt.get();
        String progressId = generateUniqueProgressId();
        ImportHistory importHistory = new ImportHistory();
        int getTotalInserted = 0;

        try {
            DuplicateContactsResponse result = processContacts(file, groupId, login, progressId);
            getTotalInserted = result.getTotalInserted();

            if (insert && result.getTotalInserted() > 0) {
                saveImportHistory(importHistory, result, progressId, login);
                contactCleaningService.insertContactsAsync(result.getAllContactsToInsert(), login, progressId);
            }

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            handleImportError(importHistory, insert, getTotalInserted, progressId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    public DuplicateContactsResponse processContacts(MultipartFile file, Long selectedGroupId, String currentUserLogin, String progressId) {
        ImportCounters counters = new ImportCounters();

        OutputFiles outputFiles = initializeOutputFiles();
        Map<String, Map<String, Object>> allowedCustomFields = loadCustomFields(currentUserLogin);
        Groupe groupe = loadGroupe(selectedGroupId);
        ImportResults results = new ImportResults();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                return createEmptyResponse(counters, outputFiles, progressId);
            }

            String[] headers = headerLine.split(CSV_DELIMITER);
            initializeOutputFileHeaders(outputFiles, headerLine);

            String line;
            int lineNumber = 1;

            while ((line = reader.readLine()) != null) {
                counters.totalFileLines.incrementAndGet();
                lineNumber++;

                processSimpleLineNoCheck(
                    line,
                    lineNumber,
                    headers,
                    allowedCustomFields,
                    groupe,
                    currentUserLogin,
                    progressId,
                    outputFiles,
                    counters,
                    results
                );
            }
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors du traitement du fichier CSV", e);
        } finally {
            closeOutputFiles(outputFiles);
        }

        progressTracker.init(progressId, results.contactsToInsert.size());
        return buildSimplifiedResponse(results, counters, outputFiles, progressId);
    }

    private void processSimpleLineNoCheck(
        String line,
        int lineNumber,
        String[] headers,
        Map<String, Map<String, Object>> allowedCustomFields,
        Groupe groupe,
        String currentUserLogin,
        String progressId,
        OutputFiles outputFiles,
        ImportCounters counters,
        ImportResults results
    ) {
        String[] fields = line.split(CSV_DELIMITER, -1);
        if (fields.length < 1) {
            writeError(outputFiles.errorWriter, lineNumber, "Colonnes insuffisantes", line);
            counters.errorCount.incrementAndGet();
            return;
        }

        String phoneNumber = extractField(fields, 0); // number
        String nom = extractField(fields, 1); // nom (contient le nom complet)
        String prenom = extractField(fields, 2); // prenom (VIDE)

        log.debug(" Ligne {}: phone={}, nom={}, prenom={}, nb_colonnes={}", lineNumber, phoneNumber, nom, prenom, fields.length);

        if (phoneNumber == null || phoneNumber.isEmpty()) {
            writeError(outputFiles.errorWriter, lineNumber, "Téléphone vide", line);
            counters.errorCount.incrementAndGet();
            return;
        }

        if (!PhoneNumberHelper.isValidPhoneNumber(phoneNumber)) {
            writeError(outputFiles.errorWriter, lineNumber, "Téléphone invalide: " + phoneNumber, line);
            counters.errorCount.incrementAndGet();
            return;
        }

        String normalizedPhone = PhoneNumberHelper.normalizePhoneNumber(phoneNumber);

        try {
            Contact contact = new Contact();
            contact.setConnom(nom); // "ROUGHAYA MT ABDERRAHMANE"
            contact.setConprenom(prenom); // null ou vide
            contact.setContelephone(normalizedPhone);
            contact.setUser_login(currentUserLogin);
            contact.setStatuttraitement(1);
            contact.setProgressId(progressId);

            Map<String, String> customValues = new HashMap<>();

            if (fields.length > 3 && headers.length > 3) {
                for (int i = 3; i < Math.min(fields.length, headers.length); i++) {
                    String key = headers[i].trim();
                    String value = extractField(fields, i);

                    if (value != null && !value.isEmpty()) {
                        if (allowedCustomFields.containsKey(key)) {
                            customValues.put(key, value);
                            log.debug("  Champ personnalisé: {} = {}", key, value);
                        }
                    }
                }
            }

            if (!customValues.isEmpty()) {
                contact.setCustomFields(new ObjectMapper().writeValueAsString(customValues));
            }

            createGroupAssociation(contact, groupe);

            synchronized (results.contactsToInsert) {
                results.contactsToInsert.add(contact);
            }

            counters.addedToGroup.incrementAndGet();
        } catch (Exception e) {
            writeError(outputFiles.errorWriter, lineNumber, "Erreur création: " + e.getMessage(), line);
            counters.errorCount.incrementAndGet();
            log.error(" Erreur ligne {}: {}", lineNumber, e.getMessage(), e);
        }
    }

    private static class ImportCounters {

        final AtomicInteger totalFileLines = new AtomicInteger(0); // Total lignes du fichier
        final AtomicInteger addedToGroup = new AtomicInteger(0); // Lignes RÉELLEMENT ajoutées au groupe
        // final AtomicInteger duplicateCount = new AtomicInteger(0); // Lignes NON ajoutées (vrais doublons)
        final AtomicInteger errorCount = new AtomicInteger(0); // Lignes avec erreurs
    }

    private static class ImportResults {

        final List<Contact> contactsToInsert = Collections.synchronizedList(new ArrayList<>());
        final List<Contact> duplicateContacts = Collections.synchronizedList(new ArrayList<>());
        final List<Contact> errorContacts = Collections.synchronizedList(new ArrayList<>());
    }

    private static class OutputFiles {

        final BufferedWriter errorWriter;
        final String errorFileLocation;

        OutputFiles(BufferedWriter errorWriter, BufferedWriter unused, String errorFileLocation, String unused2) {
            this.errorWriter = errorWriter;
            this.errorFileLocation = errorFileLocation;
        }
    }

    private OutputFiles initializeOutputFiles() {
        try {
            Path errorFilePath = Files.createTempFile("contacts_error_", ".csv");

            BufferedWriter errorWriter = Files.newBufferedWriter(errorFilePath, StandardCharsets.UTF_8);

            return new OutputFiles(
                errorWriter,
                null, // fileDuplicateWriter
                errorFilePath.getFileName().toString(),
                null // fileDuplicateFileLocation
            );
        } catch (IOException ex) {
            throw new RuntimeException("Erreur lors de la création des fichiers temporaires", ex);
        }
    }

    private DuplicateContactsResponse buildSimplifiedResponse(
        ImportResults results,
        ImportCounters counters,
        OutputFiles outputFiles,
        String progressId
    ) {
        List<Contact> displayUnique = results.contactsToInsert.size() > 10
            ? results.contactsToInsert.subList(0, 10)
            : results.contactsToInsert;

        DuplicateContactsResponse response = new DuplicateContactsResponse(
            displayUnique,
            Collections.emptyList(), //  Plus de doublons
            results.errorContacts,
            counters.totalFileLines.get(),
            counters.addedToGroup.get(),
            0, //  totalDuplicates = 0
            counters.errorCount.get(),
            outputFiles.errorFileLocation,
            null, //  Plus de fichier de doublons
            progressId
        );

        response.setAllContactsToInsert(results.contactsToInsert);

        log.info(
            "📊 Import simplifié - Total: {}, Ajoutés: {}, Erreurs: {}",
            counters.totalFileLines.get(),
            counters.addedToGroup.get(),
            counters.errorCount.get()
        );

        return response;
    }

    private void initializeOutputFileHeaders(OutputFiles outputFiles, String headerLine) throws IOException {
        // Headers pour le fichier d'erreurs
        outputFiles.errorWriter.write("Ligne,Erreur,Données");
        outputFiles.errorWriter.newLine();
    }

    private void closeOutputFiles(OutputFiles outputFiles) {
        try {
            outputFiles.errorWriter.close();
        } catch (IOException e) {
            log.error("Erreur lors de la fermeture des fichiers: {}", e.getMessage());
        }
    }

    /**
     * Réponse vide en cas d'erreur
     */
    private DuplicateContactsResponse createEmptyResponse(ImportCounters counters, OutputFiles outputFiles, String progressId) {
        return new DuplicateContactsResponse(
            new ArrayList<>(), // uniqueContacts
            new ArrayList<>(), // duplicateContacts
            new ArrayList<>(), // errorContacts
            counters.totalFileLines.get(), // totalFileLines
            0, // totalAddedToGroup
            0, // totalDuplicates
            counters.errorCount.get(), // totalErrors
            outputFiles.errorFileLocation, // errorFileLocation
            null, // duplicateFileLocation
            progressId
        );
    }

    /**
     * Représentation d'une ligne de contact du fichier
     */
    private static class ContactLine {

        final String originalLine;
        final int lineNumber;
        final String[] fields;
        final String normalizedPhone;
        final String nom;
        final String prenom;

        ContactLine(String originalLine, int lineNumber, String[] fields, String normalizedPhone, String nom, String prenom) {
            this.originalLine = originalLine;
            this.lineNumber = lineNumber;
            this.fields = fields;
            this.normalizedPhone = normalizedPhone;
            this.nom = nom;
            this.prenom = prenom;
        }
    }

    /**
     * Génère un ID de progression unique
     */
    private String generateUniqueProgressId() {
        String progressId;
        do {
            progressId = UUID.randomUUID().toString();
        } while (contactRepository.existsByProgressId(progressId) || importHistoryRepository.existsByBulkId(progressId));
        return progressId;
    }

    /**
     * Sauvegarde l'historique d'import
     */
    private void saveImportHistory(ImportHistory importHistory, DuplicateContactsResponse result, String progressId, String login) {
        importHistory.setBulkId(progressId);
        importHistory.setImportDate(ZonedDateTime.now());
        importHistory.setStatus("PENDING");
        importHistory.setUser_login(login);
        importHistory.setTotalLines(result.getTotalFileLines());
        importHistory.setInsertedCount(result.getTotalInserted());
        importHistory.setRejectedCount(result.getTotalErrors());
        importHistory.setDuplicateCount(result.getTotalDuplicates()); // Utilise le total des doublons
        importHistoryRepository.save(importHistory);
    }

    /**
     * Gère les erreurs d'import
     */
    private void handleImportError(ImportHistory importHistory, boolean insert, int totalInserted, String progressId, Exception e) {
        if (insert && totalInserted > 0) {
            importHistory.setStatus("FAILED");
            importHistoryRepository.save(importHistory);
            progressTracker.complete(progressId);
        }
        log.error("Error processing file: {}", e.getMessage(), e);
    }

    @GetMapping("/import-history/{bulkId}")
    public ResponseEntity<ImportHistory> getImportHistoryByBulkId(@PathVariable String bulkId) {
        log.debug("REST request to get ImportHistory by bulkId: {}", bulkId);
        Optional<ImportHistory> importHistoryOpt = importHistoryRepository.findByBulkId(bulkId);
        if (importHistoryOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        ImportHistory importHistory = importHistoryOpt.get();
        if (!SecurityUtils.hasCurrentUserAnyOfAuthorities("ROLE_ADMIN")) {
            Optional<String> currentUserLoginOpt = SecurityUtils.getCurrentUserLogin();
            if (currentUserLoginOpt.isEmpty() || !currentUserLoginOpt.get().equals(importHistory.getUser_login())) {
                return ResponseEntity.status(403).build();
            }
        }
        return ResponseEntity.ok(importHistory);
    }

    @GetMapping("/bulk/{bulkId}/export")
    public void exportContactsByBulkId(@PathVariable String bulkId, HttpServletResponse response) throws IOException {
        log.debug("REST request to export contacts as CSV by bulkId: {}", bulkId);
        Optional<ImportHistory> importHistoryOpt = importHistoryRepository.findByBulkId(bulkId);
        if (importHistoryOpt.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        ImportHistory importHistory = importHistoryOpt.get();
        if (!SecurityUtils.hasCurrentUserAnyOfAuthorities("ROLE_ADMIN")) {
            Optional<String> currentUserLoginOpt = SecurityUtils.getCurrentUserLogin();
            if (currentUserLoginOpt.isEmpty() || !currentUserLoginOpt.get().equals(importHistory.getUser_login())) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
        }

        // Set response headers for CSV download
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", String.format("attachment; filename=\"contacts_%s.csv\"", bulkId));
        response.setCharacterEncoding("UTF-8");

        // Write CSV
        try (PrintWriter writer = response.getWriter()) {
            writer.write("id,connom,conprenom,contelephone\n");

            int page = 0;
            int size = 1000;
            while (true) {
                Page<Contact> contactPage = contactRepository.findByProgressId(bulkId, PageRequest.of(page, size));
                if (contactPage.isEmpty()) {
                    break;
                }
                for (Contact contact : contactPage.getContent()) {
                    writer.write(
                        String.format(
                            "%d,\"%s\",\"%s\",\"%s\"\n",
                            contact.getId(),
                            contact.getConnom() != null ? contact.getConnom().replace("\"", "\"\"") : "",
                            contact.getConprenom() != null ? contact.getConprenom().replace("\"", "\"\"") : "",
                            contact.getContelephone() != null ? contact.getContelephone().replace("\"", "\"\"") : ""
                        )
                    );
                }
                page++;
            }
        } catch (Exception e) {
            log.error("Error exporting contacts for bulkId {}: {}", bulkId, e.getMessage(), e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/bulk/{bulkId}")
    public ResponseEntity<Page<Contact>> getContactsByBulkId(
        @PathVariable String bulkId,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        log.debug("REST request to get contacts by bulkId: {}", bulkId);
        Optional<ImportHistory> importHistoryOpt = importHistoryRepository.findByBulkId(bulkId);
        if (importHistoryOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        ImportHistory importHistory = importHistoryOpt.get();
        if (!SecurityUtils.hasCurrentUserAnyOfAuthorities("ROLE_ADMIN")) {
            Optional<String> currentUserLoginOpt = SecurityUtils.getCurrentUserLogin();
            if (currentUserLoginOpt.isEmpty() || !currentUserLoginOpt.get().equals(importHistory.getUser_login())) {
                return ResponseEntity.status(403).build();
            }
        }
        Page<Contact> page = contactRepository.findByProgressId(bulkId, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page);
    }

    @GetMapping("/progress/{progressId}")
    public ResponseEntity<ProgressStatus> getProgress(@PathVariable String progressId) {
        ProgressStatus status = progressTracker.getProgress(progressId);
        return ResponseEntity.ok(status);
    }

    @DeleteMapping("/delete-all")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Void> deleteAllContacts() {
        try {
            contactCleaningService.deleteAllContactsAsync();
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error initiating deletion: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    private ContactProcessingResult processContactLineOptimized(
        String line,
        int lineNumber,
        String[] headers,
        Map<String, Map<String, Object>> allowedCustomFields,
        Map<String, Contact> existingContactsCache,
        Set<String> processedPhones,
        String currentUserLogin,
        String progressId,
        BufferedWriter errorWriter,
        BufferedWriter duplicateWriter
    ) {
        String[] fields = line.split(CSV_DELIMITER);

        if (fields.length < 1) {
            writeError(errorWriter, lineNumber, "Colonnes insuffisantes", line);
            Contact errorContact = createErrorContact(line, null, null, null);
            return new ContactProcessingResult(ContactStatus.ERROR, errorContact);
        }

        String phoneNumber = extractField(fields, 0);
        String nom = extractField(fields, 1);
        String prenom = extractField(fields, 2);

        if (!PhoneNumberHelper.isValidPhoneNumber(phoneNumber)) {
            String errorReason = "Telephone invalide : " + phoneNumber;
            writeError(errorWriter, lineNumber, errorReason, line);
            Contact errorContact = createErrorContact(line, nom, prenom, phoneNumber);
            return new ContactProcessingResult(ContactStatus.ERROR, errorContact);
        }

        String normalizedPhone = PhoneNumberHelper.normalizePhoneNumber(phoneNumber);

        if (!processedPhones.add(normalizedPhone) || existingContactsCache.containsKey(normalizedPhone)) {
            writeDuplicate(duplicateWriter, line);

            Contact duplicateContact = existingContactsCache.get(normalizedPhone);
            if (duplicateContact == null) {
                // Doublon dans le fichier uniquement
                duplicateContact = new Contact();
                duplicateContact.setConnom(nom);
                duplicateContact.setConprenom(prenom);
                duplicateContact.setContelephone(normalizedPhone);
                duplicateContact.setUser_login(currentUserLogin);
            }
            duplicateContact.setStatuttraitement(3);

            return new ContactProcessingResult(ContactStatus.DUPLICATE, duplicateContact);
        }

        Contact contact = new Contact();
        contact.setConnom(nom);
        contact.setConprenom(prenom);
        contact.setContelephone(normalizedPhone);
        contact.setUser_login(currentUserLogin);
        contact.setStatuttraitement(1);
        contact.setProgressId(progressId);

        // Traitement des champs personnalisés
        try {
            Map<String, String> customValues = parseCustomFields(fields, headers, allowedCustomFields);
            if (!customValues.isEmpty()) {
                contact.setCustomFields(new ObjectMapper().writeValueAsString(customValues));
            }
        } catch (IOException e) {
            writeError(errorWriter, lineNumber, "Erreur champs personnalisés : " + e.getMessage(), line);
            return new ContactProcessingResult(ContactStatus.ERROR, contact);
        }

        return new ContactProcessingResult(ContactStatus.SUCCESS, contact);
    }

    private void createGroupAssociation(Contact contact, Groupe groupe) {
        Groupedecontact association = new Groupedecontact();
        association.setContact(contact);
        association.setCgrgroupe(groupe);
        contact.getGroupedecontacts().add(association);
    }

    /**
     * Extrait un champ du tableau en gérant les cas null/vide
     */
    private String extractField(String[] fields, int index) {
        if (index < 0 || index >= fields.length) {
            return null;
        }

        String value = fields[index];

        if (value == null || value.trim().isEmpty() || value.equalsIgnoreCase("null")) {
            return null;
        }

        return value.trim();
    }

    /**
     * Crée un contact d'erreur avec les informations disponibles
     */
    private Contact createErrorContact(String line, String nom, String prenom, String phoneNumber) {
        Contact errorContact = new Contact();
        errorContact.setConnom(nom);
        errorContact.setConprenom(prenom);
        errorContact.setContelephone(phoneNumber);
        errorContact.setCustomFields(line); // Stockage de la ligne pour le reporting d'erreur
        return errorContact;
    }

    /**
     * Initialise les fichiers temporaires
     */
    private FilePathResult initializeTempFiles() {
        try {
            Path errorFilePath = Files.createTempFile("contacts_error_", ".csv");
            Path duplicateFilePath = Files.createTempFile("contacts_duplicate_", ".csv");
            String errorFileLocation = errorFilePath.getFileName().toString();
            String duplicateFileLocation = duplicateFilePath.getFileName().toString();

            return new FilePathResult(errorFilePath, duplicateFilePath, errorFileLocation, duplicateFileLocation);
        } catch (IOException ex) {
            throw new RuntimeException("Erreur lors de la création des fichiers temporaires", ex);
        }
    }

    /**
     * Initialise les headers des fichiers de sortie
     */
    private void initializeFileHeaders(BufferedWriter errorWriter, BufferedWriter duplicateWriter, BufferedReader reader)
        throws IOException {
        errorWriter.write(ERROR_HEADER);
        errorWriter.newLine();
    }

    /**
     * Charge le groupe depuis la base de données
     */
    private Groupe loadGroupe(Long selectedGroupId) {
        return groupeRepository
            .findById(selectedGroupId)
            .orElseThrow(() -> new IllegalArgumentException("Groupe introuvable pour l'ID " + selectedGroupId));
    }

    /**
     * Construit la réponse finale
     */
    private DuplicateContactsResponse buildResponse(
        List<Contact> contactsToInsert,
        List<Contact> duplicateContacts,
        List<Contact> errorContacts,
        int totalLines,
        int successCount,
        int duplicateCount,
        int errorCount,
        String errorFileLocation,
        String duplicateFileLocation,
        String progressId
    ) {
        List<Contact> displayUnique = contactsToInsert.size() > 10 ? contactsToInsert.subList(0, 10) : contactsToInsert;

        DuplicateContactsResponse response = new DuplicateContactsResponse(
            displayUnique,
            duplicateContacts,
            errorContacts,
            totalLines,
            successCount,
            duplicateCount,
            errorCount,
            errorFileLocation,
            duplicateFileLocation,
            progressId
        );

        response.setAllContactsToInsert(contactsToInsert);
        return response;
    }

    private void writeError(BufferedWriter errorWriter, int lineNumber, String errorReason, String line) {
        try {
            synchronized (errorWriter) {
                errorWriter.write(String.format("%d,%s,%s", lineNumber, errorReason, line));
                errorWriter.newLine();
            }
        } catch (IOException e) {
            log.error("Erreur lors de l'écriture dans le fichier d'erreur: {}", e.getMessage(), e);
        }
    }

    private void writeDuplicate(BufferedWriter duplicateWriter, String line) {
        try {
            synchronized (duplicateWriter) {
                duplicateWriter.write(line);
                duplicateWriter.newLine();
            }
        } catch (IOException e) {
            log.error("Erreur lors de l'écriture dans le fichier de doublons: {}", e.getMessage(), e);
        }
    }

    private Map<String, Map<String, Object>> loadCustomFields(String currentUserLogin) {
        Map<String, Map<String, Object>> allowedCustomFields = new HashMap<>();
        userRepository
            .findOneByLogin(currentUserLogin)
            .ifPresent(user -> {
                if (user.getCustomFields() != null && !user.getCustomFields().isEmpty()) {
                    try {
                        ObjectMapper mapper = new ObjectMapper();
                        allowedCustomFields.putAll(mapper.readValue(user.getCustomFields(), new TypeReference<>() {}));
                    } catch (IOException e) {
                        throw new RuntimeException("Erreur de parsing des customFields utilisateur", e);
                    }
                }
            });
        return allowedCustomFields;
    }

    private Map<String, String> parseCustomFields(String[] fields, String[] headers, Map<String, Map<String, Object>> allowedCustomFields)
        throws IOException {
        Map<String, String> customValues = new HashMap<>();
        for (int i = 3; i < Math.min(fields.length, headers.length); i++) {
            String key = headers[i].trim();
            String value = fields[i].trim();
            if (!key.isEmpty() && !value.isEmpty() && allowedCustomFields.containsKey(key)) {
                customValues.put(key, value);
            }
        }
        return customValues;
    }

    private DuplicateContactsResponse emptyResponse(
        int totalFileLines,
        int errorCount,
        String errorFileLocation,
        String duplicateFileLocation,
        String progressId
    ) {
        return new DuplicateContactsResponse(
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            totalFileLines,
            0,
            0,
            errorCount,
            errorFileLocation,
            duplicateFileLocation,
            progressId
        );
    }

    private static class FilePathResult {

        final Path errorFilePath;
        final Path duplicateFilePath;
        final String errorFileLocation;
        final String duplicateFileLocation;

        FilePathResult(Path errorFilePath, Path duplicateFilePath, String errorFileLocation, String duplicateFileLocation) {
            this.errorFilePath = errorFilePath;
            this.duplicateFilePath = duplicateFilePath;
            this.errorFileLocation = errorFileLocation;
            this.duplicateFileLocation = duplicateFileLocation;
        }
    }

    private static class ContactProcessingResult {

        final ContactStatus status;
        final Contact contact;

        ContactProcessingResult(ContactStatus status, Contact contact) {
            this.status = status;
            this.contact = contact;
        }
    }

    private enum ContactStatus {
        SUCCESS, // Contact valide à insérer et ajouter au groupe
        DUPLICATE, // Contact doublon à ajouter au groupe (statut 3)
        ERROR, // Contact avec erreur (ne pas ajouter au groupe)
    }

    @PostMapping(value = "/custom-fields", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> addCustomFields(@RequestBody List<CustomFieldDTO> fields) {
        String login = SecurityUtils.getCurrentUserLogin().orElseThrow(() -> new IllegalStateException("User not authenticated"));

        User user = userRepository.findOneByLogin(login).orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));

        try {
            Map<String, Map<String, Object>> existing = new HashMap<>();
            String raw = user.getCustomFields();
            if (raw != null && !raw.isBlank()) {
                existing = objectMapper.readValue(raw, new TypeReference<Map<String, Map<String, Object>>>() {});
            }

            for (CustomFieldDTO dto : fields) {
                Map<String, Object> meta = new HashMap<>();
                meta.put("label", dto.getLabel());
                meta.put("maxLength", dto.getMaxLength());
                existing.put(dto.getApiName(), meta);
            }

            user.setCustomFields(objectMapper.writeValueAsString(existing));
            userRepository.save(user);
            return ResponseEntity.ok().build();
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/custom-fields")
    public ResponseEntity<Map<String, CustomFieldDTO>> getCustomFields() {
        String login = SecurityUtils.getCurrentUserLogin().orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        User user = userRepository.findOneByLogin(login).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        String raw = user.getCustomFields();
        if (raw == null || raw.isBlank()) {
            return ResponseEntity.ok(Collections.emptyMap());
        }

        try {
            Map<String, Map<String, Object>> temp = new ObjectMapper().readValue(raw, new TypeReference<>() {});

            Map<String, CustomFieldDTO> result = new HashMap<>();
            for (var entry : temp.entrySet()) {
                CustomFieldDTO dto = new CustomFieldDTO();
                dto.setApiName(entry.getKey());
                dto.setLabel((String) entry.getValue().get("label"));
                dto.setMaxLength((Integer) entry.getValue().get("maxLength"));
                result.put(entry.getKey(), dto);
            }
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.emptyMap());
        }
    }

    @GetMapping("/download/error/{fileName}")
    public ResponseEntity<Resource> downloadErrorFile(@PathVariable String fileName) {
        try {
            // Supposons que les fichiers sont dans le répertoire temporaire
            Path filePath = Paths.get(System.getProperty("java.io.tmpdir")).resolve(fileName);
            Resource resource = new FileSystemResource(filePath);
            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/download/duplicate/{fileName}")
    public ResponseEntity<Resource> downloadDuplicateFile(@PathVariable String fileName) {
        try {
            Path filePath = Paths.get(System.getProperty("java.io.tmpdir")).resolve(fileName);
            Resource resource = new FileSystemResource(filePath);
            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/download/database-duplicate/{fileName}")
    public ResponseEntity<Resource> downloadDatabaseDuplicateFile(@PathVariable String fileName) {
        try {
            Path filePath = Paths.get(System.getProperty("java.io.tmpdir")).resolve(fileName);
            Resource resource = new FileSystemResource(filePath);
            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/download/file-duplicate/{fileName}")
    public ResponseEntity<Resource> downloadFileDuplicateFile(@PathVariable String fileName) {
        try {
            Path filePath = Paths.get(System.getProperty("java.io.tmpdir")).resolve(fileName);
            Resource resource = new FileSystemResource(filePath);
            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
