package com.example.myproject.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty; // 🚀 AJOUT DE L'IMPORT

public class ProgressStatus {

    private final int total;
    private final int inserted;
    private final double percentage;
    private final double insertionRate; // Contacts per second
    private final double estimatedTimeRemaining; // Temps restant estimé en secondes
    private final boolean completed;

    @JsonProperty("current")
    public int getCurrent() {
        return inserted;
    }

    public ProgressStatus(int total, int inserted, double insertionRate, double estimatedTimeRemaining, boolean completed) {
        this.total = total;
        this.inserted = inserted;
        this.percentage = total > 0 ? (inserted * 100.0) / total : 0.0;
        this.insertionRate = insertionRate;
        this.estimatedTimeRemaining = estimatedTimeRemaining;
        this.completed = completed;
    }

    public int getTotal() {
        return total;
    }

    public int getInserted() {
        return inserted;
    }

    public double getPercentage() {
        return percentage;
    }

    public double getInsertionRate() {
        return insertionRate;
    }

    public double getEstimatedTimeRemaining() {
        return estimatedTimeRemaining;
    }

    public boolean isCompleted() {
        return completed;
    }

    @Override
    public String toString() {
        return String.format(
            "ProgressStatus{total=%d, inserted=%d, percentage=%.1f%%, completed=%s}",
            total,
            inserted,
            percentage,
            completed
        );
    }
}
