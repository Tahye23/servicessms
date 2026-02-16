package com.example.myproject.service;

import com.example.myproject.domain.ExtendedUser;
import com.example.myproject.domain.SendSms;
import com.example.myproject.domain.Sms;
import com.example.myproject.repository.ExtendedUserRepository;
import com.example.myproject.repository.SendSmsRepository;
import com.example.myproject.repository.SmsRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SmsMigrationService {

    private final Logger log = LoggerFactory.getLogger(SmsMigrationService.class);

    private final SmsRepository smsRepository;
    private final SendSmsRepository sendSmsRepository;
    private final ExtendedUserRepository extendedUserRepository;

    public SmsMigrationService(
        SmsRepository smsRepository,
        SendSmsRepository sendSmsRepository,
        ExtendedUserRepository extendedUserRepository
    ) {
        this.smsRepository = smsRepository;
        this.sendSmsRepository = sendSmsRepository;
        this.extendedUserRepository = extendedUserRepository;
    }

    /**
     * Migrer les user_login pour un utilisateur spécifique
     */
    @Transactional
    public MigrationResult migrateUserLoginForUser(String userLogin) {
        log.info("🔄 Début migration user_login pour: {}", userLogin);

        MigrationResult result = new MigrationResult();
        result.setUserLogin(userLogin);

        try {
            // 1. Trouver l'ExtendedUser
            ExtendedUser extendedUser = extendedUserRepository
                .findByUserLogin(userLogin)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé: " + userLogin));

            log.info("✅ ExtendedUser trouvé: ID={}, Login={}", extendedUser.getId(), extendedUser.getUser().getLogin());

            // 2. Récupérer tous les SendSms de cet utilisateur
            List<SendSms> sendSmsList = sendSmsRepository.findByUser(extendedUser);

            if (sendSmsList.isEmpty()) {
                log.warn("⚠️ Aucun SendSms trouvé pour l'utilisateur: {}", userLogin);
                result.setSuccess(true);
                result.setMessage("Aucun SendSms trouvé pour cet utilisateur");
                return result;
            }

            log.info("📊 {} SendSms trouvés pour l'utilisateur {}", sendSmsList.size(), userLogin);

            int totalSmsUpdated = 0;

            // 3. Pour chaque SendSms, mettre à jour tous les SMS associés
            for (SendSms sendSms : sendSmsList) {
                log.debug("Traitement SendSms ID={}", sendSms.getId());

                // Récupérer tous les SMS associés à ce SendSms
                List<Sms> smsList = smsRepository.findALLBYsendSmsId(sendSms.getId());

                log.debug("  → {} SMS trouvés pour SendSms ID={}", smsList.size(), sendSms.getId());

                // Mettre à jour chaque SMS avec le user_login
                for (Sms sms : smsList) {
                    if (sms.getUser_login() == null || sms.getUser_login().isEmpty()) {
                        sms.setUser_login(userLogin);
                        smsRepository.save(sms);
                        totalSmsUpdated++;

                        log.debug("    ✓ SMS ID={} mis à jour avec user_login={}", sms.getId(), userLogin);
                    }
                }
            }

            result.setTotalSendSms(sendSmsList.size());
            result.setMigrated(totalSmsUpdated);
            result.setSuccess(true);
            result.setMessage(String.format("Migration réussie: %d SMS mis à jour depuis %d SendSms", totalSmsUpdated, sendSmsList.size()));

            log.info("✅ Migration terminée pour {}: {} SMS mis à jour depuis {} SendSms", userLogin, totalSmsUpdated, sendSmsList.size());
        } catch (Exception e) {
            log.error("❌ Erreur lors de la migration pour {}: {}", userLogin, e.getMessage(), e);
            result.setSuccess(false);
            result.setMessage("Erreur: " + e.getMessage());
        }

        return result;
    }

    /**
     * Migrer les user_login pour TOUS les utilisateurs
     */
    @Transactional
    public GlobalMigrationResult migrateAllUsers() {
        log.info("🔄 Début migration globale de tous les utilisateurs");

        GlobalMigrationResult globalResult = new GlobalMigrationResult();

        try {
            // Récupérer tous les ExtendedUser
            List<ExtendedUser> allUsers = extendedUserRepository.findAll();

            log.info("📊 {} utilisateurs trouvés", allUsers.size());

            int totalUsersProcessed = 0;
            int totalSmsUpdated = 0;
            int totalSendSmsProcessed = 0;

            for (ExtendedUser extendedUser : allUsers) {
                String userLogin = extendedUser.getUser().getLogin();

                log.info("Traitement utilisateur {}/{}: {}", totalUsersProcessed + 1, allUsers.size(), userLogin);

                MigrationResult userResult = migrateUserLoginForUser(userLogin);

                if (userResult.isSuccess()) {
                    totalUsersProcessed++;
                    totalSmsUpdated += userResult.getMigrated();
                    totalSendSmsProcessed += userResult.getTotalSendSms();
                }
            }

            globalResult.setTotalUsersProcessed(totalUsersProcessed);
            globalResult.setTotalSmsUpdated(totalSmsUpdated);
            globalResult.setTotalSendSmsProcessed(totalSendSmsProcessed);
            globalResult.setSuccess(true);
            globalResult.setMessage(
                String.format(
                    "Migration globale terminée: %d utilisateurs, %d SMS mis à jour depuis %d SendSms",
                    totalUsersProcessed,
                    totalSmsUpdated,
                    totalSendSmsProcessed
                )
            );

            log.info("✅ Migration globale terminée: {} utilisateurs, {} SMS mis à jour", totalUsersProcessed, totalSmsUpdated);
        } catch (Exception e) {
            log.error("❌ Erreur lors de la migration globale: {}", e.getMessage(), e);
            globalResult.setSuccess(false);
            globalResult.setMessage("Erreur: " + e.getMessage());
        }

        return globalResult;
    }

    // Classes de résultats
    public static class MigrationResult {

        private boolean success;
        private String message;
        private String userLogin;
        private Integer totalSendSms = 0;
        private Integer migrated = 0;

        // Getters et Setters
        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getUserLogin() {
            return userLogin;
        }

        public void setUserLogin(String userLogin) {
            this.userLogin = userLogin;
        }

        public Integer getTotalSendSms() {
            return totalSendSms;
        }

        public void setTotalSendSms(Integer totalSendSms) {
            this.totalSendSms = totalSendSms;
        }

        public Integer getMigrated() {
            return migrated;
        }

        public void setMigrated(Integer migrated) {
            this.migrated = migrated;
        }
    }

    public static class GlobalMigrationResult {

        private boolean success;
        private String message;
        private Integer totalUsersProcessed = 0;
        private Integer totalSendSmsProcessed = 0;
        private Integer totalSmsUpdated = 0;

        // Getters et Setters
        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public Integer getTotalUsersProcessed() {
            return totalUsersProcessed;
        }

        public void setTotalUsersProcessed(Integer totalUsersProcessed) {
            this.totalUsersProcessed = totalUsersProcessed;
        }

        public Integer getTotalSendSmsProcessed() {
            return totalSendSmsProcessed;
        }

        public void setTotalSendSmsProcessed(Integer totalSendSmsProcessed) {
            this.totalSendSmsProcessed = totalSendSmsProcessed;
        }

        public Integer getTotalSmsUpdated() {
            return totalSmsUpdated;
        }

        public void setTotalSmsUpdated(Integer totalSmsUpdated) {
            this.totalSmsUpdated = totalSmsUpdated;
        }
    }
}
