package com.example.myproject.service;

import com.example.myproject.domain.SendSms;
import com.example.myproject.repository.SendSmsRepository;
import com.example.myproject.repository.SmsRepository;
import com.example.myproject.web.rest.dto.BulkProgressResponse;
import com.example.myproject.web.rest.dto.BulkStats;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SendSmsProgressService {

    private static final Logger log = LoggerFactory.getLogger(SendSmsProgressService.class);

    @Autowired
    private SendSmsRepository sendSmsRepository;

    @Autowired
    private SmsRepository smsRepository;

    @Autowired
    private SmsBulkService smsBulkService;

    public BulkProgressResponse calculateBulkProgress(Long sendSmsId) {
        SendSms sendSms = sendSmsRepository.findById(sendSmsId).orElseThrow(() -> new RuntimeException("SendSms not found: " + sendSmsId));

        String bulkId = sendSms.getBulkId();
        if (bulkId == null) {
            return BulkProgressResponse.nonBulk();
        }

        return calculateProgress(sendSms, bulkId);
    }

    private BulkProgressResponse calculateProgress(SendSms sendSms, String bulkId) {
        long totalRecipients = safeLong(sendSms.getTotalRecipients());

        BulkStats stats = calculateStatsFromDb(bulkId, totalRecipients);

        double insertionProgress = totalRecipients > 0 ? (stats.getInserted() * 100.0) / totalRecipients : 0.0;

        long processed = stats.getSent() + stats.getFailed();
        double sendProgress = stats.getInserted() > 0 ? (processed * 100.0) / stats.getInserted() : 0.0;

        Instant startTime = sendSms.getBulkCreatedAt() != null
            ? sendSms.getBulkCreatedAt()
            : sendSms.getLastRetryDate() != null ? sendSms.getLastRetryDate() : Instant.now();

        Duration elapsed = Duration.between(startTime, Instant.now());
        long elapsedSeconds = Math.max(1, elapsed.getSeconds());

        double currentRate = (double) stats.getSent() / elapsedSeconds;

        long pendingInsertion = Math.max(0, totalRecipients - stats.getInserted());
        long pendingSend = Math.max(0, stats.getInserted() - processed);

        long etaInsertSeconds = currentRate > 0 ? (long) (pendingInsertion / currentRate) : -1;
        long etaSendSeconds = currentRate > 0 ? (long) (pendingSend / currentRate) : -1;

        boolean insertionComplete = stats.getInserted() >= totalRecipients;
        boolean isRunning = smsBulkService.isCampaignRunning(sendSms.getId());

        return BulkProgressResponse.builder()
            .bulkId(bulkId)
            .sendSmsId(sendSms.getId())
            .totalRecipients(totalRecipients)
            .stats(stats)
            .insertionProgress(insertionProgress)
            .sendProgress(sendProgress)
            .insertionComplete(insertionComplete)
            .currentRate(currentRate)
            .elapsedSeconds(elapsedSeconds)
            .etaInsertSeconds(etaInsertSeconds)
            .etaSendSeconds(etaSendSeconds)
            .inProcess(isRunning)
            .lastUpdate(Instant.now())
            .build();
    }

    private BulkStats calculateStatsFromDb(String bulkId, long totalRecipients) {
        BulkStats stats = new BulkStats();

        List<Object[]> statusCounts = smsRepository.countByBulkIdGroupByStatus(bulkId);
        for (Object[] row : statusCounts) {
            String status = safeString(row[0]);
            long count = toLong(row[1]);

            switch (status.toUpperCase()) {
                case "SENT" -> stats.setSent(count);
                case "FAILED" -> stats.setFailed(count);
                case "PENDING" -> stats.setPending(count);
            }
        }

        List<Object[]> deliveryCounts = smsRepository.countByBulkIdGroupByDeliveryStatus(bulkId);
        for (Object[] row : deliveryCounts) {
            String deliveryStatus = safeString(row[0]);
            long count = toLong(row[1]);

            switch (deliveryStatus.toLowerCase()) {
                case "delivered" -> stats.setDelivered(count);
                case "read" -> stats.setRead(count);
                case "failed" -> stats.setDeliveryFailed(count);
            }
        }

        stats.setInserted(smsRepository.countByBulkId(bulkId));
        stats.setTotal(totalRecipients);

        return stats;
    }

    private static long toLong(Object o) {
        if (o == null) return 0L;
        if (o instanceof Long l) return l;
        if (o instanceof Integer i) return i.longValue();
        if (o instanceof Number n) return n.longValue();
        return 0L;
    }

    private static long safeLong(Number v) {
        return v == null ? 0L : v.longValue();
    }

    private static String safeString(Object o) {
        return o == null ? "" : o.toString();
    }
}
