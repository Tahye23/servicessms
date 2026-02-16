package com.example.myproject.web.rest.dto;

import com.example.myproject.domain.enumeration.Channel;
import jakarta.validation.constraints.NotNull;

public class ChatRequestDTO {

    @NotNull
    private Long contactId;

    @NotNull
    private Channel channel;

    // getters & setters

    public Long getContactId() {
        return contactId;
    }

    public void setContactId(Long contactId) {
        this.contactId = contactId;
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }
}
