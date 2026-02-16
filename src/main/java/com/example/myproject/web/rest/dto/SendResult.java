package com.example.myproject.web.rest.dto;

/**
 * DTO pour le résultat d'envoi SMS avec détails opérateur
 */
public class SendResult {

    private final boolean success;
    private final String messageId;
    private final String error;
    private final Integer commandStatus;

    private SendResult(boolean success, String messageId, String error, Integer commandStatus) {
        this.success = success;
        this.messageId = messageId;
        this.error = error;
        this.commandStatus = commandStatus;
    }

    public static SendResult ok(String messageId) {
        return new SendResult(true, messageId, null, null);
    }

    public static SendResult fail(String error, Integer commandStatus) {
        return new SendResult(false, null, error, commandStatus);
    }

    // ✅ GETTERS NÉCESSAIRES
    public boolean isSuccess() {
        return success;
    }

    public String getMessageId() {
        return messageId;
    }

    public String getError() {
        return error;
    }

    public Integer getCommandStatus() {
        return commandStatus;
    }

    @Override
    public String toString() {
        return success
            ? "SendResult{success=true, messageId='" + messageId + "'}"
            : "SendResult{success=false, error='" + error + "', commandStatus=" + commandStatus + "}";
    }
}
