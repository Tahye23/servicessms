package com.example.myproject.service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SmsUpdateService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final Logger log = LoggerFactory.getLogger(SmsUpdateService.class);

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean updateSmsStatus(Long smsId, boolean success, String messageId, String deliveryStatus, String errorMessage) {
        log.info("[SMS-UPDATE-SERVICE] Mise à jour SMS {} -> success={}", smsId, success);

        try {
            String sql =
                """
                UPDATE sms
                SET status = ?,
                    is_sent = ?,
                    message_id = ?,
                    delivery_status = ?,
                    send_date = ?,
                    last_error = ?
                WHERE id = ?
                """;

            int updated = jdbcTemplate.update(
                sql,
                success ? "SENT" : "FAILED",
                success,
                messageId,
                deliveryStatus,
                Timestamp.from(Instant.now()),
                errorMessage,
                smsId
            );

            log.info("[SMS-UPDATE-SERVICE] SMS {} -> {} lignes mises à jour", smsId, updated);

            if (updated > 0) {
                // Vérification immédiate
                String verifSql = "SELECT status, is_sent FROM sms WHERE id = ?";
                Map<String, Object> result = jdbcTemplate.queryForMap(verifSql, smsId);
                log.info(
                    "[SMS-UPDATE-SERVICE] Vérification SMS {}: status={}, is_sent={}",
                    smsId,
                    result.get("status"),
                    result.get("is_sent")
                );
                return true;
            }

            return false;
        } catch (Exception e) {
            log.error("[SMS-UPDATE-SERVICE] Erreur update SMS {}: {}", smsId, e.getMessage(), e);
            return false;
        }
    }
}
