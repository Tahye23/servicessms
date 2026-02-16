package com.example.myproject.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "sms_dlr")
public class SmsDlr {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_id", nullable = false)
    private String messageId;

    @Column(name = "sms_id")
    private Long smsId;

    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    @Column(name = "dlr_status_code", nullable = false)
    private Integer dlrStatusCode;

    @Column(name = "dlr_status", nullable = false, length = 50)
    private String dlrStatus;

    @Column(name = "dlr_timestamp")
    private Long dlrTimestamp;

    @Column(name = "smsc_id", length = 50)
    private String smscId;

    @Column(name = "error_code", length = 50)
    private String errorCode;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "processed", nullable = false)
    private Boolean processed = false;

    @Column(name = "processed_at")
    private Instant processedAt;

    // Constructeurs
    public SmsDlr() {}

    // Getters et Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public Long getSmsId() {
        return smsId;
    }

    public void setSmsId(Long smsId) {
        this.smsId = smsId;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public Integer getDlrStatusCode() {
        return dlrStatusCode;
    }

    public void setDlrStatusCode(Integer dlrStatusCode) {
        this.dlrStatusCode = dlrStatusCode;
    }

    public String getDlrStatus() {
        return dlrStatus;
    }

    public void setDlrStatus(String dlrStatus) {
        this.dlrStatus = dlrStatus;
    }

    public Long getDlrTimestamp() {
        return dlrTimestamp;
    }

    public void setDlrTimestamp(Long dlrTimestamp) {
        this.dlrTimestamp = dlrTimestamp;
    }

    public String getSmscId() {
        return smscId;
    }

    public void setSmscId(String smscId) {
        this.smscId = smscId;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(Instant receivedAt) {
        this.receivedAt = receivedAt;
    }

    public Boolean getProcessed() {
        return processed;
    }

    public void setProcessed(Boolean processed) {
        this.processed = processed;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(Instant processedAt) {
        this.processedAt = processedAt;
    }
}
