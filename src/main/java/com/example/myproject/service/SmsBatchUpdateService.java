package com.example.myproject.service;

import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SmsBatchUpdateService {

    private static final Logger log = LoggerFactory.getLogger(SmsBatchUpdateService.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void batchUpdateSuccess(List<SmsUpdateResult> results) {
        if (results.isEmpty()) return;

        String sql =
            """
            UPDATE sms SET
                delivery_status = 'sent',
                status = 'SENT',
                message_id = ?,
                send_date = ?,
                is_sent = true
            WHERE id = ?
            """;

        List<Object[]> args = results
            .stream()
            .map(r -> new Object[] { r.messageId, java.sql.Timestamp.from(r.timestamp), r.smsId })
            .toList();

        log.info("💾 Mise à jour {} SMS en succès : {}", results.size(), results.stream().map(r -> r.smsId).toList());

        int[] updated = jdbcTemplate.batchUpdate(sql, args);

        log.info("✅ {} lignes mises à jour sur {} prévues", java.util.Arrays.stream(updated).sum(), results.size());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void batchUpdateFailed(List<SmsUpdateResult> results) {
        if (results.isEmpty()) return;

        String sql =
            """
            UPDATE sms SET
                delivery_status = 'failed',
                status = 'FAILED',
                last_error = ?,
                send_date = ?,
                is_sent = false
            WHERE id = ?
            """;

        List<Object[]> args = results
            .stream()
            .map(r -> new Object[] { truncate(r.error, 500), java.sql.Timestamp.from(r.timestamp), r.smsId })
            .toList();

        log.info("💾 Mise à jour {} SMS en échec : {}", results.size(), results.stream().map(r -> r.smsId).toList());

        int[] updated = jdbcTemplate.batchUpdate(sql, args);

        log.info("❌ {} lignes mises à jour sur {} prévues", java.util.Arrays.stream(updated).sum(), results.size());
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return "Unknown error";
        return s.length() > maxLen ? s.substring(0, maxLen) : s;
    }

    public static class SmsUpdateResult {

        public long smsId;
        public boolean success;
        public String messageId;
        public String error;
        public Instant timestamp;

        public SmsUpdateResult(long smsId, boolean success, String messageId, String error, Instant timestamp) {
            this.smsId = smsId;
            this.success = success;
            this.messageId = messageId;
            this.error = error;
            this.timestamp = timestamp;
        }
    }
}
