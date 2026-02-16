package com.example.myproject.service;

import com.example.myproject.repository.SmsRepository;
import jakarta.persistence.EntityManager;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SmsResetService {

    private static final Logger log = LoggerFactory.getLogger(SmsResetService.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private SmsRepository smsRepository;

    /**
     * Remet en PENDING les SMS échoués d'une campagne
     */
    @Transactional
    public ResetResult resetFailedSms(Long sendSmsId) {
        log.info("[RESET-START] Début reset pour SendSms {}", sendSmsId);

        try {
            // 1. Compter les SMS à remettre en PENDING (exclure Invalid Destination Address)
            String countSql =
                """
                SELECT COUNT(*) FROM sms
                WHERE send_sms_id = ?
                AND (status = 'FAILED' OR delivery_status = 'failed' OR is_sent = false)
                AND (last_error IS NULL
                     OR last_error NOT LIKE '%Invalid Destination Address%'
                     OR last_error = '')
                """;

            int toResetCount = jdbcTemplate.queryForObject(countSql, Integer.class, sendSmsId);

            if (toResetCount == 0) {
                log.info("[RESET-FINISH] Aucun SMS à réinitialiser pour SendSms {} (numéros invalides exclus)", sendSmsId);
                return new ResetResult(0, 0, "Aucun SMS réinitialisable (numéros invalides exclus)");
            }

            // 2. Réinitialiser les SMS (exclure Invalid Destination Address)
            String updateSql =
                """
                UPDATE sms
                SET status = 'PENDING',
                    delivery_status = 'pending',
                    is_sent = NULL,
                    message_id = NULL,
                    send_date = NULL
                WHERE send_sms_id = ?
                AND (status = 'FAILED' OR delivery_status = 'failed' OR is_sent = false)
                AND (last_error IS NULL
                     OR last_error NOT LIKE '%Invalid Destination Address%'
                     OR last_error = '')
                """;

            int resetCount = jdbcTemplate.update(updateSql, sendSmsId);

            // 3. Compter les SMS exclus (pour info)
            String excludedCountSql =
                """
                SELECT COUNT(*) FROM sms
                WHERE send_sms_id = ?
                AND (status = 'FAILED' OR delivery_status = 'failed')
                AND last_error LIKE '%Invalid Destination Address%'
                """;

            int excludedCount = jdbcTemplate.queryForObject(excludedCountSql, Integer.class, sendSmsId);

            // 4. Flush pour persistence immédiate
            entityManager.flush();

            log.info(
                "[RESET-FINISH] {} SMS remis en PENDING, {} exclus (numéros invalides) pour SendSms {}",
                resetCount,
                excludedCount,
                sendSmsId
            );

            String message = excludedCount > 0 ? resetCount + " SMS réinitialisés, " + excludedCount + " exclus (numéros invalides)" : null;

            return new ResetResult(toResetCount, resetCount, message);
        } catch (Exception e) {
            log.error("[RESET-ERROR] Erreur lors du reset pour SendSms {}: {}", sendSmsId, e.getMessage(), e);
            return new ResetResult(0, 0, e.getMessage());
        }
    }

    /**
     * Obtient les statistiques avant reset
     */
    public Map<String, Integer> getResetStatistics(Long sendSmsId) {
        try {
            String sql =
                """
                SELECT
                    COUNT(*) as total,
                    COUNT(CASE WHEN status = 'PENDING' AND delivery_status = 'pending' THEN 1 END) as already_pending,
                    COUNT(CASE WHEN status = 'FAILED' OR delivery_status = 'failed' THEN 1 END) as failed,
                    COUNT(CASE WHEN status = 'SENT' AND is_sent = true THEN 1 END) as sent
                FROM sms
                WHERE send_sms_id = ?
                """;

            Map<String, Object> result = jdbcTemplate.queryForMap(sql, sendSmsId);

            Map<String, Integer> stats = new HashMap<>();
            stats.put("total", ((Number) result.get("total")).intValue());
            stats.put("alreadyPending", ((Number) result.get("already_pending")).intValue());
            stats.put("failed", ((Number) result.get("failed")).intValue());
            stats.put("sent", ((Number) result.get("sent")).intValue());

            int toReset = stats.get("total") - stats.get("alreadyPending");
            stats.put("toReset", toReset);

            return stats;
        } catch (Exception e) {
            log.error("[RESET-STATS] Erreur récupération stats pour SendSms {}: {}", sendSmsId, e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * Vérifie si un reset est nécessaire
     */
    public boolean isResetNeeded(Long sendSmsId) {
        try {
            String sql =
                """
                SELECT COUNT(*) FROM sms
                WHERE send_sms_id = ?
                AND (status != 'PENDING' OR delivery_status != 'pending' OR is_sent IS NOT NULL)
                """;

            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, sendSmsId);
            return count != null && count > 0;
        } catch (Exception e) {
            log.error("[RESET-CHECK] Erreur vérification pour SendSms {}: {}", sendSmsId, e.getMessage());
            return false;
        }
    }

    /**
     * Classe pour retourner les résultats du reset
     */
    public static class ResetResult {

        private final int toResetCount;
        private final int actuallyResetCount;
        private final String error;

        public ResetResult(int toResetCount, int actuallyResetCount, String error) {
            this.toResetCount = toResetCount;
            this.actuallyResetCount = actuallyResetCount;
            this.error = error;
        }

        public int getToResetCount() {
            return toResetCount;
        }

        public int getActuallyResetCount() {
            return actuallyResetCount;
        }

        public String getError() {
            return error;
        }

        public boolean isSuccess() {
            return error == null;
        }

        public boolean hasReset() {
            return actuallyResetCount > 0;
        }
    }
}
