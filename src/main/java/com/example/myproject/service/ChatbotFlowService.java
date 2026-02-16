// ================================
// SERVICE AVEC ENTITÉS RELATIONNELLES - 2 FONCTIONS
// ================================
package com.example.myproject.service;

import com.example.myproject.domain.*;
import com.example.myproject.repository.*;
import com.example.myproject.web.rest.dto.flow.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ChatbotFlowService {

    private final Logger log = LoggerFactory.getLogger(ChatbotFlowService.class);
    private final ChatbotFlowRepository chatbotFlowRepository;

    public ChatbotFlowService(ChatbotFlowRepository chatbotFlowRepository) {
        this.chatbotFlowRepository = chatbotFlowRepository;
    }

    /**
     * FONCTION 1: Créer ou Modifier un flow complet
     */
    @Transactional
    public ChatbotFlow saveOrUpdateFlow(Long userId, FlowPayload flowPayload) {
        log.debug("Sauvegarde flow pour user: {}, nom: {}", userId, flowPayload.getName());

        ChatbotFlow flow;
        boolean isUpdate = false;

        // Vérifier s'il existe déjà un flow pour cet utilisateur
        Optional<ChatbotFlow> existingFlow = chatbotFlowRepository.findByUserId(userId);

        if (existingFlow.isPresent()) {
            // MODIFICATION du flow existant
            flow = existingFlow.get();
            isUpdate = true;
            flow.setVersion(flow.getVersion() + 1);
            log.debug("Modification du flow existant ID: {}, nouvelle version: {}", flow.getId(), flow.getVersion());

            // Nettoyer les anciennes relations
            clearFlowRelations(flow);
        } else {
            // CRÉATION d'un nouveau flow
            flow = new ChatbotFlow();
            flow.setUserId(userId);
            flow.setVersion(1);
            log.debug("Création nouveau flow pour user: {}", userId);
        }

        // Mise à jour des propriétés principales
        flow.setName(flowPayload.getName());
        flow.setDescription(flowPayload.getDescription());
        flow.setLanguage(flowPayload.getLanguage() != null ? flowPayload.getLanguage() : "fr");
        flow.setStatus("DRAFT");

        // Gestion de l'activation (un seul flow actif par utilisateur)
        if (Boolean.TRUE.equals(flowPayload.getActive())) {
            chatbotFlowRepository.deactivateAllUserFlows(userId);
            flow.setActive(true);
        } else {
            flow.setActive(false);
        }

        // Sauvegarder le flow principal d'abord
        flow = chatbotFlowRepository.save(flow);

        // Créer les nœuds
        createFlowNodes(flow, flowPayload);

        // Créer les variables
        createFlowVariables(flow, flowPayload);

        log.debug("Flow {} avec succès, ID: {}", isUpdate ? "modifié" : "créé", flow.getId());
        return flow;
    }

    /**
     * FONCTION 2: Récupérer le flow actuel avec toutes ses relations
     */
    @Transactional(readOnly = true)
    public FlowPayload getCurrentFlow(Long userId) {
        log.debug("Récupération flow actuel pour user: {}", userId);

        // Chercher le flow de l'utilisateur (un seul par utilisateur)
        Optional<ChatbotFlow> flowOpt = chatbotFlowRepository.findByUserId(userId);

        if (flowOpt.isEmpty()) {
            log.debug("Aucun flow trouvé pour user: {}", userId);
            return null;
        }

        ChatbotFlow flow = flowOpt.get();
        log.debug("Flow trouvé: {} (version {})", flow.getName(), flow.getVersion());

        // Construire le payload de réponse
        FlowPayload payload = new FlowPayload();
        payload.setPartnerId(flow.getUserId()); // Par défaut
        payload.setFlowId(flow.getId().toString());
        payload.setName(flow.getName());
        payload.setDescription(flow.getDescription());
        payload.setActive(flow.getActive());
        payload.setLanguage(flow.getLanguage());

        // Convertir les nœuds
        payload.setNodes(convertNodesToPayload(flow.getNodes()));

        // Convertir les variables
        payload.setVariables(convertVariablesToPayload(flow.getVariables()));

        return payload;
    }

    // ================================
    // MÉTHODES PRIVÉES DE CONVERSION
    // ================================

    private void clearFlowRelations(ChatbotFlow flow) {
        // Nettoyer les anciennes relations pour la mise à jour
        flow.getNodes().clear();
        flow.getVariables().clear();
    }

    private void createFlowNodes(ChatbotFlow flow, FlowPayload payload) {
        if (payload.getNodes() == null) return;

        for (FlowNodePayload nodePayload : payload.getNodes()) {
            FlowNode node = new FlowNode();

            // Propriétés de base
            node.setFlow(flow);
            node.setNodeId(nodePayload.getId());
            node.setNodeType(nodePayload.getType());
            node.setNodeOrder(nodePayload.getOrder());
            node.setPositionX(nodePayload.getX());
            node.setPositionY(nodePayload.getY());
            node.setLabel(nodePayload.getLabel());
            node.setNextNodeId(nodePayload.getNextNodeId());

            // Données du nœud
            if (nodePayload.getData() != null) {
                NodeDataPayload data = nodePayload.getData();
                node.setWaitForUserResponse(data.getWaitForUserResponse());
                node.setTextContent(data.getText());
                node.setImageUrl(data.getImageUrl());
                node.setFileUrl(data.getFileUrl());
                node.setFileName(data.getFileName());
                node.setWebhookUrl(data.getWebhookUrl());
                node.setMinValue(data.getMinValue());
                node.setMaxValue(data.getMaxValue());
                node.setMaxFileSize(data.getMaxFileSize());
                node.setAllowedExtensions(data.getAllowedExtensions());
                node.setMinLength(data.getMinLength());
                node.setMaxLength(data.getMaxLength());
                node.setWebhookMethod(data.getMethod());
                node.setResponseType(data.getResponseType());
                node.setIsRequired(data.getRequired());
                node.setTimeoutSeconds(data.getTimeoutSeconds());
                node.setUseVariables(data.getUseVariables());
                node.setStoreInVariable(data.getStoreInVariable());

                // Variables
                node.setVariableName(data.getVariableName());
                node.setVariableValue(data.getVariableValue());
                node.setVariableOperation(data.getVariableOperation());

                // Conditions
                node.setConditionType(data.getConditionType());
                node.setConditionVariable(data.getVariable());
                node.setConditionOperator(data.getOperator());
                node.setConditionValue(data.getValue());
                node.setDefaultNextNodeId(data.getDefaultNextNodeId());

                // Créer les boutons
                createNodeButtons(node, data.getButtons());

                // Créer les items de liste
                createNodeListItems(node, data.getItems());

                // Créer les connexions conditionnelles
                createConditionalConnections(node, data.getConditionalConnections());
            }

            flow.addNode(node);
        }
    }

    private void createNodeButtons(FlowNode node, List<ButtonPayload> buttons) {
        if (buttons == null) return;

        for (int i = 0; i < buttons.size(); i++) {
            ButtonPayload buttonPayload = buttons.get(i);

            NodeButton button = new NodeButton();
            button.setNode(node);
            button.setButtonId(buttonPayload.getId());
            button.setText(buttonPayload.getText());
            button.setValue(buttonPayload.getValue());
            button.setNextNodeId(buttonPayload.getNextNodeId());
            button.setStoreInVariable(buttonPayload.getStoreInVariable());
            button.setButtonOrder(i + 1);

            node.getButtons().add(button);
        }
    }

    private void createNodeListItems(FlowNode node, List<ListItemPayload> items) {
        if (items == null) return;

        for (int i = 0; i < items.size(); i++) {
            ListItemPayload itemPayload = items.get(i);

            NodeListItem item = new NodeListItem();
            item.setNode(node);
            item.setItemId(itemPayload.getId());
            item.setTitle(itemPayload.getTitle());
            item.setDescription(itemPayload.getDescription());
            item.setValue(itemPayload.getValue());
            item.setNextNodeId(itemPayload.getNextNodeId());
            item.setStoreInVariable(itemPayload.getStoreInVariable());
            item.setItemOrder(i + 1);

            node.getListItems().add(item);
        }
    }

    private void createConditionalConnections(FlowNode node, List<ConditionalConnectionPayload> connections) {
        if (connections == null) return;

        for (int i = 0; i < connections.size(); i++) {
            ConditionalConnectionPayload connPayload = connections.get(i);

            ConditionalConnection connection = new ConditionalConnection();
            connection.setNode(node);
            connection.setConnectionId(connPayload.getId());
            connection.setConditionValue(connPayload.getCondition());
            connection.setNextNodeId(connPayload.getNextNodeId());
            connection.setLabel(connPayload.getLabel());
            connection.setOperator(connPayload.getOperator());
            connection.setConnectionOrder(i + 1);

            node.getConditionalConnections().add(connection);
        }
    }

    private void createFlowVariables(ChatbotFlow flow, FlowPayload payload) {
        if (payload.getVariables() == null) return;

        for (VariablePayload varPayload : payload.getVariables()) {
            FlowVariable variable = new FlowVariable();
            variable.setFlow(flow);
            variable.setName(varPayload.getName());
            variable.setDefaultValue(varPayload.getValue() != null ? varPayload.getValue().toString() : null);
            variable.setVariableType(varPayload.getType());
            variable.setDescription(varPayload.getDescription());
            variable.setIsSystem(varPayload.getIsSystem());

            flow.addVariable(variable);
        }
    }

    // ================================
    // CONVERSION ENTITÉS → PAYLOAD
    // ================================

    private List<FlowNodePayload> convertNodesToPayload(List<FlowNode> nodes) {
        return nodes.stream().map(this::convertNodeToPayload).toList();
    }

    private FlowNodePayload convertNodeToPayload(FlowNode node) {
        FlowNodePayload payload = new FlowNodePayload();
        payload.setId(node.getNodeId());
        payload.setType(node.getNodeType());
        payload.setX(node.getPositionX());
        payload.setY(node.getPositionY());
        payload.setOrder(node.getNodeOrder());
        payload.setLabel(node.getLabel());
        payload.setNextNodeId(node.getNextNodeId());

        // Données du nœud
        NodeDataPayload data = new NodeDataPayload();
        data.setText(node.getTextContent());
        data.setImageUrl(node.getImageUrl());
        data.setFileUrl(node.getFileUrl());
        data.setFileName(node.getFileName());
        data.setWebhookUrl(node.getWebhookUrl());
        data.setMethod(node.getWebhookMethod());
        data.setResponseType(node.getResponseType());
        data.setRequired(node.getIsRequired());
        data.setTimeoutSeconds(node.getTimeoutSeconds());
        data.setUseVariables(node.getUseVariables());
        data.setStoreInVariable(node.getStoreInVariable());
        data.setMaxValue(node.getMaxValue());
        data.setMaxLength(node.getMaxLength());
        data.setMinLength(node.getMinLength());
        data.setMinValue(node.getMinValue());
        data.setMaxFileSize(node.getMaxFileSize());
        data.setAllowedExtensions(node.getAllowedExtensions());
        // Variables
        data.setVariableName(node.getVariableName());
        data.setVariableValue(node.getVariableValue());
        data.setVariableOperation(node.getVariableOperation());

        // Conditions
        data.setConditionType(node.getConditionType());
        data.setVariable(node.getConditionVariable());
        data.setOperator(node.getConditionOperator());
        data.setValue(node.getConditionValue());
        data.setDefaultNextNodeId(node.getDefaultNextNodeId());

        // Boutons
        data.setButtons(node.getButtons().stream().map(this::convertButtonToPayload).toList());

        // Items de liste
        data.setItems(node.getListItems().stream().map(this::convertListItemToPayload).toList());

        // Connexions conditionnelles
        data.setConditionalConnections(node.getConditionalConnections().stream().map(this::convertConnectionToPayload).toList());

        payload.setData(data);
        return payload;
    }

    private ButtonPayload convertButtonToPayload(NodeButton button) {
        ButtonPayload payload = new ButtonPayload();
        payload.setId(button.getButtonId());
        payload.setText(button.getText());
        payload.setValue(button.getValue());
        payload.setNextNodeId(button.getNextNodeId());
        payload.setStoreInVariable(button.getStoreInVariable());
        return payload;
    }

    private ListItemPayload convertListItemToPayload(NodeListItem item) {
        ListItemPayload payload = new ListItemPayload();
        payload.setId(item.getItemId());
        payload.setTitle(item.getTitle());
        payload.setDescription(item.getDescription());
        payload.setValue(item.getValue());
        payload.setNextNodeId(item.getNextNodeId());
        payload.setStoreInVariable(item.getStoreInVariable());
        return payload;
    }

    private ConditionalConnectionPayload convertConnectionToPayload(ConditionalConnection connection) {
        ConditionalConnectionPayload payload = new ConditionalConnectionPayload();
        payload.setId(connection.getConnectionId());
        payload.setCondition(connection.getConditionValue());
        payload.setNextNodeId(connection.getNextNodeId());
        payload.setLabel(connection.getLabel());
        payload.setOperator(connection.getOperator());
        return payload;
    }

    private List<VariablePayload> convertVariablesToPayload(List<FlowVariable> variables) {
        return variables.stream().map(this::convertVariableToPayload).toList();
    }

    private VariablePayload convertVariableToPayload(FlowVariable variable) {
        VariablePayload payload = new VariablePayload();
        payload.setName(variable.getName());
        payload.setValue(variable.getDefaultValue());
        payload.setType(variable.getVariableType());
        payload.setDescription(variable.getDescription());
        payload.setIsSystem(variable.getIsSystem());
        return payload;
    }
}
