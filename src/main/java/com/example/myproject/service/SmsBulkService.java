package com.example.myproject.service;

import com.example.myproject.SMSService;
import com.example.myproject.domain.Abonnement;
import com.example.myproject.domain.SendSms;
import com.example.myproject.domain.enumeration.MessageType;
import com.example.myproject.repository.AbonnementRepository;
import com.example.myproject.repository.SendSmsRepository;
import com.example.myproject.repository.SmsRepository;
import com.example.myproject.web.rest.dto.SendResult;
import com.google.common.util.concurrent.RateLimiter;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.persistence.EntityNotFoundException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SmsBulkService {

    private static final Logger log = LoggerFactory.getLogger(SmsBulkService.class);

    @Value("${sms.bulk.target-rate:9.5}")
    private double targetRatePerSecond;

    @Value("${sms.bulk.parallel-workers:8}")
    private int parallelWorkers;

    @Value("${sms.bulk.batch-size:500}")
    private int batchSize;

    @Value("${sms.bulk.update-interval-ms:1000}")
    private int updateIntervalMs;

    @Value("${sms.bulk.update-batch-size:200}")
    private int updateBatchSize;

    @Value("${sms.simulation-mode:false}")
    private boolean simulationMode;

    @Autowired
    private SMSService smsService;

    @Autowired
    private SmsRepository smsRepository;

    @Autowired
    private SendSmsRepository sendSmsRepository;

    @Autowired
    private AbonnementRepository abonnementRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AbonnementService abonnementService;

    @Autowired
    private SmsBatchUpdateService batchUpdateService;

    @Autowired
    private CampaignHistoryService campaignHistoryService;

    private RateLimiter rateLimiter;
    private ScheduledExecutorService batchUpdateScheduler;
    private final ConcurrentHashMap<Long, CampaignContext> activeCampaigns = new ConcurrentHashMap<>();
    private final BlockingQueue<SmsUpdateResult> updateQueue = new LinkedBlockingQueue<>(100000);

    @PostConstruct
    public void init() {
        this.rateLimiter = RateLimiter.create(targetRatePerSecond);

        this.batchUpdateScheduler = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "sms-batch-updater");
            t.setDaemon(true);
            return t;
        });

        batchUpdateScheduler.scheduleWithFixedDelay(this::flushUpdateQueue, updateIntervalMs, updateIntervalMs, TimeUnit.MILLISECONDS);

        log.info("SMS Bulk Service initialized - Rate: {} msg/sec, Workers: {}", targetRatePerSecond, parallelWorkers);
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down SmsBulkService");
        activeCampaigns.values().forEach(ctx -> ctx.stopRequested.set(true));
        flushUpdateQueue();

        if (batchUpdateScheduler != null) {
            batchUpdateScheduler.shutdown();
            try {
                batchUpdateScheduler.awaitTermination(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                batchUpdateScheduler.shutdownNow();
            }
        }
    }

    @Async("bulkSmsExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processBulkByBatch(String bulkId, boolean test, Long sendSmsId, Long templateId, String login) {
        Instant startTime = Instant.now();

        log.info("Starting campaign - SendSms ID: {}, Bulk ID: {}, Mode: {}", sendSmsId, bulkId, test ? "TEST" : "PROD");

        CampaignContext ctx = new CampaignContext(sendSmsId, bulkId, startTime);
        activeCampaigns.put(sendSmsId, ctx);

        try {
            SendSms sendSms = sendSmsRepository
                .findById(sendSmsId)
                .orElseThrow(() -> new EntityNotFoundException("SendSms not found: " + sendSmsId));

            long totalPending = countPendingBySendSmsId(sendSmsId);
            ctx.totalToProcess.set((int) totalPending);

            log.info("Total pending: {} SMS", totalPending);

            if (totalPending == 0) {
                finalizeCampaign(ctx, sendSms, test);
                return;
            }

            if (!test && !simulationMode) {
                ctx.subscriptions = abonnementRepository.findActiveByUserId(sendSms.getUser().getId());
            }

            int attemptNumber = sendSms.getRetryCount() != null ? sendSms.getRetryCount() : 1;
            campaignHistoryService.saveRetryAttempt(sendSms, attemptNumber);

            ExecutorService workerPool = Executors.newFixedThreadPool(parallelWorkers, r -> new Thread(r, "worker-" + sendSmsId));

            try {
                processAllBatches(ctx, sendSmsId, test, workerPool);
            } finally {
                workerPool.shutdown();
                try {
                    workerPool.awaitTermination(120, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    workerPool.shutdownNow();
                }
            }

            waitForPendingUpdates();
            finalizeCampaign(ctx, sendSms, test);

            campaignHistoryService.updateRetryAttemptCompleted(
                sendSmsId,
                attemptNumber,
                ctx.totalSuccess.get(),
                ctx.totalFailed.get(),
                ctx.totalToProcess.get() - ctx.totalProcessed.get(),
                ctx.lastError
            );
        } catch (Exception e) {
            log.error("Campaign error for {}: {}", sendSmsId, e.getMessage(), e);
            ctx.lastError = e.getMessage();

            try {
                SendSms sendSms = sendSmsRepository.findById(sendSmsId).orElse(null);
                if (sendSms != null) {
                    finalizeCampaign(ctx, sendSms, test);
                    int attemptNumber = sendSms.getRetryCount() != null ? sendSms.getRetryCount() : 1;
                    campaignHistoryService.updateRetryAttemptError(
                        sendSmsId,
                        attemptNumber,
                        ctx.totalSuccess.get(),
                        ctx.totalFailed.get(),
                        ctx.totalToProcess.get() - ctx.totalProcessed.get(),
                        e.getMessage()
                    );
                }
            } catch (Exception ex) {
                log.error("Finalization error: {}", ex.getMessage());
            }
        } finally {
            activeCampaigns.remove(sendSmsId);
        }
    }

    private void processAllBatches(CampaignContext ctx, Long sendSmsId, boolean test, ExecutorService workerPool) {
        int offset = 0;
        int batchNum = 0;

        while (!ctx.stopRequested.get()) {
            batchNum++;
            List<SmsData> batch = loadBatchBySendSmsId(sendSmsId, offset, batchSize);

            if (batch.isEmpty()) {
                log.info("No more SMS to process");
                break;
            }

            log.info("Batch #{} - {} SMS (offset {})", batchNum, batch.size(), offset);

            processSingleBatch(ctx, batch, test, workerPool);
            logProgress(ctx);

            offset += batchSize;
        }

        if (ctx.stopRequested.get()) {
            log.warn("Campaign {} stopped by user", ctx.sendSmsId);
        }
    }

    private void processSingleBatch(CampaignContext ctx, List<SmsData> batch, boolean test, ExecutorService workerPool) {
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (SmsData sms : batch) {
            if (ctx.stopRequested.get()) break;

            CompletableFuture<Void> future = CompletableFuture.runAsync(
                () -> {
                    rateLimiter.acquire();
                    if (ctx.stopRequested.get()) return;

                    SmsUpdateResult result = sendSingleSms(sms, test);

                    boolean added = updateQueue.offer(result);
                    if (!added) {
                        flushUpdateQueue();
                        updateQueue.offer(result);
                    }

                    ctx.totalProcessed.incrementAndGet();
                    if (result.success) {
                        ctx.totalSuccess.incrementAndGet();
                    } else {
                        ctx.totalFailed.incrementAndGet();
                    }
                },
                workerPool
            );

            futures.add(future);
        }

        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get();
        } catch (Exception e) {
            log.error("Batch processing error: {}", e.getMessage());
        }
    }

    private SmsUpdateResult sendSingleSms(SmsData sms, boolean test) {
        SmsUpdateResult result = new SmsUpdateResult();
        result.smsId = sms.id;
        result.timestamp = Instant.now();

        try {
            if (test) {
                result.success = true;
                result.messageId = "TEST_" + sms.id + "_" + System.currentTimeMillis();
                return result;
            }

            if (simulationMode) {
                Thread.sleep(50);
                result.success = true;
                result.messageId = "SIM_" + UUID.randomUUID().toString().substring(0, 8);
                return result;
            }

            SendResult sendResult = smsService.goforSendFastResult(sms.sender, sms.receiver, sms.message);

            result.success = sendResult.isSuccess();
            result.messageId = sendResult.getMessageId();
            result.error = sendResult.getError();
        } catch (Exception e) {
            result.success = false;
            result.error = e.getMessage();
            log.error("Error sending SMS {}: {}", sms.id, e.getMessage());
        }

        return result;
    }

    public void flushUpdateQueue() {
        if (updateQueue.isEmpty()) return;

        List<SmsUpdateResult> batch = new ArrayList<>();
        updateQueue.drainTo(batch, updateBatchSize);

        if (batch.isEmpty()) return;

        try {
            List<SmsUpdateResult> successes = batch.stream().filter(r -> r.success).toList();
            List<SmsUpdateResult> failures = batch.stream().filter(r -> !r.success).toList();

            if (!successes.isEmpty()) {
                List<SmsBatchUpdateService.SmsUpdateResult> serviceResults = successes
                    .stream()
                    .map(r -> new SmsBatchUpdateService.SmsUpdateResult(r.smsId, r.success, r.messageId, r.error, r.timestamp))
                    .toList();

                batchUpdateService.batchUpdateSuccess(serviceResults);
            }

            if (!failures.isEmpty()) {
                List<SmsBatchUpdateService.SmsUpdateResult> serviceResults = failures
                    .stream()
                    .map(r -> new SmsBatchUpdateService.SmsUpdateResult(r.smsId, r.success, r.messageId, r.error, r.timestamp))
                    .toList();

                batchUpdateService.batchUpdateFailed(serviceResults);
            }
        } catch (Exception e) {
            log.error("Flush error: {}", e.getMessage(), e);
            updateQueue.addAll(batch);
        }
    }

    private void finalizeCampaign(CampaignContext ctx, SendSms sendSms, boolean test) {
        flushUpdateQueue();

        try {
            Thread.sleep(2000);
            flushUpdateQueue();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        long duration = Math.max(1, Duration.between(ctx.startTime, Instant.now()).getSeconds());
        double avgRate = ctx.totalProcessed.get() / (double) duration;

        log.info(
            "Campaign completed - SendSms: {}, Success: {}, Failed: {}, Duration: {}s, Rate: {} msg/s",
            ctx.sendSmsId,
            ctx.totalSuccess.get(),
            ctx.totalFailed.get(),
            duration,
            String.format("%.1f", avgRate)
        );

        try {
            sendSms.setInprocess(false);
            sendSms.setTotalSent(ctx.totalSuccess.get());
            sendSms.setTotalFailed(ctx.totalFailed.get());
            sendSms.setTotalSuccess(ctx.totalSuccess.get());
            sendSms.setBulkStatus("COMPLETED");

            int remaining = ctx.totalToProcess.get() - ctx.totalProcessed.get();
            sendSms.setTotalPending(remaining);

            if (ctx.totalFailed.get() == 0 && remaining == 0) {
                sendSms.setDeliveryStatus("sent");
                sendSms.setIsSent(true);
            } else if (ctx.totalSuccess.get() > 0) {
                sendSms.setDeliveryStatus("partial");
                sendSms.setIsSent(null);
            } else {
                sendSms.setDeliveryStatus("failed");
                sendSms.setIsSent(false);
            }

            int total = ctx.totalSuccess.get() + ctx.totalFailed.get();
            if (total > 0) {
                sendSms.setSuccessRate((ctx.totalSuccess.get() * 100.0) / total);
                sendSms.setFailureRate((ctx.totalFailed.get() * 100.0) / total);
            }

            sendSmsRepository.save(sendSms);
        } catch (Exception e) {
            log.error("Error updating SendSms: {}", e.getMessage());
        }

        if (!test && !simulationMode && ctx.totalSuccess.get() > 0 && ctx.subscriptions != null) {
            try {
                abonnementService.decrementQuotasAfterSend(ctx.subscriptions, MessageType.SMS, ctx.totalSuccess.get());
                abonnementRepository.saveAll(ctx.subscriptions);
            } catch (Exception e) {
                log.error("Error updating quotas: {}", e.getMessage());
            }
        }
    }

    public boolean stopBulkProcessing(Long sendSmsId) {
        log.warn("Stop requested for SendSms {}", sendSmsId);

        CampaignContext ctx = activeCampaigns.get(sendSmsId);
        if (ctx == null) {
            log.warn("Campaign {} not found or already completed", sendSmsId);
            return false;
        }

        ctx.stopRequested.set(true);
        return true;
    }

    public boolean isCampaignRunning(Long sendSmsId) {
        return activeCampaigns.containsKey(sendSmsId);
    }

    private List<SmsData> loadBatchBySendSmsId(Long sendSmsId, int offset, int limit) {
        return jdbcTemplate.query(
            """
            SELECT id, sender, receiver, msgdata
            FROM sms
            WHERE send_sms_id = ? AND delivery_status = 'pending'
            ORDER BY id
            LIMIT ? OFFSET ?
            """,
            (rs, rowNum) -> new SmsData(rs.getLong("id"), rs.getString("sender"), rs.getString("receiver"), rs.getString("msgdata")),
            sendSmsId,
            limit,
            offset
        );
    }

    private long countPendingBySendSmsId(Long sendSmsId) {
        Long count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM sms WHERE send_sms_id = ? AND delivery_status = 'pending'",
            Long.class,
            sendSmsId
        );
        return count != null ? count : 0;
    }

    private void waitForPendingUpdates() {
        int maxWait = 60;
        int waited = 0;

        while (!updateQueue.isEmpty() && waited < maxWait) {
            try {
                Thread.sleep(500);
                waited++;
                flushUpdateQueue();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void logProgress(CampaignContext ctx) {
        long elapsed = Duration.between(ctx.startTime, Instant.now()).toMillis();
        double rate = elapsed > 0 ? (ctx.totalProcessed.get() * 1000.0) / elapsed : 0;
        double progress = ctx.totalToProcess.get() > 0 ? (ctx.totalProcessed.get() * 100.0) / ctx.totalToProcess.get() : 0;

        log.info(
            "Progress: {}/{} ({}%) | Success: {} Failed: {} | Rate: {} msg/s",
            ctx.totalProcessed.get(),
            ctx.totalToProcess.get(),
            String.format("%.1f", progress),
            ctx.totalSuccess.get(),
            ctx.totalFailed.get(),
            String.format("%.1f", rate)
        );
    }

    private static class CampaignContext {

        final Long sendSmsId;
        final String bulkId;
        final Instant startTime;
        final AtomicBoolean stopRequested = new AtomicBoolean(false);
        final AtomicInteger totalToProcess = new AtomicInteger(0);
        final AtomicInteger totalProcessed = new AtomicInteger(0);
        final AtomicInteger totalSuccess = new AtomicInteger(0);
        final AtomicInteger totalFailed = new AtomicInteger(0);
        List<Abonnement> subscriptions;
        String lastError;

        CampaignContext(Long sendSmsId, String bulkId, Instant startTime) {
            this.sendSmsId = sendSmsId;
            this.bulkId = bulkId;
            this.startTime = startTime;
        }
    }

    private record SmsData(long id, String sender, String receiver, String message) {}

    private static class SmsUpdateResult {

        long smsId;
        boolean success;
        String messageId;
        String error;
        Instant timestamp;
    }
}
