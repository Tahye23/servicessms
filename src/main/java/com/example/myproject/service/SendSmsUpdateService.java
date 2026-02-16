package com.example.myproject.service;

import com.example.myproject.domain.SendSms;
import com.example.myproject.repository.SendSmsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service dédié aux mises à jour atomiques de SendSms
 * Utilise REQUIRES_NEW pour garantir l'isolation des transactions
 */
@Service
public class SendSmsUpdateService {

    private static final Logger log = LoggerFactory.getLogger(SendSmsUpdateService.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SendSmsRepository sendSmsRepository;

    /**
     * Incrémente le compteur de succès de manière atomique
     * Transaction indépendante pour éviter les rollbacks
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void incrementSuccess(Long sendSmsId) {
        try {
            String sql =
                """
                UPDATE send_sms
                SET total_sent = COALESCE(total_sent, 0) + 1,
                    total_success = COALESCE(total_success, 0) + 1,
                    total_pending = GREATEST(0, COALESCE(total_pending, 0) - 1)
                WHERE id = ?
                """;

            int updated = jdbcTemplate.update(sql, sendSmsId);

            if (updated > 0) {
                log.debug("[SENDSMS-UPDATE] Succès incrémenté pour SendSms {}", sendSmsId);
            } else {
                log.warn("[SENDSMS-UPDATE] Aucune ligne mise à jour pour SendSms {}", sendSmsId);
            }
        } catch (Exception e) {
            log.error("[SENDSMS-UPDATE] Erreur increment success pour {}: {}", sendSmsId, e.getMessage());
        }
    }

    /**
     * Incrémente le compteur d'échec de manière atomique
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void incrementFailed(Long sendSmsId) {
        try {
            String sql =
                """
                UPDATE send_sms
                SET total_failed = COALESCE(total_failed, 0) + 1,
                    total_pending = GREATEST(0, COALESCE(total_pending, 0) - 1)
                WHERE id = ?
                """;

            int updated = jdbcTemplate.update(sql, sendSmsId);

            if (updated > 0) {
                log.debug("[SENDSMS-UPDATE] Échec incrémenté pour SendSms {}", sendSmsId);
            } else {
                log.warn("[SENDSMS-UPDATE] Aucune ligne mise à jour pour SendSms {}", sendSmsId);
            }
        } catch (Exception e) {
            log.error("[SENDSMS-UPDATE] Erreur increment failed pour {}: {}", sendSmsId, e.getMessage());
        }
    }

    /**
     * Incrémente plusieurs compteurs en une seule opération atomique
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void incrementCounters(Long sendSmsId, int successDelta, int failDelta) {
        try {
            String sql =
                """
                UPDATE send_sms
                SET total_sent = COALESCE(total_sent, 0) + ?,
                    total_success = COALESCE(total_success, 0) + ?,
                    total_failed = COALESCE(total_failed, 0) + ?,
                    total_pending = GREATEST(0, COALESCE(total_pending, 0) - ?)
                WHERE id = ?
                """;

            int totalDelta = successDelta + failDelta;
            int updated = jdbcTemplate.update(sql, totalDelta, successDelta, failDelta, totalDelta, sendSmsId);

            if (updated > 0) {
                log.debug(
                    "[SENDSMS-UPDATE] Compteurs mis à jour pour SendSms {}: +{} succès, +{} échecs",
                    sendSmsId,
                    successDelta,
                    failDelta
                );
            }
        } catch (Exception e) {
            log.error("[SENDSMS-UPDATE] Erreur increment counters pour {}: {}", sendSmsId, e.getMessage());
        }
    }

    /**
     * Marque le SendSms comme terminé (arrêté ou complété)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markAsCompleted(Long sendSmsId, String lastError) {
        try {
            String sql =
                """
                UPDATE send_sms
                SET inprocess = false,
                    last_error = ?
                WHERE id = ?
                """;

            int updated = jdbcTemplate.update(sql, lastError, sendSmsId);

            if (updated > 0) {
                log.info("[SENDSMS-UPDATE] SendSms {} marqué comme terminé", sendSmsId);
            } else {
                log.warn("[SENDSMS-UPDATE] Impossible de marquer comme terminé SendSms {}", sendSmsId);
            }
        } catch (Exception e) {
            log.error("[SENDSMS-UPDATE] Erreur mark completed pour {}: {}", sendSmsId, e.getMessage());
        }
    }

    /**
     * Marque le SendSms comme en cours de traitement
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markAsInProgress(Long sendSmsId) {
        try {
            String sql =
                """
                UPDATE send_sms
                SET inprocess = true,
                    last_error = NULL
                WHERE id = ?
                """;

            int updated = jdbcTemplate.update(sql, sendSmsId);

            if (updated > 0) {
                log.info("[SENDSMS-UPDATE] SendSms {} marqué comme en cours", sendSmsId);
            }
        } catch (Exception e) {
            log.error("[SENDSMS-UPDATE] Erreur mark in progress pour {}: {}", sendSmsId, e.getMessage());
        }
    }

    /**
     * Mise à jour finale avec calcul des taux de succès/échec
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateFinalCounters(Long sendSmsId, int successCount, int failCount, String lastError) {
        try {
            int total = successCount + failCount;
            double successRate = total > 0 ? (successCount * 100.0) / total : 0;
            double failureRate = total > 0 ? (failCount * 100.0) / total : 0;

            String sql =
                """
                UPDATE send_sms
                SET total_success = ?,
                    success_rate = ?,
                    failure_rate = ?,
                    is_sent = ?,
                    last_error = ?,
                    inprocess = false
                WHERE id = ?
                """;

            int updated = jdbcTemplate.update(sql, successCount, successRate, failureRate, successCount > 0, lastError, sendSmsId);

            if (updated > 0) {
                log.info(
                    "[SENDSMS-UPDATE] SendSms {} finalisé: {} succès, {} échecs (taux: {:.2f}%)",
                    sendSmsId,
                    successCount,
                    failCount,
                    successRate
                );
            } else {
                log.warn("[SENDSMS-UPDATE] Échec finalisation SendSms {}", sendSmsId);
            }
        } catch (Exception e) {
            log.error("[SENDSMS-UPDATE] Erreur update final counters pour {}: {}", sendSmsId, e.getMessage());
        }
    }

    /**
     * Mise à jour du compteur total_pending uniquement
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updatePendingCount(Long sendSmsId, int pendingCount) {
        try {
            String sql =
                """
                UPDATE send_sms
                SET total_pending = ?
                WHERE id = ?
                """;

            int updated = jdbcTemplate.update(sql, pendingCount, sendSmsId);

            if (updated > 0) {
                log.debug("[SENDSMS-UPDATE] Pending mis à jour pour SendSms {}: {}", sendSmsId, pendingCount);
            }
        } catch (Exception e) {
            log.error("[SENDSMS-UPDATE] Erreur update pending pour {}: {}", sendSmsId, e.getMessage());
        }
    }

    /**
     * Réinitialise les compteurs pour un nouveau retry
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void resetCountersForRetry(Long sendSmsId, int totalPending) {
        try {
            String sql =
                """
                UPDATE send_sms
                SET total_success = 0,
                    total_failed = 0,
                    total_pending = ?,
                    inprocess = true,
                    is_sent = NULL,
                    last_error = NULL
                WHERE id = ?
                """;

            int updated = jdbcTemplate.update(sql, totalPending, sendSmsId);

            if (updated > 0) {
                log.info("[SENDSMS-UPDATE] Compteurs réinitialisés pour retry SendSms {}: {} pending", sendSmsId, totalPending);
            }
        } catch (Exception e) {
            log.error("[SENDSMS-UPDATE] Erreur reset counters pour {}: {}", sendSmsId, e.getMessage());
        }
    }

    /**
     * Met à jour uniquement le message d'erreur
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateLastError(Long sendSmsId, String errorMessage) {
        try {
            String sql =
                """
                UPDATE send_sms
                SET last_error = ?
                WHERE id = ?
                """;

            int updated = jdbcTemplate.update(sql, errorMessage, sendSmsId);

            if (updated > 0) {
                log.debug("[SENDSMS-UPDATE] Erreur mise à jour pour SendSms {}", sendSmsId);
            }
        } catch (Exception e) {
            log.error("[SENDSMS-UPDATE] Erreur update last error pour {}: {}", sendSmsId, e.getMessage());
        }
    }

    /**
     * Récupère les compteurs actuels (méthode de lecture)
     */
    @Transactional(readOnly = true)
    public SendSmsCounters getCounters(Long sendSmsId) {
        try {
            String sql =
                """
                SELECT
                    COALESCE(total_sent, 0) as total_sent,
                    COALESCE(total_success, 0) as total_success,
                    COALESCE(total_failed, 0) as total_failed,
                    COALESCE(total_pending, 0) as total_pending,
                    inprocess
                FROM send_sms
                WHERE id = ?
                """;

            return jdbcTemplate.queryForObject(
                sql,
                (rs, rowNum) ->
                    new SendSmsCounters(
                        rs.getInt("total_sent"),
                        rs.getInt("total_success"),
                        rs.getInt("total_failed"),
                        rs.getInt("total_pending"),
                        rs.getBoolean("inprocess")
                    ),
                sendSmsId
            );
        } catch (Exception e) {
            log.error("[SENDSMS-UPDATE] Erreur get counters pour {}: {}", sendSmsId, e.getMessage());
            return new SendSmsCounters(0, 0, 0, 0, false);
        }
    }

    /**
     * Vérifie si un SendSms est en cours de traitement
     */
    @Transactional(readOnly = true)
    public boolean isInProcess(Long sendSmsId) {
        try {
            String sql = "SELECT inprocess FROM send_sms WHERE id = ?";
            Boolean inProcess = jdbcTemplate.queryForObject(sql, Boolean.class, sendSmsId);
            return Boolean.TRUE.equals(inProcess);
        } catch (Exception e) {
            log.error("[SENDSMS-UPDATE] Erreur is in process pour {}: {}", sendSmsId, e.getMessage());
            return false;
        }
    }

    /**
     * Classe interne pour encapsuler les compteurs
     */
    public static class SendSmsCounters {

        private final int totalSent;
        private final int totalSuccess;
        private final int totalFailed;
        private final int totalPending;
        private final boolean inProcess;

        public SendSmsCounters(int totalSent, int totalSuccess, int totalFailed, int totalPending, boolean inProcess) {
            this.totalSent = totalSent;
            this.totalSuccess = totalSuccess;
            this.totalFailed = totalFailed;
            this.totalPending = totalPending;
            this.inProcess = inProcess;
        }

        public int getTotalSent() {
            return totalSent;
        }

        public int getTotalSuccess() {
            return totalSuccess;
        }

        public int getTotalFailed() {
            return totalFailed;
        }

        public int getTotalPending() {
            return totalPending;
        }

        public boolean isInProcess() {
            return inProcess;
        }

        @Override
        public String toString() {
            return String.format(
                "SendSmsCounters{sent=%d, success=%d, failed=%d, pending=%d, inProcess=%s}",
                totalSent,
                totalSuccess,
                totalFailed,
                totalPending,
                inProcess
            );
        }
    }
}
