package com.example.myproject.domain;

import jakarta.persistence.*;
import java.time.Duration;
import java.time.Instant;

@Entity
@Table(name = "send_sms_history")
public class SendSmsHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "send_sms_id", nullable = false)
    private Long sendSmsId;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount;

    @Column(name = "attempt_date", nullable = false)
    private Instant attemptDate;

    // ✅ NOUVEAUX CHAMPS POUR LE TIMING
    @Column(name = "start_time")
    private Instant startTime;

    @Column(name = "end_time")
    private Instant endTime;

    @Column(name = "duration_seconds")
    private Long durationSeconds;

    // ✅ NOUVEAU CHAMP POUR LE STATUT
    @Column(name = "completion_status", length = 20)
    private String completionStatus; // "COMPLETED", "STOPPED_BY_USER", "ERROR", "IN_PROGRESS"

    @Column(name = "total_success")
    private Integer totalSuccess = 0;

    @Column(name = "total_failed")
    private Integer totalFailed = 0;

    @Column(name = "total_pending")
    private Integer totalPending = 0;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "created_at")
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (attemptDate == null) {
            attemptDate = Instant.now();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (startTime == null) {
            startTime = Instant.now();
        }
        if (completionStatus == null) {
            completionStatus = "IN_PROGRESS";
        }
    }

    // ✅ MÉTHODE POUR CALCULER LA DURÉE
    public void calculateAndSetDuration() {
        if (startTime != null && endTime != null) {
            this.durationSeconds = Duration.between(startTime, endTime).getSeconds();
        }
    }

    // Constructeurs
    public SendSmsHistory() {}

    public SendSmsHistory(
        Long sendSmsId,
        Integer retryCount,
        Integer totalSuccess,
        Integer totalFailed,
        Integer totalPending,
        String lastError
    ) {
        this.sendSmsId = sendSmsId;
        this.retryCount = retryCount;
        this.totalSuccess = totalSuccess;
        this.totalFailed = totalFailed;
        this.totalPending = totalPending;
        this.lastError = lastError;
        this.attemptDate = Instant.now();
        this.startTime = Instant.now();
        this.createdAt = Instant.now();
        this.completionStatus = "IN_PROGRESS";
    }

    // Getters et setters existants...
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSendSmsId() {
        return sendSmsId;
    }

    public void setSendSmsId(Long sendSmsId) {
        this.sendSmsId = sendSmsId;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public Instant getAttemptDate() {
        return attemptDate;
    }

    public void setAttemptDate(Instant attemptDate) {
        this.attemptDate = attemptDate;
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

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    // ✅ NOUVEAUX GETTERS/SETTERS
    public Instant getStartTime() {
        return startTime;
    }

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
        calculateAndSetDuration();
    }

    public Long getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Long durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public String getCompletionStatus() {
        return completionStatus;
    }

    public void setCompletionStatus(String completionStatus) {
        this.completionStatus = completionStatus;
    }
}
