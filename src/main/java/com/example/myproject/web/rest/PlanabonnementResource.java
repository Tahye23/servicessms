package com.example.myproject.web.rest;

import com.example.myproject.domain.PlanAbonnement;
import com.example.myproject.repository.PlanabonnementRepository;
import com.example.myproject.service.dto.PlanabonnementDTO;
import com.example.myproject.web.rest.errors.BadRequestAlertException;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.PaginationUtil;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link com.example.myproject.domain.PlanAbonnement}.
 */
@RestController
@RequestMapping("/api/planabonnements")
@Transactional
public class PlanabonnementResource {

    private final Logger log = LoggerFactory.getLogger(PlanabonnementResource.class);

    private static final String ENTITY_NAME = "planAbonnement";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final PlanabonnementRepository planAbonnementRepository;

    public PlanabonnementResource(PlanabonnementRepository planAbonnementRepository) {
        this.planAbonnementRepository = planAbonnementRepository;
    }

    /**
     * {@code POST  /planabonnements} : Create a new plan abonnement (ADMIN only).
     *
     * @param planAbonnementDTO the plan abonnement to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new plan abonnement.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<PlanabonnementDTO> createPlanAbonnement(@Valid @RequestBody PlanabonnementDTO planAbonnementDTO)
        throws URISyntaxException {
        log.debug("REST request to save PlanAbonnement : {}", planAbonnementDTO);

        if (planAbonnementDTO.getId() != null) {
            throw new BadRequestAlertException("A new plan abonnement cannot already have an ID", ENTITY_NAME, "idexists");
        }

        // Validation métier selon le type de plan
        validatePlanBusinessRules(planAbonnementDTO);

        // Conversion et sauvegarde
        PlanAbonnement planAbonnement = convertFromDTO(planAbonnementDTO);
        planAbonnement = planAbonnementRepository.save(planAbonnement);
        PlanabonnementDTO result = convertToDTO(planAbonnement);

        return ResponseEntity.created(new URI("/api/planabonnements/" + planAbonnement.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, planAbonnement.getId().toString()))
            .body(result);
    }

    /**
     * {@code PUT  /planabonnements/:id} : Updates an existing plan abonnement (ADMIN only).
     *
     * @param id the id of the plan abonnement to save.
     * @param planAbonnementDTO the plan abonnement to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated plan abonnement.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<PlanabonnementDTO> updatePlanAbonnement(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody PlanabonnementDTO planAbonnementDTO
    ) throws URISyntaxException {
        log.debug("REST request to update PlanAbonnement : {}, {}", id, planAbonnementDTO);

        if (planAbonnementDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, planAbonnementDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!planAbonnementRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        // Validation métier selon le type de plan
        validatePlanBusinessRules(planAbonnementDTO);

        PlanAbonnement planAbonnement = convertFromDTO(planAbonnementDTO);
        planAbonnement.setId(id);
        planAbonnement = planAbonnementRepository.save(planAbonnement);
        PlanabonnementDTO result = convertToDTO(planAbonnement);

        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, planAbonnement.getId().toString()))
            .body(result);
    }

    /**
     * {@code PATCH  /planabonnements/:id} : Partial updates given fields of an existing plan abonnement (ADMIN only).
     *
     * @param id the id of the plan abonnement to save.
     * @param planAbonnementDTO the plan abonnement to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated plan abonnement.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<PlanabonnementDTO> partialUpdatePlanAbonnement(
        @PathVariable(value = "id", required = false) final Long id,
        @RequestBody PlanabonnementDTO planAbonnementDTO
    ) throws URISyntaxException {
        log.debug("REST request to partial update PlanAbonnement partially : {}, {}", id, planAbonnementDTO);

        if (planAbonnementDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, planAbonnementDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!planAbonnementRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<PlanAbonnement> result = planAbonnementRepository
            .findById(planAbonnementDTO.getId())
            .map(existingPlan -> {
                // Mise à jour des champs de base
                if (planAbonnementDTO.getAbpName() != null) {
                    existingPlan.setAbpName(planAbonnementDTO.getAbpName());
                }
                if (planAbonnementDTO.getAbpDescription() != null) {
                    existingPlan.setAbpDescription(planAbonnementDTO.getAbpDescription());
                }
                if (planAbonnementDTO.getAbpPrice() != null) {
                    existingPlan.setAbpPrice(planAbonnementDTO.getAbpPrice());
                }
                if (planAbonnementDTO.getAbpCurrency() != null) {
                    existingPlan.setAbpCurrency(planAbonnementDTO.getAbpCurrency());
                }
                if (planAbonnementDTO.getAbpPeriod() != null) {
                    existingPlan.setAbpPeriod(planAbonnementDTO.getAbpPeriod());
                }
                if (planAbonnementDTO.getAbpFeatures() != null) {
                    existingPlan.setAbpFeatures(planAbonnementDTO.getAbpFeatures());
                }
                if (planAbonnementDTO.getAbpButtonText() != null) {
                    existingPlan.setAbpButtonText(planAbonnementDTO.getAbpButtonText());
                }
                if (planAbonnementDTO.getButtonClass() != null) {
                    existingPlan.setButtonClass(planAbonnementDTO.getButtonClass());
                }
                if (planAbonnementDTO.getAbpPopular() != null) {
                    existingPlan.setAbpPopular(planAbonnementDTO.getAbpPopular());
                }

                // Mise à jour du type de plan
                if (planAbonnementDTO.getPlanType() != null) {
                    existingPlan.setPlanType(PlanAbonnement.PlanType.valueOf(planAbonnementDTO.getPlanType()));
                }

                // Mise à jour des limites
                if (planAbonnementDTO.getSmsLimit() != null) {
                    existingPlan.setSmsLimit(planAbonnementDTO.getSmsLimit());
                }
                if (planAbonnementDTO.getWhatsappLimit() != null) {
                    existingPlan.setWhatsappLimit(planAbonnementDTO.getWhatsappLimit());
                }
                if (planAbonnementDTO.getUsersLimit() != null) {
                    existingPlan.setUsersLimit(planAbonnementDTO.getUsersLimit());
                }
                if (planAbonnementDTO.getTemplatesLimit() != null) {
                    existingPlan.setTemplatesLimit(planAbonnementDTO.getTemplatesLimit());
                }

                // Mise à jour des permissions
                if (planAbonnementDTO.getCanManageUsers() != null) {
                    existingPlan.setCanManageUsers(planAbonnementDTO.getCanManageUsers());
                }
                if (planAbonnementDTO.getCanManageTemplates() != null) {
                    existingPlan.setCanManageTemplates(planAbonnementDTO.getCanManageTemplates());
                }
                if (planAbonnementDTO.getCanViewConversations() != null) {
                    existingPlan.setCanViewConversations(planAbonnementDTO.getCanViewConversations());
                }
                if (planAbonnementDTO.getCanViewAnalytics() != null) {
                    existingPlan.setCanViewAnalytics(planAbonnementDTO.getCanViewAnalytics());
                }
                if (planAbonnementDTO.getPrioritySupport() != null) {
                    existingPlan.setPrioritySupport(planAbonnementDTO.getPrioritySupport());
                }

                // Mise à jour des limites techniques
                if (planAbonnementDTO.getMaxApiCallsPerDay() != null) {
                    existingPlan.setMaxApiCallsPerDay(planAbonnementDTO.getMaxApiCallsPerDay());
                }
                if (planAbonnementDTO.getStorageLimitMb() != null) {
                    existingPlan.setStorageLimitMb(planAbonnementDTO.getStorageLimitMb());
                }
                if (planAbonnementDTO.getSortOrder() != null) {
                    existingPlan.setSortOrder(planAbonnementDTO.getSortOrder());
                }

                // Mise à jour du statut
                if (planAbonnementDTO.getActive() != null) {
                    existingPlan.setActive(planAbonnementDTO.getActive());
                }

                return existingPlan;
            })
            .map(planAbonnementRepository::save);

        return ResponseUtil.wrapOrNotFound(
            result.map(this::convertToDTO),
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, planAbonnementDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /planabonnements} : get all the active plan abonnements (PUBLIC).
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of active plan abonnements in body.
     */
    @GetMapping("")
    public ResponseEntity<List<PlanabonnementDTO>> getAllActivePlanAbonnements() {
        log.debug("REST request to get all active PlanAbonnements");

        List<PlanAbonnement> plans = planAbonnementRepository.findByActiveTrueOrderBySortOrderAsc();
        List<PlanabonnementDTO> planDTOs = plans.stream().map(this::convertToDTO).collect(Collectors.toList());

        return ResponseEntity.ok(planDTOs);
    }

