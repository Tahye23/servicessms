package com.example.myproject.service;

import com.example.myproject.domain.Configuration;
import com.example.myproject.domain.SendWhatsapp;
import com.example.myproject.domain.Template;
import com.example.myproject.domain.User;
import com.example.myproject.repository.ConfigurationRepository;
import com.example.myproject.repository.SendWhatsappRepository;
import com.example.myproject.repository.TemplateRepository;
import com.example.myproject.security.SecurityUtils;
import com.example.myproject.web.rest.ExternalWhatsAppResource;
import com.example.myproject.web.rest.SendSmsResource;
import com.example.myproject.web.rest.dto.*;
import com.example.myproject.web.rest.errors.BadRequestAlertException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.AbstractMap.SimpleEntry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Pair;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Service
@Transactional
public class SendWhatsappService {

    private final Logger log = LoggerFactory.getLogger(SendWhatsappService.class);

    private final TemplateMessageBuilder builder;

    @Autowired
    private ObjectMapper objectMapper;

    private final ConfigurationRepository configurationRepository;

    @Autowired
    private RestTemplate restTemplate;

    private final ObjectMapper mapper = new ObjectMapper();
    private final RestTemplate rest = new RestTemplate();

    @Autowired
    private SendWhatsappRepository sendWhatsappRepository;

    @Autowired
    private TemplateRepository templateRepository;

    private static final String GRAPH_URL = "https://graph.facebook.com/v22.0";

    public SendWhatsappService(TemplateMessageBuilder builder, ConfigurationRepository configurationRepository) {
        this.builder = builder;
        this.configurationRepository = configurationRepository;
    }

    public SendWhatsapp save(SendWhatsapp sendWhatsapp) {
        return sendWhatsappRepository.save(sendWhatsapp);
    }

    public SendWhatsapp update(SendWhatsapp sendWhatsapp) {
        return sendWhatsappRepository.save(sendWhatsapp);
    }

    // … vos champs restTemplate, objectMapper, repo, ACCESS_TOKEN …

    /**
     * Upload le contenu Base64 en tant que media (IMAGE ou VIDEO)
     * et renvoie le media_id retourné par Meta.
     */

    public Optional<SendWhatsapp> partialUpdate(SendWhatsapp sendWhatsapp) {
        return sendWhatsappRepository
            .findById(sendWhatsapp.getId())
            .map(existing -> {
                if (sendWhatsapp.getSendDate() != null) {
                    existing.setSendDate(sendWhatsapp.getSendDate());
                }
                if (sendWhatsapp.getIsSent() != null) {
                    existing.setIsSent(sendWhatsapp.getIsSent());
                }
                // Ajoute ici d'autres champs partiels si besoin

                return sendWhatsappRepository.save(existing);
            });
    }

    public Page<SendWhatsapp> findAll(Pageable pageable) {
        return sendWhatsappRepository.findAllSms(pageable);
    }

    public Optional<SendWhatsapp> findOne(Long id) {
        return sendWhatsappRepository.findById(id);
    }

    public void delete(Long id) {
        sendWhatsappRepository.deleteById(id);
    }

    // Dans SendWhatsappService.java

    /**
     * Upload un média et retourne son media_id
     */
    public String uploadMediaAndGetId(byte[] fileBytes, String mimeType, String mediaType, Configuration cfg) throws IOException {
        log.info("Uploading {} media: type={}, size={} bytes", mediaType, mimeType, fileBytes.length);

        // Normaliser le type MIME
        String normalizedMimeType = normalizeMimeTypeForMeta(mimeType);

        // Valider la taille
        validateMediaSize(fileBytes.length, mediaType);

        // Valider le type MIME
        validateMediaMimeType(normalizedMimeType, mediaType);

        // 1. Créer une session d'upload
        String sessionId = createUploadSession(fileBytes.length, normalizedMimeType, cfg);

        // 2. Uploader le fichier
        String handle = uploadFileToSession(sessionId, fileBytes, cfg);

        // 3. Enregistrer le média et obtenir le media_id
        String mediaId = registerMedia(handle, normalizedMimeType, cfg);

        log.info("Media uploaded successfully: mediaId={}", mediaId);

        return mediaId;
    }

    /**
     * Uploads a Base64‐encoded media (IMAGE or VIDEO) via multipart/form-data
     * and returns the media_id assigned by Meta.
     */

    /** 2) Crée le template → WhatsApp Cloud → persist */

