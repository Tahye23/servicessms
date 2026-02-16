package com.example.myproject.service;

import com.example.myproject.domain.*;
import com.example.myproject.domain.enumeration.ContentType;
import com.example.myproject.domain.enumeration.MessageType;
import com.example.myproject.repository.AbonnementRepository;
import com.example.myproject.repository.ConfigurationRepository;
import com.example.myproject.repository.SendSmsRepository;
import com.example.myproject.repository.SmsRepository;
import com.example.myproject.web.rest.dto.SendMessageResult;
import com.example.myproject.web.rest.dto.VariableDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service WhatsApp Bulk Production avec support multi-providers
 * Compatible: 360dialog, Twilio, Meta Cloud API, etc.
 *
 * Features:
 * - Rate limiting adaptatif par provider
 * - Circuit breaker intelligent
 * - Retry automatique avec backoff
 * - Template validation
 * - Campaign batching pour réduire les coûts
 * - Health monitoring temps réel
 */
@Service
public class WhatsAppBulkService {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppBulkService.class);

    @Autowired
    private CampaignHistoryService campaignHistoryService;

    @Autowired
    private SmsUpdateService smsUpdateService;

    @Autowired
    private SmsRepository smsRepository;

    @Autowired
    private AbonnementService abonnementService;

    @Autowired
    private AbonnementRepository abonnementRepository;

    @Autowired
    private SendSmsUpdateService sendSmsUpdateService;

    @Autowired
    private SendWhatsappService sendWhatsappService;

    @Autowired
    private ConfigurationRepository configurationRepository;

    @Autowired
    private SendSmsRepository sendSmsRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    @Qualifier("bulkSmsExecutor")
    private ThreadPoolTaskExecutor bulkExecutor;

    @PersistenceContext
    private EntityManager em;

    private final ObjectMapper mapper = new ObjectMapper();

    // Configuration adaptative par provider
    @Value("${whatsapp.bulk.target-rate:80.0}") // WhatsApp = 80 msg/sec (Meta limit)
    private double targetRatePerSecond;

    @Value("${whatsapp.bulk.parallel-workers:30}")
    private int parallelWorkers;

    @Value("${whatsapp.bulk.batch-size:100}")
    private int batchSize;

    @Value("${whatsapp.bulk.timeout-per-message:20}")
    private int timeoutPerMessage;

    @Value("${whatsapp.bulk.max-retry:3}")
    private int maxRetryAttempts;

    @Value("${whatsapp.bulk.circuit-breaker-threshold:0.25}")
    private double circuitBreakerThreshold;

    @Value("${whatsapp.bulk.enable-campaign-batching:true}")
    private boolean enableCampaignBatching;

    @Value("${whatsapp.bulk.campaign-batch-size:1000}")
    private int campaignBatchSize;

    // Rate limiter adaptatif
    private TokenBucket rateLimiter;

    // Circuit breaker
    private final AtomicInteger recentFailures = new AtomicInteger(0);
    private final AtomicInteger recentAttempts = new AtomicInteger(0);
    private volatile boolean circuitOpen = false;
    private volatile Instant circuitOpenTime;

    // Métriques
    private final AtomicLong totalProcessed = new AtomicLong(0);
    private final AtomicLong totalSuccess = new AtomicLong(0);
    private final AtomicLong totalFailed = new AtomicLong(0);
    private final AtomicLong totalRetried = new AtomicLong(0);
    private final AtomicInteger activeWorkers = new AtomicInteger(0);

    // Gestion des arrêts
    private final ConcurrentHashMap<Long, AtomicBoolean> stopFlags = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, ExecutorService> activeExecutors = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, List<Future<?>>> activeFutures = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, PriorityBlockingQueue<RetryTask>> retryQueues = new ConcurrentHashMap<>();

    private final ScheduledExecutorService retryScheduler = Executors.newScheduledThreadPool(2);
    private final ScheduledExecutorService monitoringScheduler = Executors.newScheduledThreadPool(1);

    @jakarta.annotation.PostConstruct
    public void init() {
        // Token bucket avec burst capacity pour WhatsApp
        this.rateLimiter = new TokenBucket(
            (int) (targetRatePerSecond * 2), // Burst: 160 msg
            targetRatePerSecond // 80 msg/sec
        );

        log.info("=== WhatsApp Bulk Service Production v2.0 ===");
        log.info("   Débit: {} msg/sec (burst: {})", targetRatePerSecond, targetRatePerSecond * 2);
        log.info("   Workers: {}", parallelWorkers);
        log.info("   Timeout: {}s", timeoutPerMessage);
        log.info("   Max Retry: {}", maxRetryAttempts);
        log.info("   Circuit Breaker: {}%", circuitBreakerThreshold * 100);
        log.info("   Campaign Batching: {}", enableCampaignBatching);

        // Monitoring circuit breaker
        monitoringScheduler.scheduleAtFixedRate(this::monitorCircuitBreaker, 5, 5, TimeUnit.SECONDS);
    }

    /**
     * Circuit breaker intelligent
     */
    private void monitorCircuitBreaker() {
        int attempts = recentAttempts.get();
        int failures = recentFailures.get();

        if (attempts > 50) {
            double failureRate = (double) failures / attempts;

            if (failureRate > circuitBreakerThreshold && !circuitOpen) {
                circuitOpen = true;
                circuitOpenTime = Instant.now();
                log.error("🔴 CIRCUIT BREAKER OUVERT (WhatsApp) - Taux échec: {:.1f}%", failureRate * 100);
            } else if (circuitOpen && Duration.between(circuitOpenTime, Instant.now()).getSeconds() > 30) {
                circuitOpen = false;
                recentFailures.set(0);
                recentAttempts.set(0);
                log.info("🟢 CIRCUIT BREAKER FERMÉ (WhatsApp) - Reprise normale");
            }
        }
    }

    /**
     *  TRAITEMENT PAR BATCH - Version pagination pour grands volumes
     */
    @Async("bulkSmsExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processBulkByBatch(String bulkId, boolean test, Long sendSmsId, Long templateId, String login) {
        log.info("╔══════════════════════════════════════════════════════╗");
        log.info("║#####  DÉBUT TRAITEMENT PAR BATCH WhatsApp - BulkId: {}║", bulkId);
        log.info("╚══════════════════════════════════════════════════════╝");

        int batchSize = 1000; // Traiter 1000 WhatsApp à la fois
        int pageNumber = 0;
        boolean hasMore = true;

        SendSms sendSms = sendSmsRepository.findById(sendSmsId).orElseThrow(() -> new EntityNotFoundException("SendSms non trouvé"));

        stopFlags.put(sendSmsId, new AtomicBoolean(false));
        activeFutures.put(sendSmsId, new CopyOnWriteArrayList<>());
        retryQueues.put(sendSmsId, new PriorityBlockingQueue<>(1000));

        long startTime = System.currentTimeMillis();

        //  RÉCUPÉRER LES ABONNEMENTS AU DÉBUT (avant l'envoi)
        List<Abonnement> activeSubscriptions = null;
        if (!test) {
            activeSubscriptions = abonnementRepository.findActiveByUserId(sendSms.getUser().getId());
            log.info(" {} abonnements actifs trouvés pour mise à jour", activeSubscriptions != null ? activeSubscriptions.size() : 0);
        }

        int totalSuccess = 0;
        int totalFailed = 0;

        try {
            while (hasMore && !isStopRequested(sendSmsId)) {
                //  CHARGER UNIQUEMENT 1000 WhatsApp
                Pageable pageable = PageRequest.of(pageNumber, batchSize);
                Page<Sms> page = smsRepository.findByBulkIdAndDeliveryStatusPaged(bulkId, "pending", pageable);

                List<Sms> batch = page.getContent();

                if (batch.isEmpty()) {
                    log.info(" Aucun WhatsApp restant - Fin du traitement");
                    break;
                }

                log.info(" Traitement batch {} : {} WhatsApp", pageNumber + 1, batch.size());

                //  TRAITER CE BATCH
                Template template = templateId != null ? em.find(Template.class, templateId) : null;
                Configuration cfg = configurationRepository
                    .findOneByUserLogin(login)
                    .orElseThrow(() -> new EntityNotFoundException("Configuration non trouvée"));

                List<WhatsAppProcessingData> processingData = prepareProcessingData(batch, sendSms, template, cfg);

                ExecutorService executor = Executors.newFixedThreadPool(parallelWorkers);
                activeExecutors.put(sendSmsId, executor);

                try {
                    ProcessingResult result = processParallelWithRetry(executor, processingData, test, sendSmsId);

                    totalSuccess += result.successCount;
                    totalFailed += result.failCount;

                    log.info(" Batch {} terminé: successCount :{} , failCount : {}", pageNumber + 1, result.successCount, result.failCount);

                    if (result.wasStopped) {
                        log.warn("!! Arrêt demandé");
                        break;
                    }
                } finally {
                    executor.shutdown();
                    executor.awaitTermination(10, TimeUnit.SECONDS);
                }

                pageNumber++;
                hasMore = page.hasNext();

                //  PAUSE entre les batchs
                if (hasMore) {
                    Thread.sleep(2000); // 2 secondes entre chaque batch de 1000
                }
            }

            //  MISE À JOUR ABONNEMENT APRÈS TOUS LES ENVOIS RÉUSSIS
            if (!test && totalSuccess > 0 && activeSubscriptions != null) {
                log.info(" Mise à jour des abonnements pour {} WhatsApp envoyés", totalSuccess);
                updateAbonnementAfterBulkSend(activeSubscriptions, MessageType.WHATSAPP, totalSuccess);
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("╔══════════════════════════════════════════════════════╗");
            log.info("║   TRAITEMENT TERMINÉ                               ║");
            log.info("║   Total envoyés: {}                              ║", totalSuccess);
            log.info("║   Total échecs: {}                               ║", totalFailed);
            log.info("║   Durée totale: {}s                             ║", duration / 1000);
            log.info("╚══════════════════════════════════════════════════════╝");

            //  METTRE À JOUR LE SENDSMS (inprocess = false)
            SendSms finalSendSms = sendSmsRepository.findById(sendSmsId).orElse(null);
            if (finalSendSms != null) {
                finalSendSms.setInprocess(false); //  IMPORTANT
                finalSendSms.setTotalSuccess(totalSuccess);
                finalSendSms.setTotalFailed(totalFailed);
                finalSendSms.setTotalPending(0);

                int total = totalSuccess + totalFailed;
                if (total > 0) {
                    finalSendSms.setSuccessRate((totalSuccess * 100.0) / total);
                    finalSendSms.setFailureRate((totalFailed * 100.0) / total);
                }

                finalSendSms.setIsSent(totalFailed == 0);
                sendSmsRepository.save(finalSendSms);

                log.info(" SendSms WhatsApp mis à jour: inprocess=false, success={}, failed={}", totalSuccess, totalFailed);
            }
        } catch (Exception e) {
            log.error("!!! Erreur traitement par batch WhatsApp", e);

            //  MÊME EN CAS D'ERREUR, METTRE inprocess = false
            SendSms errorSendSms = sendSmsRepository.findById(sendSmsId).orElse(null);
            if (errorSendSms != null) {
                errorSendSms.setInprocess(false);
                errorSendSms.setLast_error(e.getMessage());
                sendSmsRepository.save(errorSendSms);
            }

            handleFatalError(sendSmsId, totalSuccess + totalFailed, e);
        } finally {
            cleanup(sendSmsId);
        }
    }

    /**
     * Point d'entrée principal pour envoi WhatsApp en masse
     */
    @Async("bulkSmsExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processWhatsAppBulk(List<Sms> smsList, boolean test, Long sendSmsId, Long templateId, String userLogin) {
        if (smsList == null || smsList.isEmpty()) {
            log.info("[WA-BULK] Liste vide, aucun traitement");
            return;
        }

        stopFlags.put(sendSmsId, new AtomicBoolean(false));
        activeFutures.put(sendSmsId, new CopyOnWriteArrayList<>());
        retryQueues.put(sendSmsId, new PriorityBlockingQueue<>(1000));

        try {
            // Stratégie : Envoi individuel avec retry (meilleure délivrabilité)
            processIndividualMessagesWithRetryAndQuota(smsList, test, sendSmsId, templateId, userLogin);
        } catch (Exception e) {
            log.error("[WA-BULK] Erreur fatale pour SendSms {}", sendSmsId, e);
            handleFatalError(sendSmsId, smsList.size(), e);
        } finally {
            cleanup(sendSmsId);
        }
    }

    /**
     * ✅ STRATÉGIE AVEC MISE À JOUR ABONNEMENT
     */
    private void processIndividualMessagesWithRetryAndQuota(
        List<Sms> smsList,
        boolean test,
        Long sendSmsId,
        Long templateId,
        String userLogin
    ) {
        long startTime = System.currentTimeMillis();
        int totalMessages = smsList.size();

        log.info("[WA-INDIVIDUAL] Début: {} messages WhatsApp", totalMessages);

        SendSms sendSms = sendSmsRepository.findById(sendSmsId).orElseThrow(() -> new EntityNotFoundException("SendSms non trouvé"));

        Template template = templateId != null ? em.find(Template.class, templateId) : null;

        Configuration cfg = configurationRepository
            .findOneByUserLogin(userLogin)
            .orElseThrow(() -> new EntityNotFoundException("Configuration non trouvée"));

        // ✅ RÉCUPÉRER LES ABONNEMENTS AU DÉBUT
        List<Abonnement> activeSubscriptions = null;
        if (!test) {
            activeSubscriptions = abonnementRepository.findActiveByUserId(sendSms.getUser().getId());
            log.info("[WA-BULK] {} abonnements actifs trouvés pour mise à jour", activeSubscriptions.size());
        }

        ExecutorService executor = Executors.newFixedThreadPool(parallelWorkers);
        activeExecutors.put(sendSmsId, executor);

        try {
            List<WhatsAppProcessingData> processingData = prepareProcessingData(smsList, sendSms, template, cfg);

            ProcessingResult result = processParallelWithRetry(executor, processingData, test, sendSmsId);

            if (isStopRequested(sendSmsId) || result.wasStopped) {
                log.info("[WA-INDIVIDUAL] Arrêté pour SendSms {}", sendSmsId);
                handleStoppedCampaign(sendSmsId, result, totalMessages);
                return;
            }

            long duration = System.currentTimeMillis() - startTime;
            double actualRate = duration > 0 ? (totalMessages * 1000.0) / duration : 0;

            log.info(
                "[WA-INDIVIDUAL] ✅ Terminé: {} succès, {} échecs, {} retries, débit: {:.2f} msg/sec",
                result.successCount,
                result.failCount,
                result.retryCount,
                actualRate
            );

            // ✅ MISE À JOUR ABONNEMENT APRÈS ENVOI RÉUSSI
            if (!test && result.successCount > 0 && activeSubscriptions != null) {
                updateAbonnementAfterBulkSend(activeSubscriptions, MessageType.WHATSAPP, result.successCount);
            }

            updateSendSmsStatusFinal(sendSmsId, result.successCount, result.failCount, result.lastError);
            updateGlobalMetrics(totalMessages, result.successCount, result.failCount, result.retryCount);
        } catch (Exception e) {
            log.error("[WA-INDIVIDUAL] Erreur fatale", e);
            handleFatalError(sendSmsId, totalMessages, e);
        } finally {
            executor.shutdownNow();
            activeExecutors.remove(sendSmsId);
        }
    }

    /**
     * ✅ MISE À JOUR ABONNEMENT (Transaction séparée)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateAbonnementAfterBulkSend(List<Abonnement> subscriptions, MessageType messageType, int successCount) {
        try {
            log.info("[UPDATE-ABONNEMENT-WA] Début mise à jour: {} messages de type {}", successCount, messageType);

            // Décrémenter les quotas
            abonnementService.decrementQuotasAfterSend(subscriptions, messageType, successCount);

            // Sauvegarder les modifications
            abonnementRepository.saveAll(subscriptions);

            log.info("[UPDATE-ABONNEMENT-WA] ✅ Abonnements mis à jour avec succès");
        } catch (Exception e) {
            log.error("[UPDATE-ABONNEMENT-WA] ❌ Erreur lors de la mise à jour: {}", e.getMessage(), e);
            // Ne pas bloquer l'envoi si mise à jour échoue
        }
    }

    /**
     * STRATÉGIE 1: Campaign Batching (comme Twilio MessageSets ou 360dialog campaigns)
     * Réduit les coûts en regroupant les messages similaires
     */
    private void processCampaignBatching(List<Sms> smsList, boolean test, Long sendSmsId, Long templateId, String userLogin) {
        log.info("[WA-CAMPAIGN] Début campaign batching: {} messages", smsList.size());

        SendSms sendSms = sendSmsRepository.findById(sendSmsId).orElseThrow(() -> new EntityNotFoundException("SendSms non trouvé"));

        Template template = templateId != null ? em.find(Template.class, templateId) : null;
        if (template == null) {
            throw new IllegalStateException("Template WhatsApp obligatoire pour campaign");
        }

        Configuration cfg = configurationRepository
            .findOneByUserLogin(userLogin)
            .orElseThrow(() -> new EntityNotFoundException("Configuration non trouvée"));

        try {
            // Grouper par variables identiques pour maximiser le batching
            Map<String, List<Sms>> groupedByVars = smsList
                .stream()
                .collect(Collectors.groupingBy(sms -> sms.getVars() != null ? sms.getVars() : "[]"));

            int successCount = 0;
            int failCount = 0;
            String lastError = null;

            for (Map.Entry<String, List<Sms>> entry : groupedByVars.entrySet()) {
                if (isStopRequested(sendSmsId)) {
                    log.info("[WA-CAMPAIGN] Arrêté pour SendSms {}", sendSmsId);
                    break;
                }

                List<Sms> smsGroup = entry.getValue();
                String varsJson = entry.getKey();

                // Diviser en batches de campaignBatchSize
                List<List<Sms>> batches = partition(smsGroup, campaignBatchSize);

                for (List<Sms> batch : batches) {
                    try {
                        List<String> phoneNumbers = batch
                            .stream()
                            .map(Sms::getReceiver)
                            .filter(Objects::nonNull)
                            .distinct()
                            .map(this::formatPhoneNumber)
                            .collect(Collectors.toList());

                        List<VariableDTO> varsList = parseVarsJson(varsJson);

                        // Envoi via API campaign (360dialog, Twilio, etc.)
                        String campaignId = sendWhatsappService.sendMarketingLiteCampaign(
                            phoneNumbers,
                            template,
                            varsList,
                            cfg.getPhoneNumberId(),
                            cfg.getAccessToken()
                        );

                        // Mise à jour des statuts
                        updateSmsStatusBatch(batch, "PROCESSING", campaignId);
                        successCount += batch.size();

                        log.info("[WA-CAMPAIGN] Batch {} envoyé: {} numéros", campaignId, phoneNumbers.size());

                        // Rate limiting entre batches
                        rateLimiter.consumeBlocking(1);
                    } catch (Exception e) {
                        log.error("[WA-CAMPAIGN] Erreur batch: {}", e.getMessage());
                        updateSmsStatusBatch(batch, "FAILED", null);
                        failCount += batch.size();
                        lastError = e.getMessage();
                    }
                }
            }

            // Mise à jour finale
            updateSendSmsStatusFinal(sendSmsId, successCount, failCount, lastError);
            updateGlobalMetrics(smsList.size(), successCount, failCount, 0);
        } catch (Exception e) {
            log.error("[WA-CAMPAIGN] Erreur fatale", e);
            handleFatalError(sendSmsId, smsList.size(), e);
        }
    }

    /**
     * STRATÉGIE 2: Envoi individuel avec retry intelligent
     * Meilleure délivrabilité, tracking précis, retry automatique
     */
    private void processIndividualMessagesWithRetry(List<Sms> smsList, boolean test, Long sendSmsId, Long templateId, String userLogin) {
        long startTime = System.currentTimeMillis();
        int totalMessages = smsList.size();

        log.info("[WA-INDIVIDUAL] Début: {} messages WhatsApp", totalMessages);

        SendSms sendSms = sendSmsRepository.findById(sendSmsId).orElseThrow(() -> new EntityNotFoundException("SendSms non trouvé"));

        Template template = templateId != null ? em.find(Template.class, templateId) : null;

        Configuration cfg = configurationRepository
            .findOneByUserLogin(userLogin)
            .orElseThrow(() -> new EntityNotFoundException("Configuration non trouvée"));

        ExecutorService executor = Executors.newFixedThreadPool(parallelWorkers);
        activeExecutors.put(sendSmsId, executor);

        try {
            List<WhatsAppProcessingData> processingData = prepareProcessingData(smsList, sendSms, template, cfg);
            ProcessingResult result = processParallelWithRetry(executor, processingData, test, sendSmsId);

            if (isStopRequested(sendSmsId) || result.wasStopped) {
                log.info("[WA-INDIVIDUAL] Arrêté pour SendSms {}", sendSmsId);
                handleStoppedCampaign(sendSmsId, result, totalMessages);
                return;
            }

            long duration = System.currentTimeMillis() - startTime;
            double actualRate = duration > 0 ? (totalMessages * 1000.0) / duration : 0;

            log.info(
                "[WA-INDIVIDUAL] ✅ Terminé: {} succès, {} échecs, {} retries, débit: {:.2f} msg/sec",
                result.successCount,
                result.failCount,
                result.retryCount,
                actualRate
            );

            updateSendSmsStatusFinal(sendSmsId, result.successCount, result.failCount, result.lastError);
            updateGlobalMetrics(totalMessages, result.successCount, result.failCount, result.retryCount);
        } catch (Exception e) {
            log.error("[WA-INDIVIDUAL] Erreur fatale", e);
            handleFatalError(sendSmsId, totalMessages, e);
        } finally {
            executor.shutdownNow();
            activeExecutors.remove(sendSmsId);
        }
    }

    /**
     * Processing parallèle avec retry automatique
     */
    private ProcessingResult processParallelWithRetry(
        ExecutorService executor,
        List<WhatsAppProcessingData> data,
        boolean test,
        Long sendSmsId
    ) {
        if (data.isEmpty()) {
            return new ProcessingResult(0, 0, 0, false, null);
        }

        log.info("[WA-PARALLEL] {} messages avec {} workers", data.size(), parallelWorkers);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        AtomicInteger retryCount = new AtomicInteger(0);
        AtomicBoolean stopped = new AtomicBoolean(false);

        List<Future<?>> futures = activeFutures.get(sendSmsId);
        PriorityBlockingQueue<RetryTask> retryQueue = retryQueues.get(sendSmsId);

        // Phase 1: Envoi initial
        for (WhatsAppProcessingData waData : data) {
            if (isStopRequested(sendSmsId)) {
                stopped.set(true);
                break;
            }

            Future<WhatsAppResult> future = executor.submit(() -> sendSingleWhatsAppWithRetry(waData, test, sendSmsId, 0, retryQueue));
            futures.add(future);
        }

        // Phase 2: Worker de retry asynchrone
        Future<?> retryWorker = executor.submit(() -> {
            while (!isStopRequested(sendSmsId) && !Thread.currentThread().isInterrupted()) {
                try {
                    RetryTask task = retryQueue.poll(1, TimeUnit.SECONDS);
                    if (task == null) continue;

                    // Backoff exponentiel
                    long waitTime = (long) (Math.pow(2, task.attemptNumber) * 1000);
                    Thread.sleep(Math.min(waitTime, 10000)); // Max 10s

                    sendSingleWhatsAppWithRetry(task.waData, test, sendSmsId, task.attemptNumber, retryQueue);
                    retryCount.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        futures.add(retryWorker);

        // Phase 3: Collecte résultats
        for (Future<?> future : futures) {
            if (future == retryWorker) continue;

            if (isStopRequested(sendSmsId)) {
                future.cancel(true);
                stopped.set(true);
                continue;
            }

            try {
                WhatsAppResult result = (WhatsAppResult) future.get();
                if (result.success) {
                    successCount.incrementAndGet();
                } else {
                    failCount.incrementAndGet();
                }
            } catch (Exception e) {
                failCount.incrementAndGet();
            }
        }

        // Attendre fin des retries
        retryWorker.cancel(true);
        try {
            retryWorker.get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            // Timeout normal
        }

        return new ProcessingResult(successCount.get(), failCount.get(), retryCount.get(), stopped.get(), null);
    }

    /**
     * Envoi WhatsApp unique avec retry intelligent
     */
    private WhatsAppResult sendSingleWhatsAppWithRetry(
        WhatsAppProcessingData waData,
        boolean test,
        Long sendSmsId,
        int attemptNumber,
        PriorityBlockingQueue<RetryTask> retryQueue
    ) {
        // Circuit breaker check
        if (circuitOpen && attemptNumber == 0) {
            log.warn("[WA-{}] Circuit breaker ouvert - mise en retry", waData.smsId);
            if (attemptNumber < maxRetryAttempts) {
                retryQueue.offer(new RetryTask(waData, attemptNumber + 1));
            }
            smsUpdateService.updateSmsStatus(waData.smsId, false, null, "pending", "Circuit breaker");
            return new WhatsAppResult(false, null, "Circuit breaker");
        }

        if (isStopRequested(sendSmsId) || Thread.currentThread().isInterrupted()) {
            smsUpdateService.updateSmsStatus(waData.smsId, false, null, "pending", "Arrêté");
            return new WhatsAppResult(false, null, "Arrêté");
        }

        // Rate limiting
        if (!rateLimiter.tryConsume(1)) {
            rateLimiter.consumeBlocking(1);
        }

        if (isStopRequested(sendSmsId)) {
            smsUpdateService.updateSmsStatus(waData.smsId, false, null, "pending", "Arrêté");
            return new WhatsAppResult(false, null, "Arrêté");
        }

        activeWorkers.incrementAndGet();
        recentAttempts.incrementAndGet();

        try {
            if (test) {
                String messageId = "TEST_WA_" + System.currentTimeMillis();
                smsUpdateService.updateSmsStatus(waData.smsId, true, messageId, "sent", null);
                sendSmsUpdateService.incrementSuccess(sendSmsId);
                return new WhatsAppResult(true, messageId, null);
            }

            Future<SendMessageResult> sendFuture = bulkExecutor.submit(() -> {
                if (Thread.currentThread().isInterrupted()) {
                    return new SendMessageResult(false, null, "Annulé");
                }
                try {
                    return sendWhatsappService.sendMessageAndGetId(waData.receiver, waData.template, waData.variables, waData.userLogin);
                } catch (Exception e) {
                    return new SendMessageResult(false, null, e.getMessage());
                }
            });

            SendMessageResult result = sendFuture.get(timeoutPerMessage, TimeUnit.SECONDS);

            if (result.isSuccess()) {
                smsUpdateService.updateSmsStatus(waData.smsId, true, result.getMessageId(), "sent", null);
                sendSmsUpdateService.incrementSuccess(sendSmsId);
                return new WhatsAppResult(true, result.getMessageId(), null);
            } else {
                recentFailures.incrementAndGet();
                String error = result.getError() != null ? result.getError() : "Échec WhatsApp";

                // Retry sur erreurs temporaires
                if (isRetryableWhatsAppError(error) && attemptNumber < maxRetryAttempts) {
                    log.debug("[WA-{}] Erreur temporaire - retry #{}", waData.smsId, attemptNumber + 1);
                    retryQueue.offer(new RetryTask(waData, attemptNumber + 1));
                    smsUpdateService.updateSmsStatus(waData.smsId, false, null, "pending", error + " (retry)");
                    return new WhatsAppResult(false, null, error);
                }

                smsUpdateService.updateSmsStatus(waData.smsId, false, null, "failed", error);
                sendSmsUpdateService.incrementFailed(sendSmsId);
                return new WhatsAppResult(false, null, error);
            }
        } catch (TimeoutException e) {
            recentFailures.incrementAndGet();
            String err = "Timeout WhatsApp (" + timeoutPerMessage + "s)";

            if (attemptNumber < maxRetryAttempts) {
                log.warn("[WA-{}] Timeout - retry #{}", waData.smsId, attemptNumber + 1);
                retryQueue.offer(new RetryTask(waData, attemptNumber + 1));
                smsUpdateService.updateSmsStatus(waData.smsId, false, null, "pending", err + " (retry)");
                return new WhatsAppResult(false, null, err);
            }

            smsUpdateService.updateSmsStatus(waData.smsId, false, null, "failed", err);
            sendSmsUpdateService.incrementFailed(sendSmsId);
            return new WhatsAppResult(false, null, err);
        } catch (Exception e) {
            recentFailures.incrementAndGet();
            String err = e.getMessage() != null ? e.getMessage() : "Erreur inconnue";
            smsUpdateService.updateSmsStatus(waData.smsId, false, null, "failed", err);
            sendSmsUpdateService.incrementFailed(sendSmsId);
            return new WhatsAppResult(false, null, err);
        } finally {
            activeWorkers.decrementAndGet();
        }
    }

    /**
     * Détecte si l'erreur WhatsApp est temporaire
     */
    private boolean isRetryableWhatsAppError(String error) {
        if (error == null) return true;

        String lowerError = error.toLowerCase();

        // Erreurs temporaires WhatsApp
        return (
            lowerError.contains("rate limit") ||
            lowerError.contains("throttled") ||
            lowerError.contains("busy") ||
            lowerError.contains("temporarily") ||
            lowerError.contains("timeout") ||
            lowerError.contains("connection") ||
            lowerError.contains("network") ||
            lowerError.contains("503") ||
            lowerError.contains("429")
        );
    }

    // === Méthodes utilitaires ===

    private List<WhatsAppProcessingData> prepareProcessingData(List<Sms> smsList, SendSms sendSms, Template template, Configuration cfg) {
        return smsList
            .stream()
            .map(sms -> {
                List<VariableDTO> vars = parseVarsJson(sms.getVars());
                return new WhatsAppProcessingData(
                    sms.getId(),
                    formatPhoneNumber(sms.getReceiver()),
                    template,
                    vars,
                    cfg.getPhoneNumberId(),
                    cfg.getAccessToken(),
                    cfg.getUserLogin()
                );
            })
            .collect(Collectors.toList());
    }

    private String formatPhoneNumber(String phone) {
        if (phone == null) return null;
        phone = phone.replaceAll("[^+\\d]", "");
        return phone.startsWith("+") ? phone : "+" + phone;
    }

    private List<VariableDTO> parseVarsJson(String varsJson) {
        if (varsJson == null || varsJson.trim().isEmpty() || "[]".equals(varsJson.trim())) {
            return Collections.emptyList();
        }
        try {
            return mapper.readValue(varsJson, new TypeReference<List<VariableDTO>>() {});
        } catch (Exception e) {
            log.warn("Erreur parsing variables: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private void updateSmsStatusBatch(List<Sms> smsList, String status, String campaignId) {
        Timestamp now = Timestamp.from(Instant.now());
        List<Long> smsIds = smsList.stream().map(Sms::getId).toList();

        String sql = "UPDATE sms SET status = ?, content_type = ?, send_date = ?, message_id = ? WHERE id = ?";

        List<Object[]> batchArgs = smsIds
            .stream()
            .map(id -> new Object[] { status, ContentType.TEMPLATE.name(), now, campaignId, id })
            .collect(Collectors.toList());

        jdbcTemplate.batchUpdate(sql, batchArgs);
    }

    private void updateSendSmsStatusFinal(Long sendSmsId, int successCount, int failCount, String lastError) {
        try {
            SendSms sendSms = sendSmsRepository.findById(sendSmsId).orElseThrow(() -> new EntityNotFoundException("SendSms non trouvé"));

            int total = successCount + failCount;
            sendSms.setTotalSuccess(successCount);
            sendSms.setTotalFailure(failCount);
            sendSms.setSuccessRate(total == 0 ? 0 : (successCount * 100.0) / total);
            sendSms.setFailureRate(total == 0 ? 0 : (failCount * 100.0) / total);
            sendSms.setIsSent(failCount == 0);
            sendSms.setLast_error(lastError);
            sendSms.setInprocess(false);

            sendSmsRepository.save(sendSms);
            log.info("[WA-BULK] SendSms mis à jour: {} succès, {} échecs", successCount, failCount);
        } catch (Exception e) {
            log.error("[WA-BULK] Erreur mise à jour SendSms", e);
        }
    }

    private void handleFatalError(Long sendSmsId, int totalMessages, Exception e) {
        campaignHistoryService.updateRetryAttemptError(sendSmsId, 1, 0, totalMessages, 0, e.getMessage());
        sendSmsUpdateService.updateFinalCounters(sendSmsId, 0, totalMessages, e.getMessage());
    }

    private void handleStoppedCampaign(Long sendSmsId, ProcessingResult result, int totalMessages) {
        campaignHistoryService.updateRetryAttemptStopped(
            sendSmsId,
            1,
            result.successCount,
            result.failCount,
            totalMessages - result.successCount - result.failCount,
            "Arrêté"
        );
        sendSmsUpdateService.updateFinalCounters(sendSmsId, result.successCount, result.failCount, "Arrêté");
    }

    private void updateGlobalMetrics(int total, int success, int fail, int retried) {
        totalProcessed.addAndGet(total);
        totalSuccess.addAndGet(success);
        totalFailed.addAndGet(fail);
        totalRetried.addAndGet(retried);
    }

    public boolean stopBulkProcessing(Long sendSmsId) {
        log.info("[WA-STOP] Arrêt immédiat pour SendSms {}", sendSmsId);

        AtomicBoolean stopFlag = stopFlags.computeIfAbsent(sendSmsId, k -> new AtomicBoolean(false));
        stopFlag.set(true);

        List<Future<?>> futures = activeFutures.get(sendSmsId);
        if (futures != null) {
            futures.forEach(f -> f.cancel(true));
            activeFutures.remove(sendSmsId);
        }

        ExecutorService executor = activeExecutors.get(sendSmsId);
        if (executor != null) {
            executor.shutdownNow();
            activeExecutors.remove(sendSmsId);
        }

        retryQueues.remove(sendSmsId);
        sendSmsUpdateService.markAsCompleted(sendSmsId, "Arrêté par l'utilisateur");

        log.info("[WA-STOP] Arrêt complet pour SendSms {}", sendSmsId);
        return true;
    }

    private boolean isStopRequested(Long sendSmsId) {
        AtomicBoolean stopFlag = stopFlags.get(sendSmsId);
        return stopFlag != null && stopFlag.get();
    }

    private void cleanup(Long sendSmsId) {
        stopFlags.remove(sendSmsId);
        activeFutures.remove(sendSmsId);
        retryQueues.remove(sendSmsId);
        ExecutorService executor = activeExecutors.remove(sendSmsId);
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
        }
    }

    private static <T> List<List<T>> partition(List<T> list, int partitionSize) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += partitionSize) {
            partitions.add(list.subList(i, Math.min(i + partitionSize, list.size())));
        }
        return partitions;
    }

    public Map<String, Object> getPerformanceStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalProcessed", totalProcessed.get());
        stats.put("totalSuccess", totalSuccess.get());
        stats.put("totalFailed", totalFailed.get());
        stats.put("totalRetried", totalRetried.get());
        stats.put("activeWorkers", activeWorkers.get());
        stats.put("targetRate", targetRatePerSecond);
        stats.put("circuitBreakerOpen", circuitOpen);
        stats.put("successRate", calculateSuccessRate());
        stats.put("availableTokens", rateLimiter.getAvailableTokens());
        return stats;
    }

    private double calculateSuccessRate() {
        long total = totalProcessed.get();
        return total > 0 ? (totalSuccess.get() * 100.0) / total : 0.0;
    }

    public void resetStats() {
        totalProcessed.set(0);
        totalSuccess.set(0);
        totalFailed.set(0);
        totalRetried.set(0);
        recentAttempts.set(0);
        recentFailures.set(0);
        log.info("[WA-BULK] Statistiques réinitialisées");
    }

    // === Classes internes ===

    private static class WhatsAppProcessingData {

        final Long smsId;
        final String receiver;
        final Template template;
        final List<VariableDTO> variables;
        final String phoneNumberId;
        final String accessToken;
        final String userLogin;

        WhatsAppProcessingData(
            Long smsId,
            String receiver,
            Template template,
            List<VariableDTO> variables,
            String phoneNumberId,
            String accessToken,
            String userLogin
        ) {
            this.smsId = smsId;
            this.receiver = receiver;
            this.template = template;
            this.variables = variables;
            this.phoneNumberId = phoneNumberId;
            this.accessToken = accessToken;
            this.userLogin = userLogin;
        }
    }

    private static class ProcessingResult {

        final int successCount;
        final int failCount;
        final int retryCount;
        final boolean wasStopped;
        final String lastError;

        ProcessingResult(int success, int fail, int retry, boolean stopped, String error) {
            this.successCount = success;
            this.failCount = fail;
            this.retryCount = retry;
            this.wasStopped = stopped;
            this.lastError = error;
        }
    }

    private static class WhatsAppResult {

        final boolean success;
        final String messageId;
        final String errorMessage;

        WhatsAppResult(boolean success, String messageId, String errorMessage) {
            this.success = success;
            this.messageId = messageId;
            this.errorMessage = errorMessage;
        }
    }

    private static class RetryTask implements Comparable<RetryTask> {

        final WhatsAppProcessingData waData;
        final int attemptNumber;
        final long timestamp;

        RetryTask(WhatsAppProcessingData waData, int attemptNumber) {
            this.waData = waData;
            this.attemptNumber = attemptNumber;
            this.timestamp = System.currentTimeMillis();
        }

        @Override
        public int compareTo(RetryTask other) {
            int cmp = Integer.compare(this.attemptNumber, other.attemptNumber);
            return cmp != 0 ? cmp : Long.compare(this.timestamp, other.timestamp);
        }
    }

    /**
     * Token Bucket Algorithm - Rate Limiter adapté pour WhatsApp
     * Thread-safe, supporte les bursts (80 msg/sec avec burst 160)
     */
    private static class TokenBucket {

        private final int capacity;
        private final double refillRate;
        private double tokens;
        private long lastRefillTime;
        private final Object lock = new Object();

        TokenBucket(int capacity, double refillRate) {
            this.capacity = capacity;
            this.refillRate = refillRate;
            this.tokens = capacity;
            this.lastRefillTime = System.nanoTime();
        }

        boolean tryConsume(int tokensToConsume) {
            synchronized (lock) {
                refill();
                if (tokens >= tokensToConsume) {
                    tokens -= tokensToConsume;
                    return true;
                }
                return false;
            }
        }

        void consumeBlocking(int tokensToConsume) {
            synchronized (lock) {
                while (true) {
                    refill();
                    if (tokens >= tokensToConsume) {
                        tokens -= tokensToConsume;
                        return;
                    }

                    double tokensNeeded = tokensToConsume - tokens;
                    long waitTimeMs = (long) ((tokensNeeded / refillRate) * 1000);

                    try {
                        lock.wait(Math.max(1, waitTimeMs));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        }

        private void refill() {
            long now = System.nanoTime();
            double elapsedSeconds = (now - lastRefillTime) / 1_000_000_000.0;
            double tokensToAdd = elapsedSeconds * refillRate;

            if (tokensToAdd > 0) {
                tokens = Math.min(capacity, tokens + tokensToAdd);
                lastRefillTime = now;
                lock.notifyAll();
            }
        }

        double getAvailableTokens() {
            synchronized (lock) {
                refill();
                return tokens;
            }
        }
    }
}