    /**
     * {@code GET  /planabonnements/admin} : get all plan abonnements for admin (ADMIN only).
     *
     * @param pageable the pagination information.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of plan abonnements in body.
     */
    @GetMapping("/admin")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<PlanabonnementDTO>> getAllPlanAbonnementsForAdmin(
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        log.debug("REST request to get a page of PlanAbonnements for admin");

        Page<PlanAbonnement> page = planAbonnementRepository.findAll(pageable);
        Page<PlanabonnementDTO> dtoPage = page.map(this::convertToDTO);

        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(dtoPage.getContent());
    }

    /**
     * {@code GET  /planabonnements/all} : get all plan abonnements without pagination (ADMIN only).
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of all plan abonnements in body.
     */
    @GetMapping("/all")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<PlanabonnementDTO>> getAllPlanabonnements() {
        log.debug("REST request to get all PlanAbonnements");

        List<PlanAbonnement> plans = planAbonnementRepository.findAll();
        List<PlanabonnementDTO> planDTOs = plans.stream().map(this::convertToDTO).collect(Collectors.toList());

        return ResponseEntity.ok(planDTOs);
    }

    /**
     * {@code GET  /planabonnements/:id} : get the "id" plan abonnement.
     *
     * @param id the id of the plan abonnement to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the plan abonnement, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<PlanabonnementDTO> getPlanAbonnement(@PathVariable("id") Long id) {
        log.debug("REST request to get PlanAbonnement : {}", id);

        Optional<PlanAbonnement> planAbonnement = planAbonnementRepository.findById(id);
        return ResponseUtil.wrapOrNotFound(planAbonnement.map(this::convertToDTO));
    }

    /**
     * {@code GET  /planabonnements/type/:type} : get plan abonnements by type.
     *
     * @param type the type of plan abonnements to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the list of plan abonnements.
     */
    @GetMapping("/type/{type}")
    public ResponseEntity<List<PlanabonnementDTO>> getPlanAbonnementsByType(@PathVariable String type) {
        log.debug("REST request to get PlanAbonnements by type: {}", type);

        try {
            PlanAbonnement.PlanType planType = PlanAbonnement.PlanType.valueOf(type.toUpperCase());
            List<PlanAbonnement> plans = planAbonnementRepository.findByPlanTypeAndActiveTrueOrderBySortOrderAsc(planType);
            List<PlanabonnementDTO> planDTOs = plans.stream().map(this::convertToDTO).collect(Collectors.toList());

            return ResponseEntity.ok(planDTOs);
        } catch (IllegalArgumentException e) {
            log.error("Invalid plan type: {}", type);
            throw new BadRequestAlertException("Invalid plan type", ENTITY_NAME, "invalidplantype");
        }
    }

