package com.example.myproject.web.rest.dto;

public class DlrCallbackRequest {

    private String messageId;
    private Integer statusCode;
    private String phoneNumber;
    private Long timestamp;
    private String smscId;
    private String errorCode;

    // Constructeur vide
    public DlrCallbackRequest() {}

    // Constructeur complet
    public DlrCallbackRequest(String messageId, Integer statusCode, String phoneNumber, Long timestamp, String smscId, String errorCode) {
        this.messageId = messageId;
        this.statusCode = statusCode;
        this.phoneNumber = phoneNumber;
        this.timestamp = timestamp;
        this.smscId = smscId;
        this.errorCode = errorCode;
    }

    // ✅ BUILDER PATTERN
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String messageId;
        private Integer statusCode;
        private String phoneNumber;
        private Long timestamp;
        private String smscId;
        private String errorCode;

        public Builder messageId(String messageId) {
            this.messageId = messageId;
            return this;
        }

        public Builder statusCode(Integer statusCode) {
            this.statusCode = statusCode;
            return this;
        }

        public Builder phoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
            return this;
        }

        public Builder timestamp(Long timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder smscId(String smscId) {
            this.smscId = smscId;
            return this;
        }

        public Builder errorCode(String errorCode) {
            this.errorCode = errorCode;
            return this;
        }

        public DlrCallbackRequest build() {
            return new DlrCallbackRequest(messageId, statusCode, phoneNumber, timestamp, smscId, errorCode);
        }
    }

    // ✅ GETTERS
    public String getMessageId() {
        return messageId;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public String getSmscId() {
        return smscId;
    }

    public String getErrorCode() {
        return errorCode;
    }

    // ✅ SETTERS
    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public void setSmscId(String smscId) {
        this.smscId = smscId;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    // ✅ toString() pour debug
    @Override
    public String toString() {
        return (
            "DlrCallbackRequest{" +
            "messageId='" +
            messageId +
            '\'' +
            ", statusCode=" +
            statusCode +
            ", phoneNumber='" +
            phoneNumber +
            '\'' +
            ", timestamp=" +
            timestamp +
            ", smscId='" +
            smscId +
            '\'' +
            ", errorCode='" +
            errorCode +
            '\'' +
            '}'
        );
    }
}