    public ResponseEntity<String> sendTemplateToWhatsApp(TemplateRequest req, User user, Configuration cfg) {
        try {
            log.info("Création du template: {} pour l'utilisateur: {}", req.getName(), user.getLogin());

            // 1) Validation des données
            validateTemplateRequest(req);

            // 2) Construire le payload WhatsApp ET récupérer les media_id
            List<String> mediaHandles = new ArrayList<>(); // ✅ CORRECTION: Créer la liste ici
            ObjectNode payload = buildWhatsAppPayloadWithMediaHandles(req, cfg, mediaHandles);
            String jsonPayload = objectMapper.writeValueAsString(payload);

            log.debug("Payload à envoyer: {}", jsonPayload);
            log.info("Media handles collectés: {}", mediaHandles); // ✅ DEBUG

            // 3) Envoyer à Meta
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(cfg.getAccessToken());

            String url = GRAPH_URL + "/" + cfg.getBusinessId() + "/message_templates";
            ResponseEntity<String> response = restTemplate.postForEntity(url, new HttpEntity<>(jsonPayload, headers), String.class);

            log.info("Réponse de Meta: {} - {}", response.getStatusCode(), response.getBody());

            // 4) Si succès, persister en base AVEC les media_id
            if (response.getStatusCode().is2xxSuccessful()) {
                persistTemplate(response.getBody(), req, user, mediaHandles); // ✅ CORRECTION: Passer les vrais media_id
                log.info("Template persisté avec succès avec {} media_id", mediaHandles.size());
            }

            return response;
        } catch (IllegalArgumentException ex) {
            // ... gestion d'erreurs existante
            log.warn("Erreur de validation: {}", ex.getMessage());
            ObjectNode errorResponse = objectMapper.createObjectNode();
            errorResponse.put("success", false);
            errorResponse.put("error_type", "VALIDATION_ERROR");
            errorResponse.put("message", ex.getMessage());

            if (ex.getMessage().contains("Type de fichier") || ex.getMessage().contains("document")) {
                ObjectNode suggestions = errorResponse.putObject("suggestions");
                suggestions.put("pdf", "Convertissez votre document en PDF");
                suggestions.put("txt", "Utilisez un fichier texte simple");
                suggestions.put("alternative", "Ou envoyez le document en message direct (non-template)");
            } else if (ex.getMessage().contains("audio") || ex.getMessage().contains("AUDIO")) {
                // ✅ NOUVEAUTÉ: Suggestions spécifiques pour l'audio
                ObjectNode suggestions = errorResponse.putObject("suggestions");
                suggestions.put("document_format", "Utilisez 'DOCUMENT' comme format au lieu de 'AUDIO'");
                suggestions.put("supported_formats", "Formats audio supportés: MP3, AAC, MP4, AMR, OGG, OPUS");
                suggestions.put("direct_message", "Ou envoyez l'audio en message direct (non-template)");
            }

            return ResponseEntity.badRequest().body(errorResponse.toString());
        } catch (HttpClientErrorException ex) {
            log.error("Erreur Meta: {} - {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            return ResponseEntity.status(ex.getStatusCode()).body(ex.getResponseBodyAsString());
        } catch (Exception ex) {
            log.error("Erreur lors de la création du template", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                String.format("{\"error\": {\"error_user_msg\": \"Erreur interne: %s\"}}", ex.getMessage())
            );
        }
    }

    /**
     * Validation simple des données du template
     */
    private void validateTemplateRequest(TemplateRequest req) {
        if (req.getName() == null || !req.getName().matches("^[a-z0-9_]+$")) {
            throw new IllegalArgumentException("Le nom du template doit contenir uniquement a-z, 0-9 et _");
        }

        if (req.getComponents() == null || req.getComponents().isEmpty()) {
            throw new IllegalArgumentException("Au moins un composant est requis");
        }

        // Vérifier qu'il y a un composant BODY
        boolean hasBody = req.getComponents().stream().anyMatch(c -> "BODY".equals(c.getType()));

        if (!hasBody) {
            throw new IllegalArgumentException("Le composant BODY est obligatoire");
        }

        // ✅ NOUVELLE VALIDATION: Vérifier les formats de header supportés
        validateHeaderFormats(req);

        // Validations existantes
        validateDocumentTypesForTemplate(req);
        validateByCategory(req);
    }

    private void validateHeaderFormats(TemplateRequest req) {
        // Formats supportés par Meta pour les headers de templates
        Set<String> supportedHeaderFormats = Set.of("TEXT", "IMAGE", "DOCUMENT", "VIDEO", "LOCATION", "GIF");

        for (ComponentRequest component : req.getComponents()) {
            if ("HEADER".equals(component.getType()) && component.getFormat() != null) {
                String format = component.getFormat().toUpperCase();

                if (!supportedHeaderFormats.contains(format)) {
                    if ("AUDIO".equals(format)) {
                        throw new IllegalArgumentException(
                            "Les fichiers audio ne sont pas supportés dans les headers de templates WhatsApp. " +
                            "Solutions: 1) Utilisez 'DOCUMENT' comme format pour envoyer l'audio comme fichier téléchargeable, " +
                            "2) Ou utilisez un message direct pour envoyer de l'audio."
                        );
                    } else {
                        throw new IllegalArgumentException(
                            String.format("Format de header non supporté: %s. " + "Formats acceptés: %s", format, supportedHeaderFormats)
                        );
                    }
                }
            }
        }
    }

    /**
     * 🔧 NOUVELLE MÉTHODE: Validation des types de documents pour templates
     */

    /**
     * 🔧 NOUVELLE MÉTHODE: Validation spécifique des documents pour templates
     */
    private void validateDocumentTypesForTemplate(TemplateRequest req) {
        for (ComponentRequest component : req.getComponents()) {
            if ("HEADER".equals(component.getType()) && "DOCUMENT".equals(component.getFormat()) && component.getMediaUrl() != null) {
                String mediaUrl = component.getMediaUrl();
                if (mediaUrl.startsWith("data:")) {
                    String mimeType = extractMimeType(mediaUrl.split(",")[0]);
                    validateDocumentTypeForTemplate(mimeType);
                }
            }
        }
    }

    private void validateDocumentTypeForTemplate(String mimeType) {
        // Types supportés pour les templates DOCUMENT (incluant audio)
        Set<String> templateSupportedDocuments = Set.of(
            // Documents traditionnels
            "application/pdf",
            "text/plain",
            // ✅ AJOUT: Types audio supportés comme documents dans les templates
            "audio/aac",
            "audio/mp4",
            "audio/mpeg",
            "audio/amr",
            "audio/ogg",
            "audio/opus"
            // Note: Exclure Word/Excel/PowerPoint car non supportés pour templates
        );

        String normalizedType = normalizeMimeTypeForMeta(mimeType);

        if (!templateSupportedDocuments.contains(normalizedType)) {
            if (isWordDocument(normalizedType)) {
                throw new IllegalArgumentException(
                    "Les documents Word (.docx) ne sont pas supportés dans les templates WhatsApp. " +
                    "Veuillez convertir votre document en PDF et réessayer."
                );
            } else if (isExcelDocument(normalizedType)) {
                throw new IllegalArgumentException(
                    "Les documents Excel (.xlsx) ne sont pas supportés dans les templates WhatsApp. " +
                    "Veuillez convertir votre document en PDF et réessayer."
                );
            } else if (isPowerPointDocument(normalizedType)) {
                throw new IllegalArgumentException(
                    "Les documents PowerPoint (.pptx) ne sont pas supportés dans les templates WhatsApp. " +
                    "Veuillez convertir votre document en PDF et réessayer."
                );
            } else if (normalizedType.startsWith("audio/")) {
                throw new IllegalArgumentException(
                    String.format(
                        "Format audio non supporté: %s. " + "Formats audio acceptés: MP3, AAC, MP4, AMR, OGG, OPUS",
                        normalizedType
                    )
                );
            } else {
                throw new IllegalArgumentException(
                    String.format(
                        "Type de fichier non supporté pour les templates: %s. " + "Types supportés: PDF, TXT, Audio (MP3, AAC, etc.)",
                        normalizedType
                    )
                );
            }
        }
    }

    private boolean isAudioDocument(String mimeType) {
        return mimeType.startsWith("audio/");
    }

    /**
     * Helper methods pour identifier les types de documents
     */
    private boolean isWordDocument(String mimeType) {
        return (
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document".equals(mimeType) ||
            "application/msword".equals(mimeType)
        );
    }

    private boolean isExcelDocument(String mimeType) {
        return (
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet".equals(mimeType) ||
            "application/vnd.ms-excel".equals(mimeType)
        );
    }

    private boolean isPowerPointDocument(String mimeType) {
        return (
            "application/vnd.openxmlformats-officedocument.presentationml.presentation".equals(mimeType) ||
            "application/vnd.ms-powerpoint".equals(mimeType)
        );
    }

    /**
     * Validation par catégorie
     */
    private void validateByCategory(TemplateRequest req) {
        String category = req.getCategory().toUpperCase();
        ComponentRequest body = req.getComponents().stream().filter(c -> "BODY".equals(c.getType())).findFirst().orElse(null);

        if (body == null || body.getText() == null) {
            throw new IllegalArgumentException("Le texte du body est requis");
        }

        switch (category) {
            case "AUTHENTICATION":
                validateAuthTemplate(body.getText());
                break;
            case "UTILITY":
                validateUtilityTemplate(body.getText());
                break;
        }
    }

    private void validateAuthTemplate(String bodyText) {
        if (!bodyText.contains("{{otp}}") && !bodyText.contains("{{1}}")) {
            throw new IllegalArgumentException("Les templates d'authentification doivent contenir un placeholder OTP");
        }
        if (bodyText.length() > 500) {
            throw new IllegalArgumentException("Les templates d'authentification doivent être concis (max 500 caractères)");
        }
    }

    private void validateUtilityTemplate(String bodyText) {
        if (bodyText.length() < 20) {
            throw new IllegalArgumentException("Les templates utilitaires doivent contenir suffisamment d'informations");
        }
    }

    private void validateMarketingTemplate(TemplateRequest req) {
        boolean hasButtons = req
            .getComponents()
            .stream()
            .anyMatch(c -> "BUTTONS".equals(c.getType()) && c.getButtons() != null && !c.getButtons().isEmpty());

        if (!hasButtons) {
            throw new IllegalArgumentException("Les templates marketing doivent inclure au moins un bouton");
        }
    }

    /**
     * Construction du payload WhatsApp
     */
    private ObjectNode buildWhatsAppPayloadWithMediaHandles(TemplateRequest req, Configuration cfg, List<String> mediaHandles)
        throws IOException {
        ObjectNode payload = objectMapper.createObjectNode();
        payload
            .put("messaging_product", "whatsapp")
            .put("name", req.getName())
            .put("language", req.getLanguage())
            .put("category", req.getCategory().toUpperCase());

        ArrayNode components = payload.putArray("components");

        for (ComponentRequest component : req.getComponents()) {
            ObjectNode comp = components.addObject();
            comp.put("type", component.getType());

            switch (component.getType()) {
                case "HEADER":
                    processHeaderComponent(component, comp, mediaHandles, cfg);
                    break;
                case "BODY":
                    processBodyComponent(component, comp);
                    break;
                case "FOOTER":
                    processFooterComponent(component, comp);
                    break;
                case "BUTTONS":
                    processButtonsComponent(component, comp);
                    break;
            }
        }

        // ✅ DEBUG: Afficher le payload final
        log.info("🔍 PAYLOAD FINAL ENVOYÉ À META:");
        log.info("{}", objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload));

        // ✅ DEBUG: Vérifier spécifiquement le header
        JsonNode headerComponent = payload.path("components").path(0);
        if (headerComponent.has("format")) {
            log.info("🔍 HEADER FORMAT DÉCLARÉ: {}", headerComponent.path("format").asText());
            if (headerComponent.has("example") && headerComponent.path("example").has("header_handle")) {
                log.info("🔍 HEADER_HANDLE: {}", headerComponent.path("example").path("header_handle").asText());
            }
        }

        return payload;
    }

