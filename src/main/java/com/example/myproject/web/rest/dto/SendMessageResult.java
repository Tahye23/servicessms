package com.example.myproject.web.rest.dto;

public class SendMessageResult {

    private final boolean success;
    private final String messageId;
    private final String error;

    // Constructeur complet (utilisé dans WhatsAppBulkService)
    public SendMessageResult(boolean success, String messageId, String error) {
        this.success = success;
        this.messageId = messageId;
        this.error = error;
    }

    // Constructeur succès (garde la compatibilité)
    public SendMessageResult(String messageId) {
        this.success = true;
        this.messageId = messageId;
        this.error = null;
    }

    // Constructeur avec messageId et error (garde la compatibilité)
    public SendMessageResult(String messageId, String error) {
        this.success = messageId != null;
        this.messageId = messageId;
        this.error = error;
    }

    public static SendMessageResult error(String err) {
        return new SendMessageResult(false, null, err);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessageId() {
        return messageId;
    }

    public String getError() {
        return error;
    }
}
