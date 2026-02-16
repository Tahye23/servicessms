package com.example.myproject.web.rest.dto;

public class SmsProgress {

    private int processed;
    private int total;

    public SmsProgress(int processed, int total) {
        this.processed = processed;
        this.total = total;
    }

    public int getProcessed() {
        return processed;
    }

    public void setProcessed(int processed) {
        this.processed = processed;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }
}
