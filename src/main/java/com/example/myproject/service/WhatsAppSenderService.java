package com.example.myproject.service;

import com.example.myproject.domain.Configuration;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
public class WhatsAppSenderService {

    private final Logger log = LoggerFactory.getLogger(WhatsAppSenderService.class);
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public WhatsAppSenderService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Envoyer un message via l'API WhatsApp Business
     */
    public SendMessageResult sendMessage(Configuration config, Map<String, Object> messagePayload) {
        try {
            String url = String.format("https://graph.facebook.com/v18.0/%s/messages", config.getPhoneNumberId());

            // Headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(config.getAccessToken());

            // Payload
            String jsonPayload = objectMapper.writeValueAsString(messagePayload);
            HttpEntity<String> entity = new HttpEntity<>(jsonPayload, headers);

            log.debug("Envoi message WhatsApp vers {}: {}", messagePayload.get("to"), jsonPayload);

            // Appel API
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Message WhatsApp envoyé avec succès vers {}", messagePayload.get("to"));

                // Extraire le message ID de la réponse pour le tracking
                String messageId = extractMessageId(response.getBody());
                return SendMessageResult.success(response.getBody(), messageId);
            } else {
                log.error("Erreur lors de l'envoi du message WhatsApp: {}", response.getBody());
                return SendMessageResult.error("Erreur API WhatsApp: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Exception lors de l'envoi du message WhatsApp: {}", e.getMessage(), e);
            return SendMessageResult.error("Exception: " + e.getMessage());
        }
    }

    /**
     * Envoyer une image avec légende
     */
    public SendMessageResult sendImage(Configuration config, String phoneNumber, String mediaId, String caption) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("messaging_product", "whatsapp");
            message.put("to", phoneNumber);
            message.put("type", "image");

            Map<String, Object> image = new HashMap<>();
            image.put("id", mediaId); // Media ID obtenu lors de l'upload

            if (caption != null && !caption.isEmpty()) {
                image.put("caption", caption);
            }

            message.put("image", image);

