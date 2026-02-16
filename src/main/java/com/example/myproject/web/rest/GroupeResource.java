package com.example.myproject.web.rest;

import com.example.myproject.domain.Contact;
import com.example.myproject.domain.Groupe;
import com.example.myproject.domain.User;
import com.example.myproject.repository.*;
import com.example.myproject.security.SecurityUtils;
import com.example.myproject.service.GroupeContactSearchService;
import com.example.myproject.service.GroupeService;
import com.example.myproject.service.dto.AdvancedFiltersPayload;
import com.example.myproject.service.utils.LikeUtils;
import com.example.myproject.web.rest.errors.BadRequestAlertException;
import jakarta.persistence.EntityNotFoundException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.PaginationUtil;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link com.example.myproject.domain.Groupe}.
 */
@RestController
@RequestMapping("/api/groupes")
@Transactional
public class GroupeResource {

    private final Logger log = LoggerFactory.getLogger(GroupeResource.class);

    private static final String ENTITY_NAME = "groupe";
    private final GroupedecontactRepository groupedecontactRepository;
    private final GroupeService groupeService;
    private final GroupeContactSearchService service;
    private final SendSmsRepository sendSmsRepository;
    private final SmsRepository smsRepository;

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final GroupeRepository groupeRepository;
    private final UserRepository userRepository;
    private final ContactRepository contactRepository;

    public GroupeResource(
        GroupedecontactRepository groupedecontactRepository,
        GroupeService groupeService,
        GroupeContactSearchService service,
        GroupeRepository groupeRepository,
        UserRepository userRepository,
        ContactRepository contactRepository,
        SendSmsRepository sendSmsRepository,
        SmsRepository smsRepository
    ) {
        this.groupedecontactRepository = groupedecontactRepository;
        this.groupeService = groupeService;
        this.service = service;
        this.groupeRepository = groupeRepository;
        this.userRepository = userRepository;
        this.contactRepository = contactRepository;
        this.sendSmsRepository = sendSmsRepository;
        this.smsRepository = smsRepository;
    }

    @PostMapping("/{groupeId}/contacts")
    public ResponseEntity<Void> addContactsToGroup(@PathVariable Long groupeId, @RequestBody List<Long> contactIds) {
        groupeService.addContactsToGroup(groupeId, contactIds);
        return ResponseEntity.noContent().build();
    }

    /**
     * DELETE /groupes/{id}/with-contacts-and-messages :
     * Supprime un groupe, ses contacts, et tous les messages (SMS + SendSms) liés
     */
    @DeleteMapping("/{id}/with-contacts-and-messages")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Transactional
    public ResponseEntity<Map<String, Object>> deleteGroupeWithContactsAndMessages(@PathVariable("id") Long id) {
        log.debug("REST request to delete Groupe, contacts, and messages: {}", id);

        try {
            if (!groupeRepository.existsById(id)) {
                throw new BadRequestAlertException("Groupe not found", ENTITY_NAME, "groupenotfound");
            }

            List<Long> contactIds = contactRepository.findContactIdsByGroupeId(id);
            long contactCount = contactIds.size();
            log.info("Trouvé {} contacts pour le groupe {}", contactCount, id);

            int deletedSms = 0;
            if (!contactIds.isEmpty()) {
                deletedSms = smsRepository.deleteByContactIds(contactIds);
                log.info("Supprimé {} SMS liés aux contacts", deletedSms);
            }

            int deletedSendSms = sendSmsRepository.deleteByGroupeId(id);
            log.info("Supprimé {} SendSms liés au groupe", deletedSendSms);

            int deletedAssociations = groupedecontactRepository.deleteByGroupeId(id);
            log.info("Supprimé {} associations", deletedAssociations);

            int deletedContacts = 0;
            if (!contactIds.isEmpty()) {
                deletedContacts = contactRepository.deleteByIdIn(contactIds);
                log.info("Supprimé {} contacts", deletedContacts);
            }

            groupeRepository.deleteById(id);
            log.info("Groupe {} supprimé", id);

            return ResponseEntity.ok()
                .body(
                    Map.of(
                        "deletedGroupId",
                        id,
                        "deletedContactsCount",
                        contactCount,
                        "deletedSmsCount",
                        deletedSms,
                        "deletedSendSmsCount",
                        deletedSendSms,
                        "message",
                        String.format(
                            "Groupe %d supprimé avec %d contacts, %d SMS et %d SendSms",
                            id,
                            contactCount,
                            deletedSms,
                            deletedSendSms
                        )
                    )
                );
        } catch (Exception e) {
            log.error("Erreur lors de la suppression complète du groupe {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                Map.of("error", "Erreur lors de la suppression : " + e.getMessage())
            );
        }
    }

