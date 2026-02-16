package com.example.myproject.web.rest;

import com.example.myproject.service.SmsMonitoringService;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * ✅ CONTRÔLEUR : Monitoring SMS (version moderne)
 */
@RestController
@RequestMapping("/api/sms-monitoring")
public class SmsMonitoringController {

    private static final Logger log = LoggerFactory.getLogger(SmsMonitoringController.class);

    @Autowired
    private SmsMonitoringService monitoringService; // ✅ NOUVEAU

    /**
     * ✅ STATISTIQUES GLOBALES
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getPerformanceStats() {
        try {
            log.info("[MONITORING] Demande stats globales");
            Map<String, Object> stats = monitoringService.getPerformanceStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("[MONITORING] Erreur stats: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * ✅ STATISTIQUES D'UNE CAMPAGNE
     */
    @GetMapping("/campaign/{sendSmsId}")
    public ResponseEntity<Map<String, Object>> getCampaignStats(@PathVariable Long sendSmsId) {
        try {
            log.info("[MONITORING] Stats campagne {}", sendSmsId);
            Map<String, Object> stats = monitoringService.getCampaignStats(sendSmsId);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("[MONITORING] Erreur: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * ✅ RÉINITIALISER COMPTEURS SESSION
     */
    @PostMapping("/stats/reset")
    public ResponseEntity<Map<String, Object>> resetStats() {
        try {
            log.info("[MONITORING] Reset compteurs");
            monitoringService.resetSessionStats();

            return ResponseEntity.ok(Map.of("message", "Compteurs réinitialisés", "timestamp", System.currentTimeMillis()));
        } catch (Exception e) {
            log.error("[MONITORING] Erreur reset: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * ✅ DEBUG D'UNE CAMPAGNE
     */
    @GetMapping("/debug/{bulkId}")
    public ResponseEntity<Map<String, Object>> debugCampaign(@PathVariable String bulkId) {
        try {
            log.info("[MONITORING] Debug bulkId: {}", bulkId);
            Map<String, Object> debug = monitoringService.debugCampaign(bulkId);
            return ResponseEntity.ok(debug);
        } catch (Exception e) {
            log.error("[MONITORING] Erreur debug: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * ✅ SANTÉ DU SYSTÈME
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getSystemHealth() {
        try {
            Map<String, Object> health = monitoringService.getSystemHealth();
            return ResponseEntity.ok(health);
        } catch (Exception e) {
            log.error("[MONITORING] Erreur health: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * ✅ CAMPAGNES ACTIVES
     */
    @GetMapping("/campaigns/active")
    public ResponseEntity<Map<String, Object>> getActiveCampaigns() {
        try {
            Map<String, Object> campaigns = monitoringService.getActiveCampaigns();
            return ResponseEntity.ok(campaigns);
        } catch (Exception e) {
            log.error("[MONITORING] Erreur: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * ✅ DERNIÈRES CAMPAGNES TERMINÉES
     */
    @GetMapping("/campaigns/completed")
    public ResponseEntity<Map<String, Object>> getCompletedCampaigns(@RequestParam(defaultValue = "10") int limit) {
        try {
            Map<String, Object> campaigns = monitoringService.getRecentCompletedCampaigns(limit);
            return ResponseEntity.ok(campaigns);
        } catch (Exception e) {
            log.error("[MONITORING] Erreur: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * ✅ ARRÊTER UNE CAMPAGNE
     */
    @PostMapping("/campaign/{sendSmsId}/stop")
    public ResponseEntity<Map<String, Object>> stopCampaign(@PathVariable Long sendSmsId) {
        try {
            log.warn("[MONITORING] Arrêt campagne {}", sendSmsId);
            boolean stopped = monitoringService.stopCampaign(sendSmsId);

            return ResponseEntity.ok(
                Map.of(
                    "sendSmsId",
                    sendSmsId,
                    "stopped",
                    stopped,
                    "message",
                    stopped ? "Campagne arrêtée" : "Échec arrêt",
                    "timestamp",
                    System.currentTimeMillis()
                )
            );
        } catch (Exception e) {
            log.error("[MONITORING] Erreur: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * ✅ PING
     */
    @GetMapping("/ping")
    public ResponseEntity<Map<String, Object>> ping() {
        return ResponseEntity.ok(Map.of("message", "pong", "timestamp", System.currentTimeMillis(), "version", "3.0"));
    }

    /**
     * ✅ CONFIGURATION
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getConfiguration() {
        try {
            Map<String, Object> stats = monitoringService.getPerformanceStats();

            return ResponseEntity.ok(
                Map.of(
                    "targetRatePerSecond",
                    stats.get("targetRatePerSecond"),
                    "parallelWorkers",
                    stats.get("parallelWorkers"),
                    "realtimeUpdates",
                    true,
                    "timestamp",
                    System.currentTimeMillis()
                )
            );
        } catch (Exception e) {
            log.error("[MONITORING] Erreur config: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}
