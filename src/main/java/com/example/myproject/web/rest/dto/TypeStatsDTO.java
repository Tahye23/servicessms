package com.example.myproject.web.rest.dto;

public class TypeStatsDTO {

    private String type; // "SMS" ou "WHATSAPP"
    private long total;
    private long success;
    private long failed;
    private long pending;
    private double unitPrice;

    public TypeStatsDTO(String type, long total, long success, long failed, long pending, double unitPrice) {
        this.type = type;
        this.total = total;
        this.success = success;
        this.failed = failed;
        this.pending = pending;
        this.unitPrice = unitPrice;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public long getSuccess() {
        return success;
    }

    public void setSuccess(long success) {
        this.success = success;
    }

    public long getFailed() {
        return failed;
    }

    public void setFailed(long failed) {
        this.failed = failed;
    }

    public long getPending() {
        return pending;
    }

    public void setPending(long pending) {
        this.pending = pending;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(double unitPrice) {
        this.unitPrice = unitPrice;
    }
    // getters/setters
}