    /**
     * {@code GET  /planabonnements/popular} : get popular plan abonnements.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the list of popular plan abonnements.
     */
    @GetMapping("/popular")
    public ResponseEntity<List<PlanabonnementDTO>> getPopularPlanAbonnements() {
        log.debug("REST request to get popular PlanAbonnements");

        List<PlanAbonnement> plans = planAbonnementRepository.findByAbpPopularTrueAndActiveTrueOrderBySortOrderAsc();
        List<PlanabonnementDTO> planDTOs = plans.stream().map(this::convertToDTO).collect(Collectors.toList());

        return ResponseEntity.ok(planDTOs);
    }

    /**
     * {@code GET  /planabonnements/free} : get free plans.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the list of free plan abonnements.
     */
    @GetMapping("/free")
    public ResponseEntity<List<PlanabonnementDTO>> getFreePlanAbonnements() {
        log.debug("REST request to get free PlanAbonnements");

        List<PlanAbonnement> plans = planAbonnementRepository.findByPlanTypeAndActiveTrueOrderBySortOrderAsc(PlanAbonnement.PlanType.FREE);
        List<PlanabonnementDTO> planDTOs = plans.stream().map(this::convertToDTO).collect(Collectors.toList());

        return ResponseEntity.ok(planDTOs);
    }

