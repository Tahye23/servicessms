package com.example.myproject.service;

import com.example.myproject.domain.ChatbotFlow;
import com.example.myproject.domain.ChatbotSession;
import com.example.myproject.repository.ChatbotFlowRepository;
import com.example.myproject.repository.ChatbotSessionRepository;
import com.example.myproject.repository.ConfigurationRepository;
import com.example.myproject.service.dto.flow.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ChatbotFlowExecutionService {

    private final Logger log = LoggerFactory.getLogger(ChatbotFlowExecutionService.class);
    private final ChatbotFlowRepository chatbotFlowRepository;
    private final ChatbotSessionRepository chatbotSessionRepository;
    private final ConfigurationRepository configurationRepository;
    private final ObjectMapper objectMapper;

    // Patterns pour validation
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[+]?[0-9]{8,15}$");

    public ChatbotFlowExecutionService(
        ChatbotFlowRepository chatbotFlowRepository,
        ChatbotSessionRepository chatbotSessionRepository,
        ConfigurationRepository configurationRepository,
        ObjectMapper objectMapper
    ) {
        this.chatbotFlowRepository = chatbotFlowRepository;
        this.chatbotSessionRepository = chatbotSessionRepository;
        this.configurationRepository = configurationRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Exécuter un nœud du flow avec support des médias
     */
    private WhatsAppResponse executeNode(
        FlowNode node,
        FlowData flowData,
        ChatbotSession session,
        String userInput,
        String messageType,
        String mediaId
    ) {
        switch (node.getType()) {
            case "start":
            case "message":
                return executeMessageNode(node, flowData, session);
            case "buttons":
                return executeButtonsNode(node, flowData, session, userInput);
            case "list":
                return executeListNode(node, flowData, session, userInput);
            case "language":
                return executeLanguageNode(node, flowData, session, userInput);
            case "condition":
                return executeConditionNode(node, flowData, session);
            case "image":
                return executeImageNode(node, flowData, session);
            case "file":
                return executeFileNode(node, flowData, session, messageType, mediaId);
            case "contact":
                return executeContactNode(node, flowData, session, userInput, messageType);
            case "webhook":
                return executeWebhookNode(node, flowData, session);
            case "delay":
                return executeDelayNode(node, flowData, session);
            case "end":
                return executeEndNode(node, session);
            default:
                return createTextResponse("Type de nœud non supporté: " + node.getType());
        }
    }

    /**
     * Exécuter un nœud image
     */
    private WhatsAppResponse executeImageNode(FlowNode node, FlowData flowData, ChatbotSession session) {
        NodeData data = node.getData();

        if (data.getMediaId() == null || data.getMediaId().isEmpty()) {
            log.warn("Nœud image {} sans media ID configuré", node.getId());
            return createTextResponse("❌ Image non configurée dans ce nœud.");
        }

        // Passer au nœud suivant
        String nextNodeId = getNextNodeId(node);
        if (nextNodeId != null) {
            session.setCurrentNodeId(nextNodeId);
        }

        // Créer la réponse image
        WhatsAppResponse response = new WhatsAppResponse();
        response.setType("image");
        response.setMediaId(data.getMediaId());
        response.setCaption(data.getCaption());

        return response;
    }

    /**
     * Exécuter un nœud fichier (attendre un fichier de l'utilisateur)
     */
    private WhatsAppResponse executeFileNode(FlowNode node, FlowData flowData, ChatbotSession session, String messageType, String mediaId) {
        NodeData data = node.getData();

        // Si c'est la première fois, demander le fichier
        if (messageType == null || isFirstVisit(session, node.getId())) {
            String allowedTypesText = "";
            if (data.getAllowedTypes() != null && !data.getAllowedTypes().isEmpty()) {
                allowedTypesText = "\n📎 Types acceptés: " + String.join(", ", data.getAllowedTypes());
            }

            return createTextResponse((data.getText() != null ? data.getText() : "Veuillez envoyer votre fichier") + allowedTypesText);
        }

        // Vérifier si l'utilisateur a envoyé un fichier
        if (
            !"document".equals(messageType) && !"image".equals(messageType) && !"audio".equals(messageType) && !"video".equals(messageType)
        ) {
            return createTextResponse("❌ Veuillez envoyer un fichier, pas un message texte.");
        }

        // Vérifier le type de fichier si spécifié
        if (data.getAllowedTypes() != null && !data.getAllowedTypes().isEmpty()) {
            boolean typeAllowed = data.getAllowedTypes().stream().anyMatch(type -> messageType.toLowerCase().contains(type.toLowerCase()));

            if (!typeAllowed) {
                return createTextResponse("❌ Type de fichier non autorisé. Types acceptés: " + String.join(", ", data.getAllowedTypes()));
            }
        }

        // Sauvegarder le média ID dans les variables
        try {
            Map<String, Object> variables = objectMapper.readValue(session.getVariables(), Map.class);
            variables.put("file.mediaId", mediaId);
            variables.put("file.type", messageType);
            session.setVariables(objectMapper.writeValueAsString(variables));
        } catch (Exception e) {
            log.warn("Erreur lors de la sauvegarde du fichier: {}", e.getMessage());
        }

        // Passer au nœud suivant
        String nextNodeId = getNextNodeId(node);
        if (nextNodeId != null) {
            session.setCurrentNodeId(nextNodeId);
            FlowNode nextNode = findNodeById(flowData, nextNodeId);
            if (nextNode != null) {
                return executeNode(nextNode, flowData, session, null, null, null);
            }
        }

        return createTextResponse("✅ Fichier reçu avec succès !");
    }

    /**
     * Exécuter un nœud contact (demander les informations de contact)
     */
    private WhatsAppResponse executeContactNode(
        FlowNode node,
        FlowData flowData,
        ChatbotSession session,
        String userInput,
        String messageType
    ) {
        NodeData data = node.getData();

        // Vérifier l'état de collecte des infos
        String collectingState = getSessionVariable(session, "contact.collecting", "");

        if (collectingState.isEmpty()) {
            // Commencer la collecte
            if (Boolean.TRUE.equals(data.getRequestName())) {
                setSessionVariable(session, "contact.collecting", "name");
                return createTextResponse("👤 Veuillez indiquer votre nom :");
            } else if (Boolean.TRUE.equals(data.getRequestPhone())) {
                setSessionVariable(session, "contact.collecting", "phone");
                return createTextResponse("📞 Veuillez indiquer votre numéro de téléphone :");
            } else if (Boolean.TRUE.equals(data.getRequestEmail())) {
                setSessionVariable(session, "contact.collecting", "email");
                return createTextResponse("📧 Veuillez indiquer votre adresse email :");
            }
        }

        // Traiter la réponse selon l'état
        switch (collectingState) {
            case "name":
                if (userInput == null || userInput.trim().length() < 2) {
                    return createTextResponse("❌ Veuillez indiquer un nom valide (au moins 2 caractères) :");
                }
                setSessionVariable(session, "contact.name", userInput.trim());

                // Passer au suivant
                if (Boolean.TRUE.equals(data.getRequestPhone())) {
                    setSessionVariable(session, "contact.collecting", "phone");
                    return createTextResponse("📞 Veuillez indiquer votre numéro de téléphone :");
                } else if (Boolean.TRUE.equals(data.getRequestEmail())) {
                    setSessionVariable(session, "contact.collecting", "email");
                    return createTextResponse("📧 Veuillez indiquer votre adresse email :");
                }
                break;
            case "phone":
                if (userInput == null || !PHONE_PATTERN.matcher(userInput.trim()).matches()) {
                    return createTextResponse("❌ Veuillez indiquer un numéro de téléphone valide :");
                }
                setSessionVariable(session, "contact.phone", userInput.trim());

                // Passer au suivant
                if (Boolean.TRUE.equals(data.getRequestEmail())) {
                    setSessionVariable(session, "contact.collecting", "email");
                    return createTextResponse("📧 Veuillez indiquer votre adresse email :");
                }
                break;
            case "email":
                if (userInput == null || !EMAIL_PATTERN.matcher(userInput.trim()).matches()) {
                    return createTextResponse("❌ Veuillez indiquer une adresse email valide :");
                }
                setSessionVariable(session, "contact.email", userInput.trim());
                break;
        }

        // Terminer la collecte
        setSessionVariable(session, "contact.collecting", "completed");

        // Construire le message de confirmation
        StringBuilder confirmation = new StringBuilder("✅ Informations enregistrées :\n");
        String name = getSessionVariable(session, "contact.name", null);
        String phone = getSessionVariable(session, "contact.phone", null);
        String email = getSessionVariable(session, "contact.email", null);

        if (name != null) confirmation.append("👤 Nom: ").append(name).append("\n");
        if (phone != null) confirmation.append("📞 Téléphone: ").append(phone).append("\n");
        if (email != null) confirmation.append("📧 Email: ").append(email).append("\n");

        // Passer au nœud suivant
        String nextNodeId = getNextNodeId(node);
        if (nextNodeId != null) {
            session.setCurrentNodeId(nextNodeId);
        }

        return createTextResponse(confirmation.toString());
    }

    /**
     * Exécuter un nœud webhook
     */
    private WhatsAppResponse executeWebhookNode(FlowNode node, FlowData flowData, ChatbotSession session) {
        NodeData data = node.getData();

        // TODO: Implémenter l'appel webhook réel
        log.info("Webhook call to: {} (method: {})", data.getWebhookUrl(), data.getMethod());

        // Simuler l'appel pour l'instant
        String nextNodeId = getNextNodeId(node);
        if (nextNodeId != null) {
            session.setCurrentNodeId(nextNodeId);
        }

        return createTextResponse("🔗 Webhook exécuté avec succès");
    }

    /**
     * Exécuter un nœud délai
     */
    private WhatsAppResponse executeDelayNode(FlowNode node, FlowData flowData, ChatbotSession session) {
        NodeData data = node.getData();

        // Pour l'instant, passer directement au suivant (le délai serait géré côté client)
        String nextNodeId = getNextNodeId(node);
        if (nextNodeId != null) {
            session.setCurrentNodeId(nextNodeId);
            FlowNode nextNode = findNodeById(flowData, nextNodeId);
            if (nextNode != null) {
                return executeNode(nextNode, flowData, session, null, null, null);
            }
        }

        return createTextResponse("⏱️ Délai de " + (data.getDelay() != null ? data.getDelay() : 5) + " secondes...");
    }

    /**
     * Exécuter un nœud message
     */
    private WhatsAppResponse executeMessageNode(FlowNode node, FlowData flowData, ChatbotSession session) {
        String text = node.getData().getText();
        if (text == null || text.isEmpty()) {
            text = "Message vide";
        }

        log.debug("Exécution nœud message: {} (ID: {})", text, node.getId());

        // Pour le nœud start, passer automatiquement au suivant
        if ("start".equals(node.getType())) {
            String nextNodeId = getNextNodeId(node);
            if (nextNodeId != null) {
                session.setCurrentNodeId(nextNodeId);
                session.setLastInteraction(Instant.now());

                // Exécuter immédiatement le nœud suivant
                FlowNode nextNode = findNodeById(flowData, nextNodeId);
                if (nextNode != null) {
                    log.debug("Passage automatique au nœud suivant: {} (type: {})", nextNodeId, nextNode.getType());
                    return executeNode(nextNode, flowData, session, null, null, null);
                }
            }
        } else {
            // Pour les autres nœuds message, passer au suivant après délai
            String nextNodeId = getNextNodeId(node);
            if (nextNodeId != null) {
                session.setCurrentNodeId(nextNodeId);
            }
        }

        return createTextResponse(text);
    }

    /**
     * Exécuter un nœud boutons
     */
    private WhatsAppResponse executeButtonsNode(FlowNode node, FlowData flowData, ChatbotSession session, String userInput) {
        NodeData data = node.getData();
        log.debug("Exécution nœud boutons: {} boutons disponibles", data.getButtons() != null ? data.getButtons().size() : 0);

        // Si c'est la première fois (pas d'input utilisateur), envoyer les boutons
        if (userInput == null || isFirstVisit(session, node.getId())) {
            log.debug("Première visite du nœud boutons, envoi des options");
            if (data.getButtons() == null || data.getButtons().isEmpty()) {
                log.warn("Aucun bouton configuré pour le nœud {}", node.getId());
                return createTextResponse("Aucune option disponible.");
            }
            return createButtonsResponse(data.getText(), data.getButtons());
        }

        // Sinon, traiter la réponse de l'utilisateur
        log.debug("Traitement de la réponse utilisateur: {}", userInput);
        ButtonOption selectedButton = findButtonByText(data.getButtons(), userInput);
        if (selectedButton != null) {
            log.debug("Bouton sélectionné: {}", selectedButton.getText());

            String nextNodeId = selectedButton.getNextNodeId();
            if (nextNodeId == null) {
                nextNodeId = getNextNodeId(node);
            }

            if (nextNodeId != null) {
                session.setCurrentNodeId(nextNodeId);
                // Exécuter immédiatement le nœud suivant
                FlowNode nextNode = findNodeById(flowData, nextNodeId);
                if (nextNode != null) {
                    log.debug("Passage au nœud suivant: {}", nextNodeId);
                    return executeNode(nextNode, flowData, session, null, null, null);
                }
            }

            return createTextResponse("Merci pour votre choix: " + selectedButton.getText());
        } else {
            log.debug("Réponse invalide, renvoi des boutons");
            // Réponse invalide, renvoyer les boutons
            return createButtonsResponse("Veuillez choisir une option valide:", data.getButtons());
        }
    }

    /**
     * Exécuter un nœud liste
     */
    private WhatsAppResponse executeListNode(FlowNode node, FlowData flowData, ChatbotSession session, String userInput) {
        NodeData data = node.getData();

        if (userInput == null || isFirstVisit(session, node.getId())) {
            return createListResponse(data.getText(), data.getItems());
        }

        ListItem selectedItem = findListItemByTitle(data.getItems(), userInput);
        if (selectedItem != null) {
            String nextNodeId = selectedItem.getNextNodeId();
            if (nextNodeId == null) {
                nextNodeId = getNextNodeId(node);
            }

            if (nextNodeId != null) {
                session.setCurrentNodeId(nextNodeId);
                FlowNode nextNode = findNodeById(flowData, nextNodeId);
                if (nextNode != null) {
                    return executeNode(nextNode, flowData, session, null, null, null);
                }
            }

            return createTextResponse("Merci pour votre choix: " + selectedItem.getTitle());
        } else {
            return createListResponse("Veuillez choisir une option valide:", data.getItems());
        }
    }

    /**
     * Exécuter un nœud sélection de langue
     */
    private WhatsAppResponse executeLanguageNode(FlowNode node, FlowData flowData, ChatbotSession session, String userInput) {
        NodeData data = node.getData();

        // Si c'est la première fois, afficher les options de langue
        if (userInput == null || isFirstVisit(session, node.getId())) {
            return createLanguageResponse(data.getText(), data.getLanguages());
        }

        // Traiter la sélection de l'utilisateur
        LanguageOption selectedLanguage = findLanguageByName(data.getLanguages(), userInput);
        if (selectedLanguage != null) {
            // Sauvegarder la langue choisie dans les variables
            saveLanguageChoice(session, selectedLanguage);

            String nextNodeId = selectedLanguage.getNextNodeId();
            if (nextNodeId == null) {
                nextNodeId = getNextNodeId(node);
            }

            if (nextNodeId != null) {
                session.setCurrentNodeId(nextNodeId);
                FlowNode nextNode = findNodeById(flowData, nextNodeId);
                if (nextNode != null) {
                    return executeNode(nextNode, flowData, session, null, null, null);
                }
            }

            return createTextResponse("Langue sélectionnée: " + selectedLanguage.getName());
        } else {
            // Réponse invalide, renvoyer les options
            return createLanguageResponse("Veuillez choisir une langue valide:", data.getLanguages());
        }
    }

    /**
     * Exécuter un nœud condition
     */
    private WhatsAppResponse executeConditionNode(FlowNode node, FlowData flowData, ChatbotSession session) {
        NodeData data = node.getData();
        boolean conditionResult = evaluateCondition(data, session);

        String nextNodeId = conditionResult ? data.getTrueAction() : data.getFalseAction();
        if (nextNodeId != null) {
            session.setCurrentNodeId(nextNodeId);
            FlowNode nextNode = findNodeById(flowData, nextNodeId);
            if (nextNode != null) {
                return executeNode(nextNode, flowData, session, null, null, null);
            }
        }

        return createTextResponse("Condition évaluée: " + (conditionResult ? "VRAI" : "FAUX"));
    }

    /**
     * Exécuter un nœud de fin
     */
    private WhatsAppResponse executeEndNode(FlowNode node, ChatbotSession session) {
        String text = node.getData().getText();
        if (text == null || text.isEmpty()) {
            text = "Conversation terminée. Merci !";
        }

        // Désactiver la session
        session.setIsActive(false);

        return createTextResponse(text + "\n\n💬 Tapez n'importe quoi pour recommencer.");
    }

    private boolean evaluateCondition(NodeData data, ChatbotSession session) {
        try {
            Map<String, Object> variables = objectMapper.readValue(session.getVariables(), Map.class);
            Object value = variables.get(data.getVariable());
            String strValue = value != null ? value.toString() : "";
            String expectedValue = data.getValue() != null ? data.getValue() : "";

            switch (data.getOperator()) {
                case "equals":
                    return strValue.equals(expectedValue);
                case "not_equals":
                    return !strValue.equals(expectedValue);
                case "contains":
                    return strValue.contains(expectedValue);
                case "not_contains":
                    return !strValue.contains(expectedValue);
                case "starts_with":
                    return strValue.startsWith(expectedValue);
                case "ends_with":
                    return strValue.endsWith(expectedValue);
                case "is_empty":
                    return strValue.isEmpty();
                case "is_not_empty":
                    return !strValue.isEmpty();
                case "greater_than":
                    try {
                        double numValue = Double.parseDouble(strValue);
                        double numExpected = Double.parseDouble(expectedValue);
                        return numValue > numExpected;
                    } catch (NumberFormatException e) {
                        return false;
                    }
                case "less_than":
                    try {
                        double numValue = Double.parseDouble(strValue);
                        double numExpected = Double.parseDouble(expectedValue);
                        return numValue < numExpected;
                    } catch (NumberFormatException e) {
                        return false;
                    }
                case "regex":
                    try {
                        return Pattern.compile(expectedValue).matcher(strValue).matches();
                    } catch (Exception e) {
                        return false;
                    }
                default:
                    return false;
            }
        } catch (Exception e) {
            log.warn("Erreur lors de l'évaluation de la condition: {}", e.getMessage());
            return false;
        }
    }

    private String getNextNodeId(FlowNode node) {
        if (node.getConnections() != null && !node.getConnections().isEmpty()) {
            return node.getConnections().get(0).getTargetNodeId();
        }
        return null;
    }

    private boolean isFirstVisit(ChatbotSession session, String nodeId) {
        // Simple heuristique : si le nœud actuel == nodeId, c'est la première visite
        return nodeId.equals(session.getCurrentNodeId());
    }

    private String getSessionVariable(ChatbotSession session, String key, String defaultValue) {
        try {
            Map<String, Object> variables = objectMapper.readValue(session.getVariables(), Map.class);
            Object value = variables.get(key);
            return value != null ? value.toString() : defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private void setSessionVariable(ChatbotSession session, String key, String value) {
        try {
            Map<String, Object> variables = objectMapper.readValue(session.getVariables(), Map.class);
            variables.put(key, value);
            session.setVariables(objectMapper.writeValueAsString(variables));
        } catch (Exception e) {
            log.warn("Erreur lors de la définition de la variable {}: {}", key, e.getMessage());
        }
    }

    private void clearSessionVariables(ChatbotSession session) {
        try {
            Map<String, Object> variables = new HashMap<>();
            session.setVariables(objectMapper.writeValueAsString(variables));
        } catch (Exception e) {
            log.warn("Erreur lors de la réinitialisation des variables: {}", e.getMessage());
        }
    }

    // ================================
    // MÉTHODES DE CRÉATION DE RÉPONSES WHATSAPP
    // ================================

    private WhatsAppResponse createTextResponse(String text) {
        WhatsAppResponse response = new WhatsAppResponse();
        response.setType("text");
        response.setText(text);
        return response;
    }

    private WhatsAppResponse createButtonsResponse(String text, List<ButtonOption> buttons) {
        WhatsAppResponse response = new WhatsAppResponse();
        response.setType("buttons");
        response.setText(text);
        response.setButtons(buttons != null ? buttons : new ArrayList<>());
        return response;
    }

    private WhatsAppResponse createListResponse(String text, List<ListItem> items) {
        WhatsAppResponse response = new WhatsAppResponse();
        response.setType("list");
        response.setText(text);
        response.setItems(items != null ? items : new ArrayList<>());
        return response;
    }

    /**
     * Créer une réponse pour la sélection de langue
     */
    private WhatsAppResponse createLanguageResponse(String text, List<LanguageOption> languages) {
        if (languages == null || languages.isEmpty()) {
            return createTextResponse("Aucune langue disponible");
        }

        // Si 3 langues ou moins, utiliser des boutons
        if (languages.size() <= 3) {
            List<ButtonOption> buttons = languages
                .stream()
                .map(lang -> {
                    ButtonOption btn = new ButtonOption();
                    btn.setId(lang.getCode());
                    btn.setText(lang.getFlag() + " " + lang.getName());
                    return btn;
                })
                .collect(Collectors.toList());
            return createButtonsResponse(text, buttons);
        } else {
            // Sinon utiliser une liste
            List<ListItem> items = languages
                .stream()
                .map(lang -> {
                    ListItem item = new ListItem();
                    item.setId(lang.getCode());
                    item.setTitle(lang.getFlag() + " " + lang.getName());
                    item.setDescription("Choisir " + lang.getName());
                    return item;
                })
                .collect(Collectors.toList());
            return createListResponse(text, items);
        }
    }

    /**
     * Sauvegarder le choix de langue dans les variables de session
     */
    private void saveLanguageChoice(ChatbotSession session, LanguageOption language) {
        try {
            Map<String, Object> variables = objectMapper.readValue(session.getVariables(), Map.class);
            variables.put("user.language", language.getCode());
            variables.put("user.languageName", language.getName());
            variables.put("user.languageFlag", language.getFlag());
            session.setVariables(objectMapper.writeValueAsString(variables));

            log.debug("Langue sauvegardée pour session {}: {} ({})", session.getId(), language.getName(), language.getCode());
        } catch (Exception e) {
            log.warn("Erreur lors de la sauvegarde du choix de langue: {}", e.getMessage());
        }
    }

    private FlowNode findNodeById(FlowData flowData, String nodeId) {
        return flowData.getNodes().stream().filter(node -> nodeId.equals(node.getId())).findFirst().orElse(null);
    }

    private FlowNode findStartNode(FlowData flowData) {
        return flowData.getNodes().stream().filter(node -> "start".equals(node.getType())).findFirst().orElse(null);
    }

    private String findStartNodeId(String userLogin) {
        try {
            Long userId = getUserIdByLogin(userLogin);
            /*Optional<ChatbotFlow> flowOpt = chatbotFlowRepository.findByUserIdAndActiveTrue(userId);
            if (flowOpt.isPresent()) {
                FlowData flowData = parseFlowData(flowOpt.get().getFlowData());
                FlowNode startNode = findStartNode(flowData);
                return startNode != null ? startNode.getId() : "start_node";
            }*/
            return null;
        } catch (Exception e) {
            log.warn("Erreur lors de la recherche du nœud de démarrage: {}", e.getMessage());
        }
        return "start_node";
    }

    private ButtonOption findButtonByText(List<ButtonOption> buttons, String text) {
        if (buttons == null) return null;
        return buttons.stream().filter(button -> text.equals(button.getText())).findFirst().orElse(null);
    }

    private ListItem findListItemByTitle(List<ListItem> items, String title) {
        if (items == null) return null;
        return items.stream().filter(item -> title.equals(item.getTitle())).findFirst().orElse(null);
    }

    /**
     * Trouver une langue par nom ou flag
     */
    private LanguageOption findLanguageByName(List<LanguageOption> languages, String input) {
        if (languages == null || input == null) return null;

        return languages
            .stream()
            .filter(
                lang ->
                    input.contains(lang.getName()) ||
                    input.contains(lang.getFlag()) ||
                    input.contains(lang.getCode()) ||
                    (lang.getFlag() + " " + lang.getName()).equals(input)
            )
            .findFirst()
            .orElse(null);
    }

    private Long getUserIdByLogin(String userLogin) {
        // Vous devez implémenter cette méthode selon votre structure
        // Par exemple, via UserRepository
        return 1L; // Placeholder
    }
}
