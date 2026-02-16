package com.example.myproject.service;

import com.example.myproject.domain.SendSms;
import com.example.myproject.repository.SendSmsRepository;
import com.example.myproject.repository.SmsRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * ✅ SERVICE DÉDIÉ : Monitoring et statistiques SMS
 * - Statistiques temps réel
 * - Santé du système
 * - Debug des campagnes
 */
@Service
public class SmsMonitoringService {

    private static final Logger log = LoggerFactory.getLogger(SmsMonitoringService.class);

    @Autowired
    private SmsRepository smsRepository;

    @Autowired
    private SendSmsRepository sendSmsRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SmsBulkService smsBulkService;

    @Value("${sms.bulk.target-rate:50.0}")
    private double targetRatePerSecond;

    @Value("${sms.bulk.parallel-workers:20}")
    private int parallelWorkers;

    // ✅ COMPTEURS GLOBAUX (pour stats)
    private final AtomicLong totalProcessedGlobal = new AtomicLong(0);
    private final AtomicLong totalSuccessGlobal = new AtomicLong(0);
    private final AtomicLong totalFailedGlobal = new AtomicLong(0);
    private final ConcurrentHashMap<Long, CampaignStats> activeCampaigns = new ConcurrentHashMap<>();

    /**
     * ✅ OBTENIR STATISTIQUES GLOBALES
     */
    public Map<String, Object> getPerformanceStats() {
        Map<String, Object> stats = new HashMap<>();

        try {
            // Stats globales (tous les SMS)
            long totalSms = smsRepository.count();
            long totalSent = smsRepository.countByDeliveryStatus("sent");
            long totalFailed = smsRepository.countByDeliveryStatus("failed");
            long totalPending = smsRepository.countByDeliveryStatus("pending");

            // Stats des campagnes actives
            long activeCampaigns = sendSmsRepository.countByInprocess(true);
            long completedCampaigns = sendSmsRepository.countByInprocess(false);

            // Calcul taux de succès
            double successRate = totalSms > 0 ? (totalSent * 100.0) / totalSms : 0.0;
            double failureRate = totalSms > 0 ? (totalFailed * 100.0) / totalSms : 0.0;

            // Stats session actuelle
            stats.put("sessionProcessed", totalProcessedGlobal.get());
            stats.put("sessionSuccess", totalSuccessGlobal.get());
            stats.put("sessionFailed", totalFailedGlobal.get());

            // Stats globales DB
            stats.put("totalSms", totalSms);
            stats.put("totalSent", totalSent);
            stats.put("totalFailed", totalFailed);
            stats.put("totalPending", totalPending);

            // Stats campagnes
            stats.put("activeCampaigns", activeCampaigns);
            stats.put("completedCampaigns", completedCampaigns);

            // Taux
            stats.put("successRate", Math.round(successRate * 100.0) / 100.0);
            stats.put("failureRate", Math.round(failureRate * 100.0) / 100.0);

            // Configuration
            stats.put("targetRatePerSecond", targetRatePerSecond);
            stats.put("parallelWorkers", parallelWorkers);

            // Timestamp
            stats.put("timestamp", System.currentTimeMillis());
            stats.put("serverTime", Instant.now().toString());

            log.debug("[MONITORING] Stats: sent={}, failed={}, pending={}", totalSent, totalFailed, totalPending);
        } catch (Exception e) {
            log.error("[MONITORING] Erreur stats: {}", e.getMessage());
            stats.put("error", e.getMessage());
        }

        return stats;
    }

