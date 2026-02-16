package com.example.myproject.service;

import com.example.myproject.domain.*;
import com.example.myproject.domain.enumeration.ContentType;
import com.example.myproject.domain.enumeration.Direction;
import com.example.myproject.domain.enumeration.MessageType;
import com.example.myproject.repository.AbonnementRepository;
import com.example.myproject.repository.ExtendedUserRepository;
import com.example.myproject.repository.SmsRepository;
import com.example.myproject.repository.TokensAppRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service de traçabilité pour les envois via API externe
 * Enregistre tous les messages avec métadonnées complètes
 */
@Service
public class ExternalApiTrackingService {

    private static final Logger log = LoggerFactory.getLogger(ExternalApiTrackingService.class);

    @Autowired
    private SmsRepository smsRepository;

    @Autowired
    private TokensAppRepository tokensAppRepository;

    @Autowired
    private ExtendedUserRepository extendedUserRepository;

    @Autowired
    private AbonnementRepository abonnementRepository;

    @Autowired
    private AbonnementService abonnementService;

    /**
     * ✅ ENREGISTRER UN SMS ENVOYÉ VIA API EXTERNE
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Sms trackExternalSms(ExternalSmsTrackingData data) {
        try {
            log.info("[TRACK-EXTERNAL-SMS] Enregistrement SMS API - Token: {}, Phone: {}", data.getTokenId(), data.getPhone());

            // Créer l'entité Sms
            Sms sms = new Sms();

            // ✅ IDENTIFICATION DE LA SOURCE
            sms.setType(MessageType.SMS);
            sms.setDirection(Direction.OUTBOUND);
            sms.setContentType(ContentType.TEXT);
            sms.setUser_login(data.getUserLogin());
            // ✅ MÉTADONNÉES DE TRAÇABILITÉ
            // Stocker l'ID du token dans un champ personnalisé pour identifier la source
            sms.setBulkId("API_TOKEN_" + data.getTokenId()); // Identifiant unique de la source
            sms.setLast_error(buildApiMetadata(data)); // Métadonnées complètes en JSON

            // ✅ INFORMATIONS DU MESSAGE
            sms.setSender(data.getSender());
            sms.setReceiver(data.getPhone());
            sms.setNamereceiver(data.getPhone()); // Pas de nom pour API externe
            sms.setMsgdata(data.getMessageContent());
            sms.setVars(data.getVariablesJson()); // Variables JSON originales

            // ✅ TEMPLATE
            sms.setTemplate_id(data.getTemplateId());

            // ✅ STATUT INITIAL
            if (data.isSuccess()) {
                sms.setStatus("SENT");
                sms.setDeliveryStatus("sent");
                sms.setSent(true);
                sms.setMessageId(data.getMessageId()); // Vide pour SMS, utilisé pour WhatsApp
            } else {
                sms.setStatus("FAILED");
                sms.setDeliveryStatus("failed");
                sms.setSent(false);
                sms.setLast_error(data.getErrorMessage());
            }

            // ✅ TIMESTAMPS
            sms.setSendDate(Instant.now());
            sms.setBulkCreatedAt(Instant.now());

            // ✅ SEGMENTS (pour SMS)
            sms.setTotalMessage(calculateSmsSegments(data.getMessageContent()));

            // Sauvegarder
            Sms savedSms = smsRepository.save(sms);

            log.info("[TRACK-EXTERNAL-SMS] ✅ SMS enregistré - ID: {}, Status: {}", savedSms.getId(), savedSms.getStatus());

            // ✅ DÉCRÉMENTER L'ABONNEMENT SI SUCCÈS
            if (data.isSuccess()) {
                decrementQuotaForExternalApi(data, sms.getTotalMessage());
            }

            return savedSms;
        } catch (Exception e) {
            log.error("[TRACK-EXTERNAL-SMS] ❌ Erreur enregistrement SMS", e);
            throw e;
        }
    }

    /**
     * ✅ ENREGISTRER UN WHATSAPP ENVOYÉ VIA API EXTERNE
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Sms trackExternalWhatsApp(ExternalWhatsAppTrackingData data) {
        try {
            log.info("[TRACK-EXTERNAL-WA] Enregistrement WhatsApp API - Token: {}, Phone: {}", data.getTokenId(), data.getPhone());

            // Créer l'entité Sms
            Sms sms = new Sms();

            // ✅ IDENTIFICATION DE LA SOURCE
            sms.setType(MessageType.WHATSAPP);
            sms.setDirection(Direction.OUTBOUND);
            sms.setContentType(ContentType.TEMPLATE);
            sms.setUser_login(data.getUserLogin());
            // ✅ MÉTADONNÉES DE TRAÇABILITÉ
            sms.setBulkId("API_TOKEN_" + data.getTokenId());
            sms.setLast_error(buildApiMetadata(data));

            // ✅ INFORMATIONS DU MESSAGE
            sms.setSender(data.getPhoneNumberId()); // Phone Number ID pour WhatsApp
            sms.setReceiver(data.getPhone());
            sms.setNamereceiver(data.getPhone());
            sms.setMsgdata(data.getTemplateName()); // Nom du template pour traçabilité
            sms.setVars(data.getVariablesJson());

            // ✅ TEMPLATE
            sms.setTemplate_id(data.getTemplateId());

            // ✅ MESSAGE ID META (CRUCIAL pour webhook)
            sms.setMessageId(data.getMessageId()); // ✅ ID retourné par Meta

            // ✅ STATUT INITIAL
            if (data.isSuccess()) {
                sms.setStatus("SENT");
                sms.setDeliveryStatus("sent");
                sms.setSent(true);
            } else {
                sms.setStatus("FAILED");
                sms.setDeliveryStatus("failed");
                sms.setSent(false);
                sms.setLast_error(data.getErrorMessage());
            }

            // ✅ TIMESTAMPS
            sms.setSendDate(Instant.now());
            sms.setBulkCreatedAt(Instant.now());

            // ✅ SEGMENTS (toujours 1 pour WhatsApp)
            sms.setTotalMessage(1);

            // Sauvegarder
            Sms savedSms = smsRepository.save(sms);

            log.info(
                "[TRACK-EXTERNAL-WA] ✅ WhatsApp enregistré - ID: {}, MessageId: {}, Status: {}",
                savedSms.getId(),
                savedSms.getMessageId(),
                savedSms.getStatus()
            );

            // ✅ DÉCRÉMENTER L'ABONNEMENT SI SUCCÈS
            if (data.isSuccess()) {
                decrementQuotaForExternalApi(data, 1);
            }

            return savedSms;
        } catch (Exception e) {
            log.error("[TRACK-EXTERNAL-WA] ❌ Erreur enregistrement WhatsApp", e);
            throw e;
        }
    }

    /**
     * ✅ CONSTRUIRE LES MÉTADONNÉES JSON POUR TRAÇABILITÉ
     */
    private String buildApiMetadata(Object trackingData) {
        StringBuilder metadata = new StringBuilder();
        metadata.append("{");
        metadata.append("\"source\":\"EXTERNAL_API\",");
        metadata.append("\"timestamp\":\"").append(Instant.now()).append("\",");

        if (trackingData instanceof ExternalSmsTrackingData) {
            ExternalSmsTrackingData data = (ExternalSmsTrackingData) trackingData;
            metadata.append("\"api_type\":\"SMS\",");
            metadata.append("\"token_id\":").append(data.getTokenId()).append(",");
            metadata.append("\"user_login\":\"").append(escape(data.getUserLogin())).append("\",");
            metadata.append("\"template_name\":\"").append(escape(data.getTemplateName())).append("\",");
            metadata.append("\"app_name\":\"").append(escape(data.getAppName())).append("\"");
        } else if (trackingData instanceof ExternalWhatsAppTrackingData) {
            ExternalWhatsAppTrackingData data = (ExternalWhatsAppTrackingData) trackingData;
            metadata.append("\"api_type\":\"WHATSAPP\",");
            metadata.append("\"token_id\":").append(data.getTokenId()).append(",");
            metadata.append("\"user_login\":\"").append(escape(data.getUserLogin())).append("\",");
            metadata.append("\"template_name\":\"").append(escape(data.getTemplateName())).append("\",");
            metadata.append("\"business_id\":\"").append(escape(data.getBusinessId())).append("\",");
            metadata.append("\"app_name\":\"").append(escape(data.getAppName())).append("\"");
        }

        metadata.append("}");
        return metadata.toString();
    }