            return sendMessage(config, message);
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de l'image: {}", e.getMessage(), e);
            return SendMessageResult.error("Erreur envoi image: " + e.getMessage());
        }
    }

    /**
     * Envoyer un document/fichier
     */
    public SendMessageResult sendDocument(Configuration config, String phoneNumber, String mediaId, String filename, String caption) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("messaging_product", "whatsapp");
            message.put("to", phoneNumber);
            message.put("type", "document");

            Map<String, Object> document = new HashMap<>();
            document.put("id", mediaId);

            if (filename != null && !filename.isEmpty()) {
                document.put("filename", filename);
            }

            if (caption != null && !caption.isEmpty()) {
                document.put("caption", caption);
            }

            message.put("document", document);

            return sendMessage(config, message);
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi du document: {}", e.getMessage(), e);
            return SendMessageResult.error("Erreur envoi document: " + e.getMessage());
        }
    }

    /**
     * Envoyer un fichier audio
     */
    public SendMessageResult sendAudio(Configuration config, String phoneNumber, String mediaId) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("messaging_product", "whatsapp");
            message.put("to", phoneNumber);
            message.put("type", "audio");

            Map<String, Object> audio = new HashMap<>();
            audio.put("id", mediaId);
            message.put("audio", audio);

            return sendMessage(config, message);
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de l'audio: {}", e.getMessage(), e);
            return SendMessageResult.error("Erreur envoi audio: " + e.getMessage());
        }
    }

    /**
     * Envoyer une vidéo
     */
    public SendMessageResult sendVideo(Configuration config, String phoneNumber, String mediaId, String caption) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("messaging_product", "whatsapp");
            message.put("to", phoneNumber);
            message.put("type", "video");

            Map<String, Object> video = new HashMap<>();
            video.put("id", mediaId);

            if (caption != null && !caption.isEmpty()) {
                video.put("caption", caption);
            }

            message.put("video", video);

            return sendMessage(config, message);
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de la vidéo: {}", e.getMessage(), e);
            return SendMessageResult.error("Erreur envoi vidéo: " + e.getMessage());
        }
    }

    /**
     * Envoyer un template WhatsApp
     */
    public SendMessageResult sendTemplate(
        Configuration config,
        String phoneNumber,
        String templateName,
        String languageCode,
        Map<String, Object> parameters
    ) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("messaging_product", "whatsapp");
            message.put("to", phoneNumber);
            message.put("type", "template");

            Map<String, Object> template = new HashMap<>();
            template.put("name", templateName);

            Map<String, String> language = new HashMap<>();
            language.put("code", languageCode);
            template.put("language", language);

            if (parameters != null && !parameters.isEmpty()) {
                template.put("components", parameters);
            }

            message.put("template", template);

            return sendMessage(config, message);
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi du template: {}", e.getMessage(), e);
            return SendMessageResult.error("Erreur envoi template: " + e.getMessage());
        }
    }

    /**
     * Récupérer les informations d'un média uploadé
     */
    public MediaInfo getMediaInfo(Configuration config, String mediaId) {
        try {
            String url = String.format("https://graph.facebook.com/v18.0/%s", mediaId);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(config.getAccessToken());

            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, org.springframework.http.HttpMethod.GET, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode jsonResponse = objectMapper.readTree(response.getBody());

                MediaInfo mediaInfo = new MediaInfo();
                mediaInfo.setId(jsonResponse.path("id").asText());
                mediaInfo.setUrl(jsonResponse.path("url").asText());
                mediaInfo.setMimeType(jsonResponse.path("mime_type").asText());
                mediaInfo.setSha256(jsonResponse.path("sha256").asText());
                mediaInfo.setFileSize(jsonResponse.path("file_size").asLong());

                return mediaInfo;
            }
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des infos média: {}", e.getMessage(), e);
        }

        return null;
    }

    /**
     * Extraire l'ID du message depuis la réponse de l'API
     */
    private String extractMessageId(String responseBody) {
        try {
            JsonNode jsonResponse = objectMapper.readTree(responseBody);
            return jsonResponse.path("messages").get(0).path("id").asText();
        } catch (Exception e) {
            log.warn("Impossible d'extraire l'ID du message: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Marquer un message comme lu
     */
    public SendMessageResult markAsRead(Configuration config, String messageId) {
        try {
            String url = String.format("https://graph.facebook.com/v18.0/%s/messages", config.getPhoneNumberId());

            Map<String, Object> payload = new HashMap<>();
            payload.put("messaging_product", "whatsapp");
            payload.put("status", "read");
            payload.put("message_id", messageId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(config.getAccessToken());

            String jsonPayload = objectMapper.writeValueAsString(payload);
            HttpEntity<String> entity = new HttpEntity<>(jsonPayload, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                return SendMessageResult.success(response.getBody(), null);
            } else {
                return SendMessageResult.error("Erreur marquage lu: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Erreur lors du marquage comme lu: {}", e.getMessage(), e);
            return SendMessageResult.error("Exception marquage lu: " + e.getMessage());
        }
    }

    // Dans WhatsAppSenderService.java - AJOUTER ces méthodes

    // ================================
    // CLASSE POUR UPLOAD RESULT
    // ================================

    public static class UploadResult {

        private final boolean success;
        private final String message;
        private final String mediaId;
        private final String data;

        private UploadResult(boolean success, String message, String mediaId, String data) {
            this.success = success;
            this.message = message;
            this.mediaId = mediaId;
            this.data = data;
        }

        public static UploadResult success(String mediaId, String data) {
            return new UploadResult(true, "Success", mediaId, data);
        }

        public static UploadResult error(String message) {
            return new UploadResult(false, message, null, null);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public String getMediaId() {
            return mediaId;
        }

        public String getData() {
            return data;
        }
    }

    /**
     * Classe pour les informations de média
     */
    public static class MediaInfo {

        private String id;
        private String url;
        private String mimeType;
        private String sha256;
        private Long fileSize;

        // Getters et setters
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getMimeType() {
            return mimeType;
        }

        public void setMimeType(String mimeType) {
            this.mimeType = mimeType;
        }

        public String getSha256() {
            return sha256;
        }

        public void setSha256(String sha256) {
            this.sha256 = sha256;
        }

        public Long getFileSize() {
            return fileSize;
        }

        public void setFileSize(Long fileSize) {
            this.fileSize = fileSize;
        }
    }

    /**
     * Classe pour encapsuler le résultat de l'envoi
     */
    public static class SendMessageResult {

        private final boolean success;
        private final String message;
        private final String data;
        private final String messageId;

        private SendMessageResult(boolean success, String message, String data, String messageId) {
            this.success = success;
            this.message = message;
            this.data = data;
            this.messageId = messageId;
        }

        public static SendMessageResult success(String data, String messageId) {
            return new SendMessageResult(true, "Success", data, messageId);
        }

        public static SendMessageResult error(String message) {
            return new SendMessageResult(false, message, null, null);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public String getData() {
            return data;
        }

        public String getMessageId() {
            return messageId;
        }
    }
}
