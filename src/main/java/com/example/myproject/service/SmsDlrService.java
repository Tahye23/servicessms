package com.example.myproject.service;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class SmsDlrService {

    private static final Logger log = LoggerFactory.getLogger(SmsDlrService.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * ✅ TRAITER DLR REÇU VIA SMPP
     */
    @Async
    public void processDlrFromSmpp(String messageId, Integer smppStatus, String destAddr, String errorCode) {
        if (messageId == null || messageId.isEmpty()) {
            log.warn("[DLR] MessageId manquant");
            return;
        }

        String deliveryStatus = mapSmppStatus(smppStatus);
        String error = buildErrorMessage(smppStatus, errorCode);

        log.info("[DLR] Traitement: {} → {}", messageId, deliveryStatus);

        try {
            // ✅ METTRE À JOUR LE SMS
            int updated = jdbcTemplate.update(
                """
                UPDATE sms SET
                    delivery_status = ?,
                    is_sent = ?,
                    last_error = COALESCE(?, last_error),
                    status = CASE
                        WHEN ? = 'delivered' THEN 'DELIVERED'
                        WHEN ? = 'failed' THEN 'FAILED'
                        ELSE status
                    END
                WHERE message_id = ?
                AND delivery_status NOT IN ('delivered')
                """,
                deliveryStatus,
                "delivered".equals(deliveryStatus),
                error,
                deliveryStatus,
                deliveryStatus,
                messageId
            );

            if (updated > 0) {
                log.info("[DLR] ✅ SMS {} → {}", messageId, deliveryStatus);
                updateSendSmsCounters(messageId);
            } else {
                log.debug("[DLR] SMS non trouvé ou déjà delivered: {}", messageId);
            }
        } catch (Exception e) {
            log.error("[DLR] ❌ Erreur: {}", e.getMessage());
        }
    }

    /**
     * ✅ MAPPER STATUT SMPP → STATUT INTERNE
     */
    private String mapSmppStatus(Integer smppStatus) {
        if (smppStatus == null) return "unknown";

        return switch (smppStatus) {
            case 2 -> "delivered"; // DELIVERED ✅
            case 6 -> "sent"; // ACCEPTED
            case 1 -> "submitted"; // ENROUTE
            case 5, 8 -> "failed"; // UNDELIVERABLE, REJECTED ❌
            case 3 -> "expired"; // EXPIRED
            default -> "unknown";
        };
    }

    /**
     * ✅ CONSTRUIRE MESSAGE D'ERREUR
     */
    private String buildErrorMessage(Integer smppStatus, String errorCode) {
        if (smppStatus == null || smppStatus == 2) return null;

        String statusName =
            switch (smppStatus) {
                case 3 -> "Expired";
                case 5 -> "Undeliverable";
                case 8 -> "Rejected";
                default -> null;
            };

        if (statusName != null && errorCode != null && !"000".equals(errorCode)) {
            return statusName + " (err:" + errorCode + ")";
        }
        return statusName;
    }

    /**
     * ✅ METTRE À JOUR COMPTEURS SEND_SMS
     */
    private void updateSendSmsCounters(String messageId) {
        try {
            jdbcTemplate.update(
                """
                UPDATE send_sms SET
                    total_delivered = (SELECT COUNT(*) FROM sms WHERE send_sms_id = send_sms.id AND delivery_status = 'delivered'),
                    total_success = (SELECT COUNT(*) FROM sms WHERE send_sms_id = send_sms.id AND delivery_status IN ('delivered', 'sent')),
                    total_failed = (SELECT COUNT(*) FROM sms WHERE send_sms_id = send_sms.id AND delivery_status IN ('failed', 'expired'))
                WHERE id = (SELECT send_sms_id FROM sms WHERE message_id = ? LIMIT 1)
                """,
                messageId
            );
        } catch (Exception e) {
            log.warn("[DLR] Erreur compteurs: {}", e.getMessage());
        }
    }

    /**
     * ✅ STATISTIQUES DLR
     */
    public Map<String, Object> getDlrStatistics() {
        Map<String, Object> stats = new HashMap<>();
        try {
            stats.put("delivered", jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sms WHERE delivery_status = 'delivered'", Long.class));
            stats.put("sent", jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sms WHERE delivery_status = 'sent'", Long.class));
            stats.put("submitted", jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sms WHERE delivery_status = 'submitted'", Long.class));
            stats.put(
                "failed",
                jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sms WHERE delivery_status IN ('failed', 'expired')", Long.class)
            );
        } catch (Exception e) {
            log.error("[DLR] Erreur stats: {}", e.getMessage());
        }
        return stats;
    }
}
