package com.example.myproject.web.rest;

import com.example.myproject.domain.Abonnement;
import com.example.myproject.domain.ExtendedUser;
import com.example.myproject.domain.PlanAbonnement;
import com.example.myproject.domain.User;
import com.example.myproject.repository.AbonnementRepository;
import com.example.myproject.repository.ExtendedUserRepository;
import com.example.myproject.repository.PlanabonnementRepository;
import com.example.myproject.repository.UserRepository;
import com.example.myproject.security.SecurityUtils;
import com.example.myproject.service.AbonnementService;
import com.example.myproject.service.dto.AbonnementDTO;
import com.example.myproject.web.rest.errors.BadRequestAlertException;
import jakarta.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.util.List;
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
 * REST controller for managing {@link com.example.myproject.domain.Abonnement}.
 * Gestion des abonnements utilisateur avec quotas et usage en temps réel.
 */
@RestController
@RequestMapping("/api/abonnements")
@Transactional
public class AbonnementResource {

    private final Logger log = LoggerFactory.getLogger(AbonnementResource.class);

    private static final String ENTITY_NAME = "abonnement";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final ExtendedUserRepository extendedUserRepository;
    private final AbonnementRepository abonnementRepository;
    private final AbonnementService abonnementService;
    private final UserRepository userRepository;
    private final PlanabonnementRepository planAbonnementRepository;

    public AbonnementResource(
        ExtendedUserRepository extendedUserRepository,
        AbonnementRepository abonnementRepository,
        AbonnementService abonnementService,
        UserRepository userRepository,
        PlanabonnementRepository planAbonnementRepository
    ) {
        this.extendedUserRepository = extendedUserRepository;
        this.abonnementRepository = abonnementRepository;
        this.abonnementService = abonnementService;
        this.userRepository = userRepository;
        this.planAbonnementRepository = planAbonnementRepository;
    }

    /**
     * {@code POST  /abonnements} : Create a new abonnement.
     *
     * @param abonnementDTO the abonnement to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new abonnement.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_USER')")
    public ResponseEntity<AbonnementDTO> createAbonnement(@Valid @RequestBody AbonnementDTO abonnementDTO) throws URISyntaxException {
        log.debug("REST request to save Abonnement : {}", abonnementDTO);

        if (abonnementDTO.getId() != null) {
            throw new BadRequestAlertException("A new abonnement cannot already have an ID", ENTITY_NAME, "idexists");
        }

        // Vérifications business
        if (abonnementDTO.getUserId() == null) {
            throw new BadRequestAlertException("User ID is required", ENTITY_NAME, "useridnull");
        }

        if (abonnementDTO.getPlanId() != null) {
            PlanAbonnement plan = planAbonnementRepository
                .findById(abonnementDTO.getPlanId())
                .orElseThrow(() -> new BadRequestAlertException("Plan not found", ENTITY_NAME, "plannotfound"));
        }

        // Vérifier que l'utilisateur existe
        ExtendedUser extendeduser = extendedUserRepository
            .findById(abonnementDTO.getUserId())
            .orElseThrow(() -> new BadRequestAlertException("User not found", ENTITY_NAME, "usernotfound"));

        // Vérifier qu'il n'y a pas déjà un abonnement actif pour cet utilisateur
        /* Optional<Abonnement> existingActiveSubscription = abonnementRepository
            .findActivedByUserId(abonnementDTO.getUserId());

        if (existingActiveSubscription.isPresent()) {
            throw new BadRequestAlertException(
                "User already has an active subscription",
                ENTITY_NAME,
                "activesubscriptionexists"
            );
        }*/

        AbonnementDTO result = abonnementService.save(abonnementDTO);

