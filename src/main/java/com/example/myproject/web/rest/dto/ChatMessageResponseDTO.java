package com.example.myproject.web.rest.dto;

public class ChatMessageResponseDTO {

    private String messageId;
    private boolean success;

    public ChatMessageResponseDTO() {}

    public ChatMessageResponseDTO(String messageId, boolean success) {
        this.messageId = messageId;
        this.success = success;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }
    // getters + setters
}
