package com.example.myproject.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class SmsStatusJob {

    private static final Logger log = LoggerFactory.getLogger(SmsStatusJob.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * ✅ Toutes les 2 minutes : marquer "submitted" → "sent"
     */
    @Scheduled(fixedRate = 120_000)
    public void markSubmittedAsSent() {
        int updated = jdbcTemplate.update(
            """
                UPDATE sms SET
                    delivery_status = 'sent',
                    status = 'SENT',
                    is_sent = true
                WHERE delivery_status = 'submitted'
                  AND send_date < NOW() - INTERVAL '1 minute'
            """
        );

        if (updated > 0) {
            log.info("✅ {} SMS marqués 'sent'", updated);
            updateSendSmsCounters();
        }
    }

    /**
     * ✅ Recalculer compteurs campagnes
     */
    private void updateSendSmsCounters() {
        jdbcTemplate.update(
            """
                UPDATE send_sms ss SET
                    total_success = (SELECT COUNT(*) FROM sms WHERE bulk_id = ss.bulk_id AND delivery_status = 'sent'),
                    total_failed = (SELECT COUNT(*) FROM sms WHERE bulk_id = ss.bulk_id AND delivery_status = 'failed'),
                    total_pending = (SELECT COUNT(*) FROM sms WHERE bulk_id = ss.bulk_id AND delivery_status IN ('pending', 'submitted')),
                    is_sent = CASE
                        WHEN (SELECT COUNT(*) FROM sms WHERE bulk_id = ss.bulk_id AND delivery_status IN ('pending', 'submitted')) = 0
                        THEN true ELSE false END
                WHERE bulk_id IS NOT NULL
            """
        );
    }
}
