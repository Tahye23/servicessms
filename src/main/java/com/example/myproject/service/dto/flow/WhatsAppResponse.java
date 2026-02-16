package com.example.myproject.service.dto.flow;

import java.util.List;
import java.util.Map;

public class WhatsAppResponse {

    // ================================
    // PROPRIÉTÉS DE BASE
    // ================================
    private String type; // text, buttons, list, image, document, audio, video, template, location, contact
    private String text;

    // ================================
    // INTERACTIONS (BOUTONS ET LISTES)
    // ================================
    private List<ButtonOption> buttons;
    private List<ListItem> items;

    // ================================
    // MÉDIAS (IMAGES, DOCUMENTS, AUDIO, VIDÉO)
    // ================================
    private String mediaId; // ID du média uploadé sur Meta
    private String imageUrl; // URL directe de l'image (sera uploadée automatiquement)
    private String fileUrl; // URL directe du fichier (sera uploadé automatiquement)
    private String caption; // Légende pour image/vidéo/document
    private String fileName; // Nom du fichier pour les documents
    private String mimeType; // Type MIME du fichier

    // ================================
    // TEMPLATES WHATSAPP
    // ================================
    private String templateName;
    private String languageCode;
    private Map<String, Object> templateParameters;

    // ================================
    // LOCALISATION
    // ================================
    private Double latitude;
    private Double longitude;
    private String locationName;
    private String locationAddress;

    // ================================
    // CONTACT
    // ================================
    private String contactName;
    private String contactPhone;
    private String contactEmail;

    // ================================
    // MÉTADONNÉES ET CONTRÔLE
    // ================================
    private Long delay; // Délai avant envoi (en millisecondes)
    private Boolean markAsRead; // Marquer le message précédent comme lu
    private String replyToMessageId; // ID du message auquel répondre
    private String contextMessageId; // ID du message de contexte

    // ================================
    // CONSTRUCTEURS
    // ================================
    public WhatsAppResponse() {
        this.markAsRead = true; // Par défaut, marquer comme lu
    }

    public WhatsAppResponse(String type, String text) {
        this();
        this.type = type;
        this.text = text;
    }

    // ================================
    // MÉTHODES STATIQUES DE CRÉATION RAPIDE
    // ================================

    /**
     * Créer une réponse texte simple
     */
    public static WhatsAppResponse createText(String text) {
        return new WhatsAppResponse("text", text);
    }

    /**
     * Créer une réponse avec boutons
     */
    public static WhatsAppResponse createButtons(String text, List<ButtonOption> buttons) {
        WhatsAppResponse response = new WhatsAppResponse("buttons", text);
        response.setButtons(buttons);
        return response;
    }

    /**
     * Créer une réponse avec liste
     */
    public static WhatsAppResponse createList(String text, List<ListItem> items) {
        WhatsAppResponse response = new WhatsAppResponse("list", text);
        response.setItems(items);
        return response;
    }

    /**
     * Créer une réponse image avec media ID
     */
    public static WhatsAppResponse createImage(String mediaId, String caption) {
        WhatsAppResponse response = new WhatsAppResponse("image", null);
        response.setMediaId(mediaId);
        response.setCaption(caption);
        return response;
    }

    /**
     * Créer une réponse image avec URL
     */
    public static WhatsAppResponse createImageFromUrl(String imageUrl, String caption) {
        WhatsAppResponse response = new WhatsAppResponse("image", null);
        response.setImageUrl(imageUrl);
        response.setCaption(caption);
        return response;
    }

    /**
     * Créer une réponse document
     */
    public static WhatsAppResponse createDocument(String mediaId, String fileName, String caption) {
        WhatsAppResponse response = new WhatsAppResponse("document", null);
        response.setMediaId(mediaId);
        response.setFileName(fileName);
        response.setCaption(caption);
        return response;
    }

    /**
     * Créer une réponse document depuis URL
     */
    public static WhatsAppResponse createDocumentFromUrl(String fileUrl, String fileName, String caption) {
        WhatsAppResponse response = new WhatsAppResponse("document", null);
        response.setFileUrl(fileUrl);
        response.setFileName(fileName);
        response.setCaption(caption);
        return response;
    }

    /**
     * Créer une réponse audio
     */
    public static WhatsAppResponse createAudio(String mediaId) {
        WhatsAppResponse response = new WhatsAppResponse("audio", null);
        response.setMediaId(mediaId);
        return response;
    }

    /**
     * Créer une réponse vidéo
     */
    public static WhatsAppResponse createVideo(String mediaId, String caption) {
        WhatsAppResponse response = new WhatsAppResponse("video", null);
        response.setMediaId(mediaId);
        response.setCaption(caption);
        return response;
    }

