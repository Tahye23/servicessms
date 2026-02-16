package com.example.myproject.web.rest.dto;

import java.time.Instant;

public class BulkProgressResponse {

    private String bulkId;
    private Long sendSmsId;
    private long totalRecipients;
    private BulkStats stats;
    private double insertionProgress;
    private double sendProgress;
    private boolean insertionComplete;
    private double currentRate;
    private long elapsedSeconds;
    private long etaInsertSeconds;
    private long etaSendSeconds;
    private Boolean inProcess;
    private Instant lastUpdate;
    private String error;

    public BulkProgressResponse() {}

    public String getBulkId() {
        return bulkId;
    }

    public void setBulkId(String bulkId) {
        this.bulkId = bulkId;
    }

    public Long getSendSmsId() {
        return sendSmsId;
    }

    public void setSendSmsId(Long sendSmsId) {
        this.sendSmsId = sendSmsId;
    }

    public long getTotalRecipients() {
        return totalRecipients;
    }

    public void setTotalRecipients(long totalRecipients) {
        this.totalRecipients = totalRecipients;
    }

    public BulkStats getStats() {
        return stats;
    }

    public void setStats(BulkStats stats) {
        this.stats = stats;
    }

    public double getInsertionProgress() {
        return insertionProgress;
    }

    public void setInsertionProgress(double insertionProgress) {
        this.insertionProgress = insertionProgress;
    }

    public double getSendProgress() {
        return sendProgress;
    }

    public void setSendProgress(double sendProgress) {
        this.sendProgress = sendProgress;
    }

    public boolean isInsertionComplete() {
        return insertionComplete;
    }

    public void setInsertionComplete(boolean insertionComplete) {
        this.insertionComplete = insertionComplete;
    }

    public double getCurrentRate() {
        return currentRate;
    }

    public void setCurrentRate(double currentRate) {
        this.currentRate = currentRate;
    }

    public long getElapsedSeconds() {
        return elapsedSeconds;
    }

    public void setElapsedSeconds(long elapsedSeconds) {
        this.elapsedSeconds = elapsedSeconds;
    }

    public long getEtaInsertSeconds() {
        return etaInsertSeconds;
    }

    public void setEtaInsertSeconds(long etaInsertSeconds) {
        this.etaInsertSeconds = etaInsertSeconds;
    }

    public long getEtaSendSeconds() {
        return etaSendSeconds;
    }

    public void setEtaSendSeconds(long etaSendSeconds) {
        this.etaSendSeconds = etaSendSeconds;
    }

    public Boolean getInProcess() {
        return inProcess;
    }

    public void setInProcess(Boolean inProcess) {
        this.inProcess = inProcess;
    }

    public Instant getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(Instant lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    // --- Builder manuel pour compatibilité avec .builder() dans SendSmsResource ---
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final BulkProgressResponse inst = new BulkProgressResponse();

        public Builder bulkId(String v) {
            inst.setBulkId(v);
            return this;
        }

        public Builder sendSmsId(Long v) {
            inst.setSendSmsId(v);
            return this;
        }

        public Builder totalRecipients(long v) {
            inst.setTotalRecipients(v);
            return this;
        }

        public Builder stats(BulkStats v) {
            inst.setStats(v);
            return this;
        }

        public Builder insertionProgress(double v) {
            inst.setInsertionProgress(v);
            return this;
        }

        public Builder sendProgress(double v) {
            inst.setSendProgress(v);
            return this;
        }

        public Builder insertionComplete(boolean v) {
            inst.setInsertionComplete(v);
            return this;
        }

        public Builder currentRate(double v) {
            inst.setCurrentRate(v);
            return this;
        }

        public Builder elapsedSeconds(long v) {
            inst.setElapsedSeconds(v);
            return this;
        }

        public Builder etaInsertSeconds(long v) {
            inst.setEtaInsertSeconds(v);
            return this;
        }

        public Builder etaSendSeconds(long v) {
            inst.setEtaSendSeconds(v);
            return this;
        }

        public Builder inProcess(Boolean v) {
            inst.setInProcess(v);
            return this;
        }

        public Builder lastUpdate(Instant v) {
            inst.setLastUpdate(v);
            return this;
        }

        public Builder error(String v) {
            inst.setError(v);
            return this;
        }

        public BulkProgressResponse build() {
            return inst;
        }
    }

    public static BulkProgressResponse error(String message) {
        return BulkProgressResponse.builder().error(message).lastUpdate(Instant.now()).build();
    }

    public static BulkProgressResponse nonBulk() {
        return BulkProgressResponse.builder()
            .totalRecipients(1)
            .stats(new BulkStats())
            .insertionComplete(true)
            .lastUpdate(Instant.now())
            .build();
    }
}
