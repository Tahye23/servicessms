package com.example.myproject.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "message_delivery_status", indexes = { @Index(name = "idx_message_delivery_status_message_id", columnList = "message_id") })
public class MessageDeliveryStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_id", nullable = false)
    private String messageId;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "error_title")
    private String errorTitle;

    @Column(name = "error_details")
    private String errorDetails;

    @Column(name = "phone")
    private String phone;

    @Column(name = "received_at")
    private Instant receivedAt = Instant.now();

    @Column
    private Instant processedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(Instant processedAt) {
        this.processedAt = processedAt;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getErrorTitle() {
        return errorTitle;
    }

    public void setErrorTitle(String errorTitle) {
        this.errorTitle = errorTitle;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getErrorDetails() {
        return errorDetails;
    }

    public void setErrorDetails(String errorDetails) {
        this.errorDetails = errorDetails;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(Instant receivedAt) {
        this.receivedAt = receivedAt;
    }
}
