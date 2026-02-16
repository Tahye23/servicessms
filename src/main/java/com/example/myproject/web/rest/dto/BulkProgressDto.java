package com.example.myproject.web.rest.dto;

public class BulkProgressDto {

    private final int totalRecipients;
    private final long inserted;
    private final long sent;
    private final long failed;
    private final long pendingInsertion;
    private final long pendingSend;
    private final double insertionProgress; // en %
    private final double sendProgress; // en %
    private final double ratePerSecond; // SMS/s
    private final long elapsedSeconds;
    private final long remainingInsertSeconds;
    private final long remainingSendSeconds;
    private final boolean insertionComplete;

    public BulkProgressDto(
        int totalRecipients,
        long inserted,
        long sent,
        long failed,
        long pendingInsertion,
        long pendingSend,
        double insertionProgress,
        double sendProgress,
        double ratePerSecond,
        long elapsedSeconds,
        long remainingInsertSeconds,
        long remainingSendSeconds,
        boolean insertionComplete
    ) {
        this.totalRecipients = totalRecipients;
        this.inserted = inserted;
        this.sent = sent;
        this.failed = failed;
        this.pendingInsertion = pendingInsertion;
        this.pendingSend = pendingSend;
        this.insertionProgress = insertionProgress;
        this.sendProgress = sendProgress;
        this.ratePerSecond = ratePerSecond;
        this.elapsedSeconds = elapsedSeconds;
        this.remainingInsertSeconds = remainingInsertSeconds;
        this.remainingSendSeconds = remainingSendSeconds;
        this.insertionComplete = insertionComplete;
    }

    public boolean isInsertionComplete() {
        return insertionComplete;
    }

    public int getTotalRecipients() {
        return totalRecipients;
    }

    public long getInserted() {
        return inserted;
    }

    public long getSent() {
        return sent;
    }

    public long getFailed() {
        return failed;
    }

    public long getPendingInsertion() {
        return pendingInsertion;
    }

    public long getPendingSend() {
        return pendingSend;
    }

    public double getInsertionProgress() {
        return insertionProgress;
    }

    public double getSendProgress() {
        return sendProgress;
    }

    public double getRatePerSecond() {
        return ratePerSecond;
    }

    public long getElapsedSeconds() {
        return elapsedSeconds;
    }

    public long getRemainingInsertSeconds() {
        return remainingInsertSeconds;
    }

    public long getRemainingSendSeconds() {
        return remainingSendSeconds;
    }
}
