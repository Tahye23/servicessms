package com.example.myproject.domain;

import java.util.List;
import java.util.Map;

public class MessageRequest {

    private String recipientPhoneNumber;
    private String templateName;
    List<Map<String, Object>> parameters;

    public String getRecipientPhoneNumber() {
        return recipientPhoneNumber;
    }

    public void setRecipientPhoneNumber(String recipientPhoneNumber) {
        this.recipientPhoneNumber = recipientPhoneNumber;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public List<Map<String, Object>> getParameters() {
        return parameters;
    }

    public void setParameters(List<Map<String, Object>> parameters) {
        this.parameters = parameters;
    }
}
