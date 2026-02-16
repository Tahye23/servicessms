package com.example.myproject.service;

import com.example.myproject.domain.Configuration;
import com.example.myproject.domain.Contact;
import com.example.myproject.domain.Template;
import com.example.myproject.repository.ConfigurationRepository;
import com.example.myproject.repository.TemplateRepository;
import com.example.myproject.security.SecurityUtils;
import com.example.myproject.web.rest.SendSmsResource;
import com.example.myproject.web.rest.TemplateResource;
import com.example.myproject.web.rest.dto.ButtonRequest;
import com.example.myproject.web.rest.dto.ComponentRequest;
import com.example.myproject.web.rest.dto.TemplateRequest;
import com.example.myproject.web.rest.dto.VariableDTO;
import com.example.myproject.web.rest.errors.BadRequestAlertException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.persistence.EntityNotFoundException;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
@Transactional
public class TemplateService {

    private final Logger log = LoggerFactory.getLogger(TemplateService.class);
    private final TemplateRepository templateRepository;
    private static final Pattern VAR_PATTERN = Pattern.compile("\\{\\{([^:}]+)(?::([^}]*))?\\}\\}");
    private final ObjectMapper mapper = new ObjectMapper();
    private final RestTemplate rest = new RestTemplate();
    private final ConfigurationRepository configurationRepository;
    private static final String GRAPH_URL = "https://graph.facebook.com/v22.0";
    private static final String GRAPH_API_PREFIX = "https://graph.facebook.com/v22.0/";

    @Autowired
    private RestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public TemplateService(TemplateRepository templateRepository, ConfigurationRepository configurationRepository) {
        this.templateRepository = templateRepository;
        this.configurationRepository = configurationRepository;
    }

    public Template save(Template template) {
        return templateRepository.save(template);
    }

    public List<Template> findAll() {
        return templateRepository.findAll();
    }

    public Optional<Template> findOne(Long id) {
        return templateRepository.findById(id);
    }

    public Optional<Template> findByTemplateId(String templateId) {
        return templateRepository.findByTemplateId(templateId);
    }

    public void delete(Long id) {
        templateRepository.deleteById(id);
    }

    private String getCurrentLoginOrThrow() {
        return SecurityUtils.getCurrentUserLogin().orElseThrow(() -> new IllegalStateException("Non authentifié"));
    }

    @Transactional
    public Template approveTemplate(Long id, Boolean approved) throws JsonProcessingException {
        Template template = templateRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Template non trouvé pour l'id : " + id));

        // Si expediteur est présent => on approuve directement, sans appeler Meta
        if (template.getExpediteur() != null && !String.valueOf(template.getExpediteur()).trim().isEmpty()) {
            if (!Boolean.TRUE.equals(template.getApproved())) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                String currentDate = LocalDateTime.now().format(formatter);
                template.setApproved_at(currentDate);
            }
            template.setApproved(true); // forcer l'approbation
            template.setStatus("APPROVED"); // statut aligné
            return templateRepository.save(template);
        }

        // ----- Sinon, expediteur est null => on suit le process Meta existant -----

        // 1) Vérifier le statut Meta via API (similaire à refreshUnapprovedTemplates)
        Configuration cfg = configurationRepository
            .findOneByUserLogin(template.getUser_id())
            .orElseThrow(() -> new BadRequestAlertException("Partner has no WhatsApp configuration", "whatsapp", "noconfig"));

        // Construire requête batch GET pour ce template uniquement
        ArrayNode batch = mapper.createArrayNode();
        ObjectNode call = mapper.createObjectNode();
        call.put("method", "GET");
        call.put("relative_url", template.getTemplateId() + "?fields=status");
        batch.add(call);

        HttpHeaders hdr = new HttpHeaders();
        hdr.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("access_token", cfg.getAccessToken());
        form.add("batch", mapper.writeValueAsString(batch));

        String raw = rest.postForObject(GRAPH_URL, new HttpEntity<>(form, hdr), String.class);
        ArrayNode responses = (ArrayNode) mapper.readTree(raw);

        if (responses.isEmpty()) {
            throw new IllegalStateException("Aucune réponse de Meta pour la vérification du template");
        }

        JsonNode resp = responses.get(0);
        if (resp.path("code").asInt() != 200) {
            throw new IllegalStateException("Erreur lors de la vérification Meta du template");
        }