        return ResponseEntity.created(new URI("/api/abonnements/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * {@code PUT  /abonnements/:id} : Updates an existing abonnement.
     *
     * @param id the id of the abonnement to save.
     * @param abonnementDTO the abonnement to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated abonnement.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<AbonnementDTO> updateAbonnement(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody AbonnementDTO abonnementDTO
    ) throws URISyntaxException {
        log.debug("REST request to update Abonnement : {}, {}", id, abonnementDTO);

        if (abonnementDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, abonnementDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!abonnementRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        AbonnementDTO result = abonnementService.update(abonnementDTO);

        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, abonnementDTO.getId().toString()))
            .body(result);
    }

    /**
     * {@code PATCH  /abonnements/:id} : Partial updates given fields of an existing abonnement.
     *
     * @param id the id of the abonnement to save.
     * @param abonnementDTO the abonnement to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated abonnement.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<AbonnementDTO> partialUpdateAbonnement(
        @PathVariable(value = "id", required = false) final Long id,
        @RequestBody AbonnementDTO abonnementDTO
    ) throws URISyntaxException {
        log.debug("REST request to partial update Abonnement partially : {}, {}", id, abonnementDTO);

        if (abonnementDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, abonnementDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!abonnementRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<AbonnementDTO> result = abonnementService.partialUpdate(abonnementDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, abonnementDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /abonnements} : get all the abonnements.
     *
     * @param pageable the pagination information.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of abonnements in body.
     */
    @GetMapping("")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<AbonnementDTO>> getAllAbonnements(@org.springdoc.core.annotations.ParameterObject Pageable pageable) {
        log.debug("REST request to get a page of Abonnements");
        Page<AbonnementDTO> page = abonnementService.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /abonnements/:id} : get the "id" abonnement.
     *
     * @param id the id of the abonnement to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the abonnement.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or @abonnementService.isOwner(#id, authentication.name)")
    public ResponseEntity<AbonnementDTO> getAbonnement(@PathVariable("id") Long id) {
        log.debug("REST request to get Abonnement : {}", id);
        Optional<AbonnementDTO> abonnementDTO = abonnementService.findOne(id);
        return ResponseUtil.wrapOrNotFound(abonnementDTO);
    }

    /**
     * {@code DELETE  /abonnements/:id} : delete the "id" abonnement.
     *
     * @param id the id of the abonnement to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> deleteAbonnement(@PathVariable("id") Long id) {
        log.debug("REST request to delete Abonnement : {}", id);
        abonnementService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }

    // ===== ENDPOINTS SPÉCIFIQUES AU MODÈLE MÉTIER =====

    /**
     * {@code GET  /abonnements/user} : get the current user's active abonnement.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the abonnement.
     */
    @GetMapping("/user")
    public ResponseEntity<AbonnementDTO> getCurrentUserAbonnement() {
        log.debug("REST request to get current user's active abonnement");

        String login = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new IllegalStateException("L'utilisateur n'est pas authentifié."));

        User user = userRepository
            .findOneByLogin(login)
            .orElseThrow(() -> new IllegalStateException("Utilisateur non trouvé pour ce login."));

        Optional<AbonnementDTO> abonnementDTO = abonnementService.findActiveByUserId(user.getId());

        return ResponseUtil.wrapOrNotFound(abonnementDTO);
    }

    /**
     * {@code GET  /abonnements/user/usage} : get the current user's usage statistics.
     *
     * @return the {@link ResponseEntity} with usage information.
     */
    @GetMapping("/user/usage")
    public ResponseEntity<AbonnementDTO> getCurrentUserUsage() {
        log.debug("REST request to get current user's usage statistics");

        String login = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new IllegalStateException("L'utilisateur n'est pas authentifié."));

        User user = userRepository
            .findOneByLogin(login)
            .orElseThrow(() -> new IllegalStateException("Utilisateur non trouvé pour ce login."));

        Optional<AbonnementDTO> usageStats = abonnementService.getUserUsageStats(user.getId());

        return ResponseUtil.wrapOrNotFound(usageStats);
    }

    /**
     * {@code POST  /abonnements/user/sms/increment} : increment SMS usage for current user.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)}.
     */
    @PostMapping("/user/sms/increment")
    public ResponseEntity<Void> incrementSmsUsage() {
        log.debug("REST request to increment SMS usage for current user");

        String login = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new IllegalStateException("L'utilisateur n'est pas authentifié."));

        User user = userRepository
            .findOneByLogin(login)
            .orElseThrow(() -> new IllegalStateException("Utilisateur non trouvé pour ce login."));

        boolean success = abonnementService.incrementSmsUsage(user.getId());

