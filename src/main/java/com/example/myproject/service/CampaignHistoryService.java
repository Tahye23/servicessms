package com.example.myproject.service;

import com.example.myproject.domain.SendSms;
import com.example.myproject.domain.SendSmsHistory;
import com.example.myproject.repository.SendSmsHistoryRepository;
import com.example.myproject.repository.SendSmsRepository;
import com.example.myproject.service.dto.RetryAttemptDTO;
import com.example.myproject.web.rest.dto.CampaignHistoryDTO;
import jakarta.persistence.EntityNotFoundException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CampaignHistoryService {

    private static final Logger log = LoggerFactory.getLogger(CampaignHistoryService.class);

    @Autowired
    private SendSmsRepository sendSmsRepository;

    @Autowired
    private SendSmsHistoryRepository sendSmsHistoryRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveRetryAttempt(SendSms sendSms, int attemptNumber) {
        try {
            SendSmsHistory historyEntry = new SendSmsHistory(
                sendSms.getId(),
                attemptNumber,
                sendSms.getTotalSuccess() != null ? sendSms.getTotalSuccess() : 0,
                sendSms.getTotalFailed() != null ? sendSms.getTotalFailed() : 0,
                sendSms.getTotalPending() != null ? sendSms.getTotalPending() : 0,
                sendSms.getLast_error()
            );

            historyEntry.setCompletionStatus("IN_PROGRESS");
            historyEntry.setStartTime(Instant.now());

            sendSmsHistoryRepository.save(historyEntry);
            log.info("History created for SendSms {} - Attempt {} - Status: IN_PROGRESS", sendSms.getId(), attemptNumber);
        } catch (Exception e) {
            log.error("Error saving history for SendSms {}: {}", sendSms.getId(), e.getMessage());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateRetryAttemptCompleted(
        Long sendSmsId,
        int attemptNumber,
        int totalSuccess,
        int totalFailed,
        int totalPending,
        String lastError
    ) {
        try {
            Optional<SendSmsHistory> historyOpt = sendSmsHistoryRepository.findBySendSmsIdAndRetryCount(sendSmsId, attemptNumber);

            if (historyOpt.isPresent()) {
                SendSmsHistory history = historyOpt.get();
                history.setTotalSuccess(totalSuccess);
                history.setTotalFailed(totalFailed);
                history.setTotalPending(totalPending);
                history.setLastError(lastError);
                history.setEndTime(Instant.now());
                history.setCompletionStatus("COMPLETED");
                history.calculateAndSetDuration();

                sendSmsHistoryRepository.save(history);

                log.info(
                    "SendSms {} - Attempt {} completed - Duration: {}s - Success: {}, Failed: {}",
                    sendSmsId,
                    attemptNumber,
                    history.getDurationSeconds(),
                    totalSuccess,
                    totalFailed
                );
            } else {
                log.warn("History not found for SendSms {} - Attempt {}", sendSmsId, attemptNumber);
            }
        } catch (Exception e) {
            log.error("Error updating completed history for SendSms {}: {}", sendSmsId, e.getMessage());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateRetryAttemptStopped(
        Long sendSmsId,
        int attemptNumber,
        int totalSuccess,
        int totalFailed,
        int totalPending,
        String lastError
    ) {
        try {
            Optional<SendSmsHistory> historyOpt = sendSmsHistoryRepository.findBySendSmsIdAndRetryCount(sendSmsId, attemptNumber);

            if (historyOpt.isPresent()) {
                SendSmsHistory history = historyOpt.get();
                history.setTotalSuccess(totalSuccess);
                history.setTotalFailed(totalFailed);
                history.setTotalPending(totalPending);
                history.setLastError(lastError != null ? lastError : "Stopped by user");
                history.setEndTime(Instant.now());
                history.setCompletionStatus("STOPPED_BY_USER");
                history.calculateAndSetDuration();

                sendSmsHistoryRepository.save(history);

                log.info(
                    "SendSms {} - Attempt {} stopped - Duration: {}s - Success: {}, Failed: {}",
                    sendSmsId,
                    attemptNumber,
                    history.getDurationSeconds(),
                    totalSuccess,
                    totalFailed
                );
            } else {
                log.warn("History not found for SendSms {} - Attempt {}", sendSmsId, attemptNumber);
            }
        } catch (Exception e) {
            log.error("Error updating stopped history for SendSms {}: {}", sendSmsId, e.getMessage());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateRetryAttemptError(
        Long sendSmsId,
        int attemptNumber,
        int totalSuccess,
        int totalFailed,
        int totalPending,
        String errorMessage
    ) {
        try {
            Optional<SendSmsHistory> historyOpt = sendSmsHistoryRepository.findBySendSmsIdAndRetryCount(sendSmsId, attemptNumber);

            if (historyOpt.isPresent()) {
                SendSmsHistory history = historyOpt.get();
                history.setTotalSuccess(totalSuccess);
                history.setTotalFailed(totalFailed);
                history.setTotalPending(totalPending);
                history.setLastError(errorMessage);
                history.setEndTime(Instant.now());
                history.setCompletionStatus("ERROR");
                history.calculateAndSetDuration();

                sendSmsHistoryRepository.save(history);

                log.error(
                    "SendSms {} - Attempt {} error - Duration: {}s - Error: {}",
                    sendSmsId,
                    attemptNumber,
                    history.getDurationSeconds(),
                    errorMessage
                );
            }
        } catch (Exception e) {
            log.error("Error updating error history for SendSms {}: {}", sendSmsId, e.getMessage());
        }
    }

    public CampaignHistoryDTO getCampaignHistory(Long campaignId) {
        log.info("Retrieving history for campaign {}", campaignId);

        try {
            SendSms campaign = sendSmsRepository.findById(campaignId).orElseThrow(() -> new EntityNotFoundException("Campaign not found"));

            String sql =
                """
                SELECT
                    COALESCE(ss.retry_count, 0) as retry_count,
                    ss.last_retry_date,
                    COALESCE(ss.total_success, 0) as total_success,
                    COALESCE(ss.total_failure, 0) as total_failed,
                    COALESCE(ss.total_pending, 0) as total_pending,
                    ss.sendate_envoi as first_attempt_date,
                    COALESCE(ss.total_recipients, 0) as total_messages,
                    COUNT(s.id) as sms_count,
                    COUNT(CASE WHEN s.is_sent = true THEN 1 END) as sent_count,
                    COUNT(CASE WHEN s.status = 'FAILED' THEN 1 END) as failed_count,
                    COUNT(CASE WHEN s.delivery_status = 'pending' THEN 1 END) as pending_count
                FROM send_sms ss
                LEFT JOIN sms s ON s.send_sms_id = ss.id
                WHERE ss.id = ?
                GROUP BY ss.id, ss.retry_count, ss.last_retry_date,
                         ss.total_success, ss.total_failure, ss.total_pending,
                         ss.sendate_envoi, ss.total_recipients
                """;

            var result = jdbcTemplate.queryForMap(sql, campaignId);

            CampaignHistoryDTO dto = new CampaignHistoryDTO(
                campaignId,
                ((Number) result.get("retry_count")).intValue(),
                (Timestamp) result.get("last_retry_date"),
                (Timestamp) result.get("first_attempt_date"),
                ((Number) result.get("total_messages")).intValue(),
                ((Number) result.get("sent_count")).intValue(),
                ((Number) result.get("failed_count")).intValue(),
                ((Number) result.get("pending_count")).intValue(),
                ((Number) result.get("total_success")).intValue(),
                ((Number) result.get("total_failed")).intValue(),
                ((Number) result.get("total_pending")).intValue()
            );

            log.info("History retrieved: {} attempts", dto.getRetryCount());
            return dto;
        } catch (Exception e) {
            log.error("Error retrieving campaign history: {}", e.getMessage(), e);
            throw new RuntimeException("Cannot retrieve campaign history", e);
        }
    }

    public List<RetryAttemptDTO> getRetryHistory(Long campaignId) {
        log.info("Retrieving detailed history for campaign {}", campaignId);

        try {
            List<SendSmsHistory> historyList = sendSmsHistoryRepository.findBySendSmsIdOrderByRetryCountAsc(campaignId);

            if (historyList.isEmpty()) {
                log.warn("No history found for campaign {}", campaignId);
                return List.of();
            }

            List<RetryAttemptDTO> result = historyList
                .stream()
                .map(
                    history ->
                        new RetryAttemptDTO(
                            history.getRetryCount(),
                            history.getAttemptDate(),
                            history.getTotalSuccess() != null ? history.getTotalSuccess() : 0,
                            history.getTotalFailed() != null ? history.getTotalFailed() : 0,
                            history.getLastError(),
                            history.getStartTime(),
                            history.getEndTime(),
                            history.getDurationSeconds(),
                            history.getCompletionStatus()
                        )
                )
                .collect(Collectors.toList());

            log.info("{} attempts retrieved", result.size());
            return result;
        } catch (Exception e) {
            log.error("Error retrieving detailed history: {}", e.getMessage(), e);
            return List.of();
        }
    }
}
