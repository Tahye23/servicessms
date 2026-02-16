package com.example.myproject.web.rest.dto;

import com.example.myproject.domain.enumeration.MessageType;

public class ChatMessageRequest {

    private Long template_id;
    private Long chatId;
    private MessageType type;

    public Long getTemplate_id() {
        return template_id;
    }

    public void setTemplate_id(Long template_id) {
        this.template_id = template_id;
    }

    public Long getChatId() {
        return chatId;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }
}
