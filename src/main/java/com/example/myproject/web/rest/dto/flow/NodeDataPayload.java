package com.example.myproject.web.rest.dto.flow;

import java.util.List;

public class NodeDataPayload {

    // Contenu de base
    private String text;
    private String imageUrl;
    private String fileUrl;
    private String fileName;
    private String caption;
    private String validationMessage;
    // Configuration
    private String responseType;
    private Boolean required;
    private Integer timeoutSeconds;
    private Boolean useVariables;
    private String storeInVariable;
    private Integer minLength;
    private Integer maxLength;

    // Validation de valeur pour nombres
    private Integer minValue;
    private Integer maxValue;

    // Validation de fichiers
    private Double maxFileSize; // en MB
    private String allowedExtensions; // ex: "jpg,png,pdf"

    // Variables
    private String variableName;
    private String variableValue;
    private String variableOperation;

    // Conditions
    private String conditionType;
    private String variable;
    private String operator;
    private String value;
    private String defaultNextNodeId;

    // Webhook
    private String webhookUrl;
    private String method;

    // Relations
    private List<ButtonPayload> buttons;
    private List<ListItemPayload> items;
    private List<ConditionalConnectionPayload> conditionalConnections;
    // Nouveau champ pour waitForUserResponse
    private Boolean waitForUserResponse;

    // Getters et setters
    public Boolean getWaitForUserResponse() {
        return waitForUserResponse;
    }

    public void setWaitForUserResponse(Boolean waitForUserResponse) {
        this.waitForUserResponse = waitForUserResponse;
    }

    // Constructeurs
    public NodeDataPayload() {}

    // Getters et setters
    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Integer getMinLength() {
        return minLength;
    }

    public void setMinLength(Integer minLength) {
        this.minLength = minLength;
    }

    public Integer getMaxLength() {
        return maxLength;
    }

    public void setMaxLength(Integer maxLength) {
        this.maxLength = maxLength;
    }

    public Integer getMinValue() {
        return minValue;
    }

    public void setMinValue(Integer minValue) {
        this.minValue = minValue;
    }

    public Integer getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(Integer maxValue) {
        this.maxValue = maxValue;
    }

    public Double getMaxFileSize() {
        return maxFileSize;
    }

    public void setMaxFileSize(Double maxFileSize) {
        this.maxFileSize = maxFileSize;
    }

    public String getAllowedExtensions() {
        return allowedExtensions;
    }

    public void setAllowedExtensions(String allowedExtensions) {
        this.allowedExtensions = allowedExtensions;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public String getResponseType() {
        return responseType;
    }

    public void setResponseType(String responseType) {
        this.responseType = responseType;
    }

    public String getValidationMessage() {
        return validationMessage;
    }

    public void setValidationMessage(String validationMessage) {
        this.validationMessage = validationMessage;
    }

    public Boolean getRequired() {
        return required;
    }

    public void setRequired(Boolean required) {
        this.required = required;
    }

    public Integer getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(Integer timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public Boolean getUseVariables() {
        return useVariables;
    }

    public void setUseVariables(Boolean useVariables) {
        this.useVariables = useVariables;
    }

    public String getStoreInVariable() {
        return storeInVariable;
    }

    public void setStoreInVariable(String storeInVariable) {
        this.storeInVariable = storeInVariable;
    }

    public String getVariableName() {
        return variableName;
    }

    public void setVariableName(String variableName) {
        this.variableName = variableName;
    }

    public String getVariableValue() {
        return variableValue;
    }

    public void setVariableValue(String variableValue) {
        this.variableValue = variableValue;
    }

    public String getVariableOperation() {
        return variableOperation;
    }

    public void setVariableOperation(String variableOperation) {
        this.variableOperation = variableOperation;
    }

    public String getConditionType() {
        return conditionType;
    }

    public void setConditionType(String conditionType) {
        this.conditionType = conditionType;
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

    public String getDefaultNextNodeId() {
        return defaultNextNodeId;
    }

    public void setDefaultNextNodeId(String defaultNextNodeId) {
        this.defaultNextNodeId = defaultNextNodeId;
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

    public List<ButtonPayload> getButtons() {
        return buttons;
    }

    public void setButtons(List<ButtonPayload> buttons) {
        this.buttons = buttons;
    }

    public List<ListItemPayload> getItems() {
        return items;
    }

    public void setItems(List<ListItemPayload> items) {
        this.items = items;
    }

    public List<ConditionalConnectionPayload> getConditionalConnections() {
        return conditionalConnections;
    }

    public void setConditionalConnections(List<ConditionalConnectionPayload> conditionalConnections) {
        this.conditionalConnections = conditionalConnections;
    }
}
