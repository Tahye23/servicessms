package com.example.myproject.web.rest.dto;

import java.sql.Timestamp;
import java.time.Instant;

public class CampaignHistoryDTO {

    private Long campaignId;
    private Integer retryCount;
    private Instant lastRetryDate;
    private Instant firstAttemptDate;
    private Integer totalMessages;
    private Integer sentCount;
    private Integer failedCount;
    private Integer pendingCount;
    private Integer totalSuccess;
    private Integer totalFailed;
    private Integer totalPending;

    // Constructeur sans maxRetries
    public CampaignHistoryDTO(
        Long campaignId,
        Integer retryCount,
        Timestamp lastRetryDate,
        Timestamp firstAttemptDate,
        Integer totalMessages,
        Integer sentCount,
        Integer failedCount,
        Integer pendingCount,
        Integer totalSuccess,
        Integer totalFailed,
        Integer totalPending
    ) {
        this.campaignId = campaignId;
        this.retryCount = retryCount;
        this.lastRetryDate = lastRetryDate != null ? lastRetryDate.toInstant() : null;
        this.firstAttemptDate = firstAttemptDate != null ? firstAttemptDate.toInstant() : null;
        this.totalMessages = totalMessages;
        this.sentCount = sentCount;
        this.failedCount = failedCount;
        this.pendingCount = pendingCount;
        this.totalSuccess = totalSuccess;
        this.totalFailed = totalFailed;
        this.totalPending = totalPending;
    }

    public Long getCampaignId() {
        return campaignId;
    }

    public void setCampaignId(Long campaignId) {
        this.campaignId = campaignId;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public Instant getLastRetryDate() {
        return lastRetryDate;
    }

    public void setLastRetryDate(Instant lastRetryDate) {
        this.lastRetryDate = lastRetryDate;
    }

    public Instant getFirstAttemptDate() {
        return firstAttemptDate;
    }

    public void setFirstAttemptDate(Instant firstAttemptDate) {
        this.firstAttemptDate = firstAttemptDate;
    }

    public Integer getSentCount() {
        return sentCount;
    }

    public void setSentCount(Integer sentCount) {
        this.sentCount = sentCount;
    }

    public Integer getTotalMessages() {
        return totalMessages;
    }

    public void setTotalMessages(Integer totalMessages) {
        this.totalMessages = totalMessages;
    }

    public Integer getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(Integer failedCount) {
        this.failedCount = failedCount;
    }

    public Integer getPendingCount() {
        return pendingCount;
    }

    public void setPendingCount(Integer pendingCount) {
        this.pendingCount = pendingCount;
    }

    public Integer getTotalSuccess() {
        return totalSuccess;
    }

    public void setTotalSuccess(Integer totalSuccess) {
        this.totalSuccess = totalSuccess;
    }

    public Integer getTotalFailed() {
        return totalFailed;
    }

    public void setTotalFailed(Integer totalFailed) {
        this.totalFailed = totalFailed;
    }

    public Integer getTotalPending() {
        return totalPending;
    }

    public void setTotalPending(Integer totalPending) {
        this.totalPending = totalPending;
    }
}