    /**
     * ✅ DÉCRÉMENTER L'ABONNEMENT
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void decrementQuotaForExternalApi(BaseTrackingData data, int messageCount) {
        try {
            // Récupérer l'utilisateur
            ExtendedUser extendedUser = extendedUserRepository.findOneByUserLogin(data.getUserLogin()).orElse(null);

            if (extendedUser == null) {
                log.warn("[QUOTA-DECREMENT] Utilisateur non trouvé: {}", data.getUserLogin());
                return;
            }

            // Récupérer les abonnements actifs
            List<Abonnement> activeSubscriptions = abonnementRepository.findActiveByUserId(extendedUser.getId());

            if (activeSubscriptions.isEmpty()) {
                log.warn("[QUOTA-DECREMENT] Aucun abonnement actif pour: {}", data.getUserLogin());
                return;
            }

            MessageType messageType = data instanceof ExternalSmsTrackingData ? MessageType.SMS : MessageType.WHATSAPP;

            log.info(
                "[QUOTA-DECREMENT] Décrémentation {} messages de type {} pour utilisateur {}",
                messageCount,
                messageType,
                data.getUserLogin()
            );

            // Décrémenter via le service d'abonnement
            abonnementService.decrementQuotasAfterSend(activeSubscriptions, messageType, messageCount);

            // Sauvegarder
            abonnementRepository.saveAll(activeSubscriptions);

            log.info("[QUOTA-DECREMENT] ✅ Quota décrémenté avec succès");
        } catch (Exception e) {
            log.error("[QUOTA-DECREMENT] ❌ Erreur décrémentation quota", e);
            // Ne pas bloquer l'envoi si erreur de quota
        }
    }

    /**
     * ✅ CALCULER LE NOMBRE DE SEGMENTS SMS
     */
    private int calculateSmsSegments(String content) {
        if (content == null || content.isEmpty()) {
            return 1;
        }

        int length = content.length();

        // SMS simple : 160 caractères
        if (length <= 160) {
            return 1;
        }

        // SMS concaténé : 153 caractères par segment
        return (int) Math.ceil((double) length / 153);
    }