    /**
     * Créer une réponse template
     */
    public static WhatsAppResponse createTemplate(String templateName, String languageCode, Map<String, Object> parameters) {
        WhatsAppResponse response = new WhatsAppResponse("template", null);
        response.setTemplateName(templateName);
        response.setLanguageCode(languageCode);
        response.setTemplateParameters(parameters);
        return response;
    }

    /**
     * Créer une réponse localisation
     */
    public static WhatsAppResponse createLocation(double latitude, double longitude, String name, String address) {
        WhatsAppResponse response = new WhatsAppResponse("location", null);
        response.setLatitude(latitude);
        response.setLongitude(longitude);
        response.setLocationName(name);
        response.setLocationAddress(address);
        return response;
    }

    /**
     * Créer une réponse contact
     */
    public static WhatsAppResponse createContact(String name, String phone, String email) {
        WhatsAppResponse response = new WhatsAppResponse("contact", null);
        response.setContactName(name);
        response.setContactPhone(phone);
        response.setContactEmail(email);
        return response;
    }

    // ================================
    // MÉTHODES UTILITAIRES
    // ================================

    /**
     * Vérifier si la réponse est valide
     */
    public boolean isValid() {
        if (type == null || type.isEmpty()) {
            return false;
        }

        switch (type) {
            case "text":
                return text != null && !text.isEmpty();
            case "buttons":
                return text != null && buttons != null && !buttons.isEmpty() && buttons.size() <= 3;
            case "list":
                return text != null && items != null && !items.isEmpty() && items.size() <= 10;
            case "image":
                return (mediaId != null && !mediaId.isEmpty()) || (imageUrl != null && !imageUrl.isEmpty());
            case "document":
                return (mediaId != null && !mediaId.isEmpty()) || (fileUrl != null && !fileUrl.isEmpty());
            case "audio":
            case "video":
                return mediaId != null && !mediaId.isEmpty();
            case "template":
                return templateName != null && !templateName.isEmpty() && languageCode != null && !languageCode.isEmpty();
            case "location":
                return latitude != null && longitude != null;
            case "contact":
                return contactName != null && !contactName.isEmpty();
            default:
                return false;
        }
    }

    /**
     * Ajouter un délai avant envoi
     */
    public WhatsAppResponse withDelay(long delayMs) {
        this.delay = delayMs;
        return this;
    }

    /**
     * Répondre à un message spécifique
     */
    public WhatsAppResponse replyTo(String messageId) {
        this.replyToMessageId = messageId;
        return this;
    }

    /**
     * Ne pas marquer comme lu
     */
    public WhatsAppResponse dontMarkAsRead() {
        this.markAsRead = false;
        return this;
    }

    // ================================
    // GETTERS ET SETTERS
    // ================================

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public List<ButtonOption> getButtons() {
        return buttons;
    }

    public void setButtons(List<ButtonOption> buttons) {
        this.buttons = buttons;
    }

    public List<ListItem> getItems() {
        return items;
    }

    public void setItems(List<ListItem> items) {
        this.items = items;
    }

    public String getMediaId() {
        return mediaId;
    }

    public void setMediaId(String mediaId) {
        this.mediaId = mediaId;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
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

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public void setLanguageCode(String languageCode) {
        this.languageCode = languageCode;
    }

    public Map<String, Object> getTemplateParameters() {
        return templateParameters;
    }

    public void setTemplateParameters(Map<String, Object> templateParameters) {
        this.templateParameters = templateParameters;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public String getLocationAddress() {
        return locationAddress;
    }

    public void setLocationAddress(String locationAddress) {
        this.locationAddress = locationAddress;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public Long getDelay() {
        return delay;
    }

    public void setDelay(Long delay) {
        this.delay = delay;
    }

    public Boolean getMarkAsRead() {
        return markAsRead;
    }

    public void setMarkAsRead(Boolean markAsRead) {
        this.markAsRead = markAsRead;
    }

    public String getReplyToMessageId() {
        return replyToMessageId;
    }

    public void setReplyToMessageId(String replyToMessageId) {
        this.replyToMessageId = replyToMessageId;
    }

    public String getContextMessageId() {
        return contextMessageId;
    }

    public void setContextMessageId(String contextMessageId) {
        this.contextMessageId = contextMessageId;
    }

    // ================================
    // MÉTHODES UTILITAIRES POUR DEBUG
    // ================================

    @Override
    public String toString() {
        return String.format(
            "WhatsAppResponse{type='%s', text='%s', valid=%s}",
            type,
            text != null ? text.substring(0, Math.min(text.length(), 50)) + "..." : null,
            isValid()
        );
    }
}
