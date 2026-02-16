package com.example.myproject.service;

import static com.example.myproject.service.helper.PhoneNumberHelper.normalizePhoneNumber;

import com.example.myproject.domain.*;
import com.example.myproject.domain.enumeration.Channel;
import com.example.myproject.domain.enumeration.ContentType;
import com.example.myproject.domain.enumeration.Direction;
import com.example.myproject.domain.enumeration.MessageType;
import com.example.myproject.repository.*;
import com.example.myproject.service.dto.flow.WhatsAppResponse;
import com.example.myproject.service.helper.PhoneNumberHelper;
import com.example.myproject.web.rest.dto.flow.WhatsAppMultiResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WhatsAppWebhookService {

    private final Logger log = LoggerFactory.getLogger(WhatsAppWebhookService.class);
    private final SendSmsRepository sendSmsRepository;
    private final SmsRepository smsRepository;
    private final ChatService chatService;
    private final ContactRepository contactRepository;
    private final MessageDeliveryStatusRepository messageDeliveryStatusRepository;
    ObjectMapper mapper = new ObjectMapper();
    private final CompleteChatbotFlowExecutionService completeChatbotFlowExecutionService;
    private final ConfigurationRepository configurationRepository;
    private final WhatsAppSenderService whatsAppSenderService;
    private final AbonnementService abonnementService;

    public WhatsAppWebhookService(
        SendSmsRepository sendSmsRepository,
        SmsRepository smsRepository,
        ChatService chatService,
        ContactRepository contactRepository,
        MessageDeliveryStatusRepository messageDeliveryStatusRepository,
        CompleteChatbotFlowExecutionService completeChatbotFlowExecutionService,
        ConfigurationRepository configurationRepository,
        WhatsAppSenderService whatsAppSenderService,
        AbonnementService abonnementService
    ) {
        this.sendSmsRepository = sendSmsRepository;
        this.smsRepository = smsRepository;
        this.chatService = chatService;
        this.contactRepository = contactRepository;
        this.messageDeliveryStatusRepository = messageDeliveryStatusRepository;
        this.completeChatbotFlowExecutionService = completeChatbotFlowExecutionService;

        this.configurationRepository = configurationRepository;
        this.whatsAppSenderService = whatsAppSenderService;
        this.abonnementService = abonnementService;
    }

    @Transactional
    public void processWebhook(JsonNode value) {
        log.info("Webhook WhatsApp reçu : {} messages, {} statuts", value.path("messages").size(), value.path("statuses").size());

        // Traiter les messages entrants avec le chatbot
        processIncomingMessagesWithChatbot(value);

        // Traiter les statuts comme avant
        processStatuses(value);
    }

    /**
     * Traiter les messages avec le système de chatbot amélioré
     */
    private void processIncomingMessagesWithChatbot(JsonNode value) {
        if (!value.has("messages")) return;

        for (JsonNode msg : value.path("messages")) {
            try {
                // Log du message reçu
                String prettyJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(msg);
                log.debug("Message WhatsApp reçu :\n{}", prettyJson);

                String typeStr = msg.path("type").asText();
                String from = msg.path("from").asText();
                String msgId = msg.path("id").asText();

                // Vérifier si le message existe déjà
                if (smsRepository.findByMessageId(msgId).isPresent()) {
                    log.debug("Message déjà traité: {}", msgId);
                    continue;
                }

                // 1. Extraire le contenu et les informations du média
                MessageInfo messageInfo = extractMessageInfo(msg, typeStr);

                // 2. Trouver la configuration du propriétaire du numéro
                String userLogin = findUserLoginByPhoneNumber(from, value);
                if (userLogin == null) {
                    log.warn("Aucune configuration trouvée pour le numéro: {}", from);
                    continue;
                }

                // 3. Sauvegarder le message entrant
                //   saveIncomingMessage(msg, from, messageInfo.getContent(), msgId, typeStr);

                // 4. Traiter avec le chatbot flow (avec support des médias)
                processChatbotInteraction(from, messageInfo, userLogin);
            } catch (Exception e) {
                log.error("Erreur lors du traitement du message: {}", e.getMessage(), e);
            }
        }
    }

    /**
     * Extraire les informations du message (contenu, type, média)
     */
    private MessageInfo extractMessageInfo(JsonNode msg, String typeStr) {
        MessageInfo info = new MessageInfo();
        info.setType(typeStr);

        switch (typeStr) {
            case "text":
                info.setContent(msg.path("text").path("body").asText());
                break;
            case "button":
                info.setContent(msg.path("button").path("text").asText());
                break;
            case "interactive":
                JsonNode interactive = msg.path("interactive");
                if ("button_reply".equals(interactive.path("type").asText())) {
                    info.setContent(interactive.path("button_reply").path("title").asText());
                } else if ("list_reply".equals(interactive.path("type").asText())) {
                    info.setContent(interactive.path("list_reply").path("title").asText());
                }
                break;
            case "image":
                info.setMediaId(msg.path("image").path("id").asText());
                info.setContent(msg.path("image").path("caption").asText(""));
                info.setMimeType(msg.path("image").path("mime_type").asText());
                info.setSha256(msg.path("image").path("sha256").asText());
                break;
            case "document":
                info.setMediaId(msg.path("document").path("id").asText());
                info.setContent(msg.path("document").path("caption").asText(""));
                info.setFileName(msg.path("document").path("filename").asText());
                info.setMimeType(msg.path("document").path("mime_type").asText());
                info.setSha256(msg.path("document").path("sha256").asText());
                break;
            case "audio":
                info.setMediaId(msg.path("audio").path("id").asText());
                info.setMimeType(msg.path("audio").path("mime_type").asText());
                info.setSha256(msg.path("audio").path("sha256").asText());
                info.setContent("[Fichier audio]");
                break;
            case "video":
                info.setMediaId(msg.path("video").path("id").asText());
                info.setContent(msg.path("video").path("caption").asText(""));
                info.setMimeType(msg.path("video").path("mime_type").asText());
                info.setSha256(msg.path("video").path("sha256").asText());
                break;
            case "voice":
                info.setMediaId(msg.path("voice").path("id").asText());
                info.setMimeType(msg.path("voice").path("mime_type").asText());
                info.setSha256(msg.path("voice").path("sha256").asText());
                info.setContent("[Message vocal]");
                break;
            case "location":
                JsonNode location = msg.path("location");
                info.setContent(
                    String.format("📍 Position: %s, %s", location.path("latitude").asText(), location.path("longitude").asText())
                );
                if (location.has("name")) {
                    info.setContent(info.getContent() + " - " + location.path("name").asText());
                }
                break;
            case "contacts":
                info.setContent("[Contact partagé]");
                // Vous pouvez extraire plus d'infos si nécessaire
                break;
            default:
                info.setContent(msg.path(typeStr).path("id").asText("Message non supporté"));
        }

        return info;
    }

    /**
     * Traiter l'interaction avec le chatbot amélioré
     */
    /**
     * Traiter l'interaction avec le chatbot amélioré (support multi-réponses)
     */
    private void processChatbotInteraction(String phoneNumber, MessageInfo messageInfo, String userLogin) {
        try {
            log.debug(
                "Traitement chatbot pour {} (user: {}): {} (type: {})",
                phoneNumber,
                userLogin,
                messageInfo.getContent(),
                messageInfo.getType()
            );

            // MODIFIÉ : Appel avec nouveau type de retour
            WhatsAppMultiResponse multiResponse = completeChatbotFlowExecutionService.processIncomingMessage(
                phoneNumber,
                messageInfo.getContent(),
                userLogin,
                messageInfo.getType(),
                messageInfo.getMediaId()
            );

            if (multiResponse != null && !multiResponse.getResponses().isEmpty()) {
                // Envoyer toutes les réponses séquentiellement
                sendMultipleWhatsAppResponses(phoneNumber, userLogin, multiResponse);
            }
        } catch (Exception e) {
            log.error("Erreur lors du traitement du chatbot: {}", e.getMessage(), e);

            // En cas d'erreur, envoyer un message de fallback
            try {
                sendFallbackMessage(phoneNumber, userLogin);
            } catch (Exception fallbackError) {
                log.error("Erreur lors de l'envoi du message de fallback: {}", fallbackError.getMessage());
            }
        }
    }

    /**
     * Envoyer plusieurs réponses WhatsApp séquentiellement
     */
    private void sendMultipleWhatsAppResponses(String phoneNumber, String userLogin, WhatsAppMultiResponse multiResponse) {
        Configuration config = configurationRepository.findOneByUserLogin(userLogin).orElse(null);
        if (config == null) {
            log.error("❌ Configuration WhatsApp non trouvée pour l'utilisateur: {}", userLogin);
            return;
        }

        List<WhatsAppResponse> responses = multiResponse.getResponses();
        int delayBetweenMessages = multiResponse.getDelayBetweenMessages();

        log.info("📤 Envoi de {} réponses séquentielles avec délai de {}ms", responses.size(), delayBetweenMessages);

        for (int i = 0; i < responses.size(); i++) {
            WhatsAppResponse response = responses.get(i);

            try {
                // Délai entre les messages (sauf pour le premier)
                if (i > 0 && delayBetweenMessages > 0) {
                    Thread.sleep(delayBetweenMessages);
                }

                // Envoyer la réponse
                sendWhatsAppResponse(phoneNumber, userLogin, response);

                log.debug("✅ Réponse {}/{} envoyée avec succès", i + 1, responses.size());
            } catch (InterruptedException e) {
                log.warn("⚠️ Interruption lors du délai entre messages: {}", e.getMessage());
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("❌ Erreur envoi réponse {}/{}: {}", i + 1, responses.size(), e.getMessage(), e);
                // Continuer avec les autres réponses
            }
        }

        log.info("✅ Envoi de toutes les réponses terminé");
    }

    /**
     * Envoyer une réponse WhatsApp (méthode publique pour le contrôleur)
     */
    // Dans WhatsAppWebhookService.java - MODIFIER la méthode sendWhatsAppResponse

    public void sendWhatsAppResponse(String phoneNumber, String userLogin, WhatsAppResponse response) {
        try {
            Configuration config = configurationRepository.findOneByUserLogin(userLogin).orElse(null);
            if (config == null) {
                log.error("❌ Configuration WhatsApp non trouvée pour l'utilisateur: {}", userLogin);
                return;
            }

            WhatsAppSenderService.SendMessageResult result;

            switch (response.getType()) {
                case "text":
                    result = sendTextMessage(phoneNumber, response.getText(), config);
                    break;
                case "buttons":
                    result = sendButtonsMessage(phoneNumber, response.getText(), response.getButtons(), config);
                    break;
                case "list":
                    result = sendListMessage(phoneNumber, response.getText(), response.getItems(), config);
                    break;
                case "image":
                    result = handleImageSend(phoneNumber, response, config, userLogin);
                    break;
                case "document":
                    result = handleDocumentSend(phoneNumber, response, config, userLogin);
                    break;
                case "audio":
                    result = whatsAppSenderService.sendAudio(config, phoneNumber, response.getMediaId());
                    break;
                case "video":
                    result = whatsAppSenderService.sendVideo(config, phoneNumber, response.getMediaId(), response.getCaption());
                    break;
                case "template":
                    result = whatsAppSenderService.sendTemplate(
                        config,
                        phoneNumber,
                        response.getTemplateName(),
                        response.getLanguageCode(),
                        response.getTemplateParameters()
                    );
                    break;
                default:
                    log.warn("⚠️ Type de réponse non supporté: {}", response.getType());
                    result = sendTextMessage(phoneNumber, response.getText() != null ? response.getText() : "Message non supporté", config);
            }

            // Log du résultat
            if (result.isSuccess()) {
                log.info("✅ Message {} envoyé avec succès à {}", response.getType(), phoneNumber);
            } else {
                log.error("❌ Échec envoi message {}: {}", response.getType(), result.getMessage());
            }
        } catch (Exception e) {
            log.error("💥 Erreur lors de l'envoi de la réponse WhatsApp: {}", e.getMessage(), e);
        }
    }

    /**
     * Gérer l'envoi d'image (Meta ou URL)
     */
    // ALTERNATIVE SIMPLIFIÉE - Dans WhatsAppWebhookService.java

    /**
     * Gérer l'envoi d'image (version simplifiée)
     */
    private WhatsAppSenderService.SendMessageResult handleImageSend(
        String phoneNumber,
        WhatsAppResponse response,
        Configuration config,
        String userLogin
    ) {
        if (response.getImageUrl() != null) {
            // Vérifier si c'est un média Meta (format "meta://123456")
            if (response.getImageUrl().startsWith("meta://")) {
                String mediaId = response.getImageUrl().substring(7); // Enlever "meta://"
                log.debug("🖼️ Envoi image Meta avec mediaId: {}", mediaId);
                return whatsAppSenderService.sendImage(config, phoneNumber, mediaId, response.getCaption());
            } else {
                // URL classique - erreur ou message d'info
                log.warn(
                    "⚠️ URL d'image non Meta détectée: {}. Utilisez l'upload Meta pour de meilleures performances.",
                    response.getImageUrl()
                );
                return WhatsAppSenderService.SendMessageResult.error(
                    "Veuillez uploader l'image via l'interface pour l'envoyer sur WhatsApp"
                );
            }
        } else if (response.getMediaId() != null) {
            // MediaId direct
            return whatsAppSenderService.sendImage(config, phoneNumber, response.getMediaId(), response.getCaption());
        } else {
            return WhatsAppSenderService.SendMessageResult.error("Aucune image spécifiée");
        }
    }

    /**
     * Gérer l'envoi de document (version simplifiée)
     */
    private WhatsAppSenderService.SendMessageResult handleDocumentSend(
        String phoneNumber,
        WhatsAppResponse response,
        Configuration config,
        String userLogin
    ) {
        if (response.getFileUrl() != null) {
            // Vérifier si c'est un média Meta (format "meta://123456")
            if (response.getFileUrl().startsWith("meta://")) {
                String mediaId = response.getFileUrl().substring(7); // Enlever "meta://"
                log.debug("📄 Envoi document Meta avec mediaId: {}", mediaId);
                return whatsAppSenderService.sendDocument(config, phoneNumber, mediaId, response.getFileName(), response.getCaption());
            } else {
                // URL classique - erreur ou message d'info
                log.warn(
                    "⚠️ URL de document non Meta détectée: {}. Utilisez l'upload Meta pour de meilleures performances.",
                    response.getFileUrl()
                );
                return WhatsAppSenderService.SendMessageResult.error(
                    "Veuillez uploader le document via l'interface pour l'envoyer sur WhatsApp"
                );
            }
        } else if (response.getMediaId() != null) {
            // MediaId direct
            return whatsAppSenderService.sendDocument(
                config,
                phoneNumber,
                response.getMediaId(),
                response.getFileName(),
                response.getCaption()
            );
        } else {
            return WhatsAppSenderService.SendMessageResult.error("Aucun document spécifié");
        }
    }

    /**
     * Normaliser un numéro de téléphone pour la comparaison
     */
    private String normalizePhoneNumber(String phoneNumber) {
        if (phoneNumber == null) return "";

        // Retirer tous les caractères non numériques sauf le +
        String normalized = phoneNumber.replaceAll("[^+0-9]", "");

        // Si ça commence par +, le garder, sinon ajouter +
        if (!normalized.startsWith("+")) {
            normalized = "+" + normalized;
        }

        return normalized;
    }

    /**
     * Envoyer un message texte simple
     */
    private WhatsAppSenderService.SendMessageResult sendTextMessage(String phoneNumber, String text, Configuration config) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("messaging_product", "whatsapp");
            message.put("to", phoneNumber);
            message.put("type", "text");

            Map<String, String> textObj = new HashMap<>();
            textObj.put("body", text);
            message.put("text", textObj);

            WhatsAppSenderService.SendMessageResult result = whatsAppSenderService.sendMessage(config, message);
            log.debug("Message texte envoyé à {}: {}", phoneNumber, text);
            return result;
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi du message texte: {}", e.getMessage(), e);
            return WhatsAppSenderService.SendMessageResult.error("Erreur envoi texte: " + e.getMessage());
        }
    }

    /**
     * Envoyer un message avec boutons
     */
    private WhatsAppSenderService.SendMessageResult sendButtonsMessage(
        String phoneNumber,
        String text,
        List<com.example.myproject.service.dto.flow.ButtonOption> buttons,
        Configuration config
    ) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("messaging_product", "whatsapp");
            message.put("to", phoneNumber);
            message.put("type", "interactive");

            Map<String, Object> interactive = new HashMap<>();
            interactive.put("type", "button");

            // Header et body
            Map<String, Object> body = new HashMap<>();
            body.put("text", text);
            interactive.put("body", body);

            // Boutons (max 3 pour WhatsApp)
            Map<String, Object> action = new HashMap<>();
            List<Map<String, Object>> buttonsList = new ArrayList<>();

            int maxButtons = Math.min(buttons.size(), 3);
            for (int i = 0; i < maxButtons; i++) {
                com.example.myproject.service.dto.flow.ButtonOption btn = buttons.get(i);
                Map<String, Object> button = new HashMap<>();
                button.put("type", "reply");

                Map<String, String> reply = new HashMap<>();
                reply.put("id", btn.getId());
                reply.put("title", btn.getText().length() > 20 ? btn.getText().substring(0, 20) : btn.getText());
                button.put("reply", reply);

                buttonsList.add(button);
            }

            action.put("buttons", buttonsList);
            interactive.put("action", action);
            message.put("interactive", interactive);

            WhatsAppSenderService.SendMessageResult result = whatsAppSenderService.sendMessage(config, message);
            log.debug("Message avec boutons envoyé à {}", phoneNumber);
            return result;
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi du message avec boutons: {}", e.getMessage(), e);
            // Fallback vers message texte
            return sendTextMessage(phoneNumber, text, config);
        }
    }

    /**
     * Envoyer un message avec liste déroulante
     */
    private WhatsAppSenderService.SendMessageResult sendListMessage(
        String phoneNumber,
        String text,
        List<com.example.myproject.service.dto.flow.ListItem> items,
        Configuration config
    ) {
        try {
            if (items.size() <= 3) {
                // Si 3 éléments ou moins, utiliser des boutons
                List<com.example.myproject.service.dto.flow.ButtonOption> buttons = items
                    .stream()
                    .map(item -> {
                        com.example.myproject.service.dto.flow.ButtonOption btn = new com.example.myproject.service.dto.flow.ButtonOption();
                        btn.setId(item.getId());
                        btn.setText(item.getTitle());
                        return btn;
                    })
                    .collect(java.util.stream.Collectors.toList());
                return sendButtonsMessage(phoneNumber, text, buttons, config);
            }

            Map<String, Object> message = new HashMap<>();
            message.put("messaging_product", "whatsapp");
            message.put("to", phoneNumber);
            message.put("type", "interactive");

            Map<String, Object> interactive = new HashMap<>();
            interactive.put("type", "list");

            // Header et body
            Map<String, Object> body = new HashMap<>();
            body.put("text", text);
            interactive.put("body", body);

            // Action avec liste
            Map<String, Object> action = new HashMap<>();
            action.put("button", "Choisir");

            List<Map<String, Object>> sections = new ArrayList<>();
            Map<String, Object> section = new HashMap<>();
            section.put("title", "Options");

            List<Map<String, Object>> rows = new ArrayList<>();
            int maxItems = Math.min(items.size(), 10); // WhatsApp limite à 10

            for (int i = 0; i < maxItems; i++) {
                com.example.myproject.service.dto.flow.ListItem item = items.get(i);
                Map<String, Object> row = new HashMap<>();
                row.put("id", item.getId());
                row.put("title", item.getTitle().length() > 24 ? item.getTitle().substring(0, 24) : item.getTitle());

                if (item.getDescription() != null && !item.getDescription().isEmpty()) {
                    row.put(
                        "description",
                        item.getDescription().length() > 72 ? item.getDescription().substring(0, 72) : item.getDescription()
                    );
                }

                rows.add(row);
            }

            section.put("rows", rows);
            sections.add(section);
            action.put("sections", sections);
            interactive.put("action", action);
            message.put("interactive", interactive);

            WhatsAppSenderService.SendMessageResult result = whatsAppSenderService.sendMessage(config, message);
            log.debug("Message avec liste envoyé à {}", phoneNumber);
            return result;
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi du message avec liste: {}", e.getMessage(), e);
            // Fallback vers message texte avec options
            StringBuilder fallbackText = new StringBuilder(text).append("\n\nOptions:\n");
            for (int i = 0; i < Math.min(items.size(), 5); i++) {
                fallbackText.append((i + 1)).append(". ").append(items.get(i).getTitle()).append("\n");
            }
            return sendTextMessage(phoneNumber, fallbackText.toString(), config);
        }
    }

    /**
     * Envoyer un message de fallback en cas d'erreur
     */
    private void sendFallbackMessage(String phoneNumber, String userLogin) {
        try {
            Configuration config = configurationRepository.findOneByUserLogin(userLogin).orElse(null);
            if (config != null) {
                sendTextMessage(
                    phoneNumber,
                    "Une erreur s'est produite. Veuillez réessayer ou tapez 'aide' pour obtenir de l'assistance.",
                    config
                );
            }
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi du message de fallback: {}", e.getMessage());
        }
    }

    /**
     * Trouver le login utilisateur par numéro de téléphone
     */
    private String findUserLoginByPhoneNumber(String phoneNumber, JsonNode value) {
        try {
            log.debug("Recherche de l'utilisateur pour le numéro: {}", phoneNumber);

            // Option 1: Récupérer phone_number_id depuis les métadonnées
            String businessPhoneNumberId = value.path("metadata").path("phone_number_id").asText();
            log.debug("Phone number ID Meta: {}", businessPhoneNumberId);

            // Chercher la configuration qui correspond à ce phone_number_id
            List<Configuration> configs = configurationRepository.findAll();
            log.debug("Configurations trouvées: {}", configs.size());

            for (Configuration config : configs) {
                log.debug("Config: userLogin={}, phoneNumberId={}", config.getUserLogin(), config.getPhoneNumberId());

                if (businessPhoneNumberId.equals(config.getPhoneNumberId())) {
                    log.debug("Configuration trouvée pour l'utilisateur: {}", config.getUserLogin());
                    return config.getUserLogin();
                }
            }

            // Option 2: Fallback - utiliser le premier utilisateur (pour tests)
            if (!configs.isEmpty()) {
                Configuration firstConfig = configs.get(0);
                log.warn(
                    "Aucune config trouvée pour phoneNumberId {}, utilisation de: {}",
                    businessPhoneNumberId,
                    firstConfig.getUserLogin()
                );
                return firstConfig.getUserLogin();
            }

            log.error("Aucune configuration trouvée dans la base de données");
            return null;
        } catch (Exception e) {
            log.error("Erreur lors de la recherche de l'utilisateur: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Sauvegarder le message entrant avec support des médias
     */
    private void saveIncomingMessage(JsonNode msg, String from, String content, String msgId, String typeStr) {
        try {
            String contactName = extractContactName(msg);
            String normalized = normalizePhoneNumber(from);

            Contact contact = findOrCreateContact(normalized, contactName);
            Chat chat = chatService.getOrCreateChat(contact, Channel.WHATSAPP);

            Sms sms = buildInboundSms(msg, chat, msgId, typeStr, content);
            smsRepository.save(sms);

            chat.setLastUpdated(Instant.now());
            chatService.save(chat);

            log.debug("Message entrant sauvegardé: {} (type: {})", msgId, typeStr);
        } catch (Exception e) {
            log.error("Erreur lors de la sauvegarde du message: {}", e.getMessage(), e);
        }
    }

    // ================================
    // MÉTHODES UTILITAIRES EXISTANTES (à garder)
    // ================================

    private Contact findOrCreateContact(String phone, String name) {
        try {
            return contactRepository
                .findByContelephone(phone)
                .stream()
                .findFirst()
                .orElseGet(() -> {
                    Contact c = new Contact();
                    c.setContelephone(phone);
                    c.setConnom(name);
                    return contactRepository.save(c);
                });
        } catch (Exception e) {
            log.error("Erreur lors de la recherche/création du contact {}: {}", phone, e.getMessage());

            Contact c = new Contact();
            c.setContelephone(phone);
            c.setConnom(name != null ? name : "Utilisateur WhatsApp");
            return contactRepository.save(c);
        }
    }

    private String extractContactName(JsonNode msg) {
        return msg.path("profile").path("name").asText(null);
    }

    private Sms buildInboundSms(JsonNode msg, Chat chat, String msgId, String typeStr, String content) {
        Sms sms = new Sms();
        sms.setChat(chat);
        sms.setBatch(null);
        sms.setMessageId(msgId);
        sms.setDirection(Direction.INBOUND);
        sms.setType(MessageType.WHATSAPP);

        // Améliorer la détection du type de contenu
        ContentType contentType = determineContentType(typeStr, msg);
        sms.setContentType(contentType);

        sms.setMsgdata(content);
        sms.setSendDate(Instant.ofEpochSecond(msg.path("timestamp").asLong()));
        sms.setDeliveryStatus(null);
        return sms;
    }

    /**
     * Déterminer le type de contenu basé sur le type de message
     */
    private ContentType determineContentType(String typeStr, JsonNode msg) {
        switch (typeStr) {
            case "text":
            case "button":
            case "interactive":
                return ContentType.PLAIN_TEXT;
            case "image":
                return ContentType.MEDIA_IMAGE;
            case "video":
                return ContentType.MEDIA_VIDEO;
            case "audio":
            case "voice":
                return ContentType.MEDIA_AUDIO;
            case "document":
                return ContentType.MEDIA_DOCUMENT;
            case "location":
                return ContentType.LOCATION;
            case "contacts":
                return ContentType.CONTACT;
            case "template":
                return ContentType.TEMPLATE;
            default:
                return ContentType.PLAIN_TEXT;
        }
    }

    /**
     * 🆕 TRAITEMENT AMÉLIORÉ DES STATUTS AVEC MISE À JOUR DES CONTACTS
     */
    private void processStatuses(JsonNode value) {
        if (!value.has("statuses")) return;

        Map<String, MessageDeliveryStatus> lastStatusByMessageId = new HashMap<>();

        for (JsonNode st : value.path("statuses")) {
            String messageId = st.path("id").asText();
            String newStatus = st.path("status").asText().toLowerCase();
            String phone = st.path("recipient_id").asText("");
            String errorTitle = null, errorDetails = null;

            if ("failed".equals(newStatus) && st.has("errors")) {
                JsonNode error = st.path("errors").get(0);
                errorTitle = error.path("title").asText("");
                errorDetails = error.path("details").asText("");
            }

            MessageDeliveryStatus status = new MessageDeliveryStatus();
            status.setMessageId(messageId);
            status.setStatus(newStatus);
            status.setErrorTitle(errorTitle);
            status.setErrorDetails(errorDetails);
            status.setPhone(phone);
            status.setReceivedAt(Instant.now());
            status.setProcessedAt(null);

            lastStatusByMessageId.put(messageId, status);

            // 🆕 MISE À JOUR DES STATISTIQUES DU CONTACT
            updateContactStatistics(phone, newStatus, errorTitle, errorDetails);
        }

        // Sauvegarde des statuts comme avant
        List<MessageDeliveryStatus> toSave = new ArrayList<>();
        for (Map.Entry<String, MessageDeliveryStatus> entry : lastStatusByMessageId.entrySet()) {
            String messageId = entry.getKey();
            MessageDeliveryStatus latestStatus = entry.getValue();

            List<MessageDeliveryStatus> statusesInDb = messageDeliveryStatusRepository.findAllByMessageId(messageId);
            if (!statusesInDb.isEmpty()) {
                for (MessageDeliveryStatus statusInDb : statusesInDb) {
                    statusInDb.setStatus(latestStatus.getStatus());
                    statusInDb.setErrorTitle(latestStatus.getErrorTitle());
                    statusInDb.setErrorDetails(latestStatus.getErrorDetails());
                    statusInDb.setPhone(latestStatus.getPhone());
                    statusInDb.setReceivedAt(latestStatus.getReceivedAt());
                    statusInDb.setProcessedAt(null);
                }
                toSave.addAll(statusesInDb);
            } else {
                toSave.add(latestStatus);
            }
        }

        if (!toSave.isEmpty()) messageDeliveryStatusRepository.saveAll(toSave);
    }

    /**
     * 🆕 MISE À JOUR DES STATISTIQUES DU CONTACT
     */
    @Transactional
    protected void updateContactStatistics(String phoneNumber, String status, String errorTitle, String errorDetails) {
        try {
            String normalizedPhone = PhoneNumberHelper.normalizePhoneNumber(phoneNumber);

            // Trouver le contact par numéro de téléphone
            List<Contact> contacts = contactRepository.findBycontelephone(normalizedPhone);

            if (contacts.isEmpty()) {
                log.debug("Aucun contact trouvé pour le numéro: {}", normalizedPhone);
                return;
            }

            // Prendre le premier contact trouvé
            Contact contact = contacts.get(0);

            // Marquer que le contact a WhatsApp (puisqu'on reçoit des statuts)
            contact.setHasWhatsapp(true);

            // Initialiser les compteurs s'ils sont null
            if (contact.getTotalWhatsappSent() == null) contact.setTotalWhatsappSent(0);
            if (contact.getTotalWhatsappSuccess() == null) contact.setTotalWhatsappSuccess(0);
            if (contact.getTotalWhatsappFailed() == null) contact.setTotalWhatsappFailed(0);

            // Mettre à jour selon le statut
            switch (status.toLowerCase()) {
                case "sent":
                    contact.setTotalWhatsappSent(contact.getTotalWhatsappSent() + 1);
                    contact.setTotalWhatsappSuccess(contact.getTotalWhatsappSuccess() + 1);
                    log.debug("✅ Message WhatsApp {} pour {}", status, normalizedPhone);
                    break;
                case "failed":
                    //  abonnementService.decrementQuotasAfterSend(activeSubscriptions, MessageType.WHATSAPP, 1);
                    contact.setTotalWhatsappSent(contact.getTotalWhatsappSent() + 1);
                    contact.setTotalWhatsappFailed(contact.getTotalWhatsappFailed() + 1);
                    handleWhatsAppError(contact, errorTitle, errorDetails);
                    log.warn("❌ Message WhatsApp échoué pour {}: {} - {}", normalizedPhone, errorTitle, errorDetails);
                    break;
                default:
                    log.debug("Statut WhatsApp inconnu: {} pour {}", status, normalizedPhone);
            }

            // Sauvegarder le contact
            contactRepository.save(contact);

            log.debug(
                "📊 Statistiques mises à jour pour {}: {} envoyés, {} réussis, {} échoués",
                normalizedPhone,
                contact.getTotalWhatsappSent(),
                contact.getTotalWhatsappSuccess(),
                contact.getTotalWhatsappFailed()
            );
        } catch (Exception e) {
            log.error("Erreur lors de la mise à jour des statistiques du contact {}: {}", phoneNumber, e.getMessage(), e);
        }
    }

    /**
     * 🆕 GESTION DES ERREURS WHATSAPP
     */
    /**
     * 🆕 DÉTECTION COMPLÈTE DES ERREURS "PAS DE WHATSAPP"
     */
    private void handleWhatsAppError(Contact contact, String errorTitle, String errorDetails) {
        if (errorTitle == null) return;

        String errorLower = errorTitle.toLowerCase();
        String detailsLower = errorDetails != null ? errorDetails.toLowerCase() : "";

        // 📱❌ LISTE COMPLÈTE DES ERREURS "PAS DE WHATSAPP"
        if (
            errorLower.contains("not registered") || // Numéro non enregistré WhatsApp
            errorLower.contains("not whatsapp user") || // Pas utilisateur WhatsApp
            errorLower.contains("invalid recipient") || // Destinataire invalide
            errorLower.contains("undeliverable") || // Message non livrable (votre cas)
            errorLower.contains("recipient not found") || // Destinataire non trouvé
            errorLower.contains("phone number not found") || // Numéro de téléphone non trouvé
            errorLower.contains("user not found") || // Utilisateur non trouvé
            errorLower.contains("invalid phone number") || // Numéro de téléphone invalide
            errorLower.contains("number does not have whatsapp") || // Le numéro n'a pas WhatsApp
            errorLower.contains("recipient unavailable") || // Destinataire indisponible
            errorLower.contains("not a whatsapp number") || // Ce n'est pas un numéro WhatsApp
            errorLower.contains("whatsapp account not found") || // Compte WhatsApp non trouvé
            errorLower.contains("user offline") || // Utilisateur hors ligne (parfois = pas WhatsApp)
            errorLower.contains("invalid user") || // Utilisateur invalide
            errorLower.contains("message undeliverable") || // Message non livrable
            // Vérifier aussi dans les détails
            detailsLower.contains("not registered") ||
            detailsLower.contains("not whatsapp user") ||
            detailsLower.contains("undeliverable") ||
            detailsLower.contains("recipient not found") ||
            detailsLower.contains("phone number not found") ||
            detailsLower.contains("user not found") ||
            detailsLower.contains("invalid phone number") ||
            detailsLower.contains("number does not have whatsapp") ||
            detailsLower.contains("recipient unavailable") ||
            detailsLower.contains("not a whatsapp number") ||
            detailsLower.contains("whatsapp account not found") ||
            detailsLower.contains("invalid user") ||
            detailsLower.contains("message undeliverable")
        ) {
            contact.setHasWhatsapp(false);
            log.info("📱❌ Contact {} n'a pas WhatsApp (erreur: {})", contact.getContelephone(), errorTitle);
        } else if (
            errorLower.contains("healthy ecosystem") ||
            errorLower.contains("maintain healthy ecosystem") ||
            errorLower.contains("ecosystem engagement") ||
            errorLower.contains("engagement protection") ||
            errorLower.contains("delivery suspended") ||
            errorLower.contains("quality rating") ||
            errorLower.contains("messaging limit") ||
            detailsLower.contains("healthy ecosystem") ||
            detailsLower.contains("ecosystem engagement") ||
            detailsLower.contains("maintain healthy")
        ) {
            // 🔄 Marquer le contact comme "temporairement suspendu"
            contact.setStatuttraitement(5); // 5 = En attente / Suspendu temporairement

            log.warn("🛡️ Contact {} suspendu par protection écosystème Meta: {}", contact.getContelephone(), errorTitle);
        }
        // 🚫 DÉTECTION DES CONTACTS BLOQUÉS/SPAM
        else if (
            errorLower.contains("blocked") ||
            errorLower.contains("spam") ||
            errorLower.contains("rate limit") ||
            errorLower.contains("exceeded") ||
            errorLower.contains("throttled") ||
            errorLower.contains("restricted") ||
            errorLower.contains("banned") ||
            errorLower.contains("suspended") ||
            errorLower.contains("violation") ||
            errorLower.contains("abuse") ||
            detailsLower.contains("blocked") ||
            detailsLower.contains("spam") ||
            detailsLower.contains("rate limit") ||
            detailsLower.contains("restricted")
        ) {
            contact.setStatuttraitement(6); // 6 = Bloqué
            log.warn("🚫 Contact {} bloqué/spam détecté (erreur: {})", contact.getContelephone(), errorTitle);
        }
        // 🏥 PROBLÈMES TECHNIQUES META (garder hasWhatsapp = true car c'est temporaire)
        else if (
            errorLower.contains("internal") ||
            errorLower.contains("server") ||
            errorLower.contains("timeout") ||
            errorLower.contains("unavailable") ||
            errorLower.contains("service") ||
            errorLower.contains("network") ||
            errorLower.contains("connection") ||
            errorLower.contains("maintenance") ||
            errorLower.contains("temporary") ||
            detailsLower.contains("internal") ||
            detailsLower.contains("server") ||
            detailsLower.contains("timeout")
        ) {
            log.warn("🏥 Problème technique Meta pour {} (erreur: {})", contact.getContelephone(), errorTitle);
            // Ne pas changer hasWhatsapp car c'est temporaire
        }
        // ❓ ERREURS INCONNUES - LOG POUR ANALYSE
        else {
            log.warn(
                "❓ Erreur WhatsApp inconnue pour {} - Title: '{}' - Details: '{}'",
                contact.getContelephone(),
                errorTitle,
                errorDetails
            );
        }
    }

    /**
     * Classe pour encapsuler les informations du message
     */
    public static class MessageInfo {

        private String type;
        private String content;
        private String mediaId;
        private String fileName;
        private String mimeType;
        private String sha256;

        // Getters et setters
        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String getMediaId() {
            return mediaId;
        }

        public void setMediaId(String mediaId) {
            this.mediaId = mediaId;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
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
    }
}