    /**
     * 🔧 MÉTHODE CORRIGÉE: Traitement du composant Header avec support pour tous les médias
     */
    private void processHeaderComponent(ComponentRequest c, ObjectNode comp, List<String> mediaHandles, Configuration cfg)
        throws IOException {
        comp.put("format", c.getFormat());

        if ("TEXT".equals(c.getFormat()) && c.getText() != null) {
            // Code texte existant...
            List<String> examples = extractExamplesFromText(c.getText());
            String processedText = processTextContent(c.getText());
            comp.put("text", processedText);

            if (!examples.isEmpty()) {
                ObjectNode exampleNode = comp.putObject("example");
                ArrayNode array = exampleNode.putArray("header_text");
                examples.forEach(array::add);
            }
        } else if (isMediaFormat(c.getFormat()) && c.getMediaUrl() != null) {
            String mediaUrl = c.getMediaUrl();
            String[] parts = mediaUrl.split(",", 2);
            if (parts.length != 2) {
                throw new IllegalArgumentException("Format base64 invalide");
            }

            byte[] fileBytes = Base64.getDecoder().decode(parts[1]);
            String mimeType = extractMimeType(parts[0]);

            // ✅ LOG DÉTAILLÉ pour audio
            if (mimeType.startsWith("audio/") && "DOCUMENT".equals(c.getFormat())) {
                log.info("🎵 AUDIO COMME DOCUMENT: Type={}, Taille={}, Format=DOCUMENT", mimeType, fileBytes.length);
            }

            validateMediaSize(fileBytes.length, c.getFormat());
            validateMediaMimeType(mimeType, c.getFormat());

            log.info("Upload média: Format={}, Type MIME={}, Taille={}", c.getFormat(), mimeType, fileBytes.length);

            // ✅ PROCESSUS COMPLET: Upload + Example OBLIGATOIRE
            String sessionId = createUploadSession(fileBytes.length, mimeType, cfg);
            String handle = uploadFileToSession(sessionId, fileBytes, cfg);

            // ✅ CRITIQUE: Header_handle OBLIGATOIRE pour format DOCUMENT
            ObjectNode example = comp.putObject("example");
            example.put("header_handle", handle); // ✅ OBLIGATOIRE !

            log.info("✅ Header_handle ajouté: {}", handle);

            // ✅ Enregistrer le media_id pour l'envoi
            try {
                String mediaId = registerMedia(handle, mimeType, cfg);
                mediaHandles.add(mediaId);

                if (mimeType.startsWith("audio/")) {
                    log.info("🎵 Audio enregistré avec media_id: {}", mediaId);
                } else {
                    log.info("✅ Media_id {} ajouté pour sauvegarde", mediaId);
                }
            } catch (Exception e) {
                log.error("❌ Impossible d'enregistrer le media_id: {}", e.getMessage());
                log.warn("Template sera créé sans media_id - envoi de messages impossible");
            }
        }
    }

