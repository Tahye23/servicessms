package com.example.myproject.web.rest.dto.flow;

import com.example.myproject.service.dto.flow.WhatsAppResponse;
import java.util.ArrayList;
import java.util.List;

public class WhatsAppMultiResponse {

    private List<WhatsAppResponse> responses = new ArrayList<>();
    private boolean isSequential = true;
    private int delayBetweenMessages = 1000; // 1 seconde par défaut

    public WhatsAppMultiResponse() {}

    public WhatsAppMultiResponse(WhatsAppResponse singleResponse) {
        this.responses.add(singleResponse);
    }

    public void addResponse(WhatsAppResponse response) {
        this.responses.add(response);
    }

    public boolean hasMultipleResponses() {
        return responses.size() > 1;
    }

    public WhatsAppResponse getFirstResponse() {
        return responses.isEmpty() ? null : responses.get(0);
    }

    // Getters et setters
    public List<WhatsAppResponse> getResponses() {
        return responses;
    }

    public void setResponses(List<WhatsAppResponse> responses) {
        this.responses = responses;
    }

    public boolean isSequential() {
        return isSequential;
    }

    public void setSequential(boolean sequential) {
        isSequential = sequential;
    }

    public int getDelayBetweenMessages() {
        return delayBetweenMessages;
    }

    public void setDelayBetweenMessages(int delayBetweenMessages) {
        this.delayBetweenMessages = delayBetweenMessages;
    }
}
