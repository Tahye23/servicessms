package com.example.myproject.web.rest.dto;

public class BulkStats {

    private long total = 0;
    private long inserted = 0;
    private long sent = 0;
    private long delivered = 0;
    private long read = 0;
    private long failed = 0;
    private long deliveryFailed = 0;
    private long pending = 0;

    // --- getters/setters (nécessaires car SendSmsResource appelle setX/getX) ---
    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public long getInserted() {
        return inserted;
    }

    public void setInserted(long inserted) {
        this.inserted = inserted;
    }

    public long getSent() {
        return sent;
    }

    public void setSent(long sent) {
        this.sent = sent;
    }

    public long getDelivered() {
        return delivered;
    }

    public void setDelivered(long delivered) {
        this.delivered = delivered;
    }

    public long getRead() {
        return read;
    }

    public void setRead(long read) {
        this.read = read;
    }

    public long getFailed() {
        return failed;
    }

    public void setFailed(long failed) {
        this.failed = failed;
    }

    public long getDeliveryFailed() {
        return deliveryFailed;
    }

    public void setDeliveryFailed(long deliveryFailed) {
        this.deliveryFailed = deliveryFailed;
    }

    public long getPending() {
        return pending;
    }

    public void setPending(long pending) {
        this.pending = pending;
    }

    // --- méthodes calculées (comme avant) ---
    public long getProcessed() {
        return sent + failed;
    }

    public long getSuccessful() {
        return delivered + read;
    }

    public double getSuccessRate() {
        return getProcessed() > 0 ? (getSuccessful() * 100.0) / getProcessed() : 0;
    }

    public double getDeliveryRate() {
        return sent > 0 ? (delivered * 100.0) / sent : 0;
    }

    public double getReadRate() {
        return delivered > 0 ? (read * 100.0) / delivered : 0;
    }
}
