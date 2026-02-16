package com.example.myproject.web.rest;

import com.example.myproject.service.CompleteChatbotFlowExecutionService;
import com.example.myproject.service.WhatsAppWebhookService;
import com.example.myproject.service.dto.flow.WhatsAppResponse;
import com.example.myproject.web.rest.dto.flow.WhatsAppMultiResponse;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webhook")
public class WhatsAppWebhookController {

    @Value("${whatsapp.verify-token}")
    private String VERIFY_TOKEN;

    private final WhatsAppWebhookService whatsAppWebhookService;
    private final CompleteChatbotFlowExecutionService completeChatbotFlowExecutionService;

    public WhatsAppWebhookController(
        WhatsAppWebhookService whatsAppWebhookService,
        CompleteChatbotFlowExecutionService completeChatbotFlowExecutionService
    ) {
        this.whatsAppWebhookService = whatsAppWebhookService;
        this.completeChatbotFlowExecutionService = completeChatbotFlowExecutionService;
    }

    /**
     * Vérification initiale par Meta (GET)
     */
    @GetMapping
    public ResponseEntity<String> verifyWebhook(
        @RequestParam(name = "hub.mode", required = false) String mode,
        @RequestParam(name = "hub.verify_token", required = false) String token,
        @RequestParam(name = "hub.challenge", required = false) String challenge
    ) {
        if ("subscribe".equals(mode) && VERIFY_TOKEN.equals(token)) {
            return ResponseEntity.ok(challenge);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Invalid verify token");
    }

    /**
     * Réception POST (messages entrants & statuts)
     * AUCUNE vérification de header : on laisse passer Meta tel quel
     */

    @PostMapping("/test-message")
    public ResponseEntity<Map<String, Object>> testMessage(
        @RequestParam String phoneNumber,
        @RequestParam String message,
        @RequestParam(defaultValue = "admin") String userLogin
    ) {
        try {
            WhatsAppMultiResponse response = completeChatbotFlowExecutionService.processIncomingMessage(
                phoneNumber,
                message,
                userLogin,
                "text",
                null
            );

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("phoneNumber", phoneNumber);
            result.put("inputMessage", message);
            result.put("response", response);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("error", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }

    /**
     * AJOUTER: Endpoint de santé
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "WhatsApp Webhook");
        health.put("timestamp", java.time.Instant.now().toString());
        health.put("version", "1.0.0");

        return ResponseEntity.ok(health);
    }

    @PostMapping
    public ResponseEntity<String> receiveWebhook(@RequestBody JsonNode payload) {
        JsonNode value = payload.path("entry").get(0).path("changes").get(0).path("value");
        whatsAppWebhookService.processWebhook(value);
        return ResponseEntity.ok("EVENT_RECEIVED");
    }
}