    /**
     * ✅ STATISTIQUES D'UNE CAMPAGNE SPÉCIFIQUE
     */
    public Map<String, Object> getCampaignStats(Long sendSmsId) {
        Map<String, Object> stats = new HashMap<>();

        try {
            var sendSms = sendSmsRepository.findById(sendSmsId).orElse(null);

            if (sendSms == null) {
                stats.put("error", "Campagne non trouvée");
                return stats;
            }

            String bulkId = sendSms.getBulkId();

            // Stats depuis DB
            long totalSms = smsRepository.countByBulkId(bulkId);
            long sent = smsRepository.countByBulkIdAndDeliveryStatus(bulkId, "sent");
            long failed = smsRepository.countByBulkIdAndDeliveryStatus(bulkId, "failed");
            long pending = smsRepository.countByBulkIdAndDeliveryStatus(bulkId, "pending");

            stats.put("sendSmsId", sendSmsId);
            stats.put("bulkId", bulkId);
            stats.put("totalSms", totalSms);
            stats.put("sent", sent);
            stats.put("failed", failed);
            stats.put("pending", pending);
            stats.put("inprocess", sendSms.getInprocess());
            stats.put("retryCount", sendSms.getRetryCount());

            // Taux
            double successRate = totalSms > 0 ? (sent * 100.0) / totalSms : 0.0;
            stats.put("successRate", Math.round(successRate * 100.0) / 100.0);

            // Progression
            double progress = totalSms > 0 ? ((sent + failed) * 100.0) / totalSms : 0.0;
            stats.put("progress", Math.round(progress * 100.0) / 100.0);
        } catch (Exception e) {
            log.error("[MONITORING] Erreur stats campagne {}: {}", sendSmsId, e.getMessage());
            stats.put("error", e.getMessage());
        }

        return stats;
    }

    /**
     * ✅ SANTÉ DU SYSTÈME
     */
    public Map<String, Object> getSystemHealth() {
        Map<String, Object> health = new HashMap<>();

        try {
            Map<String, Object> stats = getPerformanceStats();

            double successRate = (Double) stats.getOrDefault("successRate", 0.0);
            long activeCampaigns = (Long) stats.getOrDefault("activeCampaigns", 0L);
            long totalPending = (Long) stats.getOrDefault("totalPending", 0L);

            // Déterminer santé
            String healthStatus;
            String healthColor;

            if (successRate >= 95 && activeCampaigns < 10) {
                healthStatus = "EXCELLENT";
                healthColor = "green";
            } else if (successRate >= 85 && activeCampaigns < 20) {
                healthStatus = "GOOD";
                healthColor = "blue";
            } else if (successRate >= 70 && activeCampaigns < 50) {
                healthStatus = "WARNING";
                healthColor = "yellow";
            } else {
                healthStatus = "CRITICAL";
                healthColor = "red";
            }

            health.put("status", healthStatus);
            health.put("color", healthColor);
            health.put("successRate", successRate);
            health.put("activeCampaigns", activeCampaigns);
            health.put("totalPending", totalPending);
            health.put("timestamp", System.currentTimeMillis());
            health.put("uptime", getUptime());
        } catch (Exception e) {
            log.error("[MONITORING] Erreur health: {}", e.getMessage());
            health.put("status", "ERROR");
            health.put("color", "red");
            health.put("error", e.getMessage());
        }

        return health;
    }

    /**
     * ✅ DEBUG D'UNE CAMPAGNE
     */
    public Map<String, Object> debugCampaign(String bulkId) {
        Map<String, Object> debug = new HashMap<>();

        try {
            log.info("[DEBUG] Analyse campagne bulkId: {}", bulkId);

            // Répartition par statut
            String sql = "SELECT delivery_status, COUNT(*) as count FROM sms WHERE bulk_id = ? GROUP BY delivery_status";
            List<Map<String, Object>> statusBreakdown = jdbcTemplate.queryForList(sql, bulkId);

            debug.put("bulkId", bulkId);
            debug.put("statusBreakdown", statusBreakdown);

            // Derniers SMS
            String sqlRecent =
                """
                SELECT id, receiver, delivery_status, message_id, error_message
                FROM sms
                WHERE bulk_id = ?
                ORDER BY id DESC
                LIMIT 10
                """;
            List<Map<String, Object>> recentSms = jdbcTemplate.queryForList(sqlRecent, bulkId);
            debug.put("recentSms", recentSms);

            // ✅ GESTION PROPRE DE L'OPTIONAL
            sendSmsRepository
                .findOneByBulkId(bulkId)
                .ifPresentOrElse(
                    sendSms -> {
                        debug.put("sendSmsId", sendSms.getId());
                        debug.put("inprocess", sendSms.getInprocess());
                        debug.put("totalMessage", sendSms.getTotalMessage());
                        debug.put("totalSuccess", sendSms.getTotalSuccess());
                        debug.put("totalFailed", sendSms.getTotalFailed());
                        debug.put("totalPending", sendSms.getTotalPending());
                        debug.put("retryCount", sendSms.getRetryCount());
                        debug.put("successRate", sendSms.getSuccessRate());
                        debug.put("failureRate", sendSms.getFailureRate());
                        debug.put("lastRetryDate", sendSms.getLastRetryDate());
                    },
                    () -> {
                        debug.put("sendSmsId", null);
                        debug.put("warning", "SendSms non trouvé pour ce bulkId");
                    }
                );

            log.info("[DEBUG] Répartition: {}", statusBreakdown);
        } catch (Exception e) {
            log.error("[DEBUG] Erreur: {}", e.getMessage(), e);
            debug.put("error", e.getMessage());
        }

        return debug;
    }

