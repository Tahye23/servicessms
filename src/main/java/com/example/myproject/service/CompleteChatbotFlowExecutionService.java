package com.example.myproject.service;

import com.example.myproject.domain.*;
import com.example.myproject.repository.*;
import com.example.myproject.service.dto.flow.*;
import com.example.myproject.web.rest.dto.flow.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

@Service
@Transactional
public class CompleteChatbotFlowExecutionService {

    private final UserRepository userRepository;
    private final Logger log = LoggerFactory.getLogger(CompleteChatbotFlowExecutionService.class);
    private final RestTemplate restTemplate;

    private final ChatbotSessionRepository chatbotSessionRepository;
    private final ObjectMapper objectMapper;

    // Services
    private final ChatbotFlowService chatbotFlowService;

    // Patterns de validation
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[+]?[0-9]{8,15}$");

    public CompleteChatbotFlowExecutionService(
        UserRepository userRepository,
        RestTemplate restTemplate,
        ChatbotSessionRepository chatbotSessionRepository,
        ChatbotFlowService chatbotFlowService,
        ObjectMapper objectMapper
    ) {
        this.userRepository = userRepository;
        this.restTemplate = restTemplate;
        this.chatbotSessionRepository = chatbotSessionRepository;
        this.chatbotFlowService = chatbotFlowService;
        this.objectMapper = objectMapper;
    }

    /**
     * ================================
     * POINT D'ENTRÉE PRINCIPAL - TRAITEMENT MESSAGE COMPLET
     * ================================
     */
    @Transactional
    public WhatsAppMultiResponse processIncomingMessage(
        String phoneNumber,
        String messageContent,
        String userLogin,
        String messageType,
        String mediaId
    ) {
        try {
            log.info("🚀 DÉBUT traitement - Phone: {}, Content: {}", phoneNumber, messageContent);

            // 1. RÉCUPÉRER LA SESSION UNIQUE (TOUJOURS UNE SEULE SESSION ACTIVE)
            ChatbotSession session = getOrCreateUniqueSession(phoneNumber, userLogin);

            // 2. VÉRIFIER ÉTAT 3CX ET TRAITER EN CONSÉQUENCE
            return process3CXFlow(session, messageContent, messageType, mediaId);
        } catch (Exception e) {
            log.error("💥 ERREUR traitement: {}", e.getMessage(), e);
            return new WhatsAppMultiResponse(createTextResponse("Erreur système. Tapez 'restart' pour recommencer."));
        }
    }

    /**
     * ================================
     * TRAITEMENT PRINCIPAL 3CX
     * ================================
     */
    private WhatsAppMultiResponse process3CXFlow(ChatbotSession session, String messageContent, String messageType, String mediaId) {
        // ÉTAPE 1 : Vérifier si conversation 3CX active
        boolean is3CXActive = checkAndUpdate3CXStatus(session);

        if (is3CXActive) {
            // ✅ CONVERSATION 3CX ACTIVE - Transfert direct
            log.info("📞 3CX ACTIF - Transfert direct du message");
            return transferDirectTo3CX(session, messageContent, messageType, mediaId);
        } else {
            // ✅ PAS DE 3CX - Traitement chatbot normal
            log.info("🤖 CHATBOT normal - Traitement du flow");
            return processChatbot(session, messageContent, messageType, mediaId);
        }
    }

    /**
     * ================================
     * CORRECTION 1: GESTION SESSION UNIQUE AMÉLIORÉE
     * ================================
     */
    private ChatbotSession getOrCreateUniqueSession(String phoneNumber, String userLogin) {
        try {
            // 1. Chercher UNE session active existante pour ce phone/user
            Optional<ChatbotSession> existingSession = chatbotSessionRepository.findByPhoneNumberAndUserLoginAndIsActiveTrue(
                phoneNumber,
                userLogin
            );

            if (existingSession.isPresent()) {
                ChatbotSession session = existingSession.get();
                log.debug("📱 Session active trouvée: {} (isActive: {})", session.getId(), session.getIsActive());

                // CORRECTION CRITIQUE: S'assurer que la session reste active
                if (!session.getIsActive()) {
                    log.warn("⚠️ Session trouvée mais inactive, réactivation");
                    session.setIsActive(true);
                    session = chatbotSessionRepository.save(session);
                }

                return session;
            }

            // 2. Aucune session active trouvée - créer une nouvelle
            log.info("✨ Création nouvelle session pour: {}", phoneNumber);

            // D'abord désactiver TOUTES les anciennes sessions
            chatbotSessionRepository.deactivateSessionsForPhoneAndUser(phoneNumber, userLogin);

            // Créer nouvelle session
            String startNodeId = findStartNodeIdForUser(userLogin);
            ChatbotSession newSession = new ChatbotSession(phoneNumber, userLogin, startNodeId);
            newSession.setIsActive(true);

            // Initialiser les variables de base
            initializeSessionVariables(newSession);

            ChatbotSession savedSession = chatbotSessionRepository.save(newSession);
            log.info("✅ Nouvelle session créée: {}", savedSession.getId());

            return savedSession;
        } catch (Exception e) {
            log.error("❌ Erreur gestion session unique: {}", e.getMessage(), e);
            throw new RuntimeException("Impossible de gérer la session", e);
        }
    }

    /**
     * ================================
     * CORRECTION - AFFICHAGE DIRECT DES IMAGES DANS 3CX
     * ================================
     */

