package com.example.myproject.service.dto.flow;

import java.util.List;
import java.util.Map;

public class NodeData {

    // Propriétés communes
    private String text;
    private Integer delay;
    private Boolean markdownEnabled;

    // Pour les boutons
    private List<ButtonOption> buttons;

    // Pour les listes
    private List<ListItem> items;

    // Pour les langues
    private List<LanguageOption> languages;

    // Pour les fichiers
    private List<String> allowedTypes;
    private String maxSize;

    // Pour les images/médias
    private String imageUrl;
    private String mediaId; // ID du média uploadé sur WhatsApp
    private String caption;
    private String fileName;

    // Pour les contacts
    private Boolean requestName;
    private Boolean requestPhone;
    private Boolean requestEmail;

    // Pour les conditions
    private String variable;
    private String operator;
    private String value;
    private String trueAction;
    private String falseAction;

    // Pour les webhooks
    private String webhookUrl;
    private String method;
    private Map<String, String> headers;
    private Map<String, Object> payload;

    // Pour les transferts d'agent
    private String department;
    private String priority;
    private String message;

    // Pour les templates
    private String templateName;
    private String languageCode;
    private Map<String, Object> templateParameters;

    // Constructeurs
    public NodeData() {}

    // Getters et setters
    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Integer getDelay() {
        return delay;
    }

    public void setDelay(Integer delay) {
        this.delay = delay;
    }

    public Boolean getMarkdownEnabled() {
        return markdownEnabled;
    }

    public void setMarkdownEnabled(Boolean markdownEnabled) {
        this.markdownEnabled = markdownEnabled;
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

    public List<LanguageOption> getLanguages() {
        return languages;
    }

    public void setLanguages(List<LanguageOption> languages) {
        this.languages = languages;
    }

    public List<String> getAllowedTypes() {
        return allowedTypes;
    }

    public void setAllowedTypes(List<String> allowedTypes) {
        this.allowedTypes = allowedTypes;
    }

    public String getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(String maxSize) {
        this.maxSize = maxSize;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getMediaId() {
        return mediaId;
    }

    public void setMediaId(String mediaId) {
        this.mediaId = mediaId;
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

    public Boolean getRequestName() {
        return requestName;
    }

    public void setRequestName(Boolean requestName) {
        this.requestName = requestName;
    }

    public Boolean getRequestPhone() {
        return requestPhone;
    }

    public void setRequestPhone(Boolean requestPhone) {
        this.requestPhone = requestPhone;
    }

    public Boolean getRequestEmail() {
        return requestEmail;
    }

    public void setRequestEmail(Boolean requestEmail) {
        this.requestEmail = requestEmail;
    }

    public String getVariable() {
        return variable;
    }

    public void setVariable(String variable) {
        this.variable = variable;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getTrueAction() {
        return trueAction;
    }

    public void setTrueAction(String trueAction) {
        this.trueAction = trueAction;
    }

    public String getFalseAction() {
        return falseAction;
    }

    public void setFalseAction(String falseAction) {
        this.falseAction = falseAction;
    }

    public String getWebhookUrl() {
        return webhookUrl;
    }

    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
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
}