    /**
     * Vérifie si c'est un format média
     */
    private boolean isMediaFormat(String format) {
        return Set.of("IMAGE", "VIDEO", "AUDIO", "DOCUMENT").contains(format);
    }

    /**
     * Validation de taille des médias
     */
    private void validateMediaSize(int size, String format) {
        Map<String, Integer> limits = Map.of(
            "IMAGE",
            5 * 1024 * 1024, // 5MB
            "VIDEO",
            16 * 1024 * 1024, // 16MB
            "AUDIO",
            16 * 1024 * 1024, // 16MB
            "DOCUMENT",
            100 * 1024 * 1024 // 100MB
        );

        Integer maxSize = limits.get(format);
        if (maxSize != null && size > maxSize) {
            throw new IllegalArgumentException(
                String.format("Fichier trop volumineux pour %s: %d bytes (max: %d bytes)", format, size, maxSize)
            );
        }
    }

    /**
     * Extraction du type MIME avec correction pour audio
     */
    private String extractMimeType(String dataHeader) {
        String[] parts = dataHeader.split(";");
        if (parts.length > 0) {
            String mimeType = parts[0].replace("data:", "");

            // ✅ CORRECTION CRITIQUE: Mapping des types audio pour Meta
            return normalizeMimeTypeForMeta(mimeType);
        }
        return "application/octet-stream";
    }

    /**
     * ✅ NOUVELLE MÉTHODE: Normalise les types MIME pour Meta
     */
    private String normalizeMimeTypeForMeta(String mimeType) {
        // Meta a des exigences strictes sur les types MIME
        return switch (mimeType.toLowerCase()) {
            // ✅ AUDIO: Conversion des types non acceptés vers des types acceptés
            case "audio/mp3" -> "audio/mpeg"; // Meta n'accepte pas audio/mp3, mais audio/mpeg
            case "audio/m4a" -> "audio/mp4"; // M4A doit être envoyé comme audio/mp4
            // ✅ VIDEO: Normalisation
            case "video/quicktime", "video/mov" -> "video/mp4";
            // ✅ IMAGE: Types déjà supportés
            case "image/jpg" -> "image/jpeg";
            // ✅ DOCUMENT: Types supportés (pour messages directs)
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "application/vnd.openxmlformats-officedocument.presentationml.presentation" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            // ✅ DÉFAUT: Retourner tel quel si déjà supporté
            default -> mimeType;
        };
    }

    /**
     * ✅ AMÉLIORATION: Validation des types MIME supportés
     */
    private void validateMediaMimeType(String mimeType, String format) {
        // Liste des types MIME supportés par Meta
        Map<String, Set<String>> supportedTypes = Map.of(
            "AUDIO",
            Set.of("audio/aac", "audio/mp4", "audio/mpeg", "audio/amr", "audio/ogg", "audio/opus"),
            "VIDEO",
            Set.of("video/mp4", "video/3gpp"),
            "IMAGE",
            Set.of("image/jpeg", "image/png", "image/webp"),
            "DOCUMENT",
            Set.of(
                // Documents traditionnels
                "application/pdf",
                "text/plain",
                "application/msword",
                "application/vnd.ms-excel",
                "application/vnd.ms-powerpoint",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                // ✅ AJOUT: Types audio supportés comme documents
                "audio/aac",
                "audio/mp4",
                "audio/mpeg",
                "audio/amr",
                "audio/ogg",
                "audio/opus"
            )
        );

        String normalizedType = normalizeMimeTypeForMeta(mimeType);
        Set<String> allowed = supportedTypes.get(format);

        if (allowed != null && !allowed.contains(normalizedType)) {
            // ✅ AMÉLIORATION: Message d'erreur plus informatif
            if (normalizedType.startsWith("audio/") && "DOCUMENT".equals(format)) {
                throw new IllegalArgumentException(
                    String.format(
                        "Type audio non supporté: %s. " + "Types audio acceptés comme documents: aac, mp4, mpeg, amr, ogg, opus",
                        normalizedType
                    )
                );
            } else {
                throw new IllegalArgumentException(
                    String.format("Type MIME non supporté pour %s: %s. Types acceptés: %s", format, normalizedType, allowed)
                );
            }
        }
    }

    /**
     * ✅ MISE À JOUR: Enregistrement média avec meilleure gestion
     */
    private String registerMedia(String handle, String mimeType, Configuration cfg) throws IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(cfg.getAccessToken());
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", handle);
        body.add("messaging_product", "whatsapp");
        body.add("type", mimeType); // ✅ Utiliser le MIME type normalisé

        String url = GRAPH_URL + "/" + cfg.getPhoneNumberId() + "/media";

        log.debug("Enregistrement média - Handle: {}, Type: {}", handle, mimeType);

        ResponseEntity<String> response = restTemplate.postForEntity(url, new HttpEntity<>(body, headers), String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            log.error("Échec enregistrement média: Status={}, Body={}", response.getStatusCode(), response.getBody());
            throw new RuntimeException("Échec enregistrement: " + response.getBody());
        }

        String mediaId = objectMapper.readTree(response.getBody()).path("id").asText();
        log.info("Média enregistré avec succès: ID={}", mediaId);

