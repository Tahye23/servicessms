package com.example.myproject.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "flow_node")
public class FlowNode extends AbstractAuditingEntity<Long> implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator", allocationSize = 1)
    private Long id;

    @NotNull
    @Size(max = 50)
    @Column(name = "node_id", length = 50, nullable = false)
    private String nodeId; // L'ID unique du nœud dans le flow

    @NotNull
    @Size(max = 20)
    @Column(name = "node_type", length = 20, nullable = false)
    private String nodeType;

    @NotNull
    @Column(name = "node_order", nullable = false)
    private Integer nodeOrder;

    @NotNull
    @Column(name = "position_x", nullable = false)
    private Integer positionX;

    @NotNull
    @Column(name = "position_y", nullable = false)
    private Integer positionY;

    @Size(max = 100)
    @Column(name = "label", length = 100)
    private String label;

    @Size(max = 1000)
    @Column(name = "description", length = 1000)
    private String description;

    // Contenu principal du nœud
    @Lob
    @Column(name = "text_content", columnDefinition = "TEXT")
    private String textContent;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "file_url")
    private String fileUrl;

    @Column(name = "file_name")
    private String fileName;

    // Configuration spécifique
    @Column(name = "webhook_url")
    private String webhookUrl;

    @Column(name = "webhook_method")
    private String webhookMethod;

    @Column(name = "response_type")
    private String responseType;

    @Column(name = "is_required")
    private Boolean isRequired = false;

    @Column(name = "timeout_seconds")
    private Integer timeoutSeconds;

    @Column(name = "use_variables")
    private Boolean useVariables = false;

    // Variables
    @Column(name = "store_in_variable")
    private String storeInVariable;

    @Column(name = "variable_name")
    private String variableName;

    @Column(name = "variable_value")
    private String variableValue;

    @Column(name = "variable_operation")
    private String variableOperation;

    // Conditions
    @Column(name = "condition_type")
    private String conditionType;

    @Column(name = "condition_variable")
    private String conditionVariable;

    @Column(name = "condition_operator")
    private String conditionOperator;

    @Column(name = "condition_value")
    private String conditionValue;

    // Relations
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flow_id")
    private ChatbotFlow flow;

    @Column(name = "next_node_id")
    private String nextNodeId;

    @Column(name = "default_next_node_id")
    private String defaultNextNodeId;

    // Relations vers les boutons et conditions
    @OneToMany(mappedBy = "node", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<NodeButton> buttons = new ArrayList<>();

    @OneToMany(mappedBy = "node", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<NodeListItem> listItems = new ArrayList<>();

    @OneToMany(mappedBy = "node", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ConditionalConnection> conditionalConnections = new ArrayList<>();

    @Column(name = "validation_message")
    private String validationMessage;

    @Column(name = "wait_for_user_response")
    private Boolean waitForUserResponse;

    /**
     * ================================
     * PROPRIÉTÉS À AJOUTER DANS NodeDataPayload
     * ================================
     */

    // Validation de longueur pour texte
    @Column(name = "min_length")
    private Integer minLength;

    @Column(name = "max_length")
    private Integer maxLength;

    // Validation de valeur pour nombres
    @Column(name = "min_value")
    private Integer minValue;

    @Column(name = "max_value")
    private Integer maxValue;

    // Validation de fichiers
    @Column(name = "max_file_size")
    private Double maxFileSize;

    @Column(name = "allowed_extensions") // en MB
    private String allowedExtensions; // ex: "jpg,png,pdf"

    // Getters et Setters
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

    // Getters et setters
    public Boolean getWaitForUserResponse() {
        return waitForUserResponse;
    }

    public void setWaitForUserResponse(Boolean waitForUserResponse) {
        this.waitForUserResponse = waitForUserResponse;
    }

    // AJOUTER le getter/setter :
    public String getValidationMessage() {
        return validationMessage;
    }

    public void setValidationMessage(String validationMessage) {
        this.validationMessage = validationMessage;
    }

    // Getters et setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getNodeType() {
        return nodeType;
    }

    public void setNodeType(String nodeType) {
        this.nodeType = nodeType;
    }

    public Integer getNodeOrder() {
        return nodeOrder;
    }

    public void setNodeOrder(Integer nodeOrder) {
        this.nodeOrder = nodeOrder;
    }

    public Integer getPositionX() {
        return positionX;
    }

    public void setPositionX(Integer positionX) {
        this.positionX = positionX;
    }

    public Integer getPositionY() {
        return positionY;
    }

    public void setPositionY(Integer positionY) {
        this.positionY = positionY;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTextContent() {
        return textContent;
    }

    public void setTextContent(String textContent) {
        this.textContent = textContent;
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

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getWebhookUrl() {
        return webhookUrl;
    }

    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    public String getWebhookMethod() {
        return webhookMethod;
    }

    public void setWebhookMethod(String webhookMethod) {
        this.webhookMethod = webhookMethod;
    }

    public String getResponseType() {
        return responseType;
    }

    public void setResponseType(String responseType) {
        this.responseType = responseType;
    }

    public Boolean getIsRequired() {
        return isRequired;
    }

    public void setIsRequired(Boolean isRequired) {
        this.isRequired = isRequired;
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

    public String getConditionVariable() {
        return conditionVariable;
    }

    public void setConditionVariable(String conditionVariable) {
        this.conditionVariable = conditionVariable;
    }

    public String getConditionOperator() {
        return conditionOperator;
    }

    public void setConditionOperator(String conditionOperator) {
        this.conditionOperator = conditionOperator;
    }

    public String getConditionValue() {
        return conditionValue;
    }

    public void setConditionValue(String conditionValue) {
        this.conditionValue = conditionValue;
    }

    public ChatbotFlow getFlow() {
        return flow;
    }

    public void setFlow(ChatbotFlow flow) {
        this.flow = flow;
    }

    public String getNextNodeId() {
        return nextNodeId;
    }

    public void setNextNodeId(String nextNodeId) {
        this.nextNodeId = nextNodeId;
    }

    public String getDefaultNextNodeId() {
        return defaultNextNodeId;
    }

    public void setDefaultNextNodeId(String defaultNextNodeId) {
        this.defaultNextNodeId = defaultNextNodeId;
    }

    public List<NodeButton> getButtons() {
        return buttons;
    }

    public void setButtons(List<NodeButton> buttons) {
        this.buttons = buttons;
    }

    public List<NodeListItem> getListItems() {
        return listItems;
    }

    public void setListItems(List<NodeListItem> listItems) {
        this.listItems = listItems;
    }

    public List<ConditionalConnection> getConditionalConnections() {
        return conditionalConnections;
    }

    public void setConditionalConnections(List<ConditionalConnection> conditionalConnections) {
        this.conditionalConnections = conditionalConnections;
    }

    // Méthodes utilitaires
    public void addButton(NodeButton button) {
        buttons.add(button);
        button.setNode(this);
    }

    public void removeButton(NodeButton button) {
        buttons.remove(button);
        button.setNode(null);
    }

    public void addListItem(NodeListItem item) {
        listItems.add(item);
        item.setNode(this);
    }

    public void removeListItem(NodeListItem item) {
        listItems.remove(item);
        item.setNode(null);
    }

    public void addConditionalConnection(ConditionalConnection connection) {
        conditionalConnections.add(connection);
        connection.setNode(this);
    }

    public void removeConditionalConnection(ConditionalConnection connection) {
        conditionalConnections.remove(connection);
        connection.setNode(null);
    }
}
