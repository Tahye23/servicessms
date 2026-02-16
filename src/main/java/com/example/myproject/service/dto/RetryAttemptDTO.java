package com.example.myproject.service.dto;

import java.time.Instant;

public class RetryAttemptDTO {

    private Integer retryCount;
    private Instant attemptDate;
    private Integer totalSuccess;
    private Integer totalFailed;
    private String lastError;

    // ✅ NOUVEAUX CHAMPS
    private Instant startTime;
    private Instant endTime;
    private Long durationSeconds;
    private String completionStatus; // "COMPLETED", "STOPPED_BY_USER", "ERROR", "IN_PROGRESS"

    // Constructeur complet
    public RetryAttemptDTO(
        Integer retryCount,
        Instant attemptDate,
        Integer totalSuccess,
        Integer totalFailed,
        String lastError,
        Instant startTime,
        Instant endTime,
        Long durationSeconds,
        String completionStatus
    ) {
        this.retryCount = retryCount;
        this.attemptDate = attemptDate;
        this.totalSuccess = totalSuccess;
        this.totalFailed = totalFailed;
        this.lastError = lastError;
        this.startTime = startTime;
        this.endTime = endTime;
        this.durationSeconds = durationSeconds;
        this.completionStatus = completionStatus;
    }

    // Constructeur pour compatibilité (ancien format)
    public RetryAttemptDTO(Integer retryCount, Instant attemptDate, Integer totalSuccess, Integer totalFailed, String lastError) {
        this(retryCount, attemptDate, totalSuccess, totalFailed, lastError, null, null, null, "UNKNOWN");
    }

    // Getters et setters
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

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

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

    // Méthode utilitaire pour formater la durée
    public String getFormattedDuration() {
        if (durationSeconds == null || durationSeconds == 0) {
            return "N/A";
        }

        long hours = durationSeconds / 3600;
        long minutes = (durationSeconds % 3600) / 60;
        long seconds = durationSeconds % 60;

        if (hours > 0) {
            return String.format("%dh %02dm %02ds", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%dm %02ds", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }

    // Méthode pour obtenir le statut en français
    public String getCompletionStatusLabel() {
        if (completionStatus == null) {
            return "Inconnu";
        }

        return switch (completionStatus) {
            case "COMPLETED" -> "Terminé";
            case "STOPPED_BY_USER" -> "Arrêté par l'utilisateur";
            case "ERROR" -> "Erreur";
            case "IN_PROGRESS" -> "En cours";
            default -> "Inconnu";
        };
    }

    // Méthode pour vérifier si la tentative est terminée
    public boolean isCompleted() {
        return "COMPLETED".equals(completionStatus) || "STOPPED_BY_USER".equals(completionStatus) || "ERROR".equals(completionStatus);
    }

    @Override
    public String toString() {
        return (
            "RetryAttemptDTO{" +
            "retryCount=" +
            retryCount +
            ", attemptDate=" +
            attemptDate +
            ", totalSuccess=" +
            totalSuccess +
            ", totalFailed=" +
            totalFailed +
            ", durationSeconds=" +
            durationSeconds +
            ", completionStatus='" +
            completionStatus +
            '\'' +
            ", lastError='" +
            lastError +
            '\'' +
            '}'
        );
    }
}