        return mediaId;
    }

    /**
     * ✅ MISE À JOUR: Création session avec type MIME correct
     */
    private String createUploadSession(int fileLength, String mimeType, Configuration cfg) throws IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(cfg.getAccessToken());
        headers.setContentType(MediaType.APPLICATION_JSON);

        String adminAppId = getAdminAppId();

        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("file_length", fileLength);
        requestBody.put("file_type", mimeType); // ✅ Utiliser le MIME type normalisé
        requestBody.put("messaging_product", "whatsapp");

        String url = GRAPH_URL + "/" + adminAppId + "/uploads";

        log.debug("Création session upload - Taille: {}, Type: {}", fileLength, mimeType);

        ResponseEntity<String> response = restTemplate.postForEntity(
            url,
            new HttpEntity<>(objectMapper.writeValueAsString(requestBody), headers),
            String.class
        );

        if (!response.getStatusCode().is2xxSuccessful()) {
            log.error("Échec création session: Status={}, Body={}", response.getStatusCode(), response.getBody());
            throw new RuntimeException("Échec création session: " + response.getBody());
        }

        String sessionId = objectMapper.readTree(response.getBody()).path("id").asText();
        log.info("Session upload créée: ID={}", sessionId);

        return sessionId;
    }

    /**
     * Upload fichier vers session
     */
    private String uploadFileToSession(String sessionId, byte[] fileBytes, Configuration cfg) throws IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(cfg.getAccessToken());
        headers.set("file_offset", "0");
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

        HttpEntity<byte[]> requestEntity = new HttpEntity<>(fileBytes, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(GRAPH_URL + "/" + sessionId, requestEntity, String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Échec upload: " + response.getBody());
        }

        // ✅ RETOURNER LE HANDLE (essentiel pour les templates)
        return objectMapper.readTree(response.getBody()).path("h").asText();
    }

    /**
     * Traitement du composant Body - VERSION CORRIGÉE
     */
    private void processBodyComponent(ComponentRequest c, ObjectNode comp) {
        if (c.getText() == null) return;

        // 1) Extraire exemples avant traitement
        List<String> examples = extractExamplesFromText(c.getText());

        // 2) Traiter le texte
        String processedText = processTextContent(c.getText());
        comp.put("text", processedText);

        // 3) Ajouter exemples si présents
        if (!examples.isEmpty()) {
            ObjectNode exampleNode = comp.putObject("example");
            ArrayNode bodyArray = exampleNode.putArray("body_text");
            ArrayNode innerArray = bodyArray.addArray();
            examples.forEach(innerArray::add);
        }
    }

    /**
     * Traitement du composant Footer - VERSION CORRIGÉE
     */
    private void processFooterComponent(ComponentRequest c, ObjectNode comp) {
        if (c.getText() == null) return;

        // 1) Extraire exemples avant traitement
        List<String> examples = extractExamplesFromText(c.getText());

        // 2) Traiter le texte
        String processedText = processTextContent(c.getText());
        comp.put("text", processedText);

        // 3) Ajouter exemples si présents
        if (!examples.isEmpty()) {
            ObjectNode exampleNode = comp.putObject("example");
            ArrayNode array = exampleNode.putArray("footer_text");
            examples.forEach(array::add);
        }
    }

    /**
     * Traitement des boutons
     */
    private void processButtonsComponent(ComponentRequest c, ObjectNode comp) {
        if (c.getButtons() == null || c.getButtons().isEmpty()) return;

        ArrayNode buttons = comp.putArray("buttons");
        for (ButtonRequest button : c.getButtons()) {
            ObjectNode btn = buttons.addObject();
            btn.put("type", button.getType());
            btn.put("text", button.getText());

            // Ajouter les propriétés spécifiques
            if (button.getUrl() != null) {
                btn.put("url", button.getUrl());
            }
            if (button.getPhoneNumber() != null) {
                btn.put("phone_number", button.getPhoneNumber());
            }
        }
    }

    /**
     * NOUVELLE MÉTHODE: Extraire les exemples avant traitement
     */
    private List<String> extractExamplesFromText(String text) {
        Pattern pattern = Pattern.compile("\\{\\{[^:}]+:([^}]+)\\}\\}");
        Matcher matcher = pattern.matcher(text);

        List<String> examples = new ArrayList<>();
        while (matcher.find()) {
            examples.add(matcher.group(1));
        }

        return examples;
    }

    /**
     * Traitement simplifié du texte et des placeholders
     */
    private String processTextContent(String html) {
        if (html == null) return "";

        // 1) Nettoyer le HTML
        String text = cleanHtmlToMarkdown(html);

        // 2) Traiter les placeholders
        return processPlaceholdersSimple(text);
    }

    /**
     * Nettoyage HTML vers Markdown
     */
    private String cleanHtmlToMarkdown(String html) {
        String text = html;

        // Décoder les entités HTML
        text = text.replace("&nbsp;", " ").replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">");

        // Convertir les balises supportées
        text = text.replaceAll("<(strong|b)[^>]*>(.*?)</(strong|b)>", "*$2*");
        text = text.replaceAll("<(em|i)[^>]*>(.*?)</(em|i)>", "_$2_");
        text = text.replaceAll("<(del|s|strike)[^>]*>(.*?)</(del|s|strike)>", "~$2~");

        // Supprimer toutes les autres balises
        text = text.replaceAll("<[^>]+>", " ");

        // Normaliser les espaces
        text = text.replaceAll("\\s+", " ").trim();

        return text;
    }

    /**
     * Traitement CORRIGÉ des placeholders
     */
    private String processPlaceholdersSimple(String text) {
        Pattern pattern = Pattern.compile("\\{\\{([^:}]+)(?::([^}]+))?\\}\\}");
        Matcher matcher = pattern.matcher(text);

        StringBuffer result = new StringBuffer();
        int placeholderIndex = 1;

        while (matcher.find()) {
            String fieldName = matcher.group(1);

            // Traitement spécial pour OTP
            if ("otp".equals(fieldName)) {
                matcher.appendReplacement(result, "{{1}}");
                placeholderIndex = 2; // Le prochain placeholder sera {{2}}
            } else {
                matcher.appendReplacement(result, "{{" + placeholderIndex + "}}");
                placeholderIndex++;
            }
        }
        matcher.appendTail(result);

        return result.toString();
    }

    private String getAdminAppId() {
        return configurationRepository
            .findOneByUserLogin("admin")
            .map(Configuration::getAppId)
            .orElseThrow(() -> new RuntimeException("Configuration admin non trouvée"));
    }

    /**
     * Persistance en base
     */
    private void persistTemplate(String responseBody, TemplateRequest req, User user, List<String> mediaHandles) throws IOException {
        JsonNode responseJson = objectMapper.readTree(responseBody);
        String templateId = responseJson.path("id").asText();
        String status = responseJson.path("status").asText();

        // Créer copie sans médias
        TemplateRequest sanitized = objectMapper.readValue(objectMapper.writeValueAsString(req), TemplateRequest.class);
        sanitized.getComponents().forEach(c -> c.setMediaUrl(null));

        String contentToStore = objectMapper.writeValueAsString(sanitized);

        // ✅ RÉCUPÉRER LE MEDIA_ID
        String mediaIdToStore = extractMediaIdFromHandles(mediaHandles);

        log.info("📝 Persistance template:");
        log.info("  - ID Meta: {}", templateId);
        log.info("  - Nom: {}", req.getName());
        log.info("  - Status: {}", status);
        log.info("  - Media_id: '{}'", mediaIdToStore);
        log.info("  - Handles reçus: {}", mediaHandles);

        // Sauvegarder
        Template template = new Template();
        template.setTemplateId(templateId);
        template.setName(req.getName());
        template.setTemplateName(req.getName());
        template.setApproved(false);
        template.setStatus(status);
        template.setUser_id(user.getLogin());
        template.setExpediteur(user.getExpediteur());
        template.setCode(mediaIdToStore); // ✅ MEDIA_ID pour l'envoi
        template.setContent(contentToStore);
        template.setCreated_at(LocalDateTime.now().toString());

        Template savedTemplate = templateRepository.save(template);
        log.info("✅ Template sauvegardé avec ID: {} et media_id: '{}'", savedTemplate.getId(), savedTemplate.getCode());
    }

    private String extractMediaIdFromHandles(List<String> mediaHandles) {
        if (mediaHandles == null || mediaHandles.isEmpty()) {
            log.warn("❌ Aucun media_id trouvé - template sans média");
            return "";
        }

        // Prendre le premier handle qui est un media_id valide
        for (String handle : mediaHandles) {
            if (handle != null && handle.matches("\\d+")) {
                log.info("✅ Media_id valide trouvé: {}", handle);
                return handle;
            }
        }

        // Si aucun media_id valide trouvé
        log.warn("❌ Aucun media_id valide dans les handles: {}", mediaHandles);
        return "";
    }

    // 🔧 MÉTHODE BONUS: Envoi direct de documents (alternative aux templates)
    public ResponseEntity<String> sendDocumentDirectMessage(String phoneNumber, String documentBase64, String filename, Configuration cfg) {
        try {
            ComponentRequest tempComponent = new ComponentRequest();
            tempComponent.setFormat("DOCUMENT");
            tempComponent.setMediaUrl(documentBase64);

            // Pour les messages directs, tous les types sont supportés
            String mediaId = processMediaUpload(tempComponent, cfg);

            ObjectNode message = objectMapper.createObjectNode();
            message.put("messaging_product", "whatsapp");
            message.put("to", phoneNumber);
            message.put("type", "document");

            ObjectNode document = message.putObject("document");
            document.put("id", mediaId);
            document.put("filename", filename);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(cfg.getAccessToken());

            String url = GRAPH_URL + "/" + cfg.getPhoneNumberId() + "/messages";

            return restTemplate.postForEntity(url, new HttpEntity<>(objectMapper.writeValueAsString(message), headers), String.class);
        } catch (Exception e) {
            log.error("Erreur envoi document direct: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(String.format("{\"error\": \"%s\"}", e.getMessage()));
        }
    }

    /**
     * Upload média pour messages directs (non-template)
     */
    private String processMediaUpload(ComponentRequest component, Configuration cfg) throws IOException {
        String mediaUrl = component.getMediaUrl();
        String format = component.getFormat();

        if (mediaUrl == null || !mediaUrl.startsWith("data:")) {
            throw new IllegalArgumentException("URL de média invalide");
        }

        // Extraire les données base64
        String[] parts = mediaUrl.split(",", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Format base64 invalide");
        }

        byte[] fileBytes = Base64.getDecoder().decode(parts[1]);
        String mimeType = extractMimeType(parts[0]);

        // Validation renforcée
        validateMediaSize(fileBytes.length, format);
        validateMediaMimeType(mimeType, format);

        log.info(
            "Upload média: Format={}, Type MIME original={}, Type normalisé={}, Taille={}",
            format,
            parts[0],
            mimeType,
            fileBytes.length
        );

        // Upload vers Meta
        return uploadMediaToMeta(fileBytes, mimeType, format, cfg);
    }

    /**
     * Upload simplifié vers Meta pour messages directs
     */
    private String uploadMediaToMeta(byte[] fileBytes, String mimeType, String format, Configuration cfg) throws IOException {
        try {
            // Créer session
            String sessionId = createUploadSession(fileBytes.length, mimeType, cfg);

            // Upload fichier
            String handle = uploadFileToSession(sessionId, fileBytes, cfg);

            // Enregistrer média et retourner media_id (pour messages directs)
            return registerMedia(handle, mimeType, cfg);
        } catch (Exception e) {
            log.error("Erreur upload média: {}", e.getMessage());
            throw new IOException("Échec upload média: " + e.getMessage());
        }
    }

    public String sendMarketingLiteCampaign(
        List<String> phoneNumbers,
        Template tpl,
        List<VariableDTO> varsList,
        String wabaId,
        String accessToken
    ) {
        Objects.requireNonNull(wabaId, "WABA ID requis");
        Objects.requireNonNull(accessToken, "Access Token requis");
        Objects.requireNonNull(phoneNumbers, "Liste de numéros requise");

        if (phoneNumbers.isEmpty()) {
            throw new IllegalArgumentException("Aucun numéro de téléphone fourni");
        }

        try {
            // Valider les numéros de téléphone
            List<String> validPhones = phoneNumbers.stream().filter(this::isValidPhoneNumber).toList();

            if (validPhones.isEmpty()) {
                throw new IllegalArgumentException("Aucun numéro de téléphone valide");
            }

            Map<String, Object> payload = builder.buildBulkMarketingLitePayload(tpl, varsList, validPhones);
            System.out.println("📦 Payload JSON : " + new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(payload));

            // Log pour debug (limité en production)
            if (log.isDebugEnabled()) {
                log.debug("📦 Payload JSON : {}", new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(payload));
            }

            // Configuration RestTemplate avec timeout
            RestTemplate restTemplate = createRestTemplateWithTimeout();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);
            headers.set("User-Agent", "WhatsApp-Bulk-Sender/1.0");

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

            String url = "https://graph.facebook.com/v19.0/" + wabaId + "/marketing_messages";

            ResponseEntity<String> resp = restTemplate.postForEntity(url, request, String.class);

            if (resp.getStatusCode().is2xxSuccessful()) {
                JsonNode responseJson = new ObjectMapper().readTree(resp.getBody());

                if (responseJson.has("campaign_id")) {
                    String campaignId = responseJson.path("campaign_id").asText("unknown");
                    log.info("[BULK-LITE] Campagne envoyée avec succès. ID={}, Recipients={}", campaignId, validPhones.size());
                    return campaignId;
                } else {
                    log.warn("[BULK-LITE] Réponse sans campaign_id: {}", resp.getBody());
                    return "success_no_id";
                }
            } else {
                String errorMsg = String.format("API Meta error [%d]: %s", resp.getStatusCode().value(), resp.getBody());
                throw new RuntimeException(errorMsg);
            }
        } catch (JsonProcessingException e) {
            log.error("[BULK-LITE] Erreur parsing JSON: {}", e.getMessage());
            throw new RuntimeException("Erreur de traitement JSON", e);
        } catch (ResourceAccessException e) {
            log.error("[BULK-LITE] Erreur de connexion: {}", e.getMessage());
            throw new RuntimeException("Erreur de connexion à l'API Meta", e);
        } catch (Exception e) {
            log.error("[BULK-LITE] Erreur générale: {}", e.getMessage(), e);
            throw new RuntimeException("Échec envoi marketing lite: " + e.getMessage(), e);
        }
    }

    private boolean isValidPhoneNumber(String phone) {
        if (phone == null || phone.trim().isEmpty()) return false;
        // Regex basique pour validation internationale
        return phone.matches("^\\+[1-9]\\d{1,14}$");
    }

    private RestTemplate createRestTemplateWithTimeout() {
        RestTemplate restTemplate = new RestTemplate();

        // Configuration des timeouts
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(30000); // 30 secondes
        factory.setReadTimeout(60000); // 60 secondes
        restTemplate.setRequestFactory(factory);

        return restTemplate;
    }

    public SendMessageResult sendMessageAndGetId(String recipient, Template tpl, List<VariableDTO> varsList, String userLogin) {
        try {
            var cfg = configurationRepository.findOneByUserLogin(userLogin).orElse(null);

            if (cfg == null) {
                return SendMessageResult.error("Partner has no WhatsApp configuration");
            }

            String to = recipient.startsWith("+") ? recipient : "+" + recipient;
            Map<String, Object> payload = builder.buildPayload(tpl, varsList, to);
            System.out.println("📦 Payload JSON : " + new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(payload));
            String url = String.format("https://graph.facebook.com/v22.0/%s/messages", cfg.getPhoneNumberId());
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(cfg.getAccessToken());

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return SendMessageResult.error("Failed to send WhatsApp message: HTTP " + response.getStatusCode());
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode messages = root.path("messages");
            if (messages.isArray() && messages.size() > 0) {
                String msgId = messages.get(0).path("id").asText();
                return new SendMessageResult(msgId);
            } else {
                return SendMessageResult.error("No message ID in WhatsApp response");
            }
        } catch (Exception e) {
            return SendMessageResult.error("Error parsing WhatsApp response: " + e.getMessage());
        }
    }

    public boolean sendMessageAuth(User user, String sendService) {
        return true;
    }

    public boolean sendMessage(String recipient, Template tpl, List<VariableDTO> varsList) {
        // 1️⃣ Récupérer le login du user connecté
        String userLogin = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new BadRequestAlertException("User login not found", "whatsapp", "nouser"));

        Configuration cfg = configurationRepository
            .findOneByUserLogin(userLogin)
            .orElseThrow(() -> new BadRequestAlertException("Partner has no WhatsApp configuration", "whatsapp", "noconfig"));

        String to = recipient.startsWith("+") ? recipient : "+" + recipient;
        Map<String, Object> payload = builder.buildPayload(tpl, varsList, to);
        System.out.println("[DEBUG] WhatsApp payload: " + payload);
        String base = "https://graph.facebook.com/v22.0";
        String phoneId = cfg.getPhoneNumberId();
        String url = String.format("%s/%s/messages", base, phoneId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(cfg.getAccessToken());

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        // 7️⃣ Appeler l’API WhatsApp
        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

        return response.getStatusCode().is2xxSuccessful();
    }

    @Transactional
    public void refreshUnapprovedTemplates() throws JsonProcessingException {
        // 1) charger en base tous les Template.approved == false
        List<Template> toRefresh = templateRepository.findByApprovedFalse();
        if (toRefresh.isEmpty()) return;
        String userLogin = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new BadRequestAlertException("User login not found", "whatsapp", "nouser"));

        Configuration cfg = configurationRepository
            .findOneByUserLogin(userLogin)
            .orElseThrow(() -> new BadRequestAlertException("Partner has no WhatsApp configuration", "whatsapp", "noconfig"));

        // 2) découper la liste en paquets de 50
        List<String> ids = toRefresh.stream().map(Template::getTemplateId).collect(Collectors.toList());

        for (List<String> chunk : Lists.partition(ids, 50)) {
            // construire le batch array
            ArrayNode batch = mapper.createArrayNode();
            for (String tplId : chunk) {
                ObjectNode call = mapper.createObjectNode();
                call.put("method", "GET");
                call.put("relative_url", tplId + "?fields=status");
                batch.add(call);
            }

            // envoyer en form-urlencoded
            HttpHeaders hdr = new HttpHeaders();
            hdr.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("access_token", cfg.getAccessToken());
            form.add("batch", mapper.writeValueAsString(batch));

            String raw = rest.postForObject(GRAPH_URL, new HttpEntity<>(form, hdr), String.class);
            ArrayNode responses = (ArrayNode) mapper.readTree(raw);

            // 3) traiter chaque réponse
            for (JsonNode resp : responses) {
                if (resp.path("code").asInt() != 200) continue;
                JsonNode body = mapper.readTree(resp.path("body").asText());
                String id = body.path("id").asText();
                String status = body.path("status").asText();
                boolean appr = "APPROVED".equalsIgnoreCase(status);

                templateRepository
                    .findByTemplateId(id)
                    .ifPresent(tpl -> {
                        tpl.setApproved(appr);
                        tpl.setStatus(status);
                        if (appr && tpl.getApproved_at() == null) {
                            tpl.setApproved_at(LocalDateTime.now().toString());
                        }
                        templateRepository.save(tpl);
                    });
            }
        }
    }

    public SendMessageResult sendTemplateWithMedia(
        String toPhone,
        String fromPhoneId,
        Template template,
        List<ExternalWhatsAppResource.VariableDTO> vars,
        ExternalWhatsAppResource.MediaAttachment media,
        String actorLogin
    ) {
        try {
            // 1) Récup config partenaire (token, phoneId par défaut, etc.)
            Configuration cfg = configurationRepository.findOneByUserLogin(actorLogin).orElse(null);

            if (cfg == null) {
                return SendMessageResult.error("Partner has no WhatsApp configuration");
            }

            // Override du phoneId si fourni
            String phoneId = (fromPhoneId != null && !fromPhoneId.isBlank()) ? fromPhoneId : cfg.getPhoneNumberId();

            // 2) Normaliser le numéro
            String to = toPhone.startsWith("+") ? toPhone : "+" + toPhone;

            // 3) Préparer l'ID média si présent (upload binaire → media_id)
            String headerMediaId = null;
            String headerMediaFilename = null;
            String headerMediaTypeForParam = null; // "document"|"image"|"video"|"audio" (mais Meta n'accepte pas "audio" en header template → utiliser "document")

            if (media != null && media.getBytes() != null && media.getBytes().length > 0) {
                // Meta n'autorise pas "AUDIO" en header de template → envoyons l'audio comme DOCUMENT
                String normalizedMime = normalizeMimeTypeForMeta(media.getContentType());
                String paramKind;
                if (normalizedMime.startsWith("image/")) {
                    paramKind = "image";
                } else if (normalizedMime.startsWith("video/")) {
                    paramKind = "video";
                } else {
                    // document par défaut (PDF, TXT, et même audio traité comme "document")
                    paramKind = "document";
                }

                headerMediaId = uploadRawMedia(media.getBytes(), normalizedMime, cfg, phoneId);
                headerMediaFilename = (media.getFilename() != null && !media.getFilename().isBlank()) ? media.getFilename() : "file";
                headerMediaTypeForParam = paramKind; // utilisé dans le payload "parameters"
            }

            // 4) Construire le payload template (avec variables + header média si dispo)
            Map<String, Object> payload = buildTemplateMessagePayloadWithMedia(
                template,
                vars,
                to,
                headerMediaId,
                headerMediaFilename,
                headerMediaTypeForParam
            );

            // 5) Appel Graph API
            String url = String.format("%s/%s/messages", GRAPH_URL, phoneId);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(cfg.getAccessToken());
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return SendMessageResult.error("Failed to send WhatsApp message: HTTP " + response.getStatusCode());
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode messages = root.path("messages");
            if (messages.isArray() && messages.size() > 0) {
                String msgId = messages.get(0).path("id").asText();
                return new SendMessageResult(msgId);
            }
            return SendMessageResult.error("No message ID in WhatsApp response");
        } catch (Exception e) {
            log.error("sendTemplateWithMedia error", e);
            return SendMessageResult.error("Error sending WhatsApp template: " + e.getMessage());
        }
    }

    /**
     * Construit le payload "template" pour /messages :
     * - BODY parameters (vars text dans l'ordre index croissant)
     * - HEADER param (document/image/video) si mediaId fourni
     */
    private Map<String, Object> buildTemplateMessagePayloadWithMedia(
        Template template,
        List<ExternalWhatsAppResource.VariableDTO> vars,
        String to,
        String headerMediaId, // nullable
        String headerMediaFilename, // nullable
        String headerMediaTypeForParam // "document" | "image" | "video" (ou "document" si audio)
    ) throws JsonProcessingException {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("messaging_product", "whatsapp");
        payload.put("to", to);
        payload.put("type", "template");

        // On tente de récupérer le nom du template & la langue depuis le JSON stocké
        String tplName = template.getTemplateName() != null ? template.getTemplateName() : template.getName();
        String lang = "fr"; // fallback
        try {
            if (template.getContent() != null && template.getContent().trim().startsWith("{")) {
                JsonNode node = objectMapper.readTree(template.getContent());
                if (node.hasNonNull("language")) {
                    lang = node.get("language").asText(lang);
                }
                if (node.hasNonNull("name")) {
                    tplName = node.get("name").asText(tplName);
                }
            }
        } catch (Exception ignore) {
            /* fallback ok */
        }

        Map<String, Object> templateNode = new LinkedHashMap<>();
        templateNode.put("name", tplName);
        Map<String, Object> language = Map.of("code", lang);
        templateNode.put("language", language);

        List<Map<String, Object>> components = new ArrayList<>();

        // HEADER media si présent
        if (headerMediaId != null && headerMediaTypeForParam != null) {
            Map<String, Object> header = new LinkedHashMap<>();
            header.put("type", "header");
            List<Map<String, Object>> params = new ArrayList<>();

            Map<String, Object> param = new LinkedHashMap<>();
            param.put("type", headerMediaTypeForParam);
            Map<String, Object> inner = new LinkedHashMap<>();
            inner.put("id", headerMediaId);
            if ("document".equals(headerMediaTypeForParam) && headerMediaFilename != null) {
                inner.put("filename", headerMediaFilename);
            }
            param.put(headerMediaTypeForParam, inner);
            params.add(param);

            header.put("parameters", params);
            components.add(header);
        }

        // BODY variables
        if (!CollectionUtils.isEmpty(vars)) {
            // Trier par index ascendant
            List<ExternalWhatsAppResource.VariableDTO> ordered = vars
                .stream()
                .filter(v -> v.getIndex() != null)
                .sorted(Comparator.comparingInt(ExternalWhatsAppResource.VariableDTO::getIndex))
                .toList();

            if (!ordered.isEmpty()) {
                Map<String, Object> body = new LinkedHashMap<>();
                body.put("type", "body");
                List<Map<String, Object>> params = new ArrayList<>();
                for (ExternalWhatsAppResource.VariableDTO v : ordered) {
                    Map<String, Object> p = new LinkedHashMap<>();
                    p.put("type", "text");
                    p.put("text", v.getText() != null ? v.getText() : "");
                    params.add(p);
                }
                body.put("parameters", params);
                components.add(body);
            }
        }

        templateNode.put("components", components);
        payload.put("template", templateNode);
        return payload;
    }

    /**
     * Upload binaire → media_id (messages directs / envoi de template avec header media).
     * Réutilise la logique session upload → handle → /media (déjà présente).
     */
    private String uploadRawMedia(byte[] bytes, String mimeType, Configuration cfg, String phoneId) throws IOException {
        // Créer session
        String sessionId = createUploadSession(bytes.length, mimeType, cfg);
        // Uploader le binaire
        String handle = uploadFileToSession(sessionId, bytes, cfg);
        // Enregistrer le média sur le numéro WA voulu
        return registerMediaForPhone(handle, mimeType, cfg, phoneId);
    }

    /**
     * Variante de registerMedia(...) scellée au phoneId utilisé pour l'envoi.
     * (garde registerMedia existante intacte pour tes autres flux)
     */
    private String registerMediaForPhone(String handle, String mimeType, Configuration cfg, String phoneId) throws IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(cfg.getAccessToken());
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", handle);
        body.add("messaging_product", "whatsapp");
        body.add("type", mimeType);

        String url = GRAPH_URL + "/" + phoneId + "/media"; // <- phoneId passé en param

        ResponseEntity<String> response = restTemplate.postForEntity(url, new HttpEntity<>(body, headers), String.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Échec enregistrement média: " + response.getBody());
        }
        return objectMapper.readTree(response.getBody()).path("id").asText();
    }
}
