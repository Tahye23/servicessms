package com.example.myproject.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

// src/main/java/com/mycompany/myapp/config/GraphApiProperties.java
@Component
@ConfigurationProperties(prefix = "graph.api")
public class GraphApiProperties {

    /**
     * Base URL de l’API Graph : par défaut https://graph.facebook.com/v18.0
     */
    private String baseUrl = "https://graph.facebook.com/v22.0";
    private String webhookCallbackUrl = "https://supreme-smoothly-ghost.ngrok-free.app/api/webhook";
    private String webhookVerifyToken = "HJSDHJSYYUHDJHJJH563265637276ghghghhg";

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getWebhookCallbackUrl() {
        return webhookCallbackUrl;
    }

    public void setWebhookCallbackUrl(String webhookCallbackUrl) {
        this.webhookCallbackUrl = webhookCallbackUrl;
    }

    public String getWebhookVerifyToken() {
        return webhookVerifyToken;
    }

    public void setWebhookVerifyToken(String webhookVerifyToken) {
        this.webhookVerifyToken = webhookVerifyToken;
    }
}