    /**
     * ✅ ÉCHAPPER LES CARACTÈRES SPÉCIAUX JSON
     */
    private String escape(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }

    // ===== CLASSES DE DONNÉES =====

    /**
     * Classe de base pour les données de traçabilité
     */
    public abstract static class BaseTrackingData {

        protected Integer tokenId;
        protected String userLogin;
        protected String appName;
        protected String phone;
        protected Long templateId;
        protected String templateName;
        protected String variablesJson;
        protected boolean success;
        protected String errorMessage;

        public Integer getTokenId() {
            return tokenId;
        }

        public String getUserLogin() {
            return userLogin;
        }

        public String getAppName() {
            return appName;
        }

        public String getPhone() {
            return phone;
        }

        public Long getTemplateId() {
            return templateId;
        }

        public String getTemplateName() {
            return templateName;
        }

        public String getVariablesJson() {
            return variablesJson;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

    /**
     * Données de traçabilité pour SMS
     */
    public static class ExternalSmsTrackingData extends BaseTrackingData {

        private String sender;
        private String messageContent;
        private String messageId; // Vide pour SMS

        public ExternalSmsTrackingData(
            Integer tokenId,
            String userLogin,
            String appName,
            String phone,
            String sender,
            String messageContent,
            Long templateId,
            String templateName,
            String variablesJson,
            boolean success,
            String errorMessage,
            String messageId
        ) {
            this.tokenId = tokenId;
            this.userLogin = userLogin;
            this.appName = appName;
            this.phone = phone;
            this.sender = sender;
            this.messageContent = messageContent;
            this.templateId = templateId;
            this.templateName = templateName;
            this.variablesJson = variablesJson;
            this.success = success;
            this.errorMessage = errorMessage;
            this.messageId = messageId;
        }

        public String getSender() {
            return sender;
        }

        public String getMessageContent() {
            return messageContent;
        }

        public String getMessageId() {
            return messageId;
        }
    }

    /**
     * Données de traçabilité pour WhatsApp
     */
    public static class ExternalWhatsAppTrackingData extends BaseTrackingData {

        private String businessId;
        private String phoneNumberId;
        private String messageId; // ✅ ID retourné par Meta

        public ExternalWhatsAppTrackingData(
            Integer tokenId,
            String userLogin,
            String appName,
            String phone,
            String businessId,
            String phoneNumberId,
            Long templateId,
            String templateName,
            String variablesJson,
            boolean success,
            String messageId,
            String errorMessage
        ) {
            this.tokenId = tokenId;
            this.userLogin = userLogin;
            this.appName = appName;
            this.phone = phone;
            this.businessId = businessId;
            this.phoneNumberId = phoneNumberId;
            this.templateId = templateId;
            this.templateName = templateName;
            this.variablesJson = variablesJson;
            this.success = success;
            this.messageId = messageId;
            this.errorMessage = errorMessage;
        }

        public String getBusinessId() {
            return businessId;
        }

        public String getPhoneNumberId() {
            return phoneNumberId;
        }

        public String getMessageId() {
            return messageId;
        }
    }
}