        JsonNode body = mapper.readTree(resp.path("body").asText());
        String statusMeta = body.path("status").asText();

        boolean isApprovedByMeta = "APPROVED".equalsIgnoreCase(statusMeta);

        // 2) Si pas approuvé par Meta, on refuse l'approbation locale
        if (!isApprovedByMeta) {
            throw new IllegalStateException("La template n'est pas encore approuvée par Meta (status: " + statusMeta + ")");
        }

        // 3) Sinon, on approuve localement la template (ou la rejette selon 'approved')
        if (!Boolean.TRUE.equals(template.getApproved())) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String currentDate = LocalDateTime.now().format(formatter);
            template.setApproved_at(currentDate);
        }
        template.setStatus(Boolean.TRUE.equals(approved) ? "APPROVED" : "REJECTED");
        template.setApproved(approved);

        return templateRepository.save(template);
    }

    public Template importTemplateFromMeta(String templateName, Configuration cfg, String userLogin) throws JsonProcessingException {
        log.debug("Importing template '{}' from Meta for user {}", templateName, userLogin);

        // 1. Appeler l'API Meta pour récupérer les templates
        String graphUrl = String.format("%s%s/message_templates?name=%s", GRAPH_API_PREFIX, cfg.getBusinessId(), templateName);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(cfg.getAccessToken());

        RequestEntity<Void> request = new RequestEntity<>(headers, HttpMethod.GET, URI.create(graphUrl));
        ResponseEntity<String> response = restTemplate.exchange(request, String.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new BadRequestAlertException("Impossible de récupérer le template depuis Meta", "template", "metaapierror");
        }

        // 2. Parser la réponse
        JsonNode rootNode = objectMapper.readTree(response.getBody());
        JsonNode dataArray = rootNode.get("data");

        if (dataArray == null || !dataArray.isArray() || dataArray.size() == 0) {
            throw new BadRequestAlertException("Template non trouvé dans Meta", "template", "templatenotfound");
        }

        // Prendre le premier template correspondant
        JsonNode templateData = dataArray.get(0);

        // 3. Créer l'objet Template
        Template template = new Template();
        template.setName(templateData.get("name").asText());
        template.setTemplateName(templateData.get("name").asText());
        template.setTemplateId(templateData.get("id").asText());
        template.setStatus(templateData.get("status").asText());
        template.setUser_id(userLogin);
        template.setApproved("APPROVED".equals(templateData.get("status").asText()));

        // ✅ NOUVEAUTÉ: Convertir le format Meta vers le format interne
        JsonNode componentsNode = templateData.get("components");
        if (componentsNode != null && componentsNode.isArray()) {
            // Convertir vers le format TemplateRequest
            ObjectNode normalizedContent = convertMetaFormatToInternalFormat(
                templateData.get("name").asText(),
                templateData.get("language").asText(),
                templateData.get("category").asText(),
                componentsNode
            );

            template.setContent(objectMapper.writeValueAsString(normalizedContent));
        } else {
            template.setContent("{}");
        }

        // Gérer le code (language + handles pour les médias)
        StringBuilder codeBuilder = new StringBuilder();

        if (templateData.has("language")) {
            codeBuilder.append(templateData.get("language").asText());
        }

        // Extraire les handles des médias si présents
        if (componentsNode != null && componentsNode.isArray()) {
            for (JsonNode component : componentsNode) {
                if (component.has("type") && "HEADER".equals(component.get("type").asText())) {
                    if (component.has("example") && component.get("example").has("header_handle")) {
                        JsonNode headerHandles = component.get("example").get("header_handle");
                        if (headerHandles.isArray() && headerHandles.size() > 0) {
                            if (codeBuilder.length() > 0) {
                                codeBuilder.append(",");
                            }
                            codeBuilder.append(headerHandles.get(0).asText());
                        }
                    }
                    break;
                }
            }
        }

        // Calculer le nombre de caractères
        int charCount = calculateCharacterCount(componentsNode);
        template.setCharacterCount(charCount);

        // Dates
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String currentDate = LocalDateTime.now().format(formatter);
        template.setCreated_at(currentDate);

        if (template.getApproved()) {
            template.setApproved_at(currentDate);
        }

        // 4. Sauvegarder en base de données
        Template savedTemplate = templateRepository.save(template);

        log.info(
            "Template '{}' importé avec succès depuis Meta (ID: {}, templateId: {})",
            templateName,
            savedTemplate.getId(),
            savedTemplate.getTemplateId()
        );

        return savedTemplate;
    }

    /**
     * ✅ NOUVELLE MÉTHODE: Convertit le format Meta vers le format interne TemplateRequest
     */
    private ObjectNode convertMetaFormatToInternalFormat(String name, String language, String category, JsonNode metaComponents) {
        ObjectNode result = objectMapper.createObjectNode();
        result.put("name", name);
        result.put("language", language);
        result.put("category", category);

        ArrayNode components = result.putArray("components");

        for (JsonNode metaComponent : metaComponents) {
            ObjectNode component = components.addObject();
            String type = metaComponent.get("type").asText();
            component.put("type", type);

            switch (type) {
                case "HEADER":
                    convertHeaderComponent(metaComponent, component);
                    break;
                case "BODY":
                    convertBodyComponent(metaComponent, component);
                    break;
                case "FOOTER":
                    convertFooterComponent(metaComponent, component);
                    break;
                case "BUTTONS":
                    convertButtonsComponent(metaComponent, component);
                    break;
            }
        }

        return result;
    }

    /**
     * Convertit un composant HEADER depuis Meta
     */
    private void convertHeaderComponent(JsonNode metaComponent, ObjectNode component) {
        if (metaComponent.has("format")) {
            String format = metaComponent.get("format").asText();
            component.put("format", format);

            if ("TEXT".equals(format) && metaComponent.has("text")) {
                // Convertir les placeholders Meta ({{1}}) vers le format interne ({{nom:default}})
                String text = metaComponent.get("text").asText();

                // Récupérer les exemples si disponibles
                if (metaComponent.has("example") && metaComponent.get("example").has("header_text")) {
                    JsonNode examples = metaComponent.get("example").get("header_text");
                    text = convertPlaceholdersWithExamples(text, examples);
                } else {
                    text = convertPlaceholdersSimple(text, "header");
                }

                component.put("text", text);
            }
        }
    }

    /**
     * Convertit un composant BODY depuis Meta
     */
    private void convertBodyComponent(JsonNode metaComponent, ObjectNode component) {
        if (metaComponent.has("text")) {
            String text = metaComponent.get("text").asText();

            // Récupérer les exemples si disponibles
            if (metaComponent.has("example") && metaComponent.get("example").has("body_text")) {
                JsonNode examples = metaComponent.get("example").get("body_text");
                if (examples.isArray() && examples.size() > 0) {
                    JsonNode firstArray = examples.get(0);
                    if (firstArray.isArray()) {
                        text = convertPlaceholdersWithExamples(text, firstArray);
                    }
                }
            } else {
                text = convertPlaceholdersSimple(text, "body");
            }

            component.put("text", text);
        }
    }

    /**
     * Convertit un composant FOOTER depuis Meta
     */
    private void convertFooterComponent(JsonNode metaComponent, ObjectNode component) {
        if (metaComponent.has("text")) {
            String text = metaComponent.get("text").asText();
            component.put("text", text);
        }
    }

    /**
     * Convertit un composant BUTTONS depuis Meta
     */
    private void convertButtonsComponent(JsonNode metaComponent, ObjectNode component) {
        if (metaComponent.has("buttons")) {
            ArrayNode buttons = component.putArray("buttons");
            JsonNode metaButtons = metaComponent.get("buttons");

            for (JsonNode metaButton : metaButtons) {
                ObjectNode button = buttons.addObject();
                button.put("type", metaButton.get("type").asText());
                button.put("text", metaButton.get("text").asText());

                if (metaButton.has("url")) {
                    button.put("url", metaButton.get("url").asText());
                }
                if (metaButton.has("phone_number")) {
                    button.put("phone_number", metaButton.get("phone_number").asText());
                }
            }
        }
    }

    /**
     * ✅ MÉTHODE CLÉ: Convertit les placeholders Meta ({{1}}, {{2}})
     * vers le format interne ({{nom:exemple}})
     */
    private String convertPlaceholdersWithExamples(String text, JsonNode examples) {
        if (!examples.isArray() || examples.size() == 0) {
            return convertPlaceholdersSimple(text, "var");
        }

        // Créer un mapping index → exemple
        Map<Integer, String> exampleMap = new HashMap<>();
        for (int i = 0; i < examples.size(); i++) {
            exampleMap.put(i + 1, examples.get(i).asText());
        }

        // Pattern pour trouver {{1}}, {{2}}, etc.
        Pattern pattern = Pattern.compile("\\{\\{(\\d+)\\}\\}");
        Matcher matcher = pattern.matcher(text);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            int index = Integer.parseInt(matcher.group(1));
            String example = exampleMap.getOrDefault(index, "valeur" + index);

            // Créer un nom de variable à partir de l'exemple
            String varName = createVarNameFromExample(example, index);

            // Remplacer par le format interne: {{nom:exemple}}
            String replacement = String.format("{{%s:%s}}", varName, example);
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * Convertit les placeholders sans exemples
     */
    private String convertPlaceholdersSimple(String text, String prefix) {
        Pattern pattern = Pattern.compile("\\{\\{(\\d+)\\}\\}");
        Matcher matcher = pattern.matcher(text);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            int index = Integer.parseInt(matcher.group(1));
            String varName = prefix + index;
            String replacement = String.format("{{%s:}}", varName);
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * Crée un nom de variable à partir d'un exemple
     */
    private String createVarNameFromExample(String example, int index) {
        // Nettoyer l'exemple pour en faire un nom de variable
        String cleaned = example.toLowerCase().replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");

        // Si l'exemple est vide ou trop long, utiliser un nom par défaut
        if (cleaned.isEmpty() || cleaned.length() > 20) {
            return "var" + index;
        }

        // Limiter à 15 caractères
        if (cleaned.length() > 15) {
            cleaned = cleaned.substring(0, 15);
        }

        return cleaned;
    }

    /**
     * Calcule le nombre de caractères dans le body
     */
    private int calculateCharacterCount(JsonNode components) {
        if (components == null || !components.isArray()) {
            return 0;
        }

        for (JsonNode component : components) {
            if (component.has("type") && "BODY".equals(component.get("type").asText())) {
                if (component.has("text")) {
                    return component.get("text").asText().length();
                }
            }
        }

        return 0;
    }

    public List<Template> findAllByUserId(Long userId) {
        return templateRepository.findAllByUser(userId);
    }

    // Méthodes existantes
    public Page<Template> findTemplatesByApprovedAndSearch(String approved, String search, Pageable pageable) {
        if (search != null && !search.isEmpty()) {
            return templateRepository.findByApprovedAndNameContainingIgnoreCase(approved, search, pageable);
        } else {
            return templateRepository.findByApproved(approved, pageable);
        }
    }

    public Page<Template> findTemplatesByUserAndApprovedAndSearch(String userId, String approved, String search, Pageable pageable) {
        if (search != null && !search.isEmpty()) {
            return templateRepository.findByUserIdAndApprovedAndNameContainingIgnoreCase(userId, approved, search, pageable);
        } else {
            return templateRepository.findByUserIdAndApproved(userId, approved, pageable);
        }
    }

    public Page<Template> findTemplatesBySearch(String search, Pageable pageable) {
        if (search != null && !search.isEmpty()) {
            return templateRepository.findByNameContainingIgnoreCase(search, pageable);
        } else {
            return templateRepository.findAllTemplates(pageable);
        }
    }

    public Page<Template> findTemplatesByUserAndSearch(String userId, String search, Pageable pageable) {
        if (search != null && !search.isEmpty()) {
            return templateRepository.findByUserIdAndNameContainingIgnoreCase(userId, search, pageable);
        } else {
            return templateRepository.findByUserId(userId, pageable);
        }
    }

    // Nouvelles méthodes pour le critère isWhatsapp
    public Page<Template> findTemplatesByWhatsappAndSearch(Boolean isWhatsapp, String search, Pageable pageable) {
        if (search != null && !search.isEmpty()) {
            return templateRepository.findByWhatsappAndNameContainingIgnoreCase(isWhatsapp, search, pageable);
        } else {
            return templateRepository.findByWhatsapp(isWhatsapp, pageable);
        }
    }

    public Page<Template> findTemplatesByApprovedAndWhatsappAndSearch(
        String approved,
        Boolean isWhatsapp,
        String search,
        Pageable pageable
    ) {
        if (search != null && !search.isEmpty()) {
            return templateRepository.findByWhatsappAndApprovedAndNameContainingIgnoreCase(isWhatsapp, approved, search, pageable);
        } else {
            return templateRepository.findByWhatsappAndApproved(isWhatsapp, approved, pageable);
        }
    }

    public Page<Template> findTemplatesByUserAndWhatsappAndSearch(String userId, Boolean isWhatsapp, String search, Pageable pageable) {
        if (search != null && !search.isEmpty()) {
            return templateRepository.findByUserIdAndWhatsappAndNameContainingIgnoreCase(userId, isWhatsapp, search, pageable);
        } else {
            return templateRepository.findByUserIdAndWhatsapp(userId, isWhatsapp, pageable);
        }
    }

    public Page<Template> findTemplatesByUserAndApprovedAndWhatsappAndSearch(
        String userId,
        String approved,
        Boolean isWhatsapp,
        String search,
        Pageable pageable
    ) {
        if (search != null && !search.isEmpty()) {
            return templateRepository.findByUserIdAndApprovedAndWhatsappAndNameContainingIgnoreCase(
                userId,
                approved,
                isWhatsapp,
                search,
                pageable
            );
        } else {
            return templateRepository.findByUserIdAndApprovedAndWhatsapp(userId, approved, isWhatsapp, pageable);
        }
    }

    @Async
    @Transactional
    public void deleteAllTempaltesAsync() {
        try {
            // Étape 1 : Supprimer les lignes dépendantes (par exemple, sms_log)
            int deletedLogs = templateRepository.deleteAllTemplates();
        } catch (Exception e) {
            throw new RuntimeException("Deletion failed", e);
        }
    }

    public String applyTemplate(String templateContent, Contact contact) {
        if (templateContent == null) {
            return "";
        }
        String result = templateContent;

        if (contact != null) {
            // Remplacement des variables standard
            result = result.replaceAll("\\{\\{\\s*nom\\s*\\}\\}", contact.getConnom() != null ? contact.getConnom() : "");
            result = result.replaceAll("\\{\\{\\s*prenom\\s*\\}\\}", contact.getConprenom() != null ? contact.getConprenom() : "");
            result = result.replaceAll("\\{\\{\\s*telephone\\s*\\}\\}", contact.getContelephone() != null ? contact.getContelephone() : "");

            // Remplacement des variables personnalisées
            if (contact.getCustomFields() != null && !contact.getCustomFields().trim().isEmpty()) {
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    Map<String, String> customMap = mapper.readValue(
                        contact.getCustomFields(),
                        new TypeReference<Map<String, String>>() {}
                    );
                    for (Map.Entry<String, String> entry : customMap.entrySet()) {
                        // On utilise Pattern.quote pour éviter les problèmes si la clé contient des caractères spéciaux
                        result = result.replaceAll(
                            "\\{\\{\\s*" + Pattern.quote(entry.getKey()) + "\\s*\\}\\}",
                            entry.getValue() != null ? entry.getValue() : ""
                        );
                    }
                } catch (Exception e) {
                    // En cas de problème de conversion, on log et on continue
                    log.error("Erreur lors du parsing des customFields pour le contact {}: {}", contact.getContelephone(), e.getMessage());
                }
            }
        }
        return result;
    }

    public List<VariableDTO> buildListeVars(TemplateRequest tplReq, Map<String, String> extracted) {
        List<VariableDTO> liste = new ArrayList<>();
        int ordre = 1;
        for (ComponentRequest comp : tplReq.getComponents()) {
            if (comp.getText() != null) {
                Matcher m = VAR_PATTERN.matcher(comp.getText());
                while (m.find()) {
                    String key = m.group(1).trim();
                    String defaultVal = m.group(2) != null ? m.group(2).trim() : "";
                    String val = extracted.getOrDefault(key, defaultVal);
                    liste.add(new VariableDTO(ordre++, val, comp.getType()));
                }
            }
            // Si boutons à traiter :
            if (comp.getButtons() != null) {
                for (ButtonRequest btn : comp.getButtons()) {
                    if (btn.getText() != null) {
                        Matcher m = VAR_PATTERN.matcher(btn.getText());
                        while (m.find()) {
                            String key = m.group(1).trim();
                            String defaultVal = m.group(2) != null ? m.group(2).trim() : "";
                            String val = extracted.getOrDefault(key, defaultVal);
                            liste.add(new VariableDTO(ordre++, val, comp.getType()));
                        }
                    }
                    if (btn.getUrl() != null) {
                        Matcher m = VAR_PATTERN.matcher(btn.getUrl());
                        while (m.find()) {
                            String key = m.group(1).trim();
                            String defaultVal = m.group(2) != null ? m.group(2).trim() : "";
                            String val = extracted.getOrDefault(key, defaultVal);
                            liste.add(new VariableDTO(ordre++, val, comp.getType()));
                        }
                    }
                }
            }
        }
        return liste;
    }

    public int calculateSmsSegments(String message) {
        if (message == null || message.isEmpty()) {
            return 0;
        }

        // Détection si le message peut être encodé en GSM-7
        boolean isGsm7 = isGsm7(message);

        if (isGsm7) {
            final int SINGLE_SMS_LIMIT = 160;
            final int MULTI_SMS_SEGMENT_LIMIT = 153;

            int length = message.length();
            if (length <= SINGLE_SMS_LIMIT) {
                return 1;
            } else {
                return (int) Math.ceil((double) length / MULTI_SMS_SEGMENT_LIMIT);
            }
        } else {
            final int SINGLE_SMS_LIMIT = 70;
            final int MULTI_SMS_SEGMENT_LIMIT = 67;

            int length = message.length();
            if (length <= SINGLE_SMS_LIMIT) {
                return 1;
            } else {
                return (int) Math.ceil((double) length / MULTI_SMS_SEGMENT_LIMIT);
            }
        }
    }

    /**
     * Vérifie si tous les caractères du message appartiennent à l’alphabet GSM-7
     */
    private boolean isGsm7(String message) {
        // Tableau des caractères GSM-7 de base
        String gsm7Chars =
            "@£$¥èéùìòÇ\nØø\rÅåΔ_ΦΓΛΩΠΨΣΘΞÆæßÉ " +
            "!\"#¤%&'()*+,-./0123456789:;<=>?" +
            "¡ABCDEFGHIJKLMNOPQRSTUVWXYZÄÖÑÜ§" +
            "¿abcdefghijklmnopqrstuvwxyzäöñüà";

        for (char c : message.toCharArray()) {
            if (gsm7Chars.indexOf(c) == -1) {
                return false; // caractère non supporté par GSM-7
            }
        }
        return true;
    }

    // Dans TemplateService.java

    /**
     * Extrait toutes les variables d'un template avec leurs informations
     */
    public List<TemplateResource.VariableInfo> extractVariableInfo(String content) throws JsonProcessingException {
        List<TemplateResource.VariableInfo> variables = new ArrayList<>();

        if (content == null || content.trim().isEmpty()) {
            return variables;
        }

        // Pattern pour {{nom:default}}
        Pattern pattern = Pattern.compile("\\{\\{([^:}]+)(?::([^}]*))?\\}\\}");

        try {
            // Parser le JSON si c'est un template WhatsApp
            if (content.trim().startsWith("{")) {
                JsonNode root = objectMapper.readTree(content);
                JsonNode components = root.get("components");

                if (components != null && components.isArray()) {
                    int globalIndex = 0;

                    for (JsonNode component : components) {
                        String type = component.get("type").asText();

                        if (component.has("text")) {
                            String text = component.get("text").asText();
                            Matcher matcher = pattern.matcher(text);

                            while (matcher.find()) {
                                String varName = matcher.group(1).trim();
                                String defaultValue = matcher.group(2) != null ? matcher.group(2).trim() : "";

                                variables.add(new TemplateResource.VariableInfo(varName, defaultValue, type, globalIndex++));
                            }
                        }

                        // Gérer les boutons
                        if (component.has("buttons")) {
                            JsonNode buttons = component.get("buttons");
                            for (JsonNode button : buttons) {
                                if (button.has("text")) {
                                    String text = button.get("text").asText();
                                    Matcher matcher = pattern.matcher(text);

                                    while (matcher.find()) {
                                        String varName = matcher.group(1).trim();
                                        String defaultValue = matcher.group(2) != null ? matcher.group(2).trim() : "";

                                        variables.add(new TemplateResource.VariableInfo(varName, defaultValue, "BUTTON", globalIndex++));
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Template SMS simple
                Matcher matcher = pattern.matcher(content);
                int index = 0;

                while (matcher.find()) {
                    String varName = matcher.group(1).trim();
                    String defaultValue = matcher.group(2) != null ? matcher.group(2).trim() : "";

                    variables.add(new TemplateResource.VariableInfo(varName, defaultValue, "BODY", index++));
                }
            }
        } catch (JsonProcessingException e) {
            log.error("Error parsing template content: {}", e.getMessage());
            throw e;
        }

        return variables;
    }

    /**
     * Applique le mapping des variables (renomme les variables dans le template)
     */
    public String applyVariableMapping(String content, Map<String, String> mappings) throws JsonProcessingException {
        if (content == null || mappings == null || mappings.isEmpty()) {
            return content;
        }

        // Si c'est un template JSON (WhatsApp)
        if (content.trim().startsWith("{")) {
            return applyVariableMappingToJson(content, mappings);
        } else {
            // Template SMS simple
            return applyVariableMappingToText(content, mappings);
        }
    }

    /**
     * Applique le mapping sur un template JSON
     */
    private String applyVariableMappingToJson(String content, Map<String, String> mappings) throws JsonProcessingException {
        JsonNode root = objectMapper.readTree(content);
        ObjectNode rootNode = (ObjectNode) root;

        JsonNode components = rootNode.get("components");
        if (components != null && components.isArray()) {
            ArrayNode componentsArray = (ArrayNode) components;

            for (int i = 0; i < componentsArray.size(); i++) {
                JsonNode component = componentsArray.get(i);
                ObjectNode componentNode = (ObjectNode) component;

                // Traiter le texte
                if (componentNode.has("text")) {
                    String text = componentNode.get("text").asText();
                    String mappedText = applyMappingToText(text, mappings);
                    componentNode.put("text", mappedText);
                }

                // Traiter les boutons
                if (componentNode.has("buttons")) {
                    JsonNode buttons = componentNode.get("buttons");
                    if (buttons.isArray()) {
                        ArrayNode buttonsArray = (ArrayNode) buttons;
                        for (int j = 0; j < buttonsArray.size(); j++) {
                            JsonNode button = buttonsArray.get(j);
                            ObjectNode buttonNode = (ObjectNode) button;

                            if (buttonNode.has("text")) {
                                String text = buttonNode.get("text").asText();
                                String mappedText = applyMappingToText(text, mappings);
                                buttonNode.put("text", mappedText);
                            }

                            if (buttonNode.has("url")) {
                                String url = buttonNode.get("url").asText();
                                String mappedUrl = applyMappingToText(url, mappings);
                                buttonNode.put("url", mappedUrl);
                            }
                        }
                    }
                }
            }
        }

        return objectMapper.writeValueAsString(rootNode);
    }

    /**
     * Applique le mapping sur un texte simple
     */
    private String applyVariableMappingToText(String content, Map<String, String> mappings) {
        String result = content;

        for (Map.Entry<String, String> entry : mappings.entrySet()) {
            String oldName = entry.getKey();
            String newName = entry.getValue();

            // Remplacer {{oldName:default}} par {{newName:default}}
            Pattern pattern = Pattern.compile("\\{\\{" + Pattern.quote(oldName) + "(?::([^}]*))?\\}\\}");
            Matcher matcher = pattern.matcher(result);
            StringBuffer sb = new StringBuffer();

            while (matcher.find()) {
                String defaultValue = matcher.group(1) != null ? matcher.group(1) : "";
                String replacement = "{{" + newName + (defaultValue.isEmpty() ? "" : ":" + defaultValue) + "}}";
                matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            }
            matcher.appendTail(sb);

            result = sb.toString();
        }

        return result;
    }

    /**
     * Helper pour appliquer le mapping sur un seul texte
     */
    private String applyMappingToText(String text, Map<String, String> mappings) {
        String result = text;

        for (Map.Entry<String, String> entry : mappings.entrySet()) {
            String oldName = entry.getKey();
            String newName = entry.getValue();

            Pattern pattern = Pattern.compile("\\{\\{" + Pattern.quote(oldName) + "(?::([^}]*))?\\}\\}");
            Matcher matcher = pattern.matcher(result);
            StringBuffer sb = new StringBuffer();

            while (matcher.find()) {
                String defaultValue = matcher.group(1) != null ? matcher.group(1) : "";
                String replacement = "{{" + newName + (defaultValue.isEmpty() ? "" : ":" + defaultValue) + "}}";
                matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            }
            matcher.appendTail(sb);

            result = sb.toString();
        }

        return result;
    }
}
