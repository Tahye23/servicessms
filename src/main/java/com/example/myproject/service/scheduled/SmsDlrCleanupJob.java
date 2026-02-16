package com.example.myproject.service.scheduled;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SmsDlrCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(SmsDlrCleanupJob.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * ✅ MARQUER SMS SANS DLR APRÈS 24H
     * Tous les jours à 4h du matin
     */
    @Scheduled(cron = "0 0 4 * * ?")
    public void markStaleSubmittedAsUnknown() {
        try {
            int updated = jdbcTemplate.update(
                """
                UPDATE sms SET
                    delivery_status = 'unknown',
                    last_error = 'No DLR received after 24h'
                WHERE delivery_status = 'submitted'
                AND send_date < NOW() - INTERVAL '24 hours'
                """
            );

            if (updated > 0) {
                log.info("[DLR-CLEANUP] {} SMS sans DLR marqués 'unknown'", updated);
            }
        } catch (Exception e) {
            log.error("[DLR-CLEANUP] Erreur: {}", e.getMessage());
        }
    }
}
