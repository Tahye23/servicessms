package com.example.myproject.service;

import com.example.myproject.domain.SendSms;
import com.example.myproject.domain.Sms;
import com.example.myproject.domain.enumeration.MessageType;
import com.example.myproject.repository.SendSmsRepository;
import com.example.myproject.repository.SmsRepository;
import com.example.myproject.service.event.SmsBulkInsertEvent;
import jakarta.persistence.EntityNotFoundException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
public class SmsBulkInsertService {

    private static final Logger log = LoggerFactory.getLogger(SmsBulkInsertService.class);
    private static final int LOG_INTERVAL = 10000;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SendSmsRepository sendSmsRepository;

    @Autowired
    private SmsRepository smsRepository;

    @Value("${sms.bulk.insert-batch-size:1000}")
    private int insertBatchSize;

    // ✅ AUTO-INJECTION POUR PASSER PAR LE PROXY SPRING
    @Lazy
    @Autowired
    private SmsBulkInsertService self;

    /**
     * ✅ LISTENER - Déclenche l'async via le proxy Spring
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleBulkInsertEvent(SmsBulkInsertEvent event) {
        log.info("[BULK-EVENT] Événement reçu pour SendSms#{} avec {} SMS", event.getSendSmsId(), event.getSmsList().size());

        // ✅ APPELER VIA LE PROXY POUR QUE @Async ET @Transactional FONCTIONNENT
        self.insertBulkSmsAsync(event.getSmsList(), event.getSendSmsId());
    }

    /**
     * ✅ INSERTION MASSIVE ASYNCHRONE
     */
    @Async("bulkInsertExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void insertBulkSmsAsync(List<Sms> smsList, Long sendSmsId) {
        Instant startTime = Instant.now();

        log.info("╔════════════════════════════════════════════════════════════╗");
        log.info("║  💾 INSERTION MASSIVE SMS                                 ║");
        log.info("╠════════════════════════════════════════════════════════════╣");
        log.info("║  SendSms ID: {}                                            ║", sendSmsId);
        log.info("║  Total à insérer: {}                                       ║", smsList.size());
        log.info("║  Batch size: {}                                            ║", insertBatchSize);
        log.info("╚════════════════════════════════════════════════════════════╝");

        if (smsList == null || smsList.isEmpty()) {
            log.warn("⚠️  Liste vide, aucune insertion");
            return;
        }

        // ✅ RÉCUPÉRER LE SENDSMS
        SendSms managedSendSms = sendSmsRepository
            .findById(sendSmsId)
            .orElseThrow(() -> new EntityNotFoundException("SendSms non trouvé: " + sendSmsId));

        try {
            //  PRÉPARER LES DONNÉES
            List<Object[]> batchArgs = smsList
                .stream()
                .map(
                    sms ->
                        new Object[] {
                            sendSmsId,
                            sms.getSender(),
                            sms.getReceiver(),
                            sms.getMsgdata(),
                            sms.getTotalMessage(),
                            false,
                            null,
                            "PENDING",
                            sms.getBulkId(),
                            Timestamp.from(sms.getBulkCreatedAt() != null ? sms.getBulkCreatedAt() : Instant.now()),
                            sms.getVars(),
                            sms.getTemplate_id(),
                            sms.getType() != null ? sms.getType().name() : MessageType.SMS.name(),
                            "pending",
                            sms.getNamereceiver(),
                            sms.getUser_login(),
                        }
                )
                .collect(Collectors.toList());

            // ✅ INSERTION PAR BATCH
            String sql =
                """
                INSERT INTO sms (
                    send_sms_id, sender, receiver, msgdata,
                    total_message, is_sent, send_date, status,
                    bulk_id, bulk_created_at, vars, template_id,
                    type, delivery_status, namereceiver, user_login
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

            List<List<Object[]>> batches = partition(batchArgs, insertBatchSize);
            int totalInserted = 0;

            for (int i = 0; i < batches.size(); i++) {
                List<Object[]> batch = batches.get(i);
                jdbcTemplate.batchUpdate(sql, batch);

                totalInserted += batch.size();

                if (totalInserted % LOG_INTERVAL == 0 || totalInserted == smsList.size()) {
                    long elapsedSeconds = Duration.between(startTime, Instant.now()).getSeconds();
                    double rate = elapsedSeconds > 0 ? (double) totalInserted / elapsedSeconds : 0;

                    log.info(
                        "📊 Insertion: {}/{} ({}%) | ⚡ {} SMS/s",
                        totalInserted,
                        smsList.size(),
                        String.format("%.1f", (totalInserted * 100.0) / smsList.size()),
                        String.format("%.0f", rate)
                    );
                }
            }

            // ✅ LOG FINAL
            long durationSeconds = Duration.between(startTime, Instant.now()).getSeconds();
            double avgRate = durationSeconds > 0 ? (double) totalInserted / durationSeconds : 0;

            log.info("╔════════════════════════════════════════════════════════════╗");
            log.info("║  ✅ INSERTION TERMINÉE                                    ║");
            log.info("╠════════════════════════════════════════════════════════════╣");
            log.info("║  Total inséré: {}                                          ║", totalInserted);
            log.info("║  Durée: {} secondes                                        ║", durationSeconds);
            log.info("║  Débit moyen: {} SMS/s                                     ║", String.format("%.0f", avgRate));
            log.info("╚════════════════════════════════════════════════════════════╝");

            // ✅ MARQUER COMME TERMINÉ (dans la même transaction)
            managedSendSms.setInprocess(false);
            sendSmsRepository.save(managedSendSms);
        } catch (Exception e) {
            log.error("❌❌❌ ERREUR INSERTION: {}", e.getMessage(), e);

            // ✅ MARQUER COMME TERMINÉ MÊME EN CAS D'ERREUR
            try {
                managedSendSms.setInprocess(false);
                sendSmsRepository.save(managedSendSms);
            } catch (Exception ex) {
                log.error("Impossible de mettre à jour inprocess: {}", ex.getMessage());
            }

            throw new RuntimeException("Échec insertion massive", e);
        }
    }

    /**
     * ✅ INSERTION SYNCHRONE (pour petits volumes)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void insertBulkSmsSync(List<Sms> smsList, SendSms sendSms) {
        log.info("💾 Insertion synchrone de {} SMS", smsList.size());

        try {
            smsRepository.saveAll(smsList);
            smsRepository.flush();
            log.info("✅ {} SMS insérés (JPA)", smsList.size());
        } catch (Exception e) {
            log.error("❌ Erreur insertion JPA: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * ✅ VÉRIFICATION POST-INSERTION
     */
    public boolean verifyInsertion(String bulkId, int expectedCount) {
        long actualCount = smsRepository.countByBulkId(bulkId);

        if (actualCount == expectedCount) {
            log.info("✅ Vérification OK: {} SMS insérés", actualCount);
            return true;
        } else {
            log.error("❌ INCOHÉRENCE: {} SMS attendus, {} trouvés", expectedCount, actualCount);
            return false;
        }
    }

    // ===== UTILITAIRES =====

    private static <T> List<List<T>> partition(List<T> list, int partitionSize) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += partitionSize) {
            partitions.add(list.subList(i, Math.min(i + partitionSize, list.size())));
        }
        return partitions;
    }
}