    /**
     * CORRECTION 1: RÉCUPÉRATION DE L'URL PUBLIQUE DE L'IMAGE DEPUIS WHATSAPP
     */
    private String getMediaUrlFromWhatsApp(String mediaId) {
        try {
            if (mediaId == null || mediaId.isEmpty()) {
                return null;
            }

            // Configuration WhatsApp Business API
            String accessToken =
                "EAAll4u9O60cBO7Odzf0NMARZBDftPsZB5ZAiyn5gAMitULUEdM91rpdt5Sye3N9fnHLUkGUH5ruZB92ZAd9pJ6unJOQDPRxhK6JBLlEnm1A7d1di7CF5nedo7H8ZB1f5G3bwxk1o3l5HplsTCZA3Yt81vRzQQVdVUqWBVPDqeHCo1B0hzuVGDUsGbJ7Yik2hH7digZDZD"; // À remplacer par votre token
            String mediaApiUrl = "https://graph.facebook.com/v17.0/" + mediaId;

            log.debug("🔍 Récupération URL média pour ID: {}", mediaId);

            // Étape 1: Récupérer les métadonnées du média
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                mediaApiUrl,
                org.springframework.http.HttpMethod.GET,
                entity,
                String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                // Parser la réponse JSON
                JsonNode mediaInfo = objectMapper.readTree(response.getBody());
                String mediaUrl = mediaInfo.get("url").asText();
                String mimeType = mediaInfo.get("mime_type").asText();

                log.info("✅ URL média récupérée: {} (type: {})", mediaUrl, mimeType);
                return mediaUrl;
            } else {
                log.error("❌ Erreur récupération métadonnées média: {}", response.getStatusCode());
                return null;
            }
        } catch (Exception e) {
            log.error("❌ Exception récupération URL média {}: {}", mediaId, e.getMessage(), e);
            return null;
        }
    }

    /**
     * CORRECTION 2: TÉLÉCHARGEMENT ET STOCKAGE DE L'IMAGE
     * Pour éviter l'expiration de l'URL WhatsApp, on peut stocker l'image sur notre serveur
     */
    private String downloadAndStoreMedia(String mediaUrl, String mediaId, String mediaType) {
        try {
            if (mediaUrl == null || mediaUrl.isEmpty()) {
                return null;
            }

            // Configuration WhatsApp Business API
            String accessToken =
                "EAAll4u9O60cBO7Odzf0NMARZBDftPsZB5ZAiyn5gAMitULUEdM91rpdt5Sye3N9fnHLUkGUH5ruZB92ZAd9pJ6unJOQDPRxhK6JBLlEnm1A7d1di7CF5nedo7H8ZB1f5G3bwxk1o3l5HplsTCZA3Yt81vRzQQVdVUqWBVPDqeHCo1B0hzuVGDUsGbJ7Yik2hH7digZDZD"; // À remplacer par votre token

            log.debug("📥 Téléchargement média depuis: {}", mediaUrl);

            // Télécharger le média avec le token d'autorisation
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<byte[]> response = restTemplate.exchange(
                mediaUrl,
                org.springframework.http.HttpMethod.GET,
                entity,
                byte[].class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                byte[] mediaData = response.getBody();

                // Déterminer l'extension du fichier
                String extension = getFileExtensionFromMimeType(mediaType);
                String fileName = "media_" + mediaId + "." + extension;

                // OPTION A: Sauvegarder sur le serveur local (exemple)
                String localPath = "/uploads/media/" + fileName;
                // Files.write(Paths.get(localPath), mediaData); // Décommentez si vous voulez sauvegarder localement

                // OPTION B: Upload vers un service cloud (AWS S3, etc.)
                String publicUrl = uploadToCloudStorage(mediaData, fileName, mediaType);

                if (publicUrl != null) {
                    log.info("✅ Média stocké: {} -> {}", mediaId, publicUrl);
                    return publicUrl;
                }
            }

            log.error("❌ Échec téléchargement média: {}", response.getStatusCode());
            return null;
        } catch (Exception e) {
            log.error("❌ Exception téléchargement média: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * CORRECTION 3: UPLOAD VERS STOCKAGE CLOUD (EXEMPLE AWS S3)
     */
    private String uploadToCloudStorage(byte[] mediaData, String fileName, String mediaType) {
        try {
            // EXEMPLE: Upload vers AWS S3 (à adapter selon votre infrastructure)
            // AmazonS3 s3Client = AmazonS3ClientBuilder.defaultClient();
            // String bucketName = "your-media-bucket";
            //
            // ObjectMetadata metadata = new ObjectMetadata();
            // metadata.setContentLength(mediaData.length);
            // metadata.setContentType(mediaType);
            //
            // PutObjectRequest request = new PutObjectRequest(
            //     bucketName,
            //     fileName,
            //     new ByteArrayInputStream(mediaData),
            //     metadata
            // );
            //
            // s3Client.putObject(request);
            // String publicUrl = "https://" + bucketName + ".s3.amazonaws.com/" + fileName;
            //
            // return publicUrl;

            // POUR LE TEST: Retourner une URL factice
            // En production, remplacez par votre vraie logique d'upload
            return "https://your-domain.com/media/" + fileName;
        } catch (Exception e) {
            log.error("❌ Erreur upload cloud: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * CORRECTION 4: UTILITAIRE POUR DÉTERMINER L'EXTENSION
     */
    private String getFileExtensionFromMimeType(String mimeType) {
        if (mimeType == null) return "bin";

        switch (mimeType.toLowerCase()) {
            case "image/jpeg":
            case "image/jpg":
                return "jpg";
            case "image/png":
                return "png";
            case "image/gif":
                return "gif";
            case "image/webp":
                return "webp";
            case "application/pdf":
                return "pdf";
            case "application/msword":
                return "doc";
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document":
                return "docx";
            case "video/mp4":
                return "mp4";
            case "audio/mpeg":
                return "mp3";
            case "audio/ogg":
                return "ogg";
            default:
                return "bin";
        }
    }

    private String getMimeTypeFromMessageType(String messageType) {
        switch (messageType.toLowerCase()) {
            case "image":
                return "image/jpeg";
            case "document":
                return "application/pdf";
            case "audio":
                return "audio/mpeg";
            case "video":
                return "video/mp4";
            default:
                return "application/octet-stream";
        }
    }

    /**
     * CORRECTION 6: CRÉATION MESSAGE MÉDIA AVEC URL PUBLIQUE
     */
    private Map<String, Object> createMediaMessageForTransfer(String userId, String messageType, String mediaId, String content) {
        try {
            String mediaMessageId = messageType + "-" + System.currentTimeMillis() + "-" + Math.random();

            Map<String, Object> mediaMessage = new HashMap<>();
            mediaMessage.put("from", userId);
            mediaMessage.put("id", mediaMessageId);
            mediaMessage.put("timestamp", String.valueOf(System.currentTimeMillis() / 1000));
            mediaMessage.put("type", messageType);

            // CORRECTION CRITIQUE: Utiliser l'URL publique au lieu du mediaId
            Map<String, Object> mediaContent = new HashMap<>();

            // Essayer de récupérer l'URL publique stockée
            String publicUrl = getStoredMediaUrl(mediaId);
            if (publicUrl != null && !publicUrl.isEmpty()) {
                // Utiliser l'URL publique pour affichage direct
                mediaContent.put("link", publicUrl);
                log.info("📎 Utilisation URL publique: {}", publicUrl);
            } else {
                // Fallback vers mediaId WhatsApp
                mediaContent.put("id", mediaId);
                log.warn("⚠️ URL publique non trouvée, utilisation mediaId: {}", mediaId);
            }

            // Caption descriptive
            String mediaCaption = buildMediaCaptionWithUrl(messageType, content, publicUrl);
            if (mediaCaption != null && !mediaCaption.isEmpty()) {
                mediaContent.put("caption", mediaCaption);
            }

            mediaMessage.put(messageType, mediaContent);

            log.debug("📎 Message média créé: type={}, url={}, caption='{}'", messageType, publicUrl, mediaCaption);

            return mediaMessage;
        } catch (Exception e) {
            log.error("❌ Erreur création message média: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * CORRECTION 7: RÉCUPÉRATION URL STOCKÉE DEPUIS LA SESSION
     */
    private String getStoredMediaUrl(String mediaId) {
        // Cette méthode devrait chercher l'URL publique stockée
        // correspondant au mediaId dans la base de données ou cache

        // IMPLÉMENTATION SIMPLIFIÉE - à adapter selon votre architecture
        try {
            // Chercher dans toutes les sessions récentes pour trouver l'URL
            // En production, utilisez une table séparée pour les médias
            return null; // À implémenter selon votre stockage
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * CORRECTION 8: CAPTION AVEC URL POUR DEBUGGING
     */
    private String buildMediaCaptionWithUrl(String messageType, String originalCaption, String publicUrl) {
        StringBuilder caption = new StringBuilder();

        switch (messageType.toLowerCase()) {
            case "image":
                caption.append("📸 Image du client");
                break;
            case "document":
                caption.append("📄 Document du client");
                break;
            case "audio":
            case "voice":
                caption.append("🎵 Audio du client");
                break;
            case "video":
                caption.append("🎬 Vidéo du client");
                break;
            default:
                caption.append("📎 Fichier du client");
                break;
        }

        if (originalCaption != null && !originalCaption.trim().isEmpty()) {
            caption.append("\n💬 ").append(originalCaption.trim());
        }

        if (publicUrl != null && !publicUrl.isEmpty()) {
            caption.append("\n🔗 ").append(publicUrl);
        }

        return caption.toString();
    }

    /**
     * ================================
     * CORRECTION 2: INITIALISATION PROPER DES VARIABLES
     * ================================
     */
    private void initializeSessionVariables(ChatbotSession session) {
        try {
            Map<String, Object> variables = new HashMap<>();

            // Variables système
            variables.put("system.sessionStart", Instant.now().toString());
            variables.put("system.lastProcessedNode", "");

            // Variables 3CX - IMPORTANT: Initialiser à false
            variables.put("3cx.active", "false");
            variables.put("3cx.conversationId", "");
            variables.put("3cx.transferTime", "");
            variables.put("3cx.lastMessageId", "");

            session.setVariables(objectMapper.writeValueAsString(variables));
            log.debug("🔧 Variables session initialisées");
        } catch (Exception e) {
            log.warn("⚠️ Erreur initialisation variables: {}", e.getMessage());
        }
    }

    /**
     * ================================
     * CORRECTION 3: VÉRIFICATION STATUT 3CX AMÉLIORÉE
     * ================================
     */
    private boolean checkAndUpdate3CXStatus(ChatbotSession session) {
        try {
            // 1. Récupérer l'ID de conversation 3CX
            String conversationId = getSessionVariable(session, "3cx.conversationId", "");

            if (conversationId == null || conversationId.trim().isEmpty() || "null".equals(conversationId)) {
                // Pas de conversation 3CX
                log.debug("🔍 Pas de conversation 3CX active");
                set3CXStatus(session, false);
                return false;
            }

            log.debug("🔍 Vérification statut conversation 3CX: {}", conversationId);

            // 2. Vérifier le statut avec l'API 3CX
            boolean isActive = checkConversationStatus(conversationId);

            // 3. Mettre à jour le statut en conséquence
            if (isActive) {
                set3CXStatus(session, true);
                log.info("✅ Conversation 3CX {} ACTIVE", conversationId);
                return true;
            } else {
                log.info("❌ Conversation 3CX {} FERMÉE - Nettoyage et retour chatbot", conversationId);
                // CORRECTION CRITIQUE: Nettoyer 3CX ET retourner au début du flow
                clean3CXVariablesAndResetFlow(session);
                return false;
            }
        } catch (Exception e) {
            log.error("❌ Erreur vérification 3CX: {}", e.getMessage());
            // En cas d'erreur, considérer comme fermée et nettoyer
            clean3CXVariablesAndResetFlow(session);
            return false;
        }
    }

    /**
     * ================================
     * CORRECTION 4: NETTOYAGE 3CX + RESET FLOW
     * ================================
     */
    private void clean3CXVariablesAndResetFlow(ChatbotSession session) {
        try {
            log.info("🧹 Nettoyage 3CX et reset du flow");

            // 1. Nettoyer variables 3CX
            setSessionVariable(session, "3cx.active", "false");
            setSessionVariable(session, "3cx.conversationId", "");
            setSessionVariable(session, "3cx.transferTime", "");
            setSessionVariable(session, "3cx.lastMessageId", "");

            // 2. CORRECTION CRITIQUE: Remettre au début du flow
            String userLogin = session.getUserLogin();
            String startNodeId = findStartNodeIdForUser(userLogin);
            session.setCurrentNodeId(startNodeId);
            session.setIsActive(true);

            // 3. Nettoyer certaines variables utilisateur pour recommencer
            clearTemporaryUserVariables(session);

            // 4. Sauvegarder
            chatbotSessionRepository.save(session);

            log.info("✅ Nettoyage 3CX et reset terminé - prêt pour nouveau flow");
        } catch (Exception e) {
            log.error("❌ Erreur nettoyage 3CX: {}", e.getMessage());
        }
    }

    /**
     * ================================
     * CORRECTION 5: NETTOYAGE VARIABLES TEMPORAIRES
     * ================================
     */
    private void clearTemporaryUserVariables(ChatbotSession session) {
        try {
            Map<String, Object> variables = getSessionVariables(session);

            // Garder les variables importantes, supprimer les temporaires
            Map<String, Object> cleanedVariables = new HashMap<>();

            for (Map.Entry<String, Object> entry : variables.entrySet()) {
                String key = entry.getKey();

                // Garder les variables système et importantes
                if (
                    key.startsWith("system.") ||
                    key.equals("nom") ||
                    key.equals("email") ||
                    key.equals("telephone") ||
                    key.equals("name") ||
                    key.equals("phone")
                ) {
                    cleanedVariables.put(key, entry.getValue());
                }
                // Supprimer les variables temporaires (user.last*, choix temporaires, etc.)
            }

            session.setVariables(objectMapper.writeValueAsString(cleanedVariables));
            log.debug("🧹 Variables temporaires nettoyées, {} variables conservées", cleanedVariables.size());
        } catch (Exception e) {
            log.warn("⚠️ Erreur nettoyage variables temporaires: {}", e.getMessage());
        }
    }

    /**
     * ================================
     * TRANSFERT DIRECT VERS 3CX (CONVERSATION ACTIVE)
     * ================================
     */
    private WhatsAppMultiResponse transferDirectTo3CX(ChatbotSession session, String messageContent, String messageType, String mediaId) {
        try {
            log.info("📤 Transfert direct vers 3CX");

            // Construire et envoyer le message direct vers 3CX
            boolean success = sendDirectMessageTo3CX(session, messageContent, messageType, mediaId);

            // Sauvegarder la session
            updateSessionAndSave(session);

            if (success) {
                // ✅ Message transféré - PAS de réponse au user (3CX s'en charge)
                return new WhatsAppMultiResponse(); // Réponse vide
            } else {
                // ❌ Échec transfert - basculer vers chatbot
                log.warn("⚠️ Échec transfert 3CX - Basculement chatbot");
                set3CXStatus(session, false);
                return processChatbot(session, messageContent, messageType, mediaId);
            }
        } catch (Exception e) {
            log.error("💥 Erreur transfert direct 3CX: {}", e.getMessage());
            // En cas d'erreur, basculer vers chatbot
            set3CXStatus(session, false);
            return processChatbot(session, messageContent, messageType, mediaId);
        }
    }

    /**
     * ================================
     * TRAITEMENT CHATBOT NORMAL
     * ================================
     */
    private WhatsAppMultiResponse processChatbot(ChatbotSession session, String messageContent, String messageType, String mediaId) {
        try {
            // 1. Récupérer le flow actif
            Long userId = getUserIdByLogin(session.getUserLogin());
            FlowPayload flowPayload = chatbotFlowService.getCurrentFlow(userId);

            if (flowPayload == null || flowPayload.getNodes() == null || flowPayload.getNodes().isEmpty()) {
                log.warn("❌ Aucun flow actif");
                return new WhatsAppMultiResponse(createTextResponse("Aucun flow configuré."));
            }

            // 2. Vérifier commandes spéciales
            WhatsAppResponse specialCommand = handleSpecialCommands(messageContent, session, flowPayload);
            if (specialCommand != null) {
                updateSessionAndSave(session);
                return new WhatsAppMultiResponse(specialCommand);
            }

            // 3. Sauvegarder l'input utilisateur
            if (messageContent != null) {
                saveUserInput(session, messageContent, messageType, mediaId);
            }

            // 4. Déterminer le nœud de départ
            FlowNodePayload startingNode = determineStartingNode(session, flowPayload, messageContent != null);
            if (startingNode == null) {
                log.error("❌ Impossible de déterminer le nœud de départ");
                return new WhatsAppMultiResponse(createTextResponse("Erreur de configuration."));
            }

            // 5. Traiter la séquence de nœuds
            WhatsAppMultiResponse response = processNodeSequence(startingNode, flowPayload, session, messageContent, messageType, mediaId);

            // 6. Sauvegarder la session
            updateSessionAndSave(session);

            log.info("✅ SUCCÈS chatbot - {} réponses", response.getResponses().size());
            return response;
        } catch (Exception e) {
            log.error("💥 Erreur traitement chatbot: {}", e.getMessage(), e);
            return new WhatsAppMultiResponse(createTextResponse("Erreur chatbot. Tapez 'restart'."));
        }
    }

    /**
     * CORRECTION 1: GESTION SYNCHRONE DU CONVERSATION ID
     * Au lieu d'attendre asynchrone, on récupère DIRECTEMENT après le transfert
     */
    private WhatsAppResponse executeWebhookNode(FlowNodePayload node, FlowPayload flowPayload, ChatbotSession session) {
        NodeDataPayload data = node.getData();

        if (data.getWebhookUrl() != null && !data.getWebhookUrl().isEmpty()) {
            log.info("🔗 TRANSFERT INITIAL vers 3CX avec historique complet");

            // 1. Préparer TOUT l'historique de la conversation
            Map<String, Object> webhookData = prepareCompleteConversationDataFixed(session, flowPayload);
            webhookData.put("sessionId", session.getId());
            webhookData.put("isInitialTransfer", true);

            // 2. Ajouter résumé intelligent pour l'agent
            String intelligentSummary = buildIntelligentConversationSummaryFixed(session, flowPayload);
            webhookData.put("agentSummary", intelligentSummary);

            // 3. Envoyer vers 3CX avec historique complet
            boolean webhookSuccess = send3CXWebhookWithHistory(data.getWebhookUrl(), webhookData);

            if (webhookSuccess) {
                // 4. CORRECTION CRITIQUE: Récupération SYNCHRONE de l'ID conversation
                String messageId = (String) webhookData.get("lastMessageId");
                if (messageId != null) {
                    // Récupérer IMMÉDIATEMENT le conversation ID
                    String conversationId = retrieveConversationIdSynchronous(messageId);

                    if (conversationId != null && !conversationId.isEmpty()) {
                        // Sauvegarder DIRECTEMENT dans la session courante
                        setSessionVariable(session, "3cx.conversationId", conversationId);
                        setSessionVariable(session, "3cx.transferTime", Instant.now().toString());
                        set3CXStatus(session, true);

                        // Sauvegarder immédiatement
                        chatbotSessionRepository.save(session);

                        log.info("✅ Conversation ID {} sauvegardé IMMÉDIATEMENT pour session {}", conversationId, session.getId());
                    } else {
                        log.warn("⚠️ Impossible de récupérer conversation ID immédiatement");
                    }
                }

                // 5. Activer le mode 3CX SANS désactiver la session
                set3CXStatus(session, true);
                session.setIsActive(true);

                log.info("✅ Transfert initial 3CX réussi - session reste active");
            } else {
                log.warn("⚠️ Échec transfert initial 3CX");
            }
        }

        // Message de confirmation
        String text = data.getText() != null
            ? replaceVariablesInText(data.getText(), session)
            : "🔗 Transfert vers notre équipe en cours...";

        return createTextResponse(text);
    }

    /**
     * CORRECTION 2: RÉCUPÉRATION SYNCHRONE DU CONVERSATION ID
     */
    private String retrieveConversationIdSynchronous(String messageId) {
        int maxAttempts = 5; // Plus de tentatives
        int waitTime = 2000; // 2 secondes entre tentatives

        log.info("🔄 Récupération SYNCHRONE conversation ID pour message: {}", messageId);

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                // Attendre que 3CX traite le message
                if (attempt > 1) {
                    Thread.sleep(waitTime);
                    waitTime += 1000; // Augmenter le délai à chaque tentative
                }

                String conversationApiUrl = "https://rjum8bum7g.execute-api.us-east-1.amazonaws.com/dev/conversation";
                String url = conversationApiUrl + "?msg_gid=" + messageId;

                log.debug("🔍 Tentative {}/{} - URL: {}", attempt, maxAttempts, url);

                ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

                if (response.getStatusCode().is2xxSuccessful()) {
                    String conversationId = response.getBody();

                    // Nettoyer la réponse
                    if (conversationId != null) {
                        conversationId = conversationId.trim().replaceAll("^\"|\"$", "");
                    }

                    if (conversationId != null && !conversationId.isEmpty() && !"null".equals(conversationId)) {
                        log.info("✅ Conversation ID récupéré (tentative {}): {}", attempt, conversationId);
                        return conversationId;
                    } else {
                        log.warn("⚠️ Réponse vide (tentative {}): '{}'", attempt, response.getBody());
                    }
                } else {
                    log.warn("⚠️ Erreur HTTP (tentative {}): {}", attempt, response.getStatusCode());
                }
            } catch (Exception e) {
                log.error("❌ Exception (tentative {}): {}", attempt, e.getMessage());
            }
        }

        log.error("❌ Échec récupération conversation ID après {} tentatives", maxAttempts);
        return null;
    }

    /**
     * CORRECTION 3: PRÉPARATION DONNÉES AVEC SESSION RÉELLE
     */
    private Map<String, Object> prepareCompleteConversationDataFixed(ChatbotSession session, FlowPayload flowPayload) {
        Map<String, Object> data = new HashMap<>();

        try {
            log.debug("📋 Préparation données conversation pour session: {}", session.getId());

            // Informations de base
            data.put("timestamp", Instant.now().toString());
            data.put("sessionId", session.getId());
            data.put("phoneNumber", session.getPhoneNumber());
            data.put("userLogin", session.getUserLogin());
            data.put("flowName", flowPayload.getName());

            // CORRECTION CRITIQUE: Récupérer les variables DIRECTEMENT depuis la session
            Map<String, Object> variables = getSessionVariablesDirect(session);
            data.put("variables", variables);

            log.debug("📊 Variables récupérées: {} entrées", variables.size());

            // Log des variables pour debugging
            for (Map.Entry<String, Object> entry : variables.entrySet()) {
                log.debug("🔧 Variable: {} = {}", entry.getKey(), entry.getValue());
            }

            // HISTORIQUE COMPLET avec variables réelles
            List<Map<String, Object>> fullHistory = buildCompleteHistoryFixed(session, variables);
            data.put("conversationHistory", fullHistory);

            log.debug("📚 Historique construit: {} entrées", fullHistory.size());

            // RÉSUMÉ INTELLIGENT avec variables réelles
            String summary = buildIntelligentSummaryFixed(variables, fullHistory);
            data.put("conversationSummary", summary);

            // Données utilisateur avec variables réelles
            Map<String, Object> userData = extractUserDataFixed(variables);
            data.put("userData", userData);

            log.info("✅ Données complètes préparées: {} variables, {} historique", variables.size(), fullHistory.size());
        } catch (Exception e) {
            log.error("💥 Erreur préparation données complètes: {}", e.getMessage(), e);
        }

        return data;
    }

    /**
     * CORRECTION 4: RÉCUPÉRATION DIRECTE DES VARIABLES SANS CACHE
     */
    private Map<String, Object> getSessionVariablesDirect(ChatbotSession session) {
        try {
            // Recharger la session depuis la base pour être sûr d'avoir les dernières données
            Optional<ChatbotSession> freshSession = chatbotSessionRepository.findById(session.getId());

            if (freshSession.isPresent()) {
                String variablesJson = freshSession.get().getVariables();

                if (variablesJson == null || variablesJson.isEmpty()) {
                    log.warn("⚠️ Variables vides pour session: {}", session.getId());
                    return new HashMap<>();
                }

                Map<String, Object> variables = objectMapper.readValue(variablesJson, HashMap.class);
                log.debug("📦 Variables rechargées: {} entrées", variables.size());

                return variables;
            } else {
                log.error("❌ Session {} non trouvée lors du rechargement", session.getId());
                return new HashMap<>();
            }
        } catch (Exception e) {
            log.error("❌ Erreur rechargement variables session: {}", e.getMessage(), e);
            return new HashMap<>();
        }
    }

    /**
     * CORRECTION 5: CONSTRUCTION HISTORIQUE AVEC VARIABLES RÉELLES
     */
    private List<Map<String, Object>> buildCompleteHistoryFixed(ChatbotSession session, Map<String, Object> variables) {
        List<Map<String, Object>> history = new ArrayList<>();

        try {
            log.info("📚 Construction historique pour session: {} avec {} variables", session.getId(), variables.size());

            // NOUVEAU: Récupérer le nombre total de messages
            int messageCount = Integer.parseInt(variables.getOrDefault("user.messageCount", "0").toString());
            log.info("📊 Nombre total de messages: {}", messageCount);

            // 1. RÉCUPÉRER TOUS LES MESSAGES PAR INDEX
            for (int i = 1; i <= messageCount; i++) {
                String messageKey = "user.message_" + i;
                String messageContent = (String) variables.get(messageKey);
                String messageType = (String) variables.get(messageKey + ".type");
                String timestamp = (String) variables.get(messageKey + ".timestamp");
                String mediaId = (String) variables.get(messageKey + ".mediaId");
                String description = (String) variables.get(messageKey + ".description");

                if (messageContent != null || mediaId != null) {
                    Map<String, Object> historyEntry = new HashMap<>();
                    historyEntry.put("type", "user_message");
                    historyEntry.put("index", i);
                    historyEntry.put("messageType", messageType != null ? messageType : "text");
                    historyEntry.put("content", messageContent != null ? messageContent : "");
                    historyEntry.put("timestamp", timestamp != null ? timestamp : Instant.now().toString());
                    historyEntry.put("description", description != null ? description : "Message utilisateur");

                    if (mediaId != null && !mediaId.isEmpty()) {
                        historyEntry.put("mediaId", mediaId);
                        historyEntry.put("hasMedia", true);
                    }

                    history.add(historyEntry);
                    log.debug("📜 Message {} ajouté: type={}, content='{}', media={}", i, messageType, messageContent, mediaId != null);
                }
            }

            // 2. AJOUTER LE DERNIER MESSAGE S'IL N'EST PAS DÉJÀ DANS L'INDEX
            String lastMessage = (String) variables.get("user.lastMessage");
            String lastMessageType = (String) variables.get("user.lastMessageType");
            String lastMediaId = (String) variables.get("user.lastMediaId");

            if (lastMessage != null && !lastMessage.isEmpty() && messageCount == 0) {
                // Si pas de messages indexés mais un dernier message existe
                Map<String, Object> lastEntry = new HashMap<>();
                lastEntry.put("type", "last_user_message");
                lastEntry.put("messageType", lastMessageType != null ? lastMessageType : "text");
                lastEntry.put("content", lastMessage);
                lastEntry.put("timestamp", variables.get("user.lastTimestamp"));
                lastEntry.put("description", "Dernier message utilisateur");

                if (lastMediaId != null && !lastMediaId.isEmpty()) {
                    lastEntry.put("mediaId", lastMediaId);
                    lastEntry.put("hasMedia", true);
                }

                history.add(lastEntry);
                log.debug("📜 Dernier message ajouté: '{}'", lastMessage);
            }

            // 3. AJOUTER LES DONNÉES COLLECTÉES (nom, email, etc.)
            for (Map.Entry<String, Object> entry : variables.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue().toString();

                if (
                    !key.startsWith("system.") &&
                    !key.startsWith("user.") &&
                    !key.startsWith("3cx.") &&
                    !value.isEmpty() &&
                    value.length() < 200
                ) {
                    Map<String, Object> dataEntry = new HashMap<>();
                    dataEntry.put("type", "collected_data");
                    dataEntry.put("field", key);
                    dataEntry.put("value", value);
                    dataEntry.put("timestamp", Instant.now().toString());
                    dataEntry.put("description", "Donnée collectée: " + key);

                    history.add(dataEntry);
                    log.debug("📋 Donnée collectée: {} = {}", key, value);
                }
            }

            // 4. TRIER PAR TIMESTAMP
            history.sort((a, b) -> {
                String timeA = (String) a.get("timestamp");
                String timeB = (String) b.get("timestamp");
                if (timeA != null && timeB != null) {
                    try {
                        return Instant.parse(timeA).compareTo(Instant.parse(timeB));
                    } catch (Exception e) {
                        return 0;
                    }
                }
                return 0;
            });

            log.info("✅ Historique construit: {} entrées trouvées", history.size());

            // Log détaillé de l'historique
            for (int i = 0; i < history.size(); i++) {
                Map<String, Object> entry = history.get(i);
                log.info("📜 Historique[{}]: {} - {} - {}", i, entry.get("type"), entry.get("description"), entry.get("content"));
            }
        } catch (Exception e) {
            log.error("❌ Erreur construction historique: {}", e.getMessage(), e);
        }

        return history;
    }

    /**
     * CORRECTION 4: RÉSUMÉ INTELLIGENT AVEC HISTORIQUE COMPLET
     */
    private String buildIntelligentSummaryFixed(Map<String, Object> variables, List<Map<String, Object>> history) {
        StringBuilder summary = new StringBuilder();

        summary.append("🤖 RÉSUMÉ CONVERSATION CHATBOT\n");
        summary.append("=====================================\n\n");
        summary.append("💬 Total interactions: ").append(history.size()).append("\n\n");

        // INFORMATIONS CLIENT
        summary.append("👤 INFORMATIONS CLIENT:\n");
        boolean hasClientInfo = false;

        String[] clientFields = { "nom", "name", "prenom", "firstname", "email", "telephone", "phone" };
        for (String field : clientFields) {
            Object value = variables.get(field);
            if (value != null && !value.toString().trim().isEmpty()) {
                summary.append("• ").append(field.toUpperCase()).append(": ").append(value).append("\n");
                hasClientInfo = true;
            }
        }

        if (!hasClientInfo) {
            summary.append("Aucune information client collectée.\n");
        }

        // HISTORIQUE DES MESSAGES
        summary.append("\n💬 HISTORIQUE DES MESSAGES:\n");
        boolean hasMessages = false;

        for (Map<String, Object> entry : history) {
            String type = (String) entry.get("type");
            if ("user_message".equals(type) || "last_user_message".equals(type)) {
                String content = (String) entry.get("content");
                String messageType = (String) entry.get("messageType");
                Boolean hasMedia = (Boolean) entry.get("hasMedia");

                if (content != null && !content.isEmpty()) {
                    summary.append("• MESSAGE: ").append(content).append("\n");
                    hasMessages = true;
                }

                if (Boolean.TRUE.equals(hasMedia)) {
                    summary.append("• MÉDIA: ").append(messageType.toUpperCase()).append(" envoyé\n");
                    hasMessages = true;
                }
            }
        }

        if (!hasMessages) {
            summary.append("Aucun message utilisateur trouvé.\n");
        }

        // DONNÉES COLLECTÉES
        summary.append("\n📋 DONNÉES COLLECTÉES:\n");
        boolean hasData = false;

        for (Map<String, Object> entry : history) {
            if ("collected_data".equals(entry.get("type"))) {
                summary.append("• ").append(entry.get("field")).append(": ").append(entry.get("value")).append("\n");
                hasData = true;
            }
        }

        if (!hasData) {
            summary.append("Aucune donnée spécifique collectée.\n");
        }

        summary.append("\n🎯 Client transféré et prêt pour prise en charge humaine.");

        String result = summary.toString();
        log.info("📝 Résumé généré: {} caractères, {} lignes", result.length(), result.split("\n").length);

        return result;
    }

    /**
     * CORRECTION 5: MESSAGE HISTORIQUE DÉTAILLÉ POUR 3CX
     */
    private String buildHistoryMessageFixed(Map<String, Object> data) {
        try {
            StringBuilder historique = new StringBuilder();

            // Récupérer l'historique depuis les données
            List<Map<String, Object>> messageHistory = (List<Map<String, Object>>) data.get("conversationHistory");

            if (messageHistory != null && !messageHistory.isEmpty()) {
                historique.append("📝 HISTORIQUE DÉTAILLÉ:\n");
                historique.append("=====================================\n");

                int messageIndex = 1;
                for (Map<String, Object> msg : messageHistory) {
                    String type = (String) msg.get("type");
                    String content = (String) msg.get("content");
                    String messageType = (String) msg.get("messageType");
                    Boolean hasMedia = (Boolean) msg.get("hasMedia");
                    String timestamp = (String) msg.get("timestamp");
                    String description = (String) msg.get("description");

                    if ("user_message".equals(type) || "last_user_message".equals(type)) {
                        historique.append(String.format("[%d] ", messageIndex++));

                        if (content != null && !content.isEmpty()) {
                            historique.append("💬 MESSAGE: ").append(content).append("\n");
                        }

                        if (Boolean.TRUE.equals(hasMedia)) {
                            historique.append("    📎 MÉDIA: ").append(messageType.toUpperCase()).append(" envoyé\n");
                        }

                        if (timestamp != null) {
                            try {
                                Instant time = Instant.parse(timestamp);
                                historique.append("    ⏰ ").append(time).append("\n");
                            } catch (Exception e) {
                                historique.append("    ⏰ ").append(timestamp).append("\n");
                            }
                        }

                        historique.append("\n");
                    }

                    if ("collected_data".equals(type)) {
                        String field = (String) msg.get("field");
                        String value = (String) msg.get("value");
                        if (field != null && value != null) {
                            historique.append("📋 ").append(field.toUpperCase()).append(": ").append(value).append("\n");
                        }
                    }
                }

                if (messageIndex == 1) {
                    historique.append("⚠️ Aucun message utilisateur trouvé dans l'historique.\n");
                }
            } else {
                historique.append("❌ HISTORIQUE VIDE\n");
                historique.append("Aucune donnée de conversation disponible.\n");

                // DEBUG: Afficher ce qui est disponible dans data
                historique.append("\n🔍 DEBUG - Données disponibles:\n");
                for (String key : data.keySet()) {
                    historique.append("• ").append(key).append("\n");
                }
            }

            String result = historique.toString();
            log.info("📚 Historique message construit: {} caractères", result.length());

            return result;
        } catch (Exception e) {
            log.error("❌ Erreur construction historique message: {}", e.getMessage(), e);
            return "❌ Erreur récupération historique conversation.";
        }
    }

    /**
     * CORRECTION 6: SAUVEGARDE MÉDIAS DANS LES NŒUDS FILE ET IMAGE
     */
    private WhatsAppResponse executeFileNode(
        FlowNodePayload node,
        FlowPayload flowPayload,
        ChatbotSession session,
        String messageType,
        String mediaId
    ) {
        NodeDataPayload data = node.getData();

        // Si le nœud envoie un fichier
        if (data.getFileUrl() != null && !data.getFileUrl().isEmpty()) {
            // Passer au nœud suivant
            String nextNodeId = node.getNextNodeId();
            if (nextNodeId != null) {
                session.setCurrentNodeId(nextNodeId);
            }

            WhatsAppResponse response = new WhatsAppResponse();
            response.setType("document");
            response.setFileUrl(data.getFileUrl());
            response.setFileName(data.getFileName());

            String caption = data.getText();
            if (caption != null) {
                caption = replaceVariablesInText(caption, session);
                response.setCaption(caption);
            }

            log.debug("📎 Envoi fichier: {} ({})", data.getFileName(), data.getFileUrl());
            return response;
        }

        // Si le nœud attend un fichier de l'utilisateur
        if (messageType == null || isFirstVisit(session, node.getId())) {
            String text = data.getText() != null ? data.getText() : "Veuillez envoyer votre fichier";
            return createTextResponse(text);
        }

        // Vérifier que l'utilisateur a envoyé un fichier
        if (
            !"document".equals(messageType) && !"image".equals(messageType) && !"audio".equals(messageType) && !"video".equals(messageType)
        ) {
            return createTextResponse("❌ Veuillez envoyer un fichier, pas un message texte.");
        }

        // Sauvegarder le fichier reçu
        Map<String, Object> variables = getSessionVariables(session);
        variables.put("file.mediaId", mediaId != null ? mediaId : "");
        variables.put("file.type", messageType);
        variables.put("file.receivedAt", Instant.now().toString());
        variables.put("user.hasReceivedFile", "true");
        variables.put("user.lastFileType", messageType);

        // Sauvegarder dans l'historique des messages
        int messageCount = Integer.parseInt(variables.getOrDefault("user.messageCount", "0").toString());
        messageCount++;
        variables.put("user.messageCount", String.valueOf(messageCount));

        String messageKey = "user.message_" + messageCount;
        variables.put(messageKey, "");
        variables.put(messageKey + ".type", messageType);
        variables.put(messageKey + ".mediaId", mediaId);
        variables.put(messageKey + ".timestamp", Instant.now().toString());
        variables.put(messageKey + ".description", "Fichier " + messageType + " reçu");

        // Sauvegarder les variables
        try {
            session.setVariables(objectMapper.writeValueAsString(variables));
            chatbotSessionRepository.save(session);
        } catch (Exception e) {
            log.error("❌ Erreur sauvegarde fichier: {}", e.getMessage());
        }

        log.info("📁 Fichier reçu et sauvegardé: type={}, mediaId={}", messageType, mediaId);

        // CORRECTION: PASSER DIRECTEMENT AU NŒUD SUIVANT SANS MESSAGE
        String nextNodeId = node.getNextNodeId();
        if (nextNodeId != null) {
            session.setCurrentNodeId(nextNodeId);
            FlowNodePayload nextNode = findNodeById(flowPayload, nextNodeId);
            if (nextNode != null) {
                // CORRECTION: PAS DE MESSAGE DE CONFIRMATION
                return executeNodeByType(nextNode, flowPayload, session, null, null, null);
            }
        }

        // Message d'erreur seulement si pas de nœud suivant
        log.error("❌ Aucun nœud suivant configuré après réception fichier");
        return createTextResponse("❌ Erreur de configuration - suite de conversation introuvable.");
    }

    /**
     * CORRECTION 7: RÉSUMÉ INTELLIGENT POUR AGENT AVEC SESSION RÉELLE
     */
    private String buildIntelligentConversationSummaryFixed(ChatbotSession session, FlowPayload flowPayload) {
        try {
            // Récupérer les variables FRAÎCHES
            Map<String, Object> variables = getSessionVariablesDirect(session);

            // Construire l'historique avec les vraies variables
            List<Map<String, Object>> history = buildCompleteHistoryFixed(session, variables);

            // Générer le résumé
            return buildIntelligentSummaryFixed(variables, history);
        } catch (Exception e) {
            log.error("❌ Erreur création résumé intelligent: {}", e.getMessage(), e);
            return "Résumé de conversation chatbot - Client transféré vers agent.";
        }
    }

    /**
     * CORRECTION 8: EXTRACTION DONNÉES UTILISATEUR AVEC VARIABLES RÉELLES
     */
    private Map<String, Object> extractUserDataFixed(Map<String, Object> variables) {
        Map<String, Object> userData = new HashMap<>();

        // Extraire TOUTES les données utilisateur (pas seulement celles qui ne commencent pas par system/user)
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue().toString();

            // Inclure les données importantes même si elles commencent par "user."
            if (!key.startsWith("system.") && !key.startsWith("3cx.") && !value.isEmpty()) {
                userData.put(key, value);
            }
        }

        log.debug("👤 Données utilisateur extraites: {} entrées", userData.size());

        return userData;
    }

    /**
     * CORRECTION 1: MODIFICATION DE call3CXWebhook POUR INCLURE LES MÉDIAS
     */
    private boolean call3CXWebhook(String webhookUrl, Map<String, Object> data) {
        try {
            log.info("🔗 Appel webhook 3CX vers: {}", webhookUrl);

            String phoneNumberId = "633542099835858";
            String businessId = "1209741957334833";
            String displayPhoneNumber = "32942393";

            String userId = (String) data.get("phoneNumber");
            String userName = extractUserName(data);

            // Construction de l'historique utilisateur
            String historiqueMessage = buildHistoryMessageFixed(data);

            // CORRECTION CRITIQUE: Construire TOUS les messages (texte + médias)
            List<Map<String, Object>> allMessages = buildAllMessagesForTransfer(data, userId, historiqueMessage);

            if (allMessages.isEmpty()) {
                log.warn("⚠️ Aucun message à envoyer à 3CX");
                return false;
            }

            // Construction du payload 3CX avec TOUS les messages
            Map<String, Object> payload = buildComplete3CXPayloadWithMedia(allMessages, userId, userName);

            // Envoi du webhook
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("User-Agent", "WhatsApp-Chatbot/1.0");

            String jsonPayload = objectMapper.writeValueAsString(payload);
            HttpEntity<String> entity = new HttpEntity<>(jsonPayload, headers);

            log.info("📤 Payload 3CX envoyé avec {} messages", allMessages.size());
            log.debug("📤 Payload JSON: {}", jsonPayload);

            ResponseEntity<String> response = restTemplate.postForEntity(webhookUrl, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("✅ Transfert réussi à 3CX: {} messages envoyés (texte + médias)", allMessages.size());
                return true;
            } else {
                log.error("❌ Erreur webhook 3CX: {} - {}", response.getStatusCode(), response.getBody());
                return false;
            }
        } catch (Exception e) {
            log.error("💥 Exception webhook 3CX: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * CORRECTION 2: CONSTRUCTION DE TOUS LES MESSAGES POUR TRANSFERT
     */
    private List<Map<String, Object>> buildAllMessagesForTransfer(Map<String, Object> data, String userId, String historiqueMessage) {
        List<Map<String, Object>> allMessages = new ArrayList<>();

        try {
            // 1. MESSAGE PRINCIPAL AVEC HISTORIQUE TEXTE
            String idMessage = "auto-" + System.currentTimeMillis();
            data.put("lastMessageId", idMessage); // Pour récupération conversation ID

            Map<String, Object> mainMessage = new HashMap<>();
            mainMessage.put("from", userId);
            mainMessage.put("id", idMessage);
            mainMessage.put("timestamp", String.valueOf(System.currentTimeMillis() / 1000));
            mainMessage.put("type", "text");

            Map<String, Object> textContent = new HashMap<>();
            textContent.put("body", buildTransferMessageFixed(data, historiqueMessage));
            mainMessage.put("text", textContent);

            allMessages.add(mainMessage);
            log.debug("📝 Message principal ajouté: {} caractères", textContent.get("body").toString().length());

            // 2. AJOUTER TOUS LES MÉDIAS DE LA CONVERSATION
            List<Map<String, Object>> conversationHistory = (List<Map<String, Object>>) data.get("conversationHistory");

            if (conversationHistory != null) {
                for (Map<String, Object> historyEntry : conversationHistory) {
                    String type = (String) historyEntry.get("type");
                    Boolean hasMedia = (Boolean) historyEntry.get("hasMedia");

                    if (("user_message".equals(type) || "last_user_message".equals(type)) && Boolean.TRUE.equals(hasMedia)) {
                        String mediaId = (String) historyEntry.get("mediaId");
                        String messageType = (String) historyEntry.get("messageType");
                        String content = (String) historyEntry.get("content");

                        if (mediaId != null && !mediaId.isEmpty() && messageType != null) {
                            Map<String, Object> mediaMessage = createMediaMessageForTransfer(userId, messageType, mediaId, content);
                            if (mediaMessage != null) {
                                allMessages.add(mediaMessage);
                                log.info("📎 Média ajouté pour transfert: type={}, mediaId={}", messageType, mediaId);
                            }
                        }
                    }
                }
            }

            log.info("📨 Total messages pour transfert: {} (1 texte + {} médias)", allMessages.size(), allMessages.size() - 1);
        } catch (Exception e) {
            log.error("❌ Erreur construction messages transfert: {}", e.getMessage(), e);
        }

        return allMessages;
    }

    /**
     * CORRECTION 5: PAYLOAD 3CX AVEC SUPPORT MULTI-MESSAGES
     */
    private Map<String, Object> buildComplete3CXPayloadWithMedia(List<Map<String, Object>> allMessages, String userId, String userName) {
        String phoneNumberId = "633542099835858";
        String businessId = "1209741957334833";
        String displayPhoneNumber = "32942393";

        Map<String, Object> payload = new HashMap<>();
        payload.put("object", "whatsapp_business_account");

        List<Map<String, Object>> entries = new ArrayList<>();
        Map<String, Object> entry = new HashMap<>();
        entry.put("id", businessId);

        List<Map<String, Object>> changes = new ArrayList<>();
        Map<String, Object> change = new HashMap<>();
        change.put("field", "messages");

        Map<String, Object> value = new HashMap<>();
        value.put("messaging_product", "whatsapp");

        // Métadonnées
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("display_phone_number", displayPhoneNumber);
        metadata.put("phone_number_id", phoneNumberId);

        Map<String, Object> customMetadata = new HashMap<>();
        customMetadata.put("agent_transfer", true);
        customMetadata.put("source", "chatbot_with_media");
        customMetadata.put("message_count", allMessages.size());
        customMetadata.put("timestamp", Instant.now().toString());
        metadata.put("custom_metadata", customMetadata);

        value.put("metadata", metadata);

        // Contacts
        List<Map<String, Object>> contacts = new ArrayList<>();
        Map<String, Object> contact = new HashMap<>();
        Map<String, Object> profile = new HashMap<>();
        profile.put("name", userName);
        contact.put("profile", profile);
        contact.put("wa_id", userId);
        contacts.add(contact);

        value.put("contacts", contacts);

        // CORRECTION CRITIQUE: Inclure TOUS les messages (texte + médias)
        value.put("messages", allMessages);

        change.put("value", value);
        changes.add(change);
        entry.put("changes", changes);
        entries.add(entry);
        payload.put("entry", entries);

        return payload;
    }

    /**
     * CORRECTION 11: MESSAGE DE TRANSFERT AVEC VRAIES DONNÉES
     */
    private String buildTransferMessageFixed(Map<String, Object> data, String historiqueMessage) {
        StringBuilder message = new StringBuilder();

        // Message principal
        message.append("🔄 TRANSFERT CHATBOT → AGENT HUMAIN\n");
        message.append("=====================================\n\n");

        // Ajouter le résumé intelligent si disponible
        String agentSummary = (String) data.get("agentSummary");
        if (agentSummary != null && !agentSummary.isEmpty()) {
            message.append(agentSummary).append("\n\n");
        }

        // Ajouter l'historique
        message.append(historiqueMessage).append("\n");

        // Informations supplémentaires
        message.append("⏰ Heure transfert: ").append(Instant.now()).append("\n");
        message.append("🔗 Source: Chatbot WhatsApp Automatique\n");

        String result = message.toString();
        log.debug("📤 Message transfert construit: {} caractères", result.length());

        return result;
    }

    /**
     * Détermine intelligemment le nœud de départ selon le contexte
     */
    private FlowNodePayload determineStartingNode(ChatbotSession session, FlowPayload flowPayload, boolean hasUserInput) {
        String currentNodeId = session.getCurrentNodeId();
        FlowNodePayload currentNode = findNodeById(flowPayload, currentNodeId);

        // CAS 1: Nœud actuel inexistant -> chercher le start
        if (currentNode == null) {
            log.debug("🔍 Nœud actuel {} non trouvé, recherche du nœud start", currentNodeId);
            FlowNodePayload startNode = findStartNode(flowPayload);
            if (startNode != null) {
                session.setCurrentNodeId(startNode.getId());
                return startNode;
            }
            return null;
        }

        // CAS 2: Nœud START -> passer automatiquement au suivant
        if ("start".equals(currentNode.getType())) {
            log.debug("🏁 Nœud START détecté, passage automatique au suivant");
            String nextNodeId = currentNode.getNextNodeId();
            if (nextNodeId != null) {
                FlowNodePayload nextNode = findNodeById(flowPayload, nextNodeId);
                if (nextNode != null) {
                    session.setCurrentNodeId(nextNodeId);
                    return nextNode;
                }
            }
            return currentNode; // Fallback si pas de suivant
        }

        // CAS 3: Nœud normal avec input utilisateur -> utiliser le nœud actuel
        if (hasUserInput) {
            log.debug("💬 Input utilisateur présent, traitement du nœud actuel: {}", currentNodeId);
            return currentNode;
        }

        // CAS 4: Pas d'input utilisateur -> vérifier si on doit continuer l'exécution
        log.debug("🔄 Pas d'input utilisateur, vérification de la continuité");
        return currentNode;
    }

    /**
     * Traitement séquentiel optimisé avec gestion waitForUserResponse
     */
    private WhatsAppMultiResponse processNodeSequence(
        FlowNodePayload startNode,
        FlowPayload flowPayload,
        ChatbotSession session,
        String userInput,
        String messageType,
        String mediaId
    ) {
        WhatsAppMultiResponse multiResponse = new WhatsAppMultiResponse();
        FlowNodePayload currentNode = startNode;
        int maxSequentialNodes = 10;
        int processedNodes = 0;
        boolean isFirstNode = true;

        while (currentNode != null && processedNodes < maxSequentialNodes) {
            log.debug(
                "🔄 Traitement nœud séquentiel: {} (type: {}, waitForUserResponse: {})",
                currentNode.getId(),
                currentNode.getType(),
                currentNode.getData().getWaitForUserResponse()
            );

            // ÉTAPE 1: Exécuter le nœud actuel
            WhatsAppResponse nodeResponse = executeNodeByType(
                currentNode,
                flowPayload,
                session,
                isFirstNode ? userInput : null, // Seul le premier nœud reçoit l'input utilisateur
                isFirstNode ? messageType : null,
                isFirstNode ? mediaId : null
            );

            // ÉTAPE 2: Ajouter la réponse si elle existe
            if (nodeResponse != null && shouldAddResponseToOutput(currentNode, nodeResponse)) {
                multiResponse.addResponse(nodeResponse);
            }

            processedNodes++;
            isFirstNode = false;

            // ÉTAPE 3: Vérifier si on doit s'arrêter
            if (shouldStopSequenceOptimized(currentNode, session)) {
                log.debug("⏹️ Arrêt séquence au nœud: {} (raison: attend réponse ou fin)", currentNode.getId());
                break;
            }

            // ÉTAPE 4: Déterminer et passer au nœud suivant
            String nextNodeId = determineNextNodeId(currentNode, session);
            if (nextNodeId == null) {
                log.debug("🏁 Fin de séquence - pas de nœud suivant");
                break;
            }

            // ÉTAPE 5: Mettre à jour la session avec le nœud suivant
            session.setCurrentNodeId(nextNodeId);
            currentNode = findNodeById(flowPayload, nextNodeId);

            if (currentNode == null) {
                log.warn("⚠️ Nœud suivant {} non trouvé", nextNodeId);
                break;
            }
        }

        if (processedNodes >= maxSequentialNodes) {
            log.warn("⚠️ Limite de nœuds séquentiels atteinte ({})", maxSequentialNodes);
            multiResponse.addResponse(createTextResponse("⚠️ Limite de traitement atteinte. Tapez 'restart' si nécessaire."));
        }

        return multiResponse;
    }

    /**
     * Détermine si on doit ajouter la réponse à la sortie
     */
    private boolean shouldAddResponseToOutput(FlowNodePayload node, WhatsAppResponse response) {
        // Ne pas ajouter les réponses vides
        if (response == null) {
            return false;
        }

        // Pour les nœuds START, ne pas ajouter de réponse
        if ("start".equals(node.getType())) {
            return false;
        }

        // Pour les nœuds de condition et variable_set, ne pas ajouter si c'est juste du texte générique
        if ("condition".equals(node.getType()) || "variable_set".equals(node.getType())) {
            if (
                "text".equals(response.getType()) &&
                (response.getText() == null ||
                    response.getText().contains("Condition évaluée") ||
                    response.getText().contains("Variable mise à jour"))
            ) {
                return false;
            }
        }

        // Ajouter toutes les autres réponses
        return true;
    }

    /**
     * Détermine si on doit arrêter la séquence avec logique optimisée
     */
    private boolean shouldStopSequenceOptimized(FlowNodePayload node, ChatbotSession session) {
        // RÈGLE 1: Arrêter si c'est un nœud de fin
        if ("end".equals(node.getType())) {
            log.debug("🏁 Arrêt: nœud de fin détecté");
            return true;
        }

        // RÈGLE 2: Arrêter si la session est inactive
        if (!session.getIsActive()) {
            log.debug("💤 Arrêt: session inactive");
            return true;
        }

        // RÈGLE 3: Vérifier le flag waitForUserResponse
        Boolean waitForUserResponse = node.getData().getWaitForUserResponse();
        if (Boolean.TRUE.equals(waitForUserResponse)) {
            log.debug("⏳ Arrêt: nœud attend une réponse utilisateur (waitForUserResponse=true)");
            return true;
        }

        // RÈGLE 4: Types de nœuds qui attendent TOUJOURS une réponse utilisateur
        String nodeType = node.getType();
        if ("buttons".equals(nodeType) || "list".equals(nodeType) || "input".equals(nodeType) || "wait_response".equals(nodeType)) {
            log.debug("⏳ Arrêt: type de nœud {} attend toujours une réponse", nodeType);
            return true;
        }

        // RÈGLE 5: Continuer pour tous les autres cas
        log.debug("▶️ Continuation: nœud {} peut être traité automatiquement", nodeType);
        return false;
    }

    /**
     * Détermine l'ID du nœud suivant selon le type de nœud et les conditions
     */
    private String determineNextNodeId(FlowNodePayload node, ChatbotSession session) {
        try {
            log.debug("🔍 Détermination nœud suivant pour: {} (type: {})", node.getId(), node.getType());

            NodeDataPayload data = node.getData();

            switch (node.getType()) {
                case "start":
                case "message":
                case "image":
                case "file":
                case "variable_set":
                case "webhook":
                    // Nœuds simples avec nextNodeId direct
                    return node.getNextNodeId();
                case "input":
                case "wait_response":
                    // Ces nœuds attendent normalement une réponse, mais si waitForUserResponse=false
                    return node.getNextNodeId();
                case "buttons":
                    // Pour les boutons, vérifier s'il y a une connexion par défaut
                    if (data.getButtons() != null && data.getButtons().size() == 1) {
                        // Si un seul bouton, on peut l'utiliser automatiquement
                        ButtonPayload singleButton = data.getButtons().get(0);
                        if (singleButton.getNextNodeId() != null) {
                            return singleButton.getNextNodeId();
                        }
                    }
                    // Sinon, utiliser le nœud suivant par défaut
                    return node.getNextNodeId();
                case "list":
                    // Pour les listes, vérifier s'il y a une connexion par défaut
                    if (data.getItems() != null && data.getItems().size() == 1) {
                        // Si un seul item, on peut l'utiliser automatiquement
                        ListItemPayload singleItem = data.getItems().get(0);
                        if (singleItem.getNextNodeId() != null) {
                            return singleItem.getNextNodeId();
                        }
                    }
                    // Sinon, utiliser le nœud suivant par défaut
                    return node.getNextNodeId();
                case "condition":
                    // Pour les conditions, évaluer et retourner le nœud approprié
                    return evaluateConditionAndGetNextNode(node, session);
                case "end":
                    // Nœud de fin, pas de suivant
                    return null;
                default:
                    log.warn("⚠️ Type de nœud non géré pour détermination suivant: {}", node.getType());
                    return node.getNextNodeId();
            }
        } catch (Exception e) {
            log.error("❌ Erreur détermination nœud suivant: {}", e.getMessage(), e);
            return node.getNextNodeId(); // Fallback vers le nœud suivant simple
        }
    }

    /**
     * Évalue une condition et retourne l'ID du nœud suivant
     */
    private String evaluateConditionAndGetNextNode(FlowNodePayload conditionNode, ChatbotSession session) {
        try {
            NodeDataPayload data = conditionNode.getData();
            Map<String, Object> variables = getSessionVariables(session);
            String lastUserInput = (String) variables.get("user.lastMessage");

            log.debug("🔀 Évaluation condition pour détermination nœud suivant");

            // Évaluer les connexions conditionnelles
            if (data.getConditionalConnections() != null && !data.getConditionalConnections().isEmpty()) {
                for (ConditionalConnectionPayload connection : data.getConditionalConnections()) {
                    boolean conditionMet = false;

                    if ("custom_expression".equals(connection.getOperator())) {
                        // Expression custom
                        String expression = connection.getCondition();
                        conditionMet = evaluateCustomExpression(expression, lastUserInput, variables);
                        log.debug("🧪 Expression custom '{}' = {}", expression, conditionMet);
                    } else {
                        // Condition standard
                        conditionMet = evaluateCondition(connection, data, session);
                        log.debug("🧪 Condition standard '{}' = {}", connection.getCondition(), conditionMet);
                    }

                    if (conditionMet && connection.getNextNodeId() != null) {
                        log.debug("✅ Condition vraie, nœud suivant: {}", connection.getNextNodeId());
                        return connection.getNextNodeId();
                    }
                }
            }

            // Si aucune condition n'est vraie, utiliser le nœud par défaut
            String defaultNodeId = data.getDefaultNextNodeId();
            if (defaultNodeId != null) {
                log.debug("🔄 Utilisation nœud par défaut: {}", defaultNodeId);
                return defaultNodeId;
            }

            log.debug("❌ Aucune condition vraie et pas de nœud par défaut");
            return null;
        } catch (Exception e) {
            log.error("❌ Erreur évaluation condition: {}", e.getMessage(), e);
            return conditionNode.getData().getDefaultNextNodeId();
        }
    }

    /**
     * Exécution du nœud START optimisée
     */
    private WhatsAppResponse executeStartNode(FlowNodePayload node, FlowPayload flowPayload, ChatbotSession session) {
        log.debug("🏁 Exécution nœud START - passage automatique au suivant");

        // Le nœud START ne fait rien d'autre que passer au suivant
        String nextNodeId = node.getNextNodeId();
        if (nextNodeId != null) {
            session.setCurrentNodeId(nextNodeId);
            log.debug("➡️ Nœud suivant configuré: {}", nextNodeId);
        }

        // Ne retourner aucune réponse pour le nœud START
        return null;
    }

    /**
     * Exécution des nœuds message avec logique waitForUserResponse
     */
    private WhatsAppResponse executeMessageNode(FlowNodePayload node, FlowPayload flowPayload, ChatbotSession session) {
        NodeDataPayload data = node.getData();
        String text = data.getText();

        if (text == null || text.isEmpty()) {
            text = "Message vide";
        }

        // Remplacer les variables dans le texte
        text = replaceVariablesInText(text, session);

        log.debug("💬 Envoi message: {}", text);

        // IMPORTANT: Ne pas passer automatiquement au nœud suivant ici
        // C'est géré dans processNodeSequence selon waitForUserResponse
        String nextNodeId = node.getNextNodeId();
        if (nextNodeId != null && !Boolean.TRUE.equals(data.getWaitForUserResponse())) {
            session.setCurrentNodeId(nextNodeId);
        }

        return createTextResponse(text);
    }

    /**
     * ================================
     * EXÉCUTION NŒUD BOUTONS
     * ================================
     */
    private WhatsAppResponse executeButtonsNode(FlowNodePayload node, FlowPayload flowPayload, ChatbotSession session, String userInput) {
        NodeDataPayload data = node.getData();

        // Si première visite, envoyer les boutons
        if (userInput == null || isFirstVisit(session, node.getId())) {
            log.debug("🔘 Envoi des boutons - {} options disponibles", data.getButtons() != null ? data.getButtons().size() : 0);

            if (data.getButtons() == null || data.getButtons().isEmpty()) {
                log.warn("⚠️ Aucun bouton configuré pour le nœud {}", node.getId());
                return createTextResponse("Aucune option disponible.");
            }

            String text = data.getText() != null ? data.getText() : "Choisissez une option:";
            text = replaceVariablesInText(text, session);

            return createButtonsResponse(text, convertToButtonOptions(data.getButtons()));
        }

        // Traiter la réponse utilisateur
        log.debug("🎯 Traitement choix utilisateur: {}", userInput);

        ButtonPayload selectedButton = findButtonByText(data.getButtons(), userInput);
        if (selectedButton != null) {
            log.debug("✅ Bouton sélectionné: {}", selectedButton.getText());

            // Sauvegarder la valeur dans la variable si configurée
            if (data.getStoreInVariable() != null && selectedButton.getValue() != null) {
                setSessionVariable(session, data.getStoreInVariable(), selectedButton.getValue());
                log.debug("💾 Variable sauvegardée: {} = {}", data.getStoreInVariable(), selectedButton.getValue());
            }

            // Déterminer le nœud suivant
            String nextNodeId = selectedButton.getNextNodeId();
            if (nextNodeId == null) {
                nextNodeId = node.getNextNodeId();
            }

            if (nextNodeId != null) {
                session.setCurrentNodeId(nextNodeId);
                FlowNodePayload nextNode = findNodeById(flowPayload, nextNodeId);
                if (nextNode != null) {
                    // CORRECTION: PASSER DIRECTEMENT AU NŒUD SUIVANT SANS MESSAGE
                    return executeNodeByType(nextNode, flowPayload, session, null, null, null);
                }
            }

            // CORRECTION: Message d'erreur seulement si pas de nœud suivant trouvé
            log.error("❌ Aucun nœud suivant trouvé pour le bouton sélectionné");
            return createTextResponse("❌ Erreur de configuration - suite de conversation introuvable.");
        } else {
            log.debug("❌ Réponse invalide, renvoi des boutons");
            String text = "❌ Veuillez choisir une option valide:";
            return createButtonsResponse(text, convertToButtonOptions(data.getButtons()));
        }
    }

    /**
     * ================================
     * EXÉCUTION NŒUD LISTE DÉROULANTE
     * ================================
     */
    private WhatsAppResponse executeListNode(FlowNodePayload node, FlowPayload flowPayload, ChatbotSession session, String userInput) {
        NodeDataPayload data = node.getData();

        // Si première visite, envoyer la liste
        if (userInput == null || isFirstVisit(session, node.getId())) {
            log.debug("📝 Envoi de la liste - {} options disponibles", data.getItems() != null ? data.getItems().size() : 0);

            if (data.getItems() == null || data.getItems().isEmpty()) {
                log.warn("⚠️ Aucune option configurée pour le nœud {}", node.getId());
                return createTextResponse("Aucune option disponible.");
            }

            String text = data.getText() != null ? data.getText() : "Sélectionnez dans la liste:";
            text = replaceVariablesInText(text, session);

            // Marquer que ce nœud a été affiché
            setSessionVariable(session, "system.lastProcessedNode", node.getId());

            return createListResponse(text, convertToListItems(data.getItems()));
        }

        // Traiter la sélection utilisateur
        log.debug("🎯 Traitement sélection utilisateur: '{}' pour nœud: {}", userInput, node.getId());

        ListItemPayload selectedItem = findListItemByTitle(data.getItems(), userInput);

        // Si pas trouvé par titre, chercher par index numérique
        if (selectedItem == null && userInput.matches("\\d+")) {
            try {
                int index = Integer.parseInt(userInput) - 1;
                if (index >= 0 && index < data.getItems().size()) {
                    selectedItem = data.getItems().get(index);
                    log.debug("🔢 Option trouvée par index: {} = {}", userInput, selectedItem.getTitle());
                }
            } catch (NumberFormatException e) {
                // Ignore
            }
        }

        if (selectedItem != null) {
            log.debug("✅ Option sélectionnée: {}", selectedItem.getTitle());

            // Sauvegarder la valeur dans la variable si configurée
            if (data.getStoreInVariable() != null && selectedItem.getValue() != null) {
                setSessionVariable(session, data.getStoreInVariable(), selectedItem.getValue());
                log.debug("💾 Variable sauvegardée: {} = {}", data.getStoreInVariable(), selectedItem.getValue());
            }

            // Déterminer le nœud suivant
            String nextNodeId = selectedItem.getNextNodeId();
            if (nextNodeId == null || nextNodeId.isEmpty()) {
                nextNodeId = node.getNextNodeId();
            }

            if (nextNodeId != null && !nextNodeId.isEmpty()) {
                session.setCurrentNodeId(nextNodeId);
                FlowNodePayload nextNode = findNodeById(flowPayload, nextNodeId);
                if (nextNode != null) {
                    // CORRECTION: PASSER DIRECTEMENT AU NŒUD SUIVANT SANS MESSAGE
                    return executeNodeByType(nextNode, flowPayload, session, null, null, null);
                } else {
                    log.error("❌ Nœud suivant '{}' non trouvé dans le flow", nextNodeId);
                    return createTextResponse("❌ Erreur de configuration - nœud suivant non trouvé.");
                }
            }

            // CORRECTION: Message d'erreur seulement si pas de nœud suivant
            log.error("❌ Aucun nœud suivant configuré pour l'option sélectionnée");
            return createTextResponse("❌ Erreur de configuration - suite de conversation introuvable.");
        } else {
            log.debug("❌ Sélection invalide '{}', renvoi de la liste", userInput);

            // Construire le message d'erreur avec les options disponibles
            StringBuilder errorMessage = new StringBuilder("❌ Veuillez sélectionner une option valide:\n\n");
            for (int i = 0; i < data.getItems().size(); i++) {
                ListItemPayload item = data.getItems().get(i);
                errorMessage.append(String.format("%d. %s\n", i + 1, item.getTitle()));
            }
            errorMessage.append("\nTapez le numéro ou le nom exact de l'option.");

            return createTextResponse(errorMessage.toString());
        }
    }

    /**
     * AMÉLIORATION 3: EXÉCUTION NŒUD INPUT AVEC VALIDATION STRICTE PAR TYPE
     */
    /**
     * ================================
     * CORRECTION MAJEURE: NŒUD INPUT AVEC SUPPORT COMPLET DES MÉDIAS
     * ================================
     */
    private WhatsAppResponse executeInputNode(
        FlowNodePayload node,
        FlowPayload flowPayload,
        ChatbotSession session,
        String userInput,
        String messageType,
        String mediaId
    ) {
        NodeDataPayload data = node.getData();

        // Si première visite, poser la question
        if ((userInput == null && mediaId == null) || isFirstVisit(session, node.getId())) {
            String text = data.getText() != null ? data.getText() : "Veuillez saisir votre réponse:";
            text = replaceVariablesInText(text, session);

            setSessionVariable(session, "system.lastProcessedNode", node.getId());
            log.debug("❓ Question posée: {}", text);
            return createTextResponse(text);
        }

        String responseType = data.getResponseType() != null ? data.getResponseType() : "text";
        log.debug(
            "🔍 Validation input: type='{}', userInput='{}', messageType='{}', mediaId='{}'",
            responseType,
            userInput,
            messageType,
            mediaId
        );

        // CORRECTION CRITIQUE: Vérifier si obligatoire selon le type attendu
        if (Boolean.TRUE.equals(data.getRequired())) {
            boolean hasRequiredData = checkRequiredDataByType(responseType, userInput, messageType, mediaId);

            if (!hasRequiredData) {
                String errorMsg = data.getValidationMessage() != null ? data.getValidationMessage() : getRequiredErrorMessage(responseType);
                return createTextResponse("❌ " + errorMsg);
            }
        }

        // VALIDATION SELON LE TYPE ATTENDU
        ValidationResult validation = validateInputByType(responseType, userInput, messageType, mediaId, data);

        if (!validation.isValid()) {
            log.debug("❌ Validation échouée: {}", validation.getErrorMessage());
            String customMessage = data.getValidationMessage();
            String errorMessage = customMessage != null && !customMessage.isEmpty() ? customMessage : validation.getErrorMessage();
            return createTextResponse("❌ " + errorMessage);
        }

        // SAUVEGARDE SELON LE TYPE DE DONNÉES REÇUES
        if (data.getStoreInVariable() != null) {
            String valueToStore = getValueToStore(responseType, userInput, messageType, mediaId);
            setSessionVariable(session, data.getStoreInVariable(), valueToStore);
            log.debug("💾 Données validées et sauvegardées: {} = {}", data.getStoreInVariable(), valueToStore);

            // SAUVEGARDE SUPPLÉMENTAIRE POUR LES MÉDIAS
            if (mediaId != null && !mediaId.isEmpty()) {
                saveMediaToSession(session, messageType, mediaId, data.getStoreInVariable());
            }
        }

        // Passer au nœud suivant
        String nextNodeId = node.getNextNodeId();
        if (nextNodeId != null) {
            session.setCurrentNodeId(nextNodeId);
            FlowNodePayload nextNode = findNodeById(flowPayload, nextNodeId);
            if (nextNode != null) {
                return executeNodeByType(nextNode, flowPayload, session, null, null, null);
            }
        }

        log.error("❌ Aucun nœud suivant configuré après validation input");
        return createTextResponse("❌ Erreur de configuration - suite de conversation introuvable.");
    }

    /**
     * ================================
     * NOUVELLE MÉTHODE: VÉRIFICATION DONNÉES OBLIGATOIRES PAR TYPE
     * ================================
     */
    private boolean checkRequiredDataByType(String responseType, String userInput, String messageType, String mediaId) {
        log.debug(
            "🔍 Vérification données obligatoires: type='{}', userInput='{}', messageType='{}', mediaId='{}'",
            responseType,
            userInput,
            messageType,
            mediaId
        );

        switch (responseType.toLowerCase()) {
            case "text":
            case "email":
            case "phone":
            case "url":
            case "date":
            case "time":
            case "number":
                // Pour les types texte, vérifier que userInput n'est pas vide
                boolean hasTextData = userInput != null && !userInput.trim().isEmpty();
                log.debug("📝 Données texte présentes: {}", hasTextData);
                return hasTextData;
            case "image":
                // Pour les images, vérifier qu'on a reçu une image
                boolean hasImageData = "image".equals(messageType) && mediaId != null && !mediaId.isEmpty();
                log.debug("📸 Données image présentes: messageType='{}', mediaId présent={}", messageType, mediaId != null);
                return hasImageData;
            case "document":
            case "file":
                // Pour les documents, vérifier qu'on a reçu un document
                boolean hasDocumentData = "document".equals(messageType) && mediaId != null && !mediaId.isEmpty();
                log.debug("📄 Données document présentes: messageType='{}', mediaId présent={}", messageType, mediaId != null);
                return hasDocumentData;
            case "audio":
            case "voice":
                // Pour l'audio, vérifier qu'on a reçu un audio ou voice
                boolean hasAudioData =
                    ("audio".equals(messageType) || "voice".equals(messageType)) && mediaId != null && !mediaId.isEmpty();
                log.debug("🎵 Données audio présentes: messageType='{}', mediaId présent={}", messageType, mediaId != null);
                return hasAudioData;
            case "video":
                // Pour les vidéos, vérifier qu'on a reçu une vidéo
                boolean hasVideoData = "video".equals(messageType) && mediaId != null && !mediaId.isEmpty();
                log.debug("🎬 Données vidéo présentes: messageType='{}', mediaId présent={}", messageType, mediaId != null);
                return hasVideoData;
            case "any":
            case "media":
                // Pour "any", accepter soit du texte soit un média
                boolean hasAnyData = (userInput != null && !userInput.trim().isEmpty()) || (mediaId != null && !mediaId.isEmpty());
                log.debug(
                    "📎 Données quelconques présentes: texte={}, média={}",
                    userInput != null && !userInput.trim().isEmpty(),
                    mediaId != null
                );
                return hasAnyData;
            default:
                // Type non reconnu, vérifier au moins le texte
                return userInput != null && !userInput.trim().isEmpty();
        }
    }

    /**
     * ================================
     * NOUVELLE MÉTHODE: MESSAGES D'ERREUR SPÉCIFIQUES PAR TYPE
     * ================================
     */
    private String getRequiredErrorMessage(String responseType) {
        switch (responseType.toLowerCase()) {
            case "image":
                return "Veuillez envoyer une image (JPG, PNG, GIF).";
            case "document":
            case "file":
                return "Veuillez envoyer un document ou fichier.";
            case "audio":
            case "voice":
                return "Veuillez envoyer un fichier audio ou message vocal.";
            case "video":
                return "Veuillez envoyer une vidéo.";
            case "email":
                return "Veuillez saisir votre adresse email.";
            case "phone":
                return "Veuillez saisir votre numéro de téléphone.";
            case "number":
                return "Veuillez saisir un nombre.";
            case "date":
                return "Veuillez saisir une date.";
            case "url":
                return "Veuillez saisir une URL.";
            case "text":
            default:
                return "Cette information est obligatoire.";
        }
    }

    /**
     * ================================
     * NOUVELLE MÉTHODE: VALIDATION PAR TYPE AVEC SUPPORT MÉDIA
     * ================================
     */
    private ValidationResult validateInputByType(
        String responseType,
        String userInput,
        String messageType,
        String mediaId,
        NodeDataPayload data
    ) {
        log.debug(
            "🔍 Validation par type: responseType='{}', messageType='{}', mediaId présent={}",
            responseType,
            messageType,
            mediaId != null
        );

        switch (responseType.toLowerCase()) {
            case "image":
                return validateImageInput(messageType, mediaId);
            case "document":
            case "file":
                return validateDocumentInput(messageType, mediaId);
            case "audio":
            case "voice":
                return validateAudioInput(messageType, mediaId);
            case "video":
                return validateVideoInput(messageType, mediaId);
            case "any":
            case "media":
                return validateAnyInput(userInput, messageType, mediaId);
            case "text":
                return validateTextInput(userInput, data);
            case "email":
                return validateEmailInput(userInput);
            case "phone":
                return validatePhoneInput(userInput);
            case "number":
                return validateNumberInput(userInput, data);
            case "url":
                return validateUrlInput(userInput);
            case "date":
                return validateDateInput(userInput);
            case "time":
                return validateTimeInput(userInput);
            default:
                log.warn("⚠️ Type de réponse non reconnu: {}", responseType);
                return ValidationResult.valid();
        }
    }

    /**
     * ================================
     * NOUVELLES MÉTHODES: VALIDATION SPÉCIFIQUE POUR CHAQUE TYPE DE MÉDIA
     * ================================
     */
    private ValidationResult validateImageInput(String messageType, String mediaId) {
        if (!"image".equals(messageType)) {
            return ValidationResult.invalid("Veuillez envoyer une image (JPG, PNG, GIF), pas " + getFileTypeDescription(messageType) + ".");
        }

        if (mediaId == null || mediaId.isEmpty()) {
            return ValidationResult.invalid("Aucune image reçue. Veuillez envoyer une image.");
        }

        log.debug("✅ Image validée: mediaId={}", mediaId);
        return ValidationResult.valid();
    }

    private ValidationResult validateDocumentInput(String messageType, String mediaId) {
        if (!"document".equals(messageType)) {
            return ValidationResult.invalid(
                "Veuillez envoyer un document (PDF, Word, Excel), pas " + getFileTypeDescription(messageType) + "."
            );
        }

        if (mediaId == null || mediaId.isEmpty()) {
            return ValidationResult.invalid("Aucun document reçu. Veuillez envoyer un fichier.");
        }

        log.debug("✅ Document validé: mediaId={}", mediaId);
        return ValidationResult.valid();
    }

    private ValidationResult validateAudioInput(String messageType, String mediaId) {
        if (!"audio".equals(messageType) && !"voice".equals(messageType)) {
            return ValidationResult.invalid(
                "Veuillez envoyer un fichier audio ou message vocal, pas " + getFileTypeDescription(messageType) + "."
            );
        }

        if (mediaId == null || mediaId.isEmpty()) {
            return ValidationResult.invalid("Aucun fichier audio reçu. Veuillez envoyer un audio.");
        }

        log.debug("✅ Audio validé: mediaId={}", mediaId);
        return ValidationResult.valid();
    }

    private ValidationResult validateVideoInput(String messageType, String mediaId) {
        if (!"video".equals(messageType)) {
            return ValidationResult.invalid("Veuillez envoyer une vidéo, pas " + getFileTypeDescription(messageType) + ".");
        }

        if (mediaId == null || mediaId.isEmpty()) {
            return ValidationResult.invalid("Aucune vidéo reçue. Veuillez envoyer une vidéo.");
        }

        log.debug("✅ Vidéo validée: mediaId={}", mediaId);
        return ValidationResult.valid();
    }

    private ValidationResult validateAnyInput(String userInput, String messageType, String mediaId) {
        // Accepter soit du texte soit un média
        boolean hasText = userInput != null && !userInput.trim().isEmpty();
        boolean hasMedia = mediaId != null && !mediaId.isEmpty();

        if (!hasText && !hasMedia) {
            return ValidationResult.invalid("Veuillez envoyer soit un message texte soit un fichier.");
        }

        log.debug("✅ Données validées: texte={}, média={}", hasText, hasMedia);
        return ValidationResult.valid();
    }

    /**
     * ================================
     * NOUVELLE MÉTHODE: DÉTERMINER LA VALEUR À STOCKER
     * ================================
     */
    private String getValueToStore(String responseType, String userInput, String messageType, String mediaId) {
        switch (responseType.toLowerCase()) {
            case "image":
            case "document":
            case "file":
            case "audio":
            case "voice":
            case "video":
                // Pour les médias, stocker le mediaId
                return mediaId != null ? mediaId : "";
            case "any":
            case "media":
                // Pour "any", priorité au texte si présent, sinon mediaId
                if (userInput != null && !userInput.trim().isEmpty()) {
                    return userInput.trim();
                } else {
                    return mediaId != null ? mediaId : "";
                }
            default:
                // Pour tous les autres types, stocker le texte
                return userInput != null ? userInput.trim() : "";
        }
    }

    /**
     * ================================
     * NOUVELLE MÉTHODE: SAUVEGARDE SUPPLÉMENTAIRE POUR LES MÉDIAS
     * ================================
     */
    private void saveMediaToSession(ChatbotSession session, String messageType, String mediaId, String variableName) {
        try {
            Map<String, Object> variables = getSessionVariables(session);

            // Sauvegarder les métadonnées du média
            variables.put(variableName + ".type", messageType);
            variables.put(variableName + ".mediaId", mediaId);
            variables.put(variableName + ".timestamp", Instant.now().toString());

            // Sauvegarder dans les variables génériques aussi
            variables.put("user.lastMediaId", mediaId);
            variables.put("user.lastMediaType", messageType);

            // Marquer le type spécifique
            switch (messageType) {
                case "image":
                    variables.put("user.lastImage", mediaId);
                    variables.put("user.hasImage", "true");
                    break;
                case "document":
                    variables.put("user.lastDocument", mediaId);
                    variables.put("user.hasDocument", "true");
                    break;
                case "audio":
                case "voice":
                    variables.put("user.lastAudio", mediaId);
                    variables.put("user.hasAudio", "true");
                    break;
                case "video":
                    variables.put("user.lastVideo", mediaId);
                    variables.put("user.hasVideo", "true");
                    break;
            }

            // Sauvegarder les variables mises à jour
            session.setVariables(objectMapper.writeValueAsString(variables));

            log.debug("💾 Métadonnées média sauvegardées: {}={}, type={}", variableName, mediaId, messageType);
        } catch (Exception e) {
            log.error("❌ Erreur sauvegarde métadonnées média: {}", e.getMessage());
        }
    }

    /**
     * ================================
     * CORRECTION: MISE À JOUR DE LA SIGNATURE executeNodeByType
     * ================================
     */
    private WhatsAppResponse executeNodeByType(
        FlowNodePayload node,
        FlowPayload flowPayload,
        ChatbotSession session,
        String userInput,
        String messageType,
        String mediaId
    ) {
        log.debug("🔄 Exécution nœud: {} (type: {})", node.getId(), node.getType());

        switch (node.getType()) {
            case "start":
                return executeStartNode(node, flowPayload, session);
            case "message":
                return executeMessageNode(node, flowPayload, session);
            case "buttons":
                return executeButtonsNode(node, flowPayload, session, userInput);
            case "list":
                return executeListNode(node, flowPayload, session, userInput);
            case "input":
                // CORRECTION: Passer tous les paramètres nécessaires
                return executeInputNode(node, flowPayload, session, userInput, messageType, mediaId);
            case "wait_response":
                return executeWaitResponseNode(node, flowPayload, session, userInput);
            case "condition":
                return executeConditionNode(node, flowPayload, session);
            case "variable_set":
                return executeVariableSetNode(node, flowPayload, session);
            case "image":
                return executeImageNode(node, flowPayload, session);
            case "file":
                return executeFileNode(node, flowPayload, session, messageType, mediaId);
            case "webhook":
                return executeWebhookNode(node, flowPayload, session);
            case "end":
                return executeEndNode(node, session);
            default:
                log.warn("⚠️ Type de nœud non supporté: {}", node.getType());
                return createTextResponse("Type de nœud non supporté: " + node.getType());
        }
    }

    /**
     * AMÉLIORATION 5: MÉTHODES DE VALIDATION SPÉCIALISÉES
     */
    private ValidationResult validateTextInput(String input, NodeDataPayload data) {
        // Validation longueur minimum
        Integer minLength = data.getMinLength();
        if (minLength != null && input.length() < minLength) {
            return ValidationResult.invalid("Le texte doit contenir au moins " + minLength + " caractères.");
        }

        // Validation longueur maximum
        Integer maxLength = data.getMaxLength();
        if (maxLength != null && input.length() > maxLength) {
            return ValidationResult.invalid("Le texte ne peut pas dépasser " + maxLength + " caractères.");
        }

        return ValidationResult.valid();
    }

    private ValidationResult validateNumberInput(String input, NodeDataPayload data) {
        try {
            double number = Double.parseDouble(input);

            // Validation valeur minimum
            Integer minValue = data.getMinValue();
            if (minValue != null && number < minValue) {
                return ValidationResult.invalid("Le nombre doit être supérieur ou égal à " + minValue + ".");
            }

            // Validation valeur maximum
            Integer maxValue = data.getMaxValue();
            if (maxValue != null && number > maxValue) {
                return ValidationResult.invalid("Le nombre doit être inférieur ou égal à " + maxValue + ".");
            }

            return ValidationResult.valid();
        } catch (NumberFormatException e) {
            return ValidationResult.invalid("Veuillez saisir un nombre valide (ex: 25 ou 12.5).");
        }
    }

    private ValidationResult validateEmailInput(String input) {
        if (!EMAIL_PATTERN.matcher(input).matches()) {
            return ValidationResult.invalid("Veuillez saisir une adresse email valide (ex: nom@domaine.com).");
        }
        return ValidationResult.valid();
    }

    private ValidationResult validatePhoneInput(String input) {
        if (!PHONE_PATTERN.matcher(input).matches()) {
            return ValidationResult.invalid("Veuillez saisir un numéro de téléphone valide (8-15 chiffres, ex: +33123456789).");
        }
        return ValidationResult.valid();
    }

    private ValidationResult validateUrlInput(String input) {
        try {
            String url = input.toLowerCase();
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                return ValidationResult.invalid("L'URL doit commencer par http:// ou https://");
            }
            // Validation basique d'URL
            java.net.URL validUrl = new java.net.URL(input);
            return ValidationResult.valid();
        } catch (Exception e) {
            return ValidationResult.invalid("Veuillez saisir une URL valide (ex: https://www.example.com).");
        }
    }

    private ValidationResult validateDateInput(String input) {
        // Validation format date DD/MM/YYYY ou DD-MM-YYYY
        Pattern datePattern = Pattern.compile("^(0[1-9]|[12][0-9]|3[01])[\\/\\-](0[1-9]|1[012])[\\/\\-](19|20)\\d\\d$");
        if (!datePattern.matcher(input).matches()) {
            return ValidationResult.invalid("Veuillez saisir une date au format JJ/MM/AAAA (ex: 15/03/2024).");
        }

        // Validation logique de la date
        try {
            String[] parts = input.split("[\\/\\-]");
            int day = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            int year = Integer.parseInt(parts[2]);

            if (month < 1 || month > 12) {
                return ValidationResult.invalid("Le mois doit être entre 1 et 12.");
            }

            if (day < 1 || day > 31) {
                return ValidationResult.invalid("Le jour doit être entre 1 et 31.");
            }

            // Validation jours par mois (simplifiée)
            if ((month == 4 || month == 6 || month == 9 || month == 11) && day > 30) {
                return ValidationResult.invalid("Ce mois n'a que 30 jours.");
            }

            if (month == 2 && day > 29) {
                return ValidationResult.invalid("Février n'a que 28 ou 29 jours.");
            }

            return ValidationResult.valid();
        } catch (Exception e) {
            return ValidationResult.invalid("Date invalide. Format attendu: JJ/MM/AAAA");
        }
    }

    private ValidationResult validateTimeInput(String input) {
        // Validation format heure HH:MM
        Pattern timePattern = Pattern.compile("^([01]?[0-9]|2[0-3]):[0-5][0-9]$");
        if (!timePattern.matcher(input).matches()) {
            return ValidationResult.invalid("Veuillez saisir une heure au format HH:MM (ex: 14:30).");
        }
        return ValidationResult.valid();
    }

    /**
     * AMÉLIORATION 8: DESCRIPTIONS DES TYPES DE FICHIERS
     */
    private String getFileTypeDescription(String fileType) {
        switch (fileType) {
            case "text":
                return "un message texte";
            case "image":
                return "une image";
            case "document":
                return "un document";
            case "audio":
                return "un fichier audio";
            case "voice":
                return "un message vocal";
            case "video":
                return "une vidéo";
            case "location":
                return "une localisation";
            case "contact":
                return "un contact";
            default:
                return "ce type de fichier";
        }
    }

    /**
     * AMÉLIORATION 9: GESTION SPÉCIALE POUR LES MÉDIAS DANS saveUserInput
     */
    private void saveUserInput(ChatbotSession session, String input, String messageType, String mediaId) {
        try {
            log.info("💾 SAUVEGARDE input utilisateur: type={}, content='{}', mediaId={}", messageType, input, mediaId);

            // Récupérer les variables existantes
            Map<String, Object> variables = getSessionVariables(session);

            String messageContent = (input != null && !input.trim().isEmpty()) ? input.trim() : "";
            String currentMessageType = (messageType != null) ? messageType : "text";

            // Sauvegarder les informations du message actuel
            variables.put("user.lastMessage", messageContent);
            variables.put("user.lastTimestamp", Instant.now().toString());
            variables.put("user.lastMessageType", currentMessageType);

            // Compter les messages pour l'historique
            int messageCount = Integer.parseInt(variables.getOrDefault("user.messageCount", "0").toString());
            messageCount++;
            variables.put("user.messageCount", String.valueOf(messageCount));

            // Sauvegarder CHAQUE message avec un index unique
            String messageKey = "user.message_" + messageCount;
            variables.put(messageKey, messageContent);
            variables.put(messageKey + ".type", currentMessageType);
            variables.put(messageKey + ".timestamp", Instant.now().toString());

            // VALIDATION ET DESCRIPTION SELON LE TYPE
            if (mediaId != null && !mediaId.isEmpty()) {
                variables.put("user.lastMediaId", mediaId);
                variables.put(messageKey + ".mediaId", mediaId);
                variables.put(messageKey + ".hasMedia", "true");

                // Récupérer l'URL publique du média si possible
                String mediaUrl = getMediaUrlFromWhatsApp(mediaId);
                if (mediaUrl != null) {
                    String permanentUrl = downloadAndStoreMedia(mediaUrl, mediaId, getMimeTypeFromMessageType(currentMessageType));
                    if (permanentUrl != null) {
                        variables.put(messageKey + ".mediaUrl", permanentUrl);
                        variables.put(messageKey + ".originalUrl", mediaUrl);
                    } else {
                        variables.put(messageKey + ".mediaUrl", mediaUrl);
                    }
                }

                // Sauvegarder selon le type de média avec validation
                switch (currentMessageType) {
                    case "image":
                        variables.put("user.lastImage", mediaId);
                        variables.put("user.hasImage", "true");
                        variables.put(messageKey + ".description", "Image validée et reçue de l'utilisateur");
                        variables.put(messageKey + ".mediaType", "image");
                        log.info("📸 Image validée et sauvegardée: mediaId={}", mediaId);
                        break;
                    case "document":
                        variables.put("user.lastDocument", mediaId);
                        variables.put("user.hasDocument", "true");
                        variables.put(messageKey + ".description", "Document validé et reçu de l'utilisateur");
                        variables.put(messageKey + ".mediaType", "document");
                        log.info("📄 Document validé et sauvegardé: mediaId={}", mediaId);
                        break;
                    case "audio":
                    case "voice":
                        variables.put("user.lastAudio", mediaId);
                        variables.put("user.hasAudio", "true");
                        variables.put(messageKey + ".description", "Audio validé et reçu de l'utilisateur");
                        variables.put(messageKey + ".mediaType", "audio");
                        log.info("🎵 Audio validé et sauvegardé: mediaId={}", mediaId);
                        break;
                    case "video":
                        variables.put("user.lastVideo", mediaId);
                        variables.put("user.hasVideo", "true");
                        variables.put(messageKey + ".description", "Vidéo validée et reçue de l'utilisateur");
                        variables.put(messageKey + ".mediaType", "video");
                        log.info("🎬 Vidéo validée et sauvegardée: mediaId={}", mediaId);
                        break;
                }
            } else if (!messageContent.isEmpty()) {
                // Message texte validé
                variables.put(messageKey + ".description", "Message texte validé: " + messageContent);
                variables.put(messageKey + ".hasMedia", "false");
            }

            // Sauvegarder immédiatement les variables
            session.setVariables(objectMapper.writeValueAsString(variables));
            chatbotSessionRepository.save(session);

            log.info(
                "✅ Input validé et sauvegardé: messageCount={}, type={}, hasMedia={}",
                messageCount,
                currentMessageType,
                mediaId != null
            );
        } catch (Exception e) {
            log.error("❌ Erreur sauvegarde input: {}", e.getMessage(), e);
        }
    }

    /**
     * AMÉLIORATION 10: MISE À JOUR DE isFirstVisit POUR ÉVITER LES BOUCLES
     */
    private boolean isFirstVisit(ChatbotSession session, String nodeId) {
        try {
            String lastProcessedNode = getSessionVariable(session, "system.lastProcessedNode", "");
            boolean isFirst = !nodeId.equals(lastProcessedNode);

            log.debug("🔍 isFirstVisit pour nœud {}: {} (dernier traité: {})", nodeId, isFirst, lastProcessedNode);

            return isFirst;
        } catch (Exception e) {
            log.warn("⚠️ Erreur vérification première visite: {}", e.getMessage());
            return true; // En cas d'erreur, considérer comme première visite
        }
    }

    /**
     * AMÉLIORATION 11: PATTERNS DE VALIDATION AVANCÉS
     */
    private static final Pattern URL_PATTERN = Pattern.compile("^(https?)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]");
    private static final Pattern DATE_PATTERN = Pattern.compile("^(0[1-9]|[12][0-9]|3[01])[\\/\\-](0[1-9]|1[012])[\\/\\-](19|20)\\d\\d$");
    private static final Pattern TIME_PATTERN = Pattern.compile("^([01]?[0-9]|2[0-3]):[0-5][0-9]$");

    /**
     * ================================
     * EXÉCUTION NŒUD WAIT_RESPONSE
     * ================================
     */
    private WhatsAppResponse executeWaitResponseNode(
        FlowNodePayload node,
        FlowPayload flowPayload,
        ChatbotSession session,
        String userInput
    ) {
        NodeDataPayload data = node.getData();

        // Si première visite, envoyer le message d'attente
        if (userInput == null || isFirstVisit(session, node.getId())) {
            String text = data.getText() != null ? data.getText() : "J'attends votre réponse...";
            text = replaceVariablesInText(text, session);

            log.debug("⏳ Attente de réponse: {}", text);
            return createTextResponse(text);
        }

        // Sauvegarder la réponse libre
        if (data.getStoreInVariable() != null && userInput != null) {
            setSessionVariable(session, data.getStoreInVariable(), userInput);
            log.debug("💾 Réponse libre sauvegardée: {} = {}", data.getStoreInVariable(), userInput);
        }

        // CORRECTION: PASSER DIRECTEMENT AU NŒUD SUIVANT
        String nextNodeId = node.getNextNodeId();
        if (nextNodeId != null) {
            session.setCurrentNodeId(nextNodeId);
            FlowNodePayload nextNode = findNodeById(flowPayload, nextNodeId);
            if (nextNode != null) {
                // CORRECTION: PAS DE MESSAGE DE CONFIRMATION
                return executeNodeByType(nextNode, flowPayload, session, null, null, null);
            }
        }

        // Message d'erreur seulement si pas de nœud suivant
        log.error("❌ Aucun nœud suivant configuré après wait_response");
        return createTextResponse("❌ Erreur de configuration - suite de conversation introuvable.");
    }

    /**
     * Exécution des nœuds condition sans réponse visible
     */
    private WhatsAppResponse executeConditionNode(FlowNodePayload node, FlowPayload flowPayload, ChatbotSession session) {
        log.debug("🔀 Évaluation condition - nœud: {}", node.getId());

        // La logique d'évaluation est déjà dans determineNextNodeId
        String nextNodeId = determineNextNodeId(node, session);
        if (nextNodeId != null) {
            session.setCurrentNodeId(nextNodeId);
            log.debug("✅ Condition évaluée, passage au nœud: {}", nextNodeId);
        }

        // Ne pas retourner de réponse visible pour les conditions
        return null;
    }

    /**
     * Exécution des nœuds variable_set sans réponse
     */
    private WhatsAppResponse executeVariableSetNode(FlowNodePayload node, FlowPayload flowPayload, ChatbotSession session) {
        NodeDataPayload data = node.getData();

        if (data.getVariableName() != null) {
            String variableValue = calculateVariableValue(data, session);
            setSessionVariable(session, data.getVariableName(), variableValue);
            log.debug("🔧 Variable modifiée: {} = {} (opération: {})", data.getVariableName(), variableValue, data.getVariableOperation());
        }

        // Passer au nœud suivant automatiquement
        String nextNodeId = node.getNextNodeId();
        if (nextNodeId != null) {
            session.setCurrentNodeId(nextNodeId);
        }

        // Ne pas retourner de réponse visible pour variable_set
        return null;
    }

    /**
     * ================================
     * EXÉCUTION NŒUD IMAGE
     * ================================
     */
    private WhatsAppResponse executeImageNode(FlowNodePayload node, FlowPayload flowPayload, ChatbotSession session) {
        NodeDataPayload data = node.getData();

        if (data.getImageUrl() == null || data.getImageUrl().isEmpty()) {
            log.warn("⚠️ Nœud image {} sans URL configurée", node.getId());
            return createTextResponse("❌ Image non configurée dans ce nœud.");
        }

        // Passer au nœud suivant
        String nextNodeId = node.getNextNodeId();
        if (nextNodeId != null) {
            session.setCurrentNodeId(nextNodeId);
        }

        // Créer la réponse image
        WhatsAppResponse response = new WhatsAppResponse();
        response.setType("image");
        response.setImageUrl(data.getImageUrl());

        String caption = data.getText();
        if (caption != null) {
            caption = replaceVariablesInText(caption, session);
            response.setCaption(caption);
        }

        log.debug("🖼️ Envoi image: {} avec caption: {}", data.getImageUrl(), caption);
        return response;
    }

    /**
     * ================================
     * CORRECTION 11: GESTION FIN DE FLOW
     * ================================
     */
    private WhatsAppResponse executeEndNode(FlowNodePayload node, ChatbotSession session) {
        NodeDataPayload data = node.getData();

        String text = data.getText();
        if (text == null || text.isEmpty()) {
            text = "Conversation terminée. Merci !";
        }
        text = replaceVariablesInText(text, session);

        // CORRECTION: Au lieu de désactiver, remettre au début
        log.info("🏁 Fin du flow atteinte - Reset au début pour: {}", session.getPhoneNumber());

        // Nettoyer et remettre au début
        clean3CXVariablesAndResetFlow(session);

        return createTextResponse(text + "\n\n💬 Tapez n'importe quoi pour recommencer.");
    }

    /**
     * ================================
     * GESTION ÉTAT 3CX
     * ================================
     */
    private void set3CXStatus(ChatbotSession session, boolean active) {
        try {
            setSessionVariable(session, "3cx.active", String.valueOf(active));
            log.debug("🔧 Statut 3CX mis à jour: {}", active);
        } catch (Exception e) {
            log.warn("⚠️ Erreur mise à jour statut 3CX: {}", e.getMessage());
        }
    }

    private boolean get3CXStatus(ChatbotSession session) {
        try {
            String status = getSessionVariable(session, "3cx.active", "false");
            return "true".equals(status);
        } catch (Exception e) {
            log.warn("⚠️ Erreur lecture statut 3CX: {}", e.getMessage());
            return false;
        }
    }

    /**
     * ================================
     * ENVOI MESSAGE DIRECT VERS 3CX
     * ================================
     */
    private boolean sendDirectMessageTo3CX(ChatbotSession session, String messageContent, String messageType, String mediaId) {
        try {
            String webhookUrl = "https://richattpay.3cx.sc/sms/whatsapp/68eccdf6b71f45b7949f68419ede194a";

            // Préparer les données du message
            Map<String, Object> messageData = new HashMap<>();
            messageData.put("phoneNumber", session.getPhoneNumber());
            messageData.put("messageContent", messageContent != null ? messageContent : "");
            messageData.put("messageType", messageType != null ? messageType : "text");
            messageData.put("mediaId", mediaId);
            messageData.put("conversationId", getSessionVariable(session, "3cx.conversationId", ""));
            messageData.put("isDirectTransfer", true);

            // Construire le payload 3CX
            Map<String, Object> payload = buildDirect3CXPayload(messageData);

            // Envoyer
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("User-Agent", "WhatsApp-Direct/1.0");

            String jsonPayload = objectMapper.writeValueAsString(payload);
            HttpEntity<String> entity = new HttpEntity<>(jsonPayload, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(webhookUrl, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("✅ Message direct 3CX envoyé");
                setSessionVariable(session, "3cx.lastDirectTransfer", Instant.now().toString());
                return true;
            } else {
                log.error("❌ Erreur envoi direct 3CX: {}", response.getStatusCode());
                return false;
            }
        } catch (Exception e) {
            log.error("💥 Exception envoi direct 3CX: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * ================================
     * CONSTRUCTION PAYLOAD 3CX DIRECT
     * ================================
     */
    private Map<String, Object> buildDirect3CXPayload(Map<String, Object> data) {
        String phoneNumberId = "633542099835858";
        String businessId = "1209741957334833";
        String displayPhoneNumber = "32942393";

        String userId = (String) data.get("phoneNumber");
        String messageContent = (String) data.get("messageContent");
        String messageType = (String) data.get("messageType");
        String mediaId = (String) data.get("mediaId");
        String conversationId = (String) data.get("conversationId");

        // ID message unique
        String idMessage = "direct-" + System.currentTimeMillis();

        // Message principal
        Map<String, Object> message = new HashMap<>();
        message.put("from", userId);
        message.put("id", idMessage);
        message.put("timestamp", String.valueOf(System.currentTimeMillis() / 1000));
        message.put("type", messageType);

        // Contenu selon le type
        if ("text".equals(messageType)) {
            Map<String, Object> textContent = new HashMap<>();
            textContent.put("body", messageContent);
            message.put("text", textContent);
        } else if (mediaId != null && !mediaId.isEmpty()) {
            Map<String, Object> mediaContent = new HashMap<>();
            mediaContent.put("id", mediaId);
            if (messageContent != null && !messageContent.isEmpty()) {
                mediaContent.put("caption", messageContent);
            }
            message.put(messageType, mediaContent);
        }

        // Payload complet
        Map<String, Object> payload = new HashMap<>();
        payload.put("object", "whatsapp_business_account");

        List<Map<String, Object>> entries = new ArrayList<>();
        Map<String, Object> entry = new HashMap<>();
        entry.put("id", businessId);

        List<Map<String, Object>> changes = new ArrayList<>();
        Map<String, Object> change = new HashMap<>();
        change.put("field", "messages");

        Map<String, Object> value = new HashMap<>();
        value.put("messaging_product", "whatsapp");

        // Métadonnées avec marqueurs
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("display_phone_number", displayPhoneNumber);
        metadata.put("phone_number_id", phoneNumberId);

        Map<String, Object> customMetadata = new HashMap<>();
        customMetadata.put("agent_transfer", true);
        customMetadata.put("direct_message", true);
        customMetadata.put("conversation_id", conversationId);
        customMetadata.put("source", "chatbot_direct");
        metadata.put("custom_metadata", customMetadata);

        value.put("metadata", metadata);

        // Contacts
        List<Map<String, Object>> contacts = new ArrayList<>();
        Map<String, Object> contact = new HashMap<>();
        Map<String, Object> profile = new HashMap<>();
        profile.put("name", userId); // Utiliser le nom si disponible
        contact.put("profile", profile);
        contact.put("wa_id", userId);
        contacts.add(contact);

        value.put("contacts", contacts);
        value.put("messages", Arrays.asList(message));

        change.put("value", value);
        changes.add(change);
        entry.put("changes", changes);
        entries.add(entry);
        payload.put("entry", entries);

        return payload;
    }

    /**
     * ================================
     * CONSTRUCTION HISTORIQUE COMPLET
     * ================================
     */
    private List<Map<String, Object>> buildCompleteHistory(ChatbotSession session, Map<String, Object> variables) {
        List<Map<String, Object>> history = new ArrayList<>();

        try {
            // 1. Tous les messages utilisateur
            for (Map.Entry<String, Object> entry : variables.entrySet()) {
                String key = entry.getKey();
                if (key.startsWith("user.") && !key.contains(".last")) {
                    Map<String, Object> historyEntry = new HashMap<>();
                    historyEntry.put("type", "user_input");
                    historyEntry.put("field", key);
                    historyEntry.put("content", entry.getValue().toString());
                    historyEntry.put("timestamp", variables.get(key + ".timestamp"));
                    history.add(historyEntry);
                }
            }

            // 2. Toutes les données collectées
            for (Map.Entry<String, Object> entry : variables.entrySet()) {
                String key = entry.getKey();
                if (!key.startsWith("system.") && !key.startsWith("user.") && !key.startsWith("3cx.")) {
                    Map<String, Object> historyEntry = new HashMap<>();
                    historyEntry.put("type", "collected_data");
                    historyEntry.put("field", key);
                    historyEntry.put("value", entry.getValue().toString());
                    historyEntry.put("timestamp", Instant.now().toString());
                    history.add(historyEntry);
                }
            }

            // 3. Trier par timestamp
            history.sort((a, b) -> {
                String timeA = (String) a.get("timestamp");
                String timeB = (String) b.get("timestamp");
                if (timeA != null && timeB != null) {
                    try {
                        return Instant.parse(timeA).compareTo(Instant.parse(timeB));
                    } catch (Exception e) {
                        return 0;
                    }
                }
                return 0;
            });
        } catch (Exception e) {
            log.warn("⚠️ Erreur construction historique: {}", e.getMessage());
        }

        return history;
    }

    /**
     * ================================
     * RÉSUMÉ INTELLIGENT
     * ================================
     */
    private String buildIntelligentSummary(Map<String, Object> variables, List<Map<String, Object>> history) {
        StringBuilder summary = new StringBuilder();

        summary.append("🤖 RÉSUMÉ CONVERSATION CHATBOT\n\n");
        summary.append("💬 Total interactions: ").append(history.size()).append("\n\n");

        // Données importantes collectées
        summary.append("📋 DONNÉES COLLECTÉES:\n");
        boolean hasData = false;
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue().toString();

            if (
                !key.startsWith("system.") &&
                !key.startsWith("user.last") &&
                !key.startsWith("3cx.") &&
                !value.isEmpty() &&
                value.length() < 100
            ) {
                summary.append("• ").append(key).append(": ").append(value).append("\n");
                hasData = true;
            }
        }

        if (!hasData) {
            summary.append("Aucune donnée spécifique collectée.\n");
        }

        // Dernier message
        String lastMessage = (String) variables.get("user.lastMessage");
        if (lastMessage != null && !lastMessage.isEmpty()) {
            summary.append("\n💭 Dernier message: ").append(lastMessage).append("\n");
        }

        summary.append("\n🎯 Client prêt pour prise en charge par agent humain.");

        return summary.toString();
    }

    /**
     * ================================
     * CORRECTION 10: VÉRIFICATION STATUT API 3CX ROBUSTE
     * ================================
     */
    private boolean checkConversationStatus(String conversationId) {
        try {
            if (conversationId == null || conversationId.trim().isEmpty()) {
                return false;
            }

            String apiUrl = "https://rjum8bum7g.execute-api.us-east-1.amazonaws.com/dev/count?idconversation=" + conversationId.trim();

            log.debug("🔍 Vérification conversation: {}", apiUrl);

            // Timeout plus court pour éviter les blocages
            ResponseEntity<String> response = restTemplate.getForEntity(apiUrl, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                String responseBody = response.getBody();
                if (responseBody != null) {
                    responseBody = responseBody.trim();
                }

                log.debug("📥 Réponse API count: '{}'", responseBody);

                // API retourne "true" si conversation TERMINÉE, "false" si ACTIVE
                boolean isConversationEnded = "true".equalsIgnoreCase(responseBody);

                if (isConversationEnded) {
                    log.info("✅ Conversation {} TERMINÉE par l'agent", conversationId);
                    return false; // Conversation terminée = retour au chatbot
                } else {
                    log.debug("📞 Conversation {} encore ACTIVE", conversationId);
                    return true; // Conversation active = rester en mode 3CX
                }
            } else {
                log.warn("⚠️ Erreur API: {} - Considération comme terminée", response.getStatusCode());
                return false;
            }
        } catch (Exception e) {
            log.error("❌ Exception vérification conversation {}: {}", conversationId, e.getMessage());
            return false; // En cas d'erreur, retour au chatbot
        }
    }

    private String retrieveConversationId(String messageId) {
        int maxRetries = 3;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                String conversationApiUrl = "https://rjum8bum7g.execute-api.us-east-1.amazonaws.com/dev/conversation";
                String url = conversationApiUrl + "?msg_gid=" + messageId;

                log.debug("🔍 Tentative {} - Récupération conversation ID pour message: {}", attempt, messageId);

                ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

                if (response.getStatusCode().is2xxSuccessful()) {
                    String conversationId = response.getBody();

                    // Nettoyer la réponse (enlever guillemets, espaces, etc.)
                    if (conversationId != null) {
                        conversationId = conversationId.trim().replaceAll("^\"|\"$", "");
                    }

                    if (conversationId != null && !conversationId.isEmpty() && !"null".equals(conversationId)) {
                        log.info("✅ Conversation ID récupéré (tentative {}): {}", attempt, conversationId);
                        return conversationId;
                    } else {
                        log.warn("⚠️ Réponse API vide ou null (tentative {}): '{}'", attempt, response.getBody());
                    }
                } else {
                    log.warn("⚠️ Erreur HTTP (tentative {}): {} - {}", attempt, response.getStatusCode(), response.getBody());
                }

                // Attendre avant le retry (sauf pour la dernière tentative)
                if (attempt < maxRetries) {
                    Thread.sleep(2000); // 2 secondes entre les tentatives
                }
            } catch (Exception e) {
                log.error("❌ Exception (tentative {}) récupération conversation ID pour {}: {}", attempt, messageId, e.getMessage());

                // Attendre avant le retry (sauf pour la dernière tentative)
                if (attempt < maxRetries) {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        log.error("❌ Échec récupération conversation ID après {} tentatives pour message: {}", maxRetries, messageId);
        return null;
    }

    /**
     * NOUVELLE MÉTHODE - Utilise maintenant le format exact de Botpress
     */
    private boolean send3CXWebhookWithHistory(String webhookUrl, Map<String, Object> data) {
        try {
            log.info("🔗 Appel webhook vers: {} ", webhookUrl);

            // Utiliser le nouveau format 3CX adapté de Botpress
            return call3CXWebhook(webhookUrl, data);
        } catch (Exception e) {
            log.error("💥 Erreur lors de l'appel webhook: {}", e.getMessage(), e);
            return false;
        }
    }

    @Transactional
    public void saveConversationIdToSession(Long sessionId, String conversationId) {
        try {
            log.debug("💾 Sauvegarde conversation ID {} pour session {}", conversationId, sessionId);

            // Recharger la session depuis la base de données
            Optional<ChatbotSession> sessionOpt = chatbotSessionRepository.findById(sessionId);

            if (sessionOpt.isPresent()) {
                ChatbotSession session = sessionOpt.get();

                // Récupérer les variables actuelles
                Map<String, Object> variables = getSessionVariables(session);

                // Mettre à jour les variables 3CX
                variables.put("3cx.conversationId", conversationId);
                variables.put("3cx.transferTime", Instant.now().toString());
                variables.put("3cx.active", "true"); // S'assurer que 3CX est marqué comme actif

                // Sauvegarder les variables mises à jour
                session.setVariables(objectMapper.writeValueAsString(variables));
                session.setLastInteraction(Instant.now());

                // Sauvegarder en base
                chatbotSessionRepository.save(session);

                log.info("✅ Conversation ID {} sauvegardé avec succès pour session {}", conversationId, sessionId);

                // AJOUT: Vérification immédiate
                verifyConversationIdSaved(sessionId, conversationId);
            } else {
                log.error("❌ Session {} non trouvée lors de la sauvegarde du conversation ID", sessionId);
            }
        } catch (Exception e) {
            log.error(
                "❌ Erreur critique lors de la sauvegarde du conversation ID {} pour session {}: {}",
                conversationId,
                sessionId,
                e.getMessage(),
                e
            );
        }
    }

    private void verifyConversationIdSaved(Long sessionId, String expectedConversationId) {
        try {
            // Recharger la session et vérifier
            Optional<ChatbotSession> sessionOpt = chatbotSessionRepository.findById(sessionId);

            if (sessionOpt.isPresent()) {
                ChatbotSession session = sessionOpt.get();
                String savedConversationId = getSessionVariable(session, "3cx.conversationId", "");

                if (expectedConversationId.equals(savedConversationId)) {
                    log.info("✅ Vérification OK: Conversation ID {} bien sauvegardé", expectedConversationId);
                } else {
                    log.error("❌ Vérification ÉCHEC: Expected {} mais trouvé {}", expectedConversationId, savedConversationId);
                }
            }
        } catch (Exception e) {
            log.warn("⚠️ Erreur lors de la vérification: {}", e.getMessage());
        }
    }

    /**
     * Extrait le nom d'utilisateur depuis les données
     */
    private String extractUserName(Map<String, Object> data) {
        try {
            // Essayer de récupérer le nom depuis les variables utilisateur
            Map<String, Object> userData = (Map<String, Object>) data.get("userData");
            if (userData != null) {
                String name = (String) userData.get("nom");
                if (name == null) name = (String) userData.get("name");
                if (name == null) name = (String) userData.get("prenom");
                if (name != null) return name;
            }

            // Fallback vers le numéro de téléphone
            String phoneNumber = (String) data.get("phoneNumber");
            return phoneNumber != null ? phoneNumber : "Client WhatsApp";
        } catch (Exception e) {
            return "Client WhatsApp";
        }
    }

    /**
     * Ajoute les messages média depuis les variables de session
     */
    private void addVariableMediaMessages(List<Map<String, Object>> messageArray, Map<String, Object> variables, String userId) {
        // Vérifier les différents types de média dans les variables
        String[] mediaTypes = { "lastImage", "lastDocument", "lastAudio", "lastVideo" };

        for (String mediaVar : mediaTypes) {
            String mediaId = (String) variables.get("user." + mediaVar);
            if (mediaId != null && !mediaId.isEmpty()) {
                String mediaType = mediaVar.replace("last", "").toLowerCase();
                Map<String, Object> mediaMessage = createMediaMessage(userId, mediaType, mediaId);
                if (mediaMessage != null) {
                    messageArray.add(mediaMessage);
                }
            }
        }
    }

    /**
     * Crée un message média pour 3CX
     */
    private Map<String, Object> createMediaMessage(String userId, String mediaType, String mediaId) {
        try {
            String messageId = mediaType + "-" + System.currentTimeMillis();

            Map<String, Object> mediaMessage = new HashMap<>();
            mediaMessage.put("from", userId);
            mediaMessage.put("id", messageId);
            mediaMessage.put("timestamp", String.valueOf(System.currentTimeMillis() / 1000));
            mediaMessage.put("type", mediaType);

            Map<String, Object> mediaContent = new HashMap<>();
            mediaContent.put("id", mediaId);

            // Ajouter caption selon le type
            switch (mediaType) {
                case "image":
                    mediaContent.put("caption", "Image fournie par le client");
                    break;
                case "document":
                    mediaContent.put("caption", "Document fourni par le client");
                    break;
                case "audio":
                    mediaContent.put("caption", "Audio fourni par le client");
                    break;
                case "video":
                    mediaContent.put("caption", "Vidéo fournie par le client");
                    break;
            }

            mediaMessage.put(mediaType, mediaContent);
            return mediaMessage;
        } catch (Exception e) {
            log.warn("⚠️ Erreur création message média: {}", e.getMessage());
            return null;
        }
    }

    private Map<String, Object> extractUserData(Map<String, Object> variables) {
        Map<String, Object> userData = new HashMap<>();

        // Extraire les données utilisateur principales
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            String key = entry.getKey();

            // Exclure les variables système
            if (!key.startsWith("system.") && !key.startsWith("user.last")) {
                userData.put(key, entry.getValue());
            }
        }

        return userData;
    }

    /**
     * ================================
     * GESTION DES COMMANDES SPÉCIALES
     * ================================
     */
    private WhatsAppResponse handleSpecialCommands(String message, ChatbotSession session, FlowPayload flowPayload) {
        if (message == null) return null;

        String lowerMessage = message.toLowerCase().trim();

        switch (lowerMessage) {
            case "restart":
            case "recommencer":
            case "reset":
            case "اعادة":
                return handleRestartCommand(session, flowPayload);
            case "help":
            case "aide":
            case "menu":
            case "مساعدة":
                return handleHelpCommand();
            case "status":
            case "statut":
                return handleStatusCommand(session);
            case "end":
            case "fin":
            case "stop":
                return handleEndCommand(session);
            case "debug":
                return handleDebugCommand(session, flowPayload);
            default:
                return null; // Pas de commande spéciale
        }
    }

    /**
     * ================================
     * CORRECTION 8: COMMANDE RESTART AMÉLIORÉE
     * ================================
     */
    private WhatsAppResponse handleRestartCommand(ChatbotSession session, FlowPayload flowPayload) {
        try {
            log.info("🔄 RESTART demandé pour session: {}", session.getId());

            // 1. Nettoyer COMPLÈTEMENT (3CX + variables)
            clean3CXVariablesAndResetFlow(session);

            // 2. Réinitialiser au début du flow
            String startNodeId = findStartNodeIdForUser(session.getUserLogin());
            session.setCurrentNodeId(startNodeId);
            session.setIsActive(true);

            // 3. Réinitialiser variables de base
            initializeSessionVariables(session);

            // 4. Sauvegarder
            chatbotSessionRepository.save(session);

            log.info("✅ Session COMPLÈTEMENT redémarrée: {}", session.getPhoneNumber());
            return createTextResponse("🔄 **Conversation redémarrée**\n\nTout est remis à zéro. Comment puis-je vous aider ?");
        } catch (Exception e) {
            log.error("❌ Erreur restart: {}", e.getMessage());
            return createTextResponse("❌ Impossible de redémarrer. Contactez le support.");
        }
    }

    private WhatsAppResponse handleHelpCommand() {
        return createTextResponse(
            "🤖 **Commandes disponibles:**\n\n" +
            "• `restart` - Redémarrer la conversation\n" +
            "• `help` - Afficher cette aide\n" +
            "• `status` - Voir où vous en êtes\n" +
            "• `end` - Terminer la conversation\n" +
            "• `debug` - Informations techniques\n\n" +
            "💬 Tapez votre réponse normale pour continuer."
        );
    }

    private WhatsAppResponse handleStatusCommand(ChatbotSession session) {
        try {
            Map<String, Object> variables = getSessionVariables(session);

            return createTextResponse(
                String.format(
                    "📍 **Statut de votre session:**\n\n" +
                    "• Nœud actuel: %s\n" +
                    "• Variables sauvées: %d\n" +
                    "• Dernière interaction: %s\n" +
                    "• Session active: %s",
                    session.getCurrentNodeId(),
                    variables.size(),
                    session.getLastInteraction(),
                    session.getIsActive() ? "Oui" : "Non"
                )
            );
        } catch (Exception e) {
            return createTextResponse("📍 **Statut:** Conversation en cours...");
        }
    }

    private WhatsAppResponse handleEndCommand(ChatbotSession session) {
        session.setIsActive(false);
        log.info("🔚 Session terminée par l'utilisateur: {}", session.getPhoneNumber());
        return createTextResponse("👋 **Conversation terminée**\n\nMerci pour votre visite. Tapez n'importe quoi pour recommencer.");
    }

    private WhatsAppResponse handleDebugCommand(ChatbotSession session, FlowPayload flowPayload) {
        try {
            Map<String, Object> variables = getSessionVariables(session);

            StringBuilder debug = new StringBuilder("🔧 **Informations de debug:**\n\n");
            debug.append("• Session ID: ").append(session.getId()).append("\n");
            debug.append("• Flow: ").append(flowPayload.getName()).append("\n");
            debug.append("• Nœuds total: ").append(flowPayload.getNodes().size()).append("\n");
            debug.append("• Variables (").append(variables.size()).append("):\n");

            for (Map.Entry<String, Object> entry : variables.entrySet()) {
                debug.append("  - ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }

            return createTextResponse(debug.toString());
        } catch (Exception e) {
            return createTextResponse("❌ Erreur lors de la récupération des informations de debug.");
        }
    }

    /**
     * ================================
     * CORRECTION 12: SAVE SYSTÉMATIQUE DE LA SESSION
     * ================================
     */
    private void updateSessionAndSave(ChatbotSession session) {
        try {
            session.setLastInteraction(Instant.now());
            // CORRECTION: S'assurer que la session reste active sauf cas spécifiques
            if (session.getIsActive() == null) {
                session.setIsActive(true);
            }
            chatbotSessionRepository.save(session);
            log.debug("💾 Session sauvegardée: {} (active: {})", session.getId(), session.getIsActive());
        } catch (Exception e) {
            log.error("❌ Erreur sauvegarde session: {}", e.getMessage());
        }
    }

    /**
     * ================================
     * MÉTHODES UTILITAIRES - VARIABLES
     * ================================
     */
    private Map<String, Object> getSessionVariables(ChatbotSession session) {
        try {
            if (session.getVariables() == null || session.getVariables().isEmpty()) {
                return new HashMap<>();
            }
            return objectMapper.readValue(session.getVariables(), HashMap.class);
        } catch (Exception e) {
            log.warn("⚠️ Erreur lecture variables session: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    private String getSessionVariable(ChatbotSession session, String key, String defaultValue) {
        try {
            Map<String, Object> variables = getSessionVariables(session);
            Object value = variables.get(key);
            return value != null ? value.toString() : defaultValue;
        } catch (Exception e) {
            log.error("❌ Erreur lors de la récupération de la variable '{}' de la session: {}", key, e.getMessage(), e);
            return defaultValue;
        }
    }

    private void setSessionVariable(ChatbotSession session, String key, String value) {
        try {
            Map<String, Object> variables = getSessionVariables(session);
            variables.put(key, value);
            session.setVariables(objectMapper.writeValueAsString(variables));
            log.debug("💾 Variable mise à jour: {} = {}", key, value);
        } catch (Exception e) {
            log.warn("⚠️ Erreur lors de la définition de la variable {}: {}", key, e.getMessage());
        }
    }

    private String replaceVariablesInText(String text, ChatbotSession session) {
        if (text == null || !text.contains("{")) {
            return text;
        }

        try {
            Map<String, Object> variables = getSessionVariables(session);
            String result = text;

            for (Map.Entry<String, Object> entry : variables.entrySet()) {
                String placeholder = "{" + entry.getKey() + "}";
                if (result.contains(placeholder)) {
                    String value = entry.getValue() != null ? entry.getValue().toString() : "";
                    result = result.replace(placeholder, value);
                }
            }

            log.debug("🔄 Variables remplacées dans le texte: {} variables utilisées", variables.size());
            return result;
        } catch (Exception e) {
            log.warn("⚠️ Erreur lors du remplacement des variables: {}", e.getMessage());
            return text;
        }
    }

    /**
     * ================================
     * MÉTHODES UTILITAIRES - CONDITIONS
     * ================================
     */
    private boolean evaluateCondition(ConditionalConnectionPayload connection, NodeDataPayload nodeData, ChatbotSession session) {
        try {
            String variable = nodeData.getVariable();
            String operator = connection.getOperator() != null ? connection.getOperator() : "equals";
            String expectedValue = connection.getCondition() != null ? connection.getCondition() : "";

            // Récupérer la valeur de la variable
            String actualValue = getSessionVariable(session, variable, "");

            log.debug("🧪 Évaluation condition: {} {} {} = ?", actualValue, operator, expectedValue);

            switch (operator.toLowerCase()) {
                case "equals":
                    return actualValue.equals(expectedValue);
                case "not_equals":
                    return !actualValue.equals(expectedValue);
                case "contains":
                    return actualValue.toLowerCase().contains(expectedValue.toLowerCase());
                case "not_contains":
                    return !actualValue.toLowerCase().contains(expectedValue.toLowerCase());
                case "starts_with":
                    return actualValue.toLowerCase().startsWith(expectedValue.toLowerCase());
                case "ends_with":
                    return actualValue.toLowerCase().endsWith(expectedValue.toLowerCase());
                case "is_empty":
                    return actualValue.isEmpty();
                case "is_not_empty":
                    return !actualValue.isEmpty();
                case "greater_than":
                    try {
                        double numActual = Double.parseDouble(actualValue);
                        double numExpected = Double.parseDouble(expectedValue);
                        return numActual > numExpected;
                    } catch (NumberFormatException e) {
                        return false;
                    }
                case "less_than":
                    try {
                        double numActual = Double.parseDouble(actualValue);
                        double numExpected = Double.parseDouble(expectedValue);
                        return numActual < numExpected;
                    } catch (NumberFormatException e) {
                        return false;
                    }
                case "regex":
                    try {
                        return Pattern.compile(expectedValue).matcher(actualValue).matches();
                    } catch (Exception e) {
                        log.warn("⚠️ Erreur dans la regex: {}", e.getMessage());
                        return false;
                    }
                default:
                    log.warn("⚠️ Opérateur de condition non supporté: {}", operator);
                    return false;
            }
        } catch (Exception e) {
            log.warn("⚠️ Erreur lors de l'évaluation de la condition: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Évalue une expression custom comme "result contains 'x' and result is text"
     */
    private boolean evaluateCustomExpression(String expression, String userInput, Map<String, Object> variables) {
        if (expression == null || expression.trim().isEmpty()) {
            return false;
        }

        try {
            String input = userInput != null ? userInput.toLowerCase().trim() : "";
            String expr = expression.toLowerCase().trim();

            log.debug("🔍 Évaluation expression custom: '{}' avec input: '{}'", expr, input);

            // Remplacer 'result' par la valeur d'input
            expr = expr.replace("result", "'" + input + "'");

            // Parser les différents patterns
            expr = parseContainsPattern(expr);
            expr = parseIsTextPattern(expr, input);
            expr = parseIsNumberPattern(expr, input);
            expr = parseIsFilePattern(expr, input);
            expr = parseLogicalOperators(expr);

            log.debug("🔄 Expression transformée: '{}'", expr);

            // Évaluation finale
            boolean result = evaluateTransformedExpression(expr);
            log.debug("✅ Résultat expression custom: {}", result);

            return result;
        } catch (Exception e) {
            log.error("❌ Erreur évaluation expression custom '{}': {}", expression, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Parse le pattern "contains 'value'"
     */
    private String parseContainsPattern(String expr) {
        return expr.replaceAll("'([^']+)'\\s+contains\\s+'([^']+)'", "'" + "$1" + "'.toLowerCase().indexOf('" + "$2" + "') >= 0");
    }

    /**
     * Parse le pattern "is text"
     */
    private String parseIsTextPattern(String expr, String input) {
        boolean isText = input.length() > 0 && !isNumeric(input);
        return expr.replaceAll("'[^']*'\\s+is\\s+text", String.valueOf(isText));
    }

    /**
     * Parse le pattern "is number"
     */
    private String parseIsNumberPattern(String expr, String input) {
        boolean isNumber = isNumeric(input);
        return expr.replaceAll("'[^']*'\\s+is\\s+number", String.valueOf(isNumber));
    }

    /**
     * Parse le pattern "is file"
     */
    private String parseIsFilePattern(String expr, String input) {
        boolean isFile = input.matches(".*\\.(pdf|doc|docx|xls|xlsx|jpg|jpeg|png|gif|mp4|mp3|wav)$");
        return expr.replaceAll("'[^']*'\\s+is\\s+file", String.valueOf(isFile));
    }

    /**
     * Parse les opérateurs logiques
     */
    private String parseLogicalOperators(String expr) {
        return expr.replaceAll("\\s+and\\s+", " && ").replaceAll("\\s+or\\s+", " || ");
    }

    /**
     * Évalue l'expression transformée
     */
    private boolean evaluateTransformedExpression(String expr) {
        if (expr.contains("&&")) {
            String[] parts = expr.split("&&");
            for (String part : parts) {
                if (!evaluateSingleExpression(part.trim())) {
                    return false;
                }
            }
            return true;
        }

        if (expr.contains("||")) {
            String[] parts = expr.split("\\|\\|");
            for (String part : parts) {
                if (evaluateSingleExpression(part.trim())) {
                    return true;
                }
            }
            return false;
        }

        return evaluateSingleExpression(expr);
    }

    /**
     * Évalue une expression simple (true/false ou comparaison)
     */
    private boolean evaluateSingleExpression(String expr) {
        expr = expr.trim();

        if ("true".equals(expr)) return true;
        if ("false".equals(expr)) return false;

        return false;
    }

    /**
     * Vérifie si une chaîne est numérique
     */
    private boolean isNumeric(String str) {
        if (str == null || str.trim().isEmpty()) {
            return false;
        }
        try {
            Double.parseDouble(str.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private String calculateVariableValue(NodeDataPayload data, ChatbotSession session) {
        String operation = data.getVariableOperation() != null ? data.getVariableOperation() : "set";
        String variableName = data.getVariableName();

        switch (operation.toLowerCase()) {
            case "set":
                return data.getVariableValue() != null ? data.getVariableValue() : "";
            case "increment":
                try {
                    String currentValue = getSessionVariable(session, variableName, "0");
                    int current = Integer.parseInt(currentValue);
                    return String.valueOf(current + 1);
                } catch (NumberFormatException e) {
                    log.warn("⚠️ Impossible d'incrémenter la variable non numérique: {}", variableName);
                    return "1";
                }
            case "decrement":
                try {
                    String currentValue = getSessionVariable(session, variableName, "0");
                    int current = Integer.parseInt(currentValue);
                    return String.valueOf(current - 1);
                } catch (NumberFormatException e) {
                    log.warn("⚠️ Impossible de décrémenter la variable non numérique: {}", variableName);
                    return "-1";
                }
            case "append":
                String currentValue = getSessionVariable(session, variableName, "");
                String appendValue = data.getVariableValue() != null ? data.getVariableValue() : "";
                return currentValue + appendValue;
            default:
                return data.getVariableValue() != null ? data.getVariableValue() : "";
        }
    }

    /**
     * ================================
     * MÉTHODES UTILITAIRES - NAVIGATION
     * ================================
     */
    private FlowNodePayload findNodeById(FlowPayload flowPayload, String nodeId) {
        if (flowPayload.getNodes() == null || nodeId == null) {
            return null;
        }

        return flowPayload.getNodes().stream().filter(node -> nodeId.equals(node.getId())).findFirst().orElse(null);
    }

    private FlowNodePayload findStartNode(FlowPayload flowPayload) {
        if (flowPayload.getNodes() == null) {
            return null;
        }

        return flowPayload.getNodes().stream().filter(node -> "start".equals(node.getType())).findFirst().orElse(null);
    }

    private String findStartNodeIdForUser(String userLogin) {
        try {
            Long userId = getUserIdByLogin(userLogin);
            FlowPayload flowPayload = chatbotFlowService.getCurrentFlow(userId);

            if (flowPayload != null) {
                FlowNodePayload startNode = findStartNode(flowPayload);
                return startNode != null ? startNode.getId() : "start_node_default";
            }
        } catch (Exception e) {
            log.warn("⚠️ Erreur lors de la recherche du nœud de démarrage: {}", e.getMessage());
        }

        return "start_node_default";
    }

    /**
     * ================================
     * MÉTHODES UTILITAIRES - RECHERCHE
     * ================================
     */
    private ButtonPayload findButtonByText(List<ButtonPayload> buttons, String text) {
        if (buttons == null || text == null) return null;

        return buttons.stream().filter(button -> text.trim().equalsIgnoreCase(button.getText())).findFirst().orElse(null);
    }

    private ListItemPayload findListItemByTitle(List<ListItemPayload> items, String title) {
        if (items == null || title == null) return null;

        String normalizedTitle = title.trim().toLowerCase();

        // 1. Recherche exacte (insensible à la casse)
        for (ListItemPayload item : items) {
            if (item.getTitle().toLowerCase().equals(normalizedTitle)) {
                return item;
            }
        }

        // 2. Recherche par ID
        for (ListItemPayload item : items) {
            if (item.getId().equals(title.trim())) {
                return item;
            }
        }

        // 3. Recherche par value
        for (ListItemPayload item : items) {
            if (item.getValue() != null && item.getValue().toLowerCase().equals(normalizedTitle)) {
                return item;
            }
        }

        // 4. Recherche partielle (contient)
        for (ListItemPayload item : items) {
            if (item.getTitle().toLowerCase().contains(normalizedTitle)) {
                return item;
            }
        }

        return null;
    }

    /**
     * ================================
     * MÉTHODES UTILITAIRES - CONVERSION
     * ================================
     */
    private List<com.example.myproject.service.dto.flow.ButtonOption> convertToButtonOptions(List<ButtonPayload> buttons) {
        if (buttons == null) return new ArrayList<>();

        return buttons
            .stream()
            .map(button -> {
                com.example.myproject.service.dto.flow.ButtonOption option = new com.example.myproject.service.dto.flow.ButtonOption();
                option.setId(button.getId());
                option.setText(button.getText());
                option.setValue(button.getValue());
                return option;
            })
            .collect(java.util.stream.Collectors.toList());
    }

    private List<com.example.myproject.service.dto.flow.ListItem> convertToListItems(List<ListItemPayload> items) {
        if (items == null) return new ArrayList<>();

        return items
            .stream()
            .map(item -> {
                com.example.myproject.service.dto.flow.ListItem listItem = new com.example.myproject.service.dto.flow.ListItem();
                listItem.setId(item.getId());
                listItem.setTitle(item.getTitle());
                listItem.setDescription(item.getDescription());
                return listItem;
            })
            .collect(java.util.stream.Collectors.toList());
    }

    /**
     * ================================
     * CRÉATION DES RÉPONSES WHATSAPP
     * ================================
     */
    private WhatsAppResponse createTextResponse(String text) {
        WhatsAppResponse response = new WhatsAppResponse();
        response.setType("text");
        response.setText(text);
        return response;
    }

    private WhatsAppResponse createButtonsResponse(String text, List<com.example.myproject.service.dto.flow.ButtonOption> buttons) {
        WhatsAppResponse response = new WhatsAppResponse();
        response.setType("buttons");
        response.setText(text);
        response.setButtons(buttons != null ? buttons : new ArrayList<>());
        return response;
    }

    private WhatsAppResponse createListResponse(String text, List<com.example.myproject.service.dto.flow.ListItem> items) {
        WhatsAppResponse response = new WhatsAppResponse();
        response.setType("list");
        response.setText(text);
        response.setItems(items != null ? items : new ArrayList<>());
        return response;
    }

    /**
     * ================================
     * MÉTHODES UTILITAIRES DIVERSES
     * ================================
     */
    private Long getUserIdByLogin(String userLogin) {
        return userRepository.findOneByLogin(userLogin).map(User::getId).orElse(1L);
    }

    /**
     * Classe pour les résultats de validation
     */
    public static class ValidationResult {

        private final boolean valid;
        private final String errorMessage;

        private ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }

        public static ValidationResult valid() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult invalid(String errorMessage) {
            return new ValidationResult(false, errorMessage);
        }

        public boolean isValid() {
            return valid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}