    /**
     * DELETE /groupes/{groupeId}/contacts : Supprime des contacts d'un groupe
     */
    @DeleteMapping("/{groupeId}/contacts")
    @Transactional
    public ResponseEntity<Map<String, Object>> removeContactsFromGroup(@PathVariable Long groupeId, @RequestBody List<Long> contactIds) {
        log.debug("REST request to remove {} contacts from groupe: {}", contactIds.size(), groupeId);

        try {
            // Vérifier si le groupe existe
            if (!groupeRepository.existsById(groupeId)) {
                throw new BadRequestAlertException("Groupe not found", ENTITY_NAME, "groupenotfound");
            }

            int deletedCount = groupedecontactRepository.deleteByGroupeIdAndContactIdIn(groupeId, contactIds);

            log.info("Supprimé {} associations groupe-contact", deletedCount);

            return ResponseEntity.ok(
                Map.of("deletedCount", deletedCount, "message", deletedCount + " contact(s) retiré(s) du groupe avec succès")
            );
        } catch (Exception e) {
            log.error("Erreur lors de la suppression des contacts du groupe: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                Map.of("error", "Erreur lors de la suppression : " + e.getMessage())
            );
        }
    }

    @PostMapping("/{sourceGroupeId}/contacts/bulk-link")
    @Transactional
    public ResponseEntity<Map<String, Object>> bulkLinkContactsByFilter(
        @PathVariable Long sourceGroupeId,
        @RequestParam Long targetGroupeId,
        @RequestBody AdvancedFiltersPayload f
    ) {
        String search = normalize(f.getSearch());
        String nom = normalize(f.getNom());
        String prenom = normalize(f.getPrenom());
        String telephone = normalize(f.getTelephone());
        String smsStatus = normalize(f.getSmsStatus());
        String deliveryStatus = normalize(f.getDeliveryStatus());
        String lastErrorContains = normalize(f.getLastErrorContains());

        if (search != null) {
            if (nom == null) nom = search;
            if (prenom == null) prenom = search;
            if (telephone == null) telephone = search;
        }

        int inserted = groupedecontactRepository.bulkLinkFilteredContactsAndCampaign(
            sourceGroupeId,
            targetGroupeId,
            nom,
            prenom,
            telephone,
            f.getStatut(),
            f.getHasWhatsapp(),
            f.getMinSmsSent(),
            f.getMaxSmsSent(),
            f.getMinWhatsappSent(),
            f.getMaxWhatsappSent(),
            f.getHasReceivedMessages(),
            f.getCampaignId(),
            smsStatus,
            deliveryStatus,
            lastErrorContains
        );

        return ResponseEntity.ok(Map.of("linked", inserted));
    }

    @GetMapping("/contacts/{id}/groupes")
    public ResponseEntity<List<Groupe>> getGroupesByContact(@PathVariable Long id) {
        List<Groupe> groupes = groupeRepository.findGroupesByContactId(id);
        return ResponseEntity.ok(groupes);
    }