        if (!success) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok().build();
    }

    /**
     * {@code POST  /abonnements/user/whatsapp/increment} : increment WhatsApp usage for current user.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)}.
     */
    @PostMapping("/user/whatsapp/increment")
    public ResponseEntity<Void> incrementWhatsappUsage() {
        log.debug("REST request to increment WhatsApp usage for current user");

        String login = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new IllegalStateException("L'utilisateur n'est pas authentifié."));

        User user = userRepository
            .findOneByLogin(login)
            .orElseThrow(() -> new IllegalStateException("Utilisateur non trouvé pour ce login."));

        boolean success = abonnementService.incrementWhatsappUsage(user.getId());

        if (!success) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok().build();
    }

    /**
     * {@code GET  /abonnements/user/can-send-sms} : check if current user can send SMS.
     *
     * @return the {@link ResponseEntity} with boolean result.
     */
    @GetMapping("/user/can-send-sms")
    public ResponseEntity<Boolean> canCurrentUserSendSms() {
        log.debug("REST request to check if current user can send SMS");

        String login = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new IllegalStateException("L'utilisateur n'est pas authentifié."));

        User user = userRepository
            .findOneByLogin(login)
            .orElseThrow(() -> new IllegalStateException("Utilisateur non trouvé pour ce login."));

        boolean canSend = abonnementService.canUserSendSms(user.getId());

        return ResponseEntity.ok(canSend);
    }

    /**
     * {@code GET  /abonnements/user/can-send-whatsapp} : check if current user can send WhatsApp.
     *
     * @return the {@link ResponseEntity} with boolean result.
     */
    @GetMapping("/user/can-send-whatsapp")
    public ResponseEntity<Boolean> canCurrentUserSendWhatsapp() {
        log.debug("REST request to check if current user can send WhatsApp");

        String login = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new IllegalStateException("L'utilisateur n'est pas authentifié."));

        User user = userRepository
            .findOneByLogin(login)
            .orElseThrow(() -> new IllegalStateException("Utilisateur non trouvé pour ce login."));

        boolean canSend = abonnementService.canUserSendWhatsapp(user.getId());

        return ResponseEntity.ok(canSend);
    }

    /**
     * {@code GET  /abonnements/expiring} : get abonnements expiring soon.
     *
     * @param days number of days to look ahead (default 7).
     * @return the {@link ResponseEntity} with the list of expiring abonnements.
     */
    @GetMapping("/expiring")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<AbonnementDTO>> getExpiringSoon(@RequestParam(defaultValue = "7") int days) {
        log.debug("REST request to get abonnements expiring in {} days", days);

        LocalDate expirationDate = LocalDate.now().plusDays(days);
        List<AbonnementDTO> expiringAbonnements = abonnementService.findExpiringSoon(expirationDate);

        return ResponseEntity.ok(expiringAbonnements);
    }

    /**
     * {@code GET  /abonnements/expired} : get expired abonnements.
     *
     * @return the {@link ResponseEntity} with the list of expired abonnements.
     */
    @GetMapping("/expired")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<AbonnementDTO>> getExpired() {
        log.debug("REST request to get expired abonnements");

        List<AbonnementDTO> expiredAbonnements = abonnementService.findExpired();

        return ResponseEntity.ok(expiredAbonnements);
    }

    /**
     * {@code POST  /abonnements/:id/suspend} : suspend an abonnement.
     *
     * @param id the id of the abonnement to suspend.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)}.
     */
    @PostMapping("/{id}/suspend")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<AbonnementDTO> suspendAbonnement(@PathVariable Long id) {
        log.debug("REST request to suspend Abonnement : {}", id);

        AbonnementDTO result = abonnementService.suspendAbonnement(id);

        return ResponseEntity.ok().headers(HeaderUtil.createAlert(applicationName, "Abonnement suspendu", id.toString())).body(result);
    }

    /**
     * {@code POST  /abonnements/:id/reactivate} : reactivate a suspended abonnement.
     *
     * @param id the id of the abonnement to reactivate.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)}.
     */
    @PostMapping("/{id}/reactivate")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<AbonnementDTO> reactivateAbonnement(@PathVariable Long id) {
        log.debug("REST request to reactivate Abonnement : {}", id);

        AbonnementDTO result = abonnementService.reactivateAbonnement(id);

        return ResponseEntity.ok().headers(HeaderUtil.createAlert(applicationName, "Abonnement réactivé", id.toString())).body(result);
    }

    /**
     * {@code POST  /abonnements/:id/renew} : renew an abonnement.
     *
     * @param id the id of the abonnement to renew.
     * @param months number of months to extend (default 1).
     * @return the {@link ResponseEntity} with status {@code 200 (OK)}.
     */
    @PostMapping("/{id}/renew")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<AbonnementDTO> renewAbonnement(@PathVariable Long id, @RequestParam(defaultValue = "1") int months) {
        log.debug("REST request to renew Abonnement : {} for {} months", id, months);

        AbonnementDTO result = abonnementService.renewAbonnement(id, months);

        return ResponseEntity.ok()
            .headers(HeaderUtil.createAlert(applicationName, "Abonnement renouvelé pour " + months + " mois", id.toString()))
            .body(result);
    }

    /**
     * {@code GET  /abonnements/statistics} : get subscription statistics.
     *
     * @return the {@link ResponseEntity} with subscription statistics.
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Object> getSubscriptionStatistics() {
        log.debug("REST request to get subscription statistics");

        Object stats = abonnementService.getSubscriptionStatistics();

        return ResponseEntity.ok(stats);
    }

    /**
     * {@code POST  /abonnements/batch/expire} : expire abonnements that are past due.
     *
     * @return the {@link ResponseEntity} with number of expired abonnements.
     */
    @PostMapping("/batch/expire")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Integer> expirePastDueAbonnements() {
        log.debug("REST request to expire past due abonnements");

        int expiredCount = abonnementService.expirePastDueAbonnements();

        return ResponseEntity.ok()
            .headers(HeaderUtil.createAlert(applicationName, expiredCount + " abonnements expirés", ""))
            .body(expiredCount);
    }
}
