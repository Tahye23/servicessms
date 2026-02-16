package com.example.myproject.service;

import com.example.myproject.domain.Configuration;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.*;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class WhatsAppApiService {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppApiService.class);
    private final RestTemplate restTemplate;
    private static final String BASE_URL = "https://graph.facebook.com/v22.0";
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+[1-9]\\d{1,14}$");

    public WhatsAppApiService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Vérifie si un numéro de téléphone a WhatsApp en utilisant la configuration utilisateur
     */
    public boolean isWhatsAppValid(String phoneNumber, Configuration config) {
        try {
            if (config == null || !config.isVerified() || !Boolean.TRUE.equals(config.getValid())) {
                log.warn("Configuration WhatsApp invalide ou non vérifiée");
                return false;
            }

            String normalizedNumber = normalizePhoneNumber(phoneNumber);
            if (!isValidPhoneFormat(normalizedNumber)) {
                log.debug("Format de numéro invalide : {}", phoneNumber);
                return false;
            }

            return checkWhatsAppNumber(normalizedNumber, config);
        } catch (Exception e) {
            log.error("Erreur lors de la vérification WhatsApp pour le numéro : {}", phoneNumber, e);
            return false;
        }
    }

    /**
     * Vérifie plusieurs numéros avec la même configuration
     */
    public Map<String, Boolean> verifyMultipleNumbers(List<String> phoneNumbers, Configuration config) {
        Map<String, Boolean> results = new HashMap<>();

        if (config == null || !config.isVerified() || !Boolean.TRUE.equals(config.getValid())) {
            log.warn("Configuration WhatsApp invalide - tous les numéros marqués comme invalides");
            phoneNumbers.forEach(number -> results.put(number, false));
            return results;
        }

        for (String phoneNumber : phoneNumbers) {
            boolean isValid = isWhatsAppValid(phoneNumber, config);
            results.put(phoneNumber, isValid);

            // Petite pause pour éviter le rate limiting
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        return results;
    }

    /**
     * Normalise le numéro de téléphone au format E.164
     */
    private String normalizePhoneNumber(String phoneNumber) {
        if (phoneNumber == null) return null;

        String cleaned = phoneNumber.replaceAll("[^+\\d]", "");

        if (!cleaned.startsWith("+") && cleaned.length() > 7) {
            cleaned = "+" + cleaned;
        }

        return cleaned;
    }

    /**
     * Vérifie le format du numéro (E.164)
     */
    private boolean isValidPhoneFormat(String phoneNumber) {
        return phoneNumber != null && PHONE_PATTERN.matcher(phoneNumber).matches();
    }

    /**
     * Appel à l'API Meta pour vérifier le numéro WhatsApp
     */
    private boolean checkWhatsAppNumber(String phoneNumber, Configuration config) {
        try {
            String url = String.format("%s/%s/contacts", BASE_URL, config.getPhoneNumberId());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(config.getAccessToken());

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("contacts", Arrays.asList(phoneNumber));
            requestBody.put("force_check", true);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            log.debug("Vérification WhatsApp pour le numéro : {} avec phoneNumberId : {}", phoneNumber, config.getPhoneNumberId());

            ResponseEntity<WhatsAppContactResponse> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                request,
                WhatsAppContactResponse.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return parseWhatsAppResponse(response.getBody(), phoneNumber);
            }

            log.warn("Réponse inattendue de l'API WhatsApp : {}", response.getStatusCode());
            return false;
        } catch (RestClientException e) {
            if (e.getMessage().contains("401")) {
                log.error("Token d'accès invalide pour l'utilisateur : {}", config.getUserLogin());
            } else if (e.getMessage().contains("404")) {
                log.error("Phone Number ID invalide : {}", config.getPhoneNumberId());
            } else {
                log.error("Erreur lors de l'appel à l'API WhatsApp pour : {}", phoneNumber, e);
            }
            return false;
        } catch (Exception e) {
            log.error("Erreur inattendue lors de la vérification WhatsApp : {}", phoneNumber, e);
            return false;
        }
    }

    /**
     * Parse la réponse de l'API Meta
     */
    private boolean parseWhatsAppResponse(WhatsAppContactResponse response, String phoneNumber) {
        if (response.getContacts() == null || response.getContacts().isEmpty()) {
            return false;
        }

        return response
            .getContacts()
            .stream()
            .anyMatch(contact -> phoneNumber.equals(contact.getInput()) && "valid".equalsIgnoreCase(contact.getStatus()));
    }

    // Classes pour la réponse de l'API Meta
    public static class WhatsAppContactResponse {

        private List<ContactInfo> contacts;

        public List<ContactInfo> getContacts() {
            return contacts;
        }

        public void setContacts(List<ContactInfo> contacts) {
            this.contacts = contacts;
        }
    }

    public static class ContactInfo {

        private String input;
        private String status;

        @JsonProperty("wa_id")
        private String waId;

        public String getInput() {
            return input;
        }

        public void setInput(String input) {
            this.input = input;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getWaId() {
            return waId;
        }

        public void setWaId(String waId) {
            this.waId = waId;
        }
    }
}
