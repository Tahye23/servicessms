package com.example.myproject.web.rest;

import com.example.myproject.domain.Abonnement;
import com.example.myproject.domain.ExtendedUser;
import com.example.myproject.repository.*;
import com.example.myproject.service.SmsMigrationService;
import java.time.Instant;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/abonnements")
@Transactional
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class AbonnementAdminResource {

    private final Logger log = LoggerFactory.getLogger(AbonnementAdminResource.class);

    private final AbonnementRepository abonnementRepository;
    private final ExtendedUserRepository extendedUserRepository;
    private final SendSmsRepository sendSmsRepository;
    private final SmsRepository smsRepository;
    private final SmsMigrationService migrationService;

    public AbonnementAdminResource(
        AbonnementRepository abonnementRepository,
        ExtendedUserRepository extendedUserRepository,
        SendSmsRepository sendSmsRepository,
        SmsRepository smsRepository,
        SmsMigrationService migrationService
    ) {
        this.abonnementRepository = abonnementRepository;
        this.extendedUserRepository = extendedUserRepository;
        this.sendSmsRepository = sendSmsRepository;
        this.smsRepository = smsRepository;
        this.migrationService = migrationService;
    }

    /**
     * POST /admin/abonnements/recalculate : Recalcule l'utilisation réelle d'un abonnement
     */
    @PostMapping("/recalculate")
    @Transactional
    public ResponseEntity<Map<String, Object>> recalculateAbonnement(@RequestBody Map<String, String> payload) {
        String userLogin = payload.get("userLogin");

        if (userLogin == null || userLogin.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Le login utilisateur est requis"));
        }

        log.debug("Recalcul de l'abonnement pour l'utilisateur: {}", userLogin);

        try {
            // 1. Trouver l'ExtendedUser
            Optional<ExtendedUser> extendedUserOpt = extendedUserRepository.findByUserLogin(userLogin);
            if (extendedUserOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Utilisateur non trouvé: " + userLogin));
            }

            ExtendedUser extendedUser = extendedUserOpt.get();

            // 2. Récupérer TOUS les abonnements ACTIFS
            List<Abonnement> abonnements = abonnementRepository.findAllByUserIdAndStatus(
                extendedUser.getId(),
                Abonnement.SubscriptionStatus.ACTIVE
            );

            if (abonnements.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    Map.of("error", "Aucun abonnement actif trouvé pour: " + userLogin)
                );
            }

            // 3. Calculer les totaux de consommation (uniquement SENT)
            Integer totalSmsUsed = smsRepository.countSuccessfulSmsByUserLogin(userLogin);
            Integer totalWhatsappUsed = smsRepository.countSuccessfulWhatsappByUserLogin(userLogin);

            log.info("Consommation totale pour {}: SMS={}, WhatsApp={}", userLogin, totalSmsUsed, totalWhatsappUsed);

            List<Map<String, Object>> abonnementsUpdated = new ArrayList<>();

            // 4. Mettre à jour chaque abonnement selon son type
            for (Abonnement abonnement : abonnements) {
                // Récupérer le nom du plan
                String planName = abonnement.getCustomName() != null && !abonnement.getCustomName().isEmpty()
                    ? abonnement.getCustomName()
                    : "Abonnement " + abonnement.getId();

                // Récupérer les limites configurées
                Integer smsLimit = abonnement.getCustomSmsLimit();
                Integer whatsappLimit = abonnement.getCustomWhatsappLimit();

                boolean hasSmsLimit = smsLimit != null && smsLimit > 0;
                boolean hasWhatsappLimit = whatsappLimit != null && whatsappLimit > 0;

                String abonnementType;
                Map<String, Object> abonnementInfo = new HashMap<>();
                abonnementInfo.put("id", abonnement.getId());
                abonnementInfo.put("planName", planName);

                // Déterminer le type et mettre à jour
                if (hasSmsLimit && !hasWhatsappLimit) {
                    // ===== ABONNEMENT SMS UNIQUEMENT =====
                    abonnementType = "SMS";
                    abonnement.setSmsUsed(totalSmsUsed != null ? totalSmsUsed : 0);
                    abonnement.setWhatsappUsed(0);

                    abonnementInfo.put("type", abonnementType);
                    abonnementInfo.put("smsUsed", totalSmsUsed != null ? totalSmsUsed : 0);
                    abonnementInfo.put("smsLimit", smsLimit);
                    abonnementInfo.put("whatsappUsed", 0);
                } else if (hasWhatsappLimit && !hasSmsLimit) {
                    // ===== ABONNEMENT WHATSAPP UNIQUEMENT =====
                    abonnementType = "WhatsApp";
                    abonnement.setSmsUsed(0);
                    abonnement.setWhatsappUsed(totalWhatsappUsed != null ? totalWhatsappUsed : 0);

                    abonnementInfo.put("type", abonnementType);
                    abonnementInfo.put("smsUsed", 0);
                    abonnementInfo.put("whatsappUsed", totalWhatsappUsed != null ? totalWhatsappUsed : 0);
                    abonnementInfo.put("whatsappLimit", whatsappLimit);
                } else if (hasSmsLimit && hasWhatsappLimit) {
                    // ===== ABONNEMENT MIXTE (SMS + WhatsApp) =====
                    abonnementType = "Mixte (SMS + WhatsApp)";
                    abonnement.setSmsUsed(totalSmsUsed != null ? totalSmsUsed : 0);
                    abonnement.setWhatsappUsed(totalWhatsappUsed != null ? totalWhatsappUsed : 0);

                    abonnementInfo.put("type", abonnementType);
                    abonnementInfo.put("smsUsed", totalSmsUsed != null ? totalSmsUsed : 0);
                    abonnementInfo.put("smsLimit", smsLimit);
                    abonnementInfo.put("whatsappUsed", totalWhatsappUsed != null ? totalWhatsappUsed : 0);
                    abonnementInfo.put("whatsappLimit", whatsappLimit);
                } else {
                    // ===== ABONNEMENT SANS LIMITE =====
                    abonnementType = "Sans limite définie";
                    abonnement.setSmsUsed(totalSmsUsed != null ? totalSmsUsed : 0);
                    abonnement.setWhatsappUsed(totalWhatsappUsed != null ? totalWhatsappUsed : 0);

                    abonnementInfo.put("type", abonnementType);
                    abonnementInfo.put("smsUsed", totalSmsUsed != null ? totalSmsUsed : 0);
                    abonnementInfo.put("whatsappUsed", totalWhatsappUsed != null ? totalWhatsappUsed : 0);
                }

                // Mettre à jour la date de modification
                abonnement.setUpdatedDate(Instant.now());
                abonnementRepository.save(abonnement);

                abonnementsUpdated.add(abonnementInfo);

                log.info(
                    "Abonnement {} ({}) mis à jour: SMS={}/{}, WhatsApp={}/{}",
                    abonnement.getId(),
                    abonnementType,
                    abonnement.getSmsUsed(),
                    smsLimit,
                    abonnement.getWhatsappUsed(),
                    whatsappLimit
                );
            }

            log.info("✅ Recalcul terminé pour {}: {} abonnement(s) mis à jour", userLogin, abonnements.size());

            return ResponseEntity.ok(
                Map.of(
                    "success",
                    true,
                    "userLogin",
                    userLogin,
                    "totalSmsUsed",
                    totalSmsUsed != null ? totalSmsUsed : 0,
                    "totalWhatsappUsed",
                    totalWhatsappUsed != null ? totalWhatsappUsed : 0,
                    "abonnementsCount",
                    abonnements.size(),
                    "abonnements",
                    abonnementsUpdated,
                    "message",
                    abonnements.size() + " abonnement(s) recalculé(s) avec succès"
                )
            );
        } catch (Exception e) {
            log.error("❌ Erreur lors du recalcul de l'abonnement pour {}: {}", userLogin, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                Map.of("success", false, "error", "Erreur lors du recalcul : " + e.getMessage())
            );
        }
    }

    @PostMapping("/migrate-user")
    public ResponseEntity<SmsMigrationService.MigrationResult> migrateUserLogin(@RequestBody Map<String, String> payload) {
        String userLogin = payload.get("userLogin");

        if (userLogin == null || userLogin.trim().isEmpty()) {
            SmsMigrationService.MigrationResult errorResult = new SmsMigrationService.MigrationResult();
            errorResult.setSuccess(false);
            errorResult.setMessage("Le userLogin est requis");
            return ResponseEntity.badRequest().body(errorResult);
        }

        log.info("📥 Requête de migration pour l'utilisateur: {}", userLogin);

        SmsMigrationService.MigrationResult result = migrationService.migrateUserLoginForUser(userLogin);

        if (result.isSuccess()) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.internalServerError().body(result);
        }
    }

    /**
     * POST /api/admin/sms-migration/migrate-all : Migrer les user_login pour TOUS les utilisateurs
     */
    @PostMapping("/migrate-all")
    public ResponseEntity<SmsMigrationService.GlobalMigrationResult> migrateAllUsers() {
        log.info("📥 Requête de migration globale");

        SmsMigrationService.GlobalMigrationResult result = migrationService.migrateAllUsers();

        if (result.isSuccess()) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.internalServerError().body(result);
        }
    }

    @PostMapping("/update-quota")
    public ResponseEntity<Map<String, Object>> updateUserQuota(@RequestBody UpdateQuotaRequest request) {
        log.info("📥 Requête de mise à jour de quota pour: {}", request.getUserLogin());

        if (request.getUserLogin() == null || request.getUserLogin().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Le login utilisateur est requis"));
        }

        try {
            // 1. Trouver l'ExtendedUser
            Optional<ExtendedUser> extendedUserOpt = extendedUserRepository.findByUserLogin(request.getUserLogin());
            if (extendedUserOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    Map.of("error", "Utilisateur non trouvé: " + request.getUserLogin())
                );
            }

            ExtendedUser extendedUser = extendedUserOpt.get();

            // 2. Récupérer tous les abonnements actifs
            List<Abonnement> abonnements = abonnementRepository.findAllByUserIdAndStatus(
                extendedUser.getId(),
                Abonnement.SubscriptionStatus.ACTIVE
            );

            if (abonnements.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    Map.of("error", "Aucun abonnement actif trouvé pour: " + request.getUserLogin())
                );
            }

            List<Map<String, Object>> updatedAbonnements = new ArrayList<>();

            // 3. Mettre à jour les quotas selon le type d'abonnement
            for (Abonnement abonnement : abonnements) {
                String planName = abonnement.getCustomName() != null ? abonnement.getCustomName() : "Abonnement " + abonnement.getId();

                boolean updated = false;
                Map<String, Object> abonnementInfo = new HashMap<>();
                abonnementInfo.put("id", abonnement.getId());
                abonnementInfo.put("planName", planName);

                // Déterminer le type d'abonnement
                Integer currentSmsLimit = abonnement.getCustomSmsLimit();
                Integer currentWhatsappLimit = abonnement.getCustomWhatsappLimit();

                boolean hasSmsLimit = currentSmsLimit != null && currentSmsLimit > 0;
                boolean hasWhatsappLimit = currentWhatsappLimit != null && currentWhatsappLimit > 0;

                // Mise à jour SMS
                if (request.getNewSmsLimit() != null) {
                    if (hasSmsLimit || (!hasSmsLimit && !hasWhatsappLimit)) {
                        Integer oldSmsLimit = abonnement.getCustomSmsLimit();
                        abonnement.setCustomSmsLimit(request.getNewSmsLimit());
                        updated = true;

                        abonnementInfo.put("type", hasWhatsappLimit ? "Mixte" : "SMS");
                        abonnementInfo.put("oldSmsLimit", oldSmsLimit);
                        abonnementInfo.put("newSmsLimit", request.getNewSmsLimit());
                        abonnementInfo.put("smsUsed", abonnement.getSmsUsed());
                    }
                }

                // Mise à jour WhatsApp
                if (request.getNewWhatsappLimit() != null) {
                    if (hasWhatsappLimit || (!hasSmsLimit && !hasWhatsappLimit)) {
                        Integer oldWhatsappLimit = abonnement.getCustomWhatsappLimit();
                        abonnement.setCustomWhatsappLimit(request.getNewWhatsappLimit());
                        updated = true;

                        if (!abonnementInfo.containsKey("type")) {
                            abonnementInfo.put("type", hasSmsLimit ? "Mixte" : "WhatsApp");
                        }
                        abonnementInfo.put("oldWhatsappLimit", oldWhatsappLimit);
                        abonnementInfo.put("newWhatsappLimit", request.getNewWhatsappLimit());
                        abonnementInfo.put("whatsappUsed", abonnement.getWhatsappUsed());
                    }
                }

                if (updated) {
                    abonnement.setUpdatedDate(Instant.now());
                    abonnementRepository.save(abonnement);
                    updatedAbonnements.add(abonnementInfo);

                    log.info("✅ Abonnement {} mis à jour pour {}", abonnement.getId(), request.getUserLogin());
                }
            }

            if (updatedAbonnements.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Aucun abonnement correspondant au type demandé"));
            }

            return ResponseEntity.ok(
                Map.of(
                    "success",
                    true,
                    "userLogin",
                    request.getUserLogin(),
                    "message",
                    "Quotas mis à jour avec succès",
                    "abonnements",
                    updatedAbonnements
                )
            );
        } catch (Exception e) {
            log.error("❌ Erreur lors de la mise à jour des quotas pour {}: {}", request.getUserLogin(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Erreur: " + e.getMessage()));
        }
    }

    /**
     * POST /api/admin/abonnement-quota/increase-quota : Augmenter les quotas d'un utilisateur
     */
    @PostMapping("/increase-quota")
    public ResponseEntity<Map<String, Object>> increaseUserQuota(@RequestBody IncreaseQuotaRequest request) {
        log.info("📥 Requête d'augmentation de quota pour: {}", request.getUserLogin());

        if (request.getUserLogin() == null || request.getUserLogin().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Le login utilisateur est requis"));
        }

        try {
            // 1. Trouver l'ExtendedUser
            Optional<ExtendedUser> extendedUserOpt = extendedUserRepository.findByUserLogin(request.getUserLogin());
            if (extendedUserOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    Map.of("error", "Utilisateur non trouvé: " + request.getUserLogin())
                );
            }

            ExtendedUser extendedUser = extendedUserOpt.get();

            // 2. Récupérer tous les abonnements actifs
            List<Abonnement> abonnements = abonnementRepository.findAllByUserIdAndStatus(
                extendedUser.getId(),
                Abonnement.SubscriptionStatus.ACTIVE
            );

            if (abonnements.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    Map.of("error", "Aucun abonnement actif trouvé pour: " + request.getUserLogin())
                );
            }

            List<Map<String, Object>> updatedAbonnements = new ArrayList<>();

            // 3. Augmenter les quotas
            for (Abonnement abonnement : abonnements) {
                String planName = abonnement.getCustomName() != null ? abonnement.getCustomName() : "Abonnement " + abonnement.getId();

                boolean updated = false;
                Map<String, Object> abonnementInfo = new HashMap<>();
                abonnementInfo.put("id", abonnement.getId());
                abonnementInfo.put("planName", planName);

                Integer currentSmsLimit = abonnement.getCustomSmsLimit();
                Integer currentWhatsappLimit = abonnement.getCustomWhatsappLimit();

                boolean hasSmsLimit = currentSmsLimit != null && currentSmsLimit > 0;
                boolean hasWhatsappLimit = currentWhatsappLimit != null && currentWhatsappLimit > 0;

                // Augmentation SMS
                if (request.getSmsIncrease() != null && request.getSmsIncrease() > 0) {
                    if (hasSmsLimit || (!hasSmsLimit && !hasWhatsappLimit)) {
                        Integer oldLimit = currentSmsLimit != null ? currentSmsLimit : 0;
                        Integer newLimit = oldLimit + request.getSmsIncrease();
                        abonnement.setCustomSmsLimit(newLimit);
                        updated = true;

                        abonnementInfo.put("type", hasWhatsappLimit ? "Mixte" : "SMS");
                        abonnementInfo.put("oldSmsLimit", oldLimit);
                        abonnementInfo.put("newSmsLimit", newLimit);
                        abonnementInfo.put("smsIncrease", request.getSmsIncrease());
                    }
                }

                // Augmentation WhatsApp
                if (request.getWhatsappIncrease() != null && request.getWhatsappIncrease() > 0) {
                    if (hasWhatsappLimit || (!hasSmsLimit && !hasWhatsappLimit)) {
                        Integer oldLimit = currentWhatsappLimit != null ? currentWhatsappLimit : 0;
                        Integer newLimit = oldLimit + request.getWhatsappIncrease();
                        abonnement.setCustomWhatsappLimit(newLimit);
                        updated = true;

                        if (!abonnementInfo.containsKey("type")) {
                            abonnementInfo.put("type", hasSmsLimit ? "Mixte" : "WhatsApp");
                        }
                        abonnementInfo.put("oldWhatsappLimit", oldLimit);
                        abonnementInfo.put("newWhatsappLimit", newLimit);
                        abonnementInfo.put("whatsappIncrease", request.getWhatsappIncrease());
                    }
                }

                if (updated) {
                    abonnement.setUpdatedDate(Instant.now());
                    abonnementRepository.save(abonnement);
                    updatedAbonnements.add(abonnementInfo);

                    log.info("✅ Quota augmenté pour abonnement {} de {}", abonnement.getId(), request.getUserLogin());
                }
            }

            if (updatedAbonnements.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Aucune augmentation effectuée"));
            }

            return ResponseEntity.ok(
                Map.of(
                    "success",
                    true,
                    "userLogin",
                    request.getUserLogin(),
                    "message",
                    "Quotas augmentés avec succès",
                    "abonnements",
                    updatedAbonnements
                )
            );
        } catch (Exception e) {
            log.error("❌ Erreur lors de l'augmentation des quotas pour {}: {}", request.getUserLogin(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Erreur: " + e.getMessage()));
        }
    }

    /**
     * GET /api/admin/abonnement-quota/view-quota/{userLogin} : Voir les quotas d'un utilisateur
     */
    @GetMapping("/view-quota/{userLogin}")
    public ResponseEntity<Map<String, Object>> viewUserQuota(@PathVariable String userLogin) {
        log.info("📥 Consultation des quotas pour: {}", userLogin);

        try {
            Optional<ExtendedUser> extendedUserOpt = extendedUserRepository.findByUserLogin(userLogin);
            if (extendedUserOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Utilisateur non trouvé: " + userLogin));
            }

            ExtendedUser extendedUser = extendedUserOpt.get();

            List<Abonnement> abonnements = abonnementRepository.findAllByUserIdAndStatus(
                extendedUser.getId(),
                Abonnement.SubscriptionStatus.ACTIVE
            );

            if (abonnements.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Aucun abonnement actif pour: " + userLogin));
            }

            List<Map<String, Object>> abonnementsInfo = new ArrayList<>();

            for (Abonnement abonnement : abonnements) {
                Map<String, Object> info = new HashMap<>();
                info.put("id", abonnement.getId());
                info.put("planName", abonnement.getCustomName());
                info.put("smsLimit", abonnement.getCustomSmsLimit());
                info.put("smsUsed", abonnement.getSmsUsed());
                info.put(
                    "smsRemaining",
                    (abonnement.getCustomSmsLimit() != null ? abonnement.getCustomSmsLimit() : 0) - abonnement.getSmsUsed()
                );
                info.put("whatsappLimit", abonnement.getCustomWhatsappLimit());
                info.put("whatsappUsed", abonnement.getWhatsappUsed());
                info.put(
                    "whatsappRemaining",
                    (abonnement.getCustomWhatsappLimit() != null ? abonnement.getCustomWhatsappLimit() : 0) - abonnement.getWhatsappUsed()
                );
                info.put("status", abonnement.getStatus());
                info.put("startDate", abonnement.getStartDate());
                info.put("endDate", abonnement.getEndDate());

                abonnementsInfo.add(info);
            }

            return ResponseEntity.ok(Map.of("success", true, "userLogin", userLogin, "abonnements", abonnementsInfo));
        } catch (Exception e) {
            log.error("❌ Erreur lors de la consultation des quotas: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Erreur: " + e.getMessage()));
        }
    }

    // Classes de requête
    public static class UpdateQuotaRequest {

        private String userLogin;
        private Integer newSmsLimit;
        private Integer newWhatsappLimit;

        public String getUserLogin() {
            return userLogin;
        }

        public void setUserLogin(String userLogin) {
            this.userLogin = userLogin;
        }

        public Integer getNewSmsLimit() {
            return newSmsLimit;
        }

        public void setNewSmsLimit(Integer newSmsLimit) {
            this.newSmsLimit = newSmsLimit;
        }

        public Integer getNewWhatsappLimit() {
            return newWhatsappLimit;
        }

        public void setNewWhatsappLimit(Integer newWhatsappLimit) {
            this.newWhatsappLimit = newWhatsappLimit;
        }
    }

    public static class IncreaseQuotaRequest {

        private String userLogin;
        private Integer smsIncrease;
        private Integer whatsappIncrease;

        public String getUserLogin() {
            return userLogin;
        }

        public void setUserLogin(String userLogin) {
            this.userLogin = userLogin;
        }

        public Integer getSmsIncrease() {
            return smsIncrease;
        }

        public void setSmsIncrease(Integer smsIncrease) {
            this.smsIncrease = smsIncrease;
        }

        public Integer getWhatsappIncrease() {
            return whatsappIncrease;
        }

        public void setWhatsappIncrease(Integer whatsappIncrease) {
            this.whatsappIncrease = whatsappIncrease;
        }
    }
}
