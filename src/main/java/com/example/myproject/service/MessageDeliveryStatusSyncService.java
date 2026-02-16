package com.example.myproject.service;

import com.example.myproject.domain.Abonnement;
import com.example.myproject.domain.SendSms;
import com.example.myproject.domain.enumeration.MessageType;
import com.example.myproject.repository.AbonnementRepository;
import com.example.myproject.repository.SendSmsRepository;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MessageDeliveryStatusSyncService {

    private final Logger log = LoggerFactory.getLogger(MessageDeliveryStatusSyncService.class);
    private static final int BATCH_SIZE = 5000;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AbonnementRepository abonnementRepository;

    @Autowired
    private SendSmsRepository sendSmsRepository;

    @Autowired
    private AbonnementService abonnementService;

    private final AtomicBoolean running = new AtomicBoolean(false);

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void syncMessageDeliveryStatus() {
        if (!running.compareAndSet(false, true)) {
            log.warn("Schedule déjà en cours, on saute cette exécution !");
            return;
        }

        long start = System.currentTimeMillis();
        List<Long> idsToProcess = List.of();

        try {
            // 1. Récupération des IDs de message_delivery_status à traiter
            idsToProcess = jdbcTemplate.queryForList(
                """
                SELECT mds.id
                FROM message_delivery_status mds
                JOIN sms s ON s.message_id = mds.message_id
                WHERE mds.processed_at IS NULL
                LIMIT ?
                """,
                Long.class,
                BATCH_SIZE
            );

            if (idsToProcess.isEmpty()) return;

            log.info(">>> Début du schedule syncMessageDeliveryStatus ({} lignes à traiter) à {}", idsToProcess.size(), Instant.now());

            // 2. Récupération des send_sms_id concernés
            Set<Long> batchIds = new HashSet<>();
            for (Long statusId : idsToProcess) {
                Long sendSmsId = jdbcTemplate.queryForObject(
                    """
                    SELECT s.send_sms_id
                    FROM sms s
                    JOIN message_delivery_status mds ON s.message_id = mds.message_id
                    WHERE mds.id = ?
                    """,
                    Long.class,
                    statusId
                );
                if (sendSmsId != null) batchIds.add(sendSmsId);
            }

            // ✅ 3. NOUVEAU : Détecter les échecs AVANT mise à jour
            Map<Long, List<FailedMessageInfo>> failedMessagesBySendSms = detectFailedMessages(idsToProcess);

            // 4. Mise à jour des statuts dans sms
            for (Long statusId : idsToProcess) {
                jdbcTemplate.update(
                    """
                    UPDATE sms s
                    SET delivery_status = mds.status,
                        last_error = CASE
                            WHEN mds.error_title IS NOT NULL THEN mds.error_title || ' - ' || mds.error_details
                            ELSE NULL
                        END
                    FROM message_delivery_status mds
                    WHERE s.message_id = mds.message_id
                      AND mds.id = ?
                    """,
                    statusId
                );
            }

            // 5. Marquer les statuts comme traités
            for (Long statusId : idsToProcess) {
                jdbcTemplate.update(
                    """
                    UPDATE message_delivery_status
                    SET processed_at = now()
                    WHERE id = ?
                    """,
                    statusId
                );
            }

            // ✅ 6. NOUVEAU : Rembourser les échecs par send_sms_id
            if (!failedMessagesBySendSms.isEmpty()) {
                refundFailedMessages(failedMessagesBySendSms);
            }

            // 7. Mettre à jour send_sms (totaux + delivery_status)
            for (Long sendSmsId : batchIds) {
                // 7.1 Mise à jour des totaux
                jdbcTemplate.update(
                    """
                    UPDATE send_sms s
                    SET
                        total_sent = COALESCE((SELECT COUNT(*) FROM sms WHERE send_sms_id = s.id AND delivery_status = 'sent'), 0),
                        total_delivered = COALESCE((SELECT COUNT(*) FROM sms WHERE send_sms_id = s.id AND delivery_status = 'delivered'), 0),
                        total_read = COALESCE((SELECT COUNT(*) FROM sms WHERE send_sms_id = s.id AND delivery_status = 'read'), 0),
                        total_pending = COALESCE((SELECT COUNT(*) FROM sms WHERE send_sms_id = s.id AND delivery_status = 'pending'), 0),
                        total_failed = COALESCE((SELECT COUNT(*) FROM sms WHERE send_sms_id = s.id AND delivery_status = 'failed'), 0)
                    WHERE s.id = ?
                    """,
                    sendSmsId
                );

                // 7.2 Mise à jour du statut global
                jdbcTemplate.update(
                    """
                    UPDATE send_sms s SET delivery_status = sub.status
                    FROM (
                        SELECT
                            id,
                            total_sent,
                            total_delivered,
                            total_read,
                            total_pending,
                            total_failed,
                            total_sent + total_delivered + total_read + total_pending + total_failed AS total_all,
                            CASE
                                WHEN total_read > 0 AND total_read = total_sent + total_delivered + total_read + total_pending + total_failed THEN 'read'
                                WHEN total_delivered > 0 AND total_delivered = total_sent + total_delivered + total_read + total_pending + total_failed THEN 'delivered'
                                WHEN total_failed > 0 THEN 'failed'
                                WHEN total_pending > 0 THEN 'pending'
                                WHEN total_sent > 0 THEN 'sent'
                                ELSE 'pending'
                            END AS status
                        FROM send_sms
                        WHERE id = ?
                    ) sub
                    WHERE s.id = sub.id
                    """,
                    sendSmsId
                );
            }

            log.info("✅ Sync terminée : {} messages traités, {} send_sms mis à jour", idsToProcess.size(), batchIds.size());
        } catch (Exception e) {
            log.error("Erreur pendant le schedule syncMessageDeliveryStatus", e);
        } finally {
            running.set(false);
            if (!idsToProcess.isEmpty()) {
                log.info(
                    "<<< Fin du schedule syncMessageDeliveryStatus à {} (durée: {} ms)",
                    Instant.now(),
                    System.currentTimeMillis() - start
                );
            }
        }
    }

    /**
     * ✅ NOUVEAU : Détecter les messages qui vont passer en failed
     */
    private Map<Long, List<FailedMessageInfo>> detectFailedMessages(List<Long> statusIds) {
        if (statusIds.isEmpty()) {
            return Collections.emptyMap();
        }

        String sql =
            """
                SELECT
                    s.id as sms_id,
                    s.send_sms_id,
                    s.type,
                    s.total_message,
                    s.delivery_status as old_status,
                    mds.status as new_status
                FROM sms s
                JOIN message_delivery_status mds ON s.message_id = mds.message_id
                WHERE mds.id IN (%s)
                  AND s.delivery_status != 'failed'
                  AND mds.status = 'failed'
            """;

        // Construire la clause IN
        String placeholders = statusIds.stream().map(id -> "?").collect(Collectors.joining(","));

        String finalSql = String.format(sql, placeholders);

        List<FailedMessageInfo> failedMessages = jdbcTemplate.query(
            finalSql,
            statusIds.toArray(),
            (rs, rowNum) ->
                new FailedMessageInfo(
                    rs.getLong("sms_id"),
                    rs.getLong("send_sms_id"),
                    MessageType.valueOf(rs.getString("type")),
                    rs.getInt("total_message"),
                    rs.getString("old_status"),
                    rs.getString("new_status")
                )
        );

        // Grouper par send_sms_id
        return failedMessages.stream().collect(Collectors.groupingBy(FailedMessageInfo::getSendSmsId));
    }

    /**
     * ✅ NOUVEAU : Rembourser les messages échoués
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void refundFailedMessages(Map<Long, List<FailedMessageInfo>> failedMessagesBySendSms) {
        log.info("[REFUND] Début du remboursement pour {} send_sms", failedMessagesBySendSms.size());

        for (Map.Entry<Long, List<FailedMessageInfo>> entry : failedMessagesBySendSms.entrySet()) {
            Long sendSmsId = entry.getKey();
            List<FailedMessageInfo> failedMessages = entry.getValue();

            try {
                // Récupérer le SendSms
                SendSms sendSms = sendSmsRepository.findById(sendSmsId).orElse(null);
                if (sendSms == null || sendSms.getUser() == null) {
                    log.warn("[REFUND] SendSms {} non trouvé ou sans utilisateur", sendSmsId);
                    continue;
                }

                // Récupérer les abonnements actifs
                List<Abonnement> activeSubscriptions = abonnementRepository.findActiveByUserId(sendSms.getUser().getId());

                if (activeSubscriptions.isEmpty()) {
                    log.warn("[REFUND] Aucun abonnement actif pour utilisateur {}", sendSms.getUser().getId());
                    continue;
                }

                // Grouper par type de message
                Map<MessageType, Integer> failedCountByType = failedMessages
                    .stream()
                    .collect(
                        Collectors.groupingBy(FailedMessageInfo::getMessageType, Collectors.summingInt(FailedMessageInfo::getTotalMessage))
                    );

                // Rembourser pour chaque type
                for (Map.Entry<MessageType, Integer> typeEntry : failedCountByType.entrySet()) {
                    MessageType messageType = typeEntry.getKey();
                    int failedCount = typeEntry.getValue();

                    log.info("[REFUND] SendSms {} : Remboursement de {} messages de type {}", sendSmsId, failedCount, messageType);

                    // ✅ REMBOURSEMENT : Incrémenter les quotas
                    refundQuotas(activeSubscriptions, messageType, failedCount);
                }

                // Sauvegarder les abonnements mis à jour
                abonnementRepository.saveAll(activeSubscriptions);

                log.info("[REFUND] ✅ Remboursement réussi pour SendSms {} : {} messages", sendSmsId, failedMessages.size());
            } catch (Exception e) {
                log.error("[REFUND] ❌ Erreur remboursement SendSms {}", sendSmsId, e);
            }
        }

        log.info("[REFUND] Fin du remboursement");
    }

    /**
     * ✅ REMBOURSEMENT DES QUOTAS (inverse de la décrémentation)
     */
    private void refundQuotas(List<Abonnement> subscriptions, MessageType messageType, int amountToRefund) {
        int remaining = amountToRefund;

        log.debug("[REFUND-QUOTA] Remboursement de {} messages de type {}", amountToRefund, messageType);

        for (Abonnement abonnement : subscriptions) {
            if (remaining <= 0) break;

            if (messageType == MessageType.SMS) {
                remaining = refundSmsQuota(abonnement, remaining);
            } else if (messageType == MessageType.WHATSAPP) {
                remaining = refundWhatsappQuota(abonnement, remaining);
            }
        }

        if (remaining > 0) {
            log.warn("[REFUND-QUOTA] ⚠️ Impossible de rembourser {} messages (quotas déjà au max)", remaining);
        }
    }

    /**
     * ✅ REMBOURSER LES SMS
     */
    private int refundSmsQuota(Abonnement abonnement, int toRefund) {
        int currentUsed = abonnement.getSmsUsed() != null ? abonnement.getSmsUsed() : 0;

        if (currentUsed <= 0) {
            log.debug("[REFUND-SMS] Abonnement {} : déjà à 0, rien à rembourser", abonnement.getId());
            return toRefund;
        }

        // Rembourser en priorité le quota principal (smsUsed)
        int canRefund = Math.min(toRefund, currentUsed);

        if (canRefund > 0) {
            int newUsed = currentUsed - canRefund;
            abonnement.setSmsUsed(newUsed);
            abonnement.setUpdatedDate(Instant.now());

            log.info("[REFUND-SMS] Abonnement {} : {} SMS remboursés ({} → {})", abonnement.getId(), canRefund, currentUsed, newUsed);

            return toRefund - canRefund;
        }

        return toRefund;
    }

    /**
     * ✅ REMBOURSER LES WHATSAPP
     */
    private int refundWhatsappQuota(Abonnement abonnement, int toRefund) {
        int currentUsed = abonnement.getWhatsappUsed() != null ? abonnement.getWhatsappUsed() : 0;

        if (currentUsed <= 0) {
            log.debug("[REFUND-WHATSAPP] Abonnement {} : déjà à 0, rien à rembourser", abonnement.getId());
            return toRefund;
        }

        // Rembourser en priorité le quota principal (whatsappUsed)
        int canRefund = Math.min(toRefund, currentUsed);

        if (canRefund > 0) {
            int newUsed = currentUsed - canRefund;
            abonnement.setWhatsappUsed(newUsed);
            abonnement.setUpdatedDate(Instant.now());

            log.info(
                "[REFUND-WHATSAPP] Abonnement {} : {} WhatsApp remboursés ({} → {})",
                abonnement.getId(),
                canRefund,
                currentUsed,
                newUsed
            );

            return toRefund - canRefund;
        }

        return toRefund;
    }

    // ===== MÉTHODES EXISTANTES (inchangées) =====

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void purgeOldDeliveryStatuses() {
        Instant cutoffInstant = Instant.now().minus(Duration.ofDays(14));
        Timestamp cutoff = Timestamp.from(cutoffInstant);

        int batchSize = 5000;
        log.info(">>> Début de purgeOldDeliveryStatuses à {}", cutoffInstant);

        int deleted;
        String sql =
            """
            WITH to_delete AS (
              SELECT id
              FROM message_delivery_status
              WHERE processed_at < ?
              LIMIT ?
            )
            DELETE FROM message_delivery_status
            WHERE id IN (SELECT id FROM to_delete)
            """;

        do {
            deleted = jdbcTemplate.update(sql, cutoff, batchSize);
            log.info("  -> {} enregistrements supprimés dans ce lot", deleted);
        } while (deleted == batchSize);

        log.info(">>> Fin de purgeOldDeliveryStatuses");
    }

    @Transactional
    public void updateSendSmsStatus(Long sendSmsId) {
        if (sendSmsId == null) {
            log.warn("Aucun ID fourni pour updateSendSmsStatus");
            return;
        }

        try {
            jdbcTemplate.update(
                """
                UPDATE send_sms s
                SET
                    total_sent = COALESCE((SELECT COUNT(*) FROM sms WHERE send_sms_id = s.id AND delivery_status = 'sent'), 0),
                    total_delivered = COALESCE((SELECT COUNT(*) FROM sms WHERE send_sms_id = s.id AND delivery_status = 'delivered'), 0),
                    total_read = COALESCE((SELECT COUNT(*) FROM sms WHERE send_sms_id = s.id AND delivery_status = 'read'), 0),
                    total_pending = COALESCE((SELECT COUNT(*) FROM sms WHERE send_sms_id = s.id AND delivery_status = 'pending'), 0),
                    total_failed = COALESCE((SELECT COUNT(*) FROM sms WHERE send_sms_id = s.id AND delivery_status = 'failed'), 0)
                WHERE s.id = ?
                """,
                sendSmsId
            );

            jdbcTemplate.update(
                """
                UPDATE send_sms s SET delivery_status = sub.status
                FROM (
                    SELECT
                        id,
                        total_sent,
                        total_delivered,
                        total_read,
                        total_pending,
                        total_failed,
                        total_sent + total_delivered + total_read + total_pending + total_failed AS total_all,
                        CASE
                            WHEN total_read > 0 AND total_read = (total_sent + total_delivered + total_read + total_pending + total_failed) THEN 'read'
                            WHEN total_delivered > 0 AND total_delivered = (total_sent + total_delivered + total_read + total_pending + total_failed) THEN 'delivered'
                            WHEN total_failed > 0 THEN 'failed'
                            WHEN total_pending > 0 THEN 'pending'
                            WHEN total_sent > 0 THEN 'sent'
                            ELSE 'pending'
                        END AS status
                    FROM send_sms
                    WHERE id = ?
                ) sub
                WHERE s.id = sub.id
                """,
                sendSmsId
            );

            log.info("✅ Mise à jour réussie du statut de send_sms id={}", sendSmsId);
        } catch (Exception e) {
            log.error("❌ Erreur lors de la mise à jour du send_sms id={}", sendSmsId, e);
        }
    }

    @Transactional
    public void syncDeliveryStatusBySendSmsId(Long sendSmsId) {
        if (sendSmsId == null) {
            log.warn("Aucun ID fourni pour syncDeliveryStatusBySendSmsId");
            return;
        }

        try {
            List<Long> statusIds = jdbcTemplate.queryForList(
                """
                    SELECT mds.id
                    FROM message_delivery_status mds
                    JOIN sms s ON s.message_id = mds.message_id
                    WHERE s.send_sms_id = ? AND mds.processed_at IS NULL
                """,
                Long.class,
                sendSmsId
            );

            if (statusIds.isEmpty()) {
                log.info("Aucun status à synchroniser pour send_sms_id={}", sendSmsId);
                return;
            }

            // ✅ Détecter les échecs AVANT mise à jour
            Map<Long, List<FailedMessageInfo>> failedMessages = detectFailedMessages(statusIds);

            for (Long statusId : statusIds) {
                jdbcTemplate.update(
                    """
                    UPDATE sms s
                    SET delivery_status = mds.status,
                        last_error = CASE
                            WHEN mds.error_title IS NOT NULL THEN mds.error_title || ' - ' || mds.error_details
                            ELSE NULL
                        END
                    FROM message_delivery_status mds
                    WHERE s.message_id = mds.message_id
                      AND mds.id = ?
                    """,
                    statusId
                );
            }

            for (Long statusId : statusIds) {
                jdbcTemplate.update(
                    """
                    UPDATE message_delivery_status
                    SET processed_at = now()
                    WHERE id = ?
                    """,
                    statusId
                );
            }

            // ✅ Rembourser les échecs
            if (!failedMessages.isEmpty()) {
                refundFailedMessages(failedMessages);
            }

            updateSendSmsStatus(sendSmsId);

            log.info("✅ Synchronisation terminée pour send_sms_id={} ({} statuts traités)", sendSmsId, statusIds.size());
        } catch (Exception e) {
            log.error("❌ Erreur lors de la synchronisation du statut de delivery pour send_sms_id={}", sendSmsId, e);
        }
    }

    @Transactional
    public void deleteSendSmsWithMessages(Long sendSmsId) {
        jdbcTemplate.update("DELETE FROM sms WHERE send_sms_id = ?", sendSmsId);
        jdbcTemplate.update("DELETE FROM send_sms WHERE id = ?", sendSmsId);
        log.info("Suppression du send_sms {} et de ses messages associées terminée", sendSmsId);
    }

    // ===== CLASSE INTERNE =====

    /**
     * DTO pour les informations de messages échoués
     */
    private static class FailedMessageInfo {

        private final Long smsId;
        private final Long sendSmsId;
        private final MessageType messageType;
        private final int totalMessage;
        private final String oldStatus;
        private final String newStatus;

        public FailedMessageInfo(
            Long smsId,
            Long sendSmsId,
            MessageType messageType,
            int totalMessage,
            String oldStatus,
            String newStatus
        ) {
            this.smsId = smsId;
            this.sendSmsId = sendSmsId;
            this.messageType = messageType;
            this.totalMessage = totalMessage;
            this.oldStatus = oldStatus;
            this.newStatus = newStatus;
        }

        public Long getSmsId() {
            return smsId;
        }

        public Long getSendSmsId() {
            return sendSmsId;
        }

        public MessageType getMessageType() {
            return messageType;
        }

        public int getTotalMessage() {
            return totalMessage;
        }

        public String getOldStatus() {
            return oldStatus;
        }

        public String getNewStatus() {
            return newStatus;
        }
    }
}