    /**
     * {@code DELETE  /planabonnements/:id} : delete the "id" plan abonnement (ADMIN only).
     *
     * @param id the id of the plan abonnement to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> deletePlanAbonnement(@PathVariable("id") Long id) {
        log.debug("REST request to delete PlanAbonnement : {}", id);

        Optional<PlanAbonnement> planOpt = planAbonnementRepository.findById(id);
        if (planOpt.isEmpty()) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        // Plutôt que de supprimer complètement, désactiver le plan pour préserver l'intégrité des données
        PlanAbonnement plan = planOpt.get();
        plan.setActive(false);
        planAbonnementRepository.save(plan);

        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }

    /**
     * {@code PUT  /planabonnements/:id/toggle-active} : toggle active status of a plan abonnement (ADMIN only).
     *
     * @param id the id of the plan abonnement to toggle.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)}.
     */
    @PutMapping("/{id}/toggle-active")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<PlanabonnementDTO> toggleActivePlanAbonnement(@PathVariable("id") Long id) {
        log.debug("REST request to toggle active status of PlanAbonnement : {}", id);

        Optional<PlanAbonnement> planOpt = planAbonnementRepository.findById(id);
        if (planOpt.isEmpty()) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        PlanAbonnement plan = planOpt.get();
        plan.setActive(!plan.getActive());
        plan = planAbonnementRepository.save(plan);

        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, plan.getId().toString()))
            .body(convertToDTO(plan));
    }

    /**
     * {@code POST  /planabonnements/:id/duplicate} : duplicate a plan abonnement (ADMIN only).
     *
     * @param id the id of the plan abonnement to duplicate.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new duplicated plan abonnement.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("/{id}/duplicate")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<PlanabonnementDTO> duplicatePlanAbonnement(@PathVariable("id") Long id) throws URISyntaxException {
        log.debug("REST request to duplicate PlanAbonnement : {}", id);

        Optional<PlanAbonnement> originalPlanOpt = planAbonnementRepository.findById(id);
        if (originalPlanOpt.isEmpty()) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        PlanAbonnement originalPlan = originalPlanOpt.get();
        PlanAbonnement duplicatedPlan = new PlanAbonnement();

        // Copier tous les champs sauf l'ID et les dates
        duplicatedPlan.setAbpName(originalPlan.getAbpName() + " (Copie)");
        duplicatedPlan.setAbpDescription(originalPlan.getAbpDescription());
        duplicatedPlan.setAbpPrice(originalPlan.getAbpPrice());
        duplicatedPlan.setAbpCurrency(originalPlan.getAbpCurrency());
        duplicatedPlan.setAbpPeriod(originalPlan.getAbpPeriod());
        duplicatedPlan.setAbpFeatures(originalPlan.getAbpFeatures());
        duplicatedPlan.setAbpButtonText(originalPlan.getAbpButtonText());
        duplicatedPlan.setButtonClass(originalPlan.getButtonClass());
        duplicatedPlan.setAbpPopular(false); // Désactiver le statut populaire pour la copie
        duplicatedPlan.setActive(false); // Désactiver par défaut
        duplicatedPlan.setPlanType(originalPlan.getPlanType());
        duplicatedPlan.setSmsLimit(originalPlan.getSmsLimit());
        duplicatedPlan.setWhatsappLimit(originalPlan.getWhatsappLimit());
        duplicatedPlan.setUsersLimit(originalPlan.getUsersLimit());
        duplicatedPlan.setTemplatesLimit(originalPlan.getTemplatesLimit());
        duplicatedPlan.setCanManageUsers(originalPlan.getCanManageUsers());
        duplicatedPlan.setCanManageTemplates(originalPlan.getCanManageTemplates());
        duplicatedPlan.setCanViewConversations(originalPlan.getCanViewConversations());
        duplicatedPlan.setCanViewAnalytics(originalPlan.getCanViewAnalytics());
        duplicatedPlan.setPrioritySupport(originalPlan.getPrioritySupport());
        duplicatedPlan.setMaxApiCallsPerDay(originalPlan.getMaxApiCallsPerDay());
        duplicatedPlan.setStorageLimitMb(originalPlan.getStorageLimitMb());
        duplicatedPlan.setSortOrder(originalPlan.getSortOrder() + 1);

        duplicatedPlan = planAbonnementRepository.save(duplicatedPlan);
        PlanabonnementDTO result = convertToDTO(duplicatedPlan);

        return ResponseEntity.created(new URI("/api/planabonnements/" + duplicatedPlan.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, duplicatedPlan.getId().toString()))
            .body(result);
    }

    // ===== MÉTHODES DE VALIDATION =====
    private void validatePlanBusinessRules(PlanabonnementDTO dto) {
        // Validation selon le type de plan
        if ("FREE".equals(dto.getPlanType())) {
            // Plan gratuit : prix doit être 0 ou null
            if (dto.getAbpPrice() != null && dto.getAbpPrice().compareTo(BigDecimal.ZERO) > 0) {
                throw new BadRequestAlertException("Un plan gratuit ne peut pas avoir un prix", ENTITY_NAME, "freeprice");
            }
        } else {
            // Plans payants : prix et période obligatoires
            if (dto.getAbpPrice() == null || dto.getAbpPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestAlertException("Les plans payants doivent avoir un prix positif", ENTITY_NAME, "paidprice");
            }
            if (dto.getAbpPeriod() == null || dto.getAbpPeriod().trim().isEmpty()) {
                throw new BadRequestAlertException("Les plans payants doivent avoir une période", ENTITY_NAME, "paidperiod");
            }
        }

        // Validation des limites selon le type
        if ("SMS".equals(dto.getPlanType())) {
            if (dto.getSmsLimit() == null || (dto.getSmsLimit() <= 0 && dto.getSmsLimit() != -1)) {
                throw new BadRequestAlertException(
                    "Un plan SMS doit avoir une limite SMS définie (positive ou -1 pour illimité)",
                    ENTITY_NAME,
                    "smslimit"
                );
            }
        }

        if ("WHATSAPP".equals(dto.getPlanType())) {
            if (dto.getWhatsappLimit() == null || (dto.getWhatsappLimit() <= 0 && dto.getWhatsappLimit() != -1)) {
                throw new BadRequestAlertException(
                    "Un plan WhatsApp doit avoir une limite WhatsApp définie (positive ou -1 pour illimité)",
                    ENTITY_NAME,
                    "whatsapplimit"
                );
            }
        }

        // Validation des utilisateurs
        if (dto.getUsersLimit() != null && dto.getUsersLimit() < 1 && dto.getUsersLimit() != -1) {
            throw new BadRequestAlertException(
                "La limite d'utilisateurs doit être au moins 1 ou -1 pour illimité",
                ENTITY_NAME,
                "userslimit"
            );
        }
    }

    // ===== MÉTHODES DE CONVERSION =====
    private PlanAbonnement convertFromDTO(PlanabonnementDTO dto) {
        PlanAbonnement entity = new PlanAbonnement();

        entity.setId(dto.getId());
        entity.setAbpName(dto.getAbpName());
        entity.setAbpDescription(dto.getAbpDescription());
        entity.setAbpPrice(dto.getAbpPrice());
        entity.setAbpCurrency(dto.getAbpCurrency());
        entity.setCustomPlan(dto.getCustomPlan());
        entity.setAbpPeriod(dto.getAbpPeriod());
        entity.setAbpFeatures(dto.getAbpFeatures());
        entity.setAbpButtonText(dto.getAbpButtonText());
        entity.setButtonClass(dto.getButtonClass());
        entity.setAbpPopular(dto.getAbpPopular());
        entity.setActive(dto.getActive());
        entity.setCanManageAPI(dto.getCanManageAPI());
        entity.setCanViewDashboard(dto.getCanViewDashboard());
        // Conversion du type de plan
        if (dto.getPlanType() != null) {
            entity.setPlanType(PlanAbonnement.PlanType.valueOf(dto.getPlanType()));
        }

        entity.setSmsLimit(dto.getSmsLimit());
        entity.setWhatsappLimit(dto.getWhatsappLimit());
        entity.setUsersLimit(dto.getUsersLimit());
        entity.setTemplatesLimit(dto.getTemplatesLimit());
        entity.setCanManageUsers(dto.getCanManageUsers());
        entity.setCanManageTemplates(dto.getCanManageTemplates());
        entity.setCanViewConversations(dto.getCanViewConversations());
        entity.setCanViewAnalytics(dto.getCanViewAnalytics());
        entity.setPrioritySupport(dto.getPrioritySupport());
        entity.setMaxApiCallsPerDay(dto.getMaxApiCallsPerDay());
        entity.setStorageLimitMb(dto.getStorageLimitMb());
        entity.setSortOrder(dto.getSortOrder());

        return entity;
    }

    private PlanabonnementDTO convertToDTO(PlanAbonnement entity) {
        PlanabonnementDTO dto = new PlanabonnementDTO();

        dto.setId(entity.getId());
        dto.setCustomPlan(entity.getCustomPlan());
        dto.setAbpName(entity.getAbpName());
        dto.setAbpDescription(entity.getAbpDescription());
        dto.setAbpPrice(entity.getAbpPrice());
        dto.setAbpCurrency(entity.getAbpCurrency());
        dto.setAbpPeriod(entity.getAbpPeriod());
        dto.setAbpFeatures(entity.getAbpFeatures());
        dto.setAbpButtonText(entity.getAbpButtonText());
        dto.setButtonClass(entity.getButtonClass());
        dto.setAbpPopular(entity.getAbpPopular());
        dto.setCreatedDate(entity.getCreatedDate());
        dto.setUpdatedDate(entity.getUpdatedDate());
        dto.setActive(entity.getActive());
        dto.setCanViewDashboard(entity.getCanViewDashboard());
        dto.setCanManageAPI(entity.getCanManageAPI());
        // Conversion du type de plan
        if (entity.getPlanType() != null) {
            dto.setPlanType(entity.getPlanType().name());
        }

        dto.setSmsLimit(entity.getSmsLimit());
        dto.setWhatsappLimit(entity.getWhatsappLimit());
        dto.setUsersLimit(entity.getUsersLimit());
        dto.setTemplatesLimit(entity.getTemplatesLimit());
        dto.setCanManageUsers(entity.getCanManageUsers());
        dto.setCanManageTemplates(entity.getCanManageTemplates());
        dto.setCanViewConversations(entity.getCanViewConversations());
        dto.setCanViewAnalytics(entity.getCanViewAnalytics());
        dto.setPrioritySupport(entity.getPrioritySupport());
        dto.setMaxApiCallsPerDay(entity.getMaxApiCallsPerDay());
        dto.setStorageLimitMb(entity.getStorageLimitMb());
        dto.setSortOrder(entity.getSortOrder());

        return dto;
    }
}