    /**
     * {@code POST  /groupes} : Create a new groupe.
     *
     * @param groupe the groupe to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new groupe, or with status {@code 400 (Bad Request)} if the groupe has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<Groupe> createGroupe(@RequestBody Groupe groupe) throws URISyntaxException {
        log.debug("REST request to save Groupe : {}", groupe);
        if (groupe.getId() != null) {
            throw new BadRequestAlertException("A new groupe cannot already have an ID", ENTITY_NAME, "idexists");
        }

        Optional<String> currentUserLoginOpt = SecurityUtils.getCurrentUserLogin();
        if (currentUserLoginOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String currentUser = currentUserLoginOpt.get();
        String currentUserLogin = determineEffectiveUserLogin(currentUser);
        Optional<User> userOpt = userRepository.findOneByLogin(currentUserLogin);
        if (userOpt.isEmpty()) {
            throw new IllegalStateException("Utilisateur non trouvé pour le login : " + currentUserLogin);
        }

        User user = userOpt.get();

        boolean isUserRole = user.getAuthorities().stream().anyMatch(auth -> auth.getName().equals("ROLE_USER"));
        boolean isAdminRole = user.getAuthorities().stream().anyMatch(auth -> auth.getName().equals("ROLE_ADMIN"));
        boolean isPartner = user.getAuthorities().stream().anyMatch(auth -> auth.getName().equals("ROLE_PARTNER"));
        if (isUserRole && !isAdminRole && !isPartner) {
            // Remplacer currentUserLogin par l'expéditeur
            String expediteur = user.getExpediteur(); // adapte selon ta méthode/attribut
            if (expediteur == null || expediteur.isEmpty()) {
                throw new IllegalStateException("Expéditeur non défini pour l'utilisateur");
            }
            currentUserLogin = expediteur;
        }

        groupe.setUser_id(currentUserLogin);

        groupe = groupeRepository.save(groupe);

        return ResponseEntity.created(new URI("/api/groupes/" + groupe.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, groupe.getId().toString()))
            .body(groupe);
    }

    @GetMapping("/by-contact")
    public ResponseEntity<List<Groupe>> getGroupesByContact(
        @RequestParam("contactId") Long contactId,
        @RequestParam(value = "search", required = false) String search,
        Pageable pageable
    ) {
        // Récupération paginée depuis le repository.
        Page<Groupe> page = groupedecontactRepository.findGroupesByContactIdAndSearch(contactId, search, pageable);

        // Génération des en-têtes de pagination.
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    /**
     * DELETE /groupes/{id}/with-contacts : Supprime un groupe et tous ses contacts liés
     */
    /**
     * DELETE /groupes/{id}/with-contacts : Supprime un groupe et tous ses contacts liés
     */
    @DeleteMapping("/{id}/with-contacts")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Transactional
    public ResponseEntity<Map<String, Object>> deleteGroupeWithContacts(@PathVariable("id") Long id) {
        log.debug("REST request to delete Groupe and its contacts : {}", id);

        try {
            // Vérifier si le groupe existe
            if (!groupeRepository.existsById(id)) {
                throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
            }

            // Récupérer les IDs des contacts avant suppression
            List<Long> contactIds = contactRepository.findContactIdsByGroupeId(id);
            long contactCount = contactIds.size();

            log.info("Trouvé {} contacts à supprimer pour le groupe {}", contactCount, id);

            // 1. Supprimer les associations groupe-contact
            int deletedAssociations = groupedecontactRepository.deleteByGroupeId(id);
            log.info("Supprimé {} associations", deletedAssociations);

            // 2. Supprimer les contacts
            if (!contactIds.isEmpty()) {
                int deletedContacts = contactRepository.deleteByIdIn(contactIds);
                log.info("Supprimé {} contacts", deletedContacts);
            }

            // 3. Supprimer le groupe
            groupeRepository.deleteById(id);
            log.info("Groupe {} supprimé", id);

            return ResponseEntity.ok()
                .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
                .body(
                    Map.of(
                        "deletedGroupId",
                        id,
                        "deletedContactsCount",
                        contactCount,
                        "message",
                        "Groupe et " + contactCount + " contacts supprimés avec succès"
                    )
                );
        } catch (Exception e) {
            log.error("Erreur lors de la suppression du groupe {} et ses contacts: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                Map.of("error", "Erreur lors de la suppression : " + e.getMessage())
            );
        }
    }