    /**
     * ✅ RÉINITIALISER COMPTEURS SESSION
     */
    public void resetSessionStats() {
        totalProcessedGlobal.set(0);
        totalSuccessGlobal.set(0);
        totalFailedGlobal.set(0);
        activeCampaigns.clear();
        log.info("[MONITORING] Compteurs session réinitialisés");
    }

    /**
     * ✅ INCRÉMENTER COMPTEURS (appelé par SmsBulkService)
     */
    public void incrementProcessed(int count) {
        totalProcessedGlobal.addAndGet(count);
    }

    public void incrementSuccess(int count) {
        totalSuccessGlobal.addAndGet(count);
    }

    public void incrementFailed(int count) {
        totalFailedGlobal.addAndGet(count);
    }

    /**
     * ✅ CAMPAGNES EN COURS
     */
    public Map<String, Object> getActiveCampaigns() {
        Map<String, Object> result = new HashMap<>();

        try {
            String sql =
                """
                SELECT id, bulk_id, total_message, total_success, total_failed, total_pending,
                       retry_count, last_retry_date, created_date
                FROM send_sms
                WHERE inprocess = true
                ORDER BY last_retry_date DESC NULLS LAST
                LIMIT 20
                """;

            List<Map<String, Object>> campaigns = jdbcTemplate.queryForList(sql);

            result.put("count", campaigns.size());
            result.put("campaigns", campaigns);
            result.put("timestamp", System.currentTimeMillis());
        } catch (Exception e) {
            log.error("[MONITORING] Erreur active campaigns: {}", e.getMessage());
            result.put("error", e.getMessage());
        }

        return result;
    }

    /**
     * ✅ DERNIÈRES CAMPAGNES TERMINÉES
     */
    public Map<String, Object> getRecentCompletedCampaigns(int limit) {
        Map<String, Object> result = new HashMap<>();

        try {
            String sql =
                """
                SELECT id, bulk_id, total_message, total_success, total_failed,
                       success_rate, failure_rate, created_date, last_retry_date
                FROM send_sms
                WHERE inprocess = false
                ORDER BY last_retry_date DESC NULLS LAST, created_date DESC
                LIMIT ?
                """;

            List<Map<String, Object>> campaigns = jdbcTemplate.queryForList(sql, limit);

            result.put("count", campaigns.size());
            result.put("campaigns", campaigns);
            result.put("timestamp", System.currentTimeMillis());
        } catch (Exception e) {
            log.error("[MONITORING] Erreur completed campaigns: {}", e.getMessage());
            result.put("error", e.getMessage());
        }

        return result;
    }

    /**
     * ✅ ARRÊTER UNE CAMPAGNE
     */
    public boolean stopCampaign(Long sendSmsId) {
        try {
            log.warn("[MONITORING] Demande arrêt campagne {}", sendSmsId);
            return smsBulkService.stopBulkProcessing(sendSmsId);
        } catch (Exception e) {
            log.error("[MONITORING] Erreur arrêt: {}", e.getMessage());
            return false;
        }
    }

    // ===== UTILITAIRES =====

    private String getUptime() {
        // Simpliste: depuis le démarrage de la JVM
        long uptimeMs = System.currentTimeMillis() - java.lang.management.ManagementFactory.getRuntimeMXBean().getStartTime();
        long seconds = uptimeMs / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        return String.format("%dd %dh %dm", days, hours % 24, minutes % 60);
    }

    // Classe interne pour stats campagne
    private static class CampaignStats {

        int processed;
        int success;
        int failed;
        Instant startTime;

        CampaignStats() {
            this.startTime = Instant.now();
        }
    }
}
