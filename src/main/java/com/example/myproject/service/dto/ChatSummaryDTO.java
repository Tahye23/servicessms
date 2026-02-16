package com.example.myproject.service.dto;

import java.time.Instant;

public class ChatSummaryDTO {

    private Long chatId;
    private Instant lastUpdated;

    public ChatSummaryDTO(Long chatId, Instant lastUpdated) {
        this.chatId = chatId;
        this.lastUpdated = lastUpdated;
    }

    public Instant getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Instant lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public Long getChatId() {
        return chatId;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }
}