    /**
     * GET  /{groupeId} : Récupère la liste paginée des contacts associés à un groupe.
     *
     * @param groupeId l'id du groupe
     * @param search (optionnel) terme de recherche à appliquer sur le nom ou le prénom du contact
     * @param pageable paramètres de pagination (page, size, sort, etc.)
     * @return une ResponseEntity contenant la liste paginée des contacts et les en-têtes de pagination
     */
    @GetMapping("/contact/{groupeId}")
    public ResponseEntity<List<Contact>> getContactsByGroupe(
        @PathVariable Long groupeId,
        @RequestParam(value = "search", required = false) String search,
        @RequestParam(required = false) String nom,
        @RequestParam(required = false) String prenom,
        @RequestParam(required = false) String telephone,
        @RequestParam(required = false) Integer statut,
        @RequestParam(required = false) Boolean hasWhatsapp,
        @RequestParam(required = false) Integer minSmsSent,
        @RequestParam(required = false) Integer maxSmsSent,
        @RequestParam(required = false) Integer minWhatsappSent,
        @RequestParam(required = false) Integer maxWhatsappSent,
        @RequestParam(required = false) Boolean hasReceivedMessages,
        @RequestParam(required = false) Long campaignId,
        @RequestParam(required = false) String smsStatus,
        @RequestParam(required = false) String deliveryStatus,
        @RequestParam(required = false) String lastErrorContains,
        Pageable pageable
    ) {
        log.debug(
            "REST request to get contacts for groupe: {} with filters (campaignId={}, smsStatus={}, deliveryStatus={})",
            groupeId,
            campaignId,
            smsStatus,
            deliveryStatus
        );

        search = normalize(search);
        nom = normalize(nom);
        prenom = normalize(prenom);
        telephone = normalize(telephone);
        smsStatus = normalize(smsStatus);
        deliveryStatus = normalize(deliveryStatus);
        lastErrorContains = normalize(lastErrorContains);

        if (search != null) {
            if (nom == null) nom = search;
            if (prenom == null) prenom = search;
            if (telephone == null) telephone = search;
        }

        boolean hasAdvancedFilters =
            nom != null ||
            prenom != null ||
            telephone != null ||
            statut != null ||
            hasWhatsapp != null ||
            minSmsSent != null ||
            maxSmsSent != null ||
            minWhatsappSent != null ||
            maxWhatsappSent != null ||
            hasReceivedMessages != null;

        boolean hasCampaignFilters = campaignId != null || smsStatus != null || deliveryStatus != null || lastErrorContains != null;

        Page<Contact> page;

        if (hasCampaignFilters) {
            page = groupedecontactRepository.findContactsByGroupeIdWithAdvancedFiltersAndCampaign(
                groupeId,
                nom,
                prenom,
                telephone,
                statut,
                hasWhatsapp,
                minSmsSent,
                maxSmsSent,
                minWhatsappSent,
                maxWhatsappSent,
                hasReceivedMessages,
                campaignId,
                smsStatus,
                deliveryStatus,
                lastErrorContains,
                pageable
            );
        } else if (hasAdvancedFilters) {
            page = groupedecontactRepository.findContactsByGroupeIdWithAdvancedFilters(
                groupeId,
                nom,
                prenom,
                telephone,
                statut,
                hasWhatsapp,
                minSmsSent,
                maxSmsSent,
                minWhatsappSent,
                maxWhatsappSent,
                hasReceivedMessages,
                pageable
            );
        } else {
            page = groupedecontactRepository.findContactsByGroupeIdAndSearchNative(groupeId, search, pageable);
        }

        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    private String normalize(String s) {
        return (s == null || s.trim().isEmpty()) ? null : s.trim();
    }

    @PostMapping("/contact/{groupeId}/search")
    public ResponseEntity<List<Contact>> searchContacts(
        @PathVariable Long groupeId,
        @RequestBody AdvancedFiltersPayload payload,
        Pageable pageable
    ) {
        if (payload.getNomFilterType() != null && payload.getNomFilterType().equalsIgnoreCase("CONTAINS")) {
            payload.setNomFilterType("contains");
        }
        if (payload.getPrenomFilterType() != null && payload.getPrenomFilterType().equalsIgnoreCase("CONTAINS")) {
            payload.setPrenomFilterType("contains");
        }
        if (payload.getTelephoneFilterType() != null && payload.getTelephoneFilterType().equalsIgnoreCase("CONTAINS")) {
            payload.setTelephoneFilterType("contains");
        }
        Page<Contact> page = service.search(groupeId, payload, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /** Campagnes SMS liées au groupe (ID + nom), pour ton popup */
    @GetMapping("/{groupeId}/campaigns/sms")
    public ResponseEntity<List<SendSmsRepository.CampaignSummaryRow>> smsCampaigns(
        @PathVariable Long groupeId,
        @RequestParam(value = "q", required = false) String q,
        Pageable pageable
    ) {
        Page<SendSmsRepository.CampaignSummaryRow> page = sendSmsRepository.findSmsCampaignsByGroupe(
            groupeId,
            (q == null || q.isBlank()) ? null : q.trim(),
            pageable
        );
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @PostMapping("/{groupeId}/verify-whatsapp")
    public ResponseEntity<Map<String, Object>> verifyWhatsAppContacts(@PathVariable Long groupeId) {
        try {
            Map<String, Object> result = groupeService.verifyWhatsAppContactsAndCreateGroup(groupeId);
            return ResponseEntity.ok(result);
        } catch (IllegalStateException e) {
            log.warn("Configuration WhatsApp invalide : {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (EntityNotFoundException e) {
            log.warn("Groupe non trouvé : {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Erreur lors de la vérification WhatsApp", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                Map.of("error", "Erreur lors de la vérification WhatsApp : " + e.getMessage())
            );
        }
    }

    /**
     * {@code PUT  /groupes/:id} : Updates an existing groupe.
     *
     * @param id the id of the groupe to save.
     * @param groupe the groupe to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated groupe,
     * or with status {@code 400 (Bad Request)} if the groupe is not valid,
     * or with status {@code 500 (Internal Server Error)} if the groupe couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Groupe> updateGroupe(@PathVariable(value = "id", required = false) final Long id, @RequestBody Groupe groupe)
        throws URISyntaxException {
        log.debug("REST request to update Groupe : {}, {}", id, groupe);
        if (groupe.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, groupe.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!groupeRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        groupe = groupeRepository.save(groupe);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, groupe.getId().toString()))
            .body(groupe);
    }

    /**
     * {@code PATCH  /groupes/:id} : Partial updates given fields of an existing groupe, field will ignore if it is null
     *
     * @param id the id of the groupe to save.
     * @param groupe the groupe to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated groupe,
     * or with status {@code 400 (Bad Request)} if the groupe is not valid,
     * or with status {@code 404 (Not Found)} if the groupe is not found,
     * or with status {@code 500 (Internal Server Error)} if the groupe couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<Groupe> partialUpdateGroupe(
        @PathVariable(value = "id", required = false) final Long id,
        @RequestBody Groupe groupe
    ) throws URISyntaxException {
        log.debug("REST request to partial update Groupe partially : {}, {}", id, groupe);
        if (groupe.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, groupe.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!groupeRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<Groupe> result = groupeRepository
            .findById(groupe.getId())
            .map(existingGroupe -> {
                if (groupe.getGrotitre() != null) {
                    existingGroupe.setGrotitre(groupe.getGrotitre());
                }
                if (groupe.getUser() != null) {
                    existingGroupe.setUser(groupe.getUser());
                }

                return existingGroupe;
            })
            .map(groupeRepository::save);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, groupe.getId().toString())
        );
    }

    @GetMapping("")
    public ResponseEntity<List<Groupe>> getAllGroupes(
        @RequestParam(value = "search", required = false) String search,
        @RequestParam(value = "groupType", required = false) String groupType,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        log.debug("REST request to get a page of Groupes with search: {} and groupType: {}", search, groupType);

        try {
            String currentUserLogin = getCurrentUserLoginOrThrow();
            String effectiveUserLogin = determineEffectiveUserLogin(currentUserLogin);
            boolean isAdmin = SecurityUtils.hasCurrentUserAnyOfAuthorities("ROLE_ADMIN");

            Page<Groupe> page = getGroupesPage(search, groupType, effectiveUserLogin, isAdmin, pageable);

            HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);

            return ResponseEntity.ok().headers(headers).body(page.getContent());
        } catch (IllegalStateException e) {
            log.warn("Utilisateur non authentifié lors de la récupération des groupes");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des groupes", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Méthodes extraites simples

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

    private Page<Groupe> getGroupesPage(String search, String groupType, String userLogin, boolean isAdmin, Pageable pageable) {
        // Normalisation des paramètres
        String trimmedSearch = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        String trimmedGroupType = (groupType != null && !groupType.trim().isEmpty()) ? groupType.trim() : null;

        if (isAdmin) {
            return getAdminGroupesPage(trimmedSearch, trimmedGroupType, pageable);
        } else {
            return getUserGroupesPage(trimmedSearch, trimmedGroupType, userLogin, pageable);
        }
    }

    private Page<Groupe> getAdminGroupesPage(String search, String groupType, Pageable pageable) {
        log.debug("Admin requesting groupes with search: {} and groupType: {}", search, groupType);

        if (search != null && groupType != null) {
            return groupeRepository.findByGrotitreContainingIgnoreCaseAndGroupType(search, groupType, pageable);
        }

        if (search != null) {
            return groupeRepository.findByGrotitreContainingIgnoreCase(search, pageable);
        }

        if (groupType != null) {
            return groupeRepository.findByGroupType(groupType, pageable);
        }

        return groupeRepository.findAll(pageable);
    }

    private Page<Groupe> getUserGroupesPage(String search, String groupType, String userLogin, Pageable pageable) {
        log.debug("User {} requesting groupes with search: {} and groupType: {}", userLogin, search, groupType);

        return groupeRepository.findBySearchNative(search, userLogin, groupType, pageable);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Groupe> getGroupe(@PathVariable("id") Long id) {
        log.debug("REST request to get Groupe : {}", id);
        Optional<Groupe> groupe = groupeRepository.findById(id);
        return ResponseUtil.wrapOrNotFound(groupe);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGroupe(@PathVariable("id") Long id) {
        log.debug("REST request to delete Groupe : {}", id);
        groupeRepository.deleteById(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }

    @GetMapping("/contact/{groupeId}/advanced")
    public ResponseEntity<List<Contact>> getContactsWithAdvancedFilters(
        @PathVariable Long groupeId,
        @RequestParam(required = false) String nom,
        @RequestParam(required = false) String prenom,
        @RequestParam(required = false) String telephone,
        @RequestParam(required = false) Integer statut,
        @RequestParam(required = false) Boolean hasWhatsapp,
        @RequestParam(required = false) Integer minSmsSent,
        @RequestParam(required = false) Integer maxSmsSent,
        @RequestParam(required = false) Integer minWhatsappSent,
        @RequestParam(required = false) Integer maxWhatsappSent,
        @RequestParam(required = false) Boolean hasReceivedMessages,
        Pageable pageable
    ) {
        log.debug("REST request to get contacts with advanced filters for groupe: {}", groupeId);

        Page<Contact> page = groupedecontactRepository.findContactsByGroupeIdWithAdvancedFilters(
            groupeId,
            nom,
            prenom,
            telephone,
            statut,
            hasWhatsapp,
            minSmsSent,
            maxSmsSent,
            minWhatsappSent,
            maxWhatsappSent,
            hasReceivedMessages,
            pageable
        );

        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);

        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @GetMapping("/{groupeId}/message-statistics")
    public ResponseEntity<Map<String, Object>> getMessageStatistics(@PathVariable Long groupeId) {
        log.debug("REST request to get message statistics for groupe: {}", groupeId);

        List<Object[]> stats = contactRepository.getMessageStatisticsByGroupeId(groupeId);

        if (stats.isEmpty()) {
            return ResponseEntity.ok(
                Map.of(
                    "totalContacts",
                    0,
                    "contactsWithWhatsapp",
                    0,
                    "totalSmsSent",
                    0,
                    "totalWhatsappSent",
                    0,
                    "totalSmsSuccess",
                    0,
                    "totalSmsFailed",
                    0,
                    "totalWhatsappSuccess",
                    0,
                    "totalWhatsappFailed",
                    0,
                    "averageMessagesPerContact",
                    0.0
                )
            );
        }

        Object[] result = stats.get(0);
        Long totalContacts = ((Number) result[0]).longValue();
        Long contactsWithWhatsapp = ((Number) result[1]).longValue();
        Long totalSmsSent = ((Number) result[2]).longValue();
        Long totalWhatsappSent = ((Number) result[3]).longValue();
        Long totalSmsSuccess = ((Number) result[4]).longValue();
        Long totalSmsFailed = ((Number) result[5]).longValue();
        Long totalWhatsappSuccess = ((Number) result[6]).longValue();
        Long totalWhatsappFailed = ((Number) result[7]).longValue();

        double averageMessages = totalContacts > 0 ? (double) (totalSmsSent + totalWhatsappSent) / totalContacts : 0.0;

        Map<String, Object> statistics = Map.of(
            "totalContacts",
            totalContacts,
            "contactsWithWhatsapp",
            contactsWithWhatsapp,
            "totalSmsSent",
            totalSmsSent,
            "totalWhatsappSent",
            totalWhatsappSent,
            "totalSmsSuccess",
            totalSmsSuccess,
            "totalSmsFailed",
            totalSmsFailed,
            "totalWhatsappSuccess",
            totalWhatsappSuccess,
            "totalWhatsappFailed",
            totalWhatsappFailed,
            "averageMessagesPerContact",
            Math.round(averageMessages * 100.0) / 100.0
        );

        return ResponseEntity.ok(statistics);
    }
}
