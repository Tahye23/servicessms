package com.example.myproject.web.rest;

import com.example.myproject.domain.*;
import com.example.myproject.domain.Configuration;
import com.example.myproject.domain.Template;
import com.example.myproject.repository.ConfigurationRepository;
import com.example.myproject.repository.ContactRepository;
import com.example.myproject.repository.TemplateRepository;
import com.example.myproject.repository.UserRepository;
import com.example.myproject.security.SecurityUtils;
import com.example.myproject.service.SendWhatsappService;
import com.example.myproject.service.TemplateService;
import com.example.myproject.web.rest.errors.BadRequestAlertException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.persistence.EntityNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequestMapping("/api")
public class TemplateResource {

    private final Logger log = LoggerFactory.getLogger(TemplateResource.class);
    private static final String GRAPH_API_PREFIX = "https://graph.facebook.com/v22.0/";
    private final ConfigurationRepository configurationRepository;
    private final UserRepository userRepository;
    private final ContactRepository contactRepository;

    @Autowired
    private SendWhatsappService sendWhatsappService;

    @Autowired
    private RestTemplate restTemplate;

    private final TemplateService templateService;
    private final TemplateRepository templateRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TemplateResource(
        ConfigurationRepository configurationRepository,
        UserRepository userRepository,
        ContactRepository contactRepository,
        TemplateService templateService,
        TemplateRepository templateRepository
    ) {
        this.configurationRepository = configurationRepository;
        this.userRepository = userRepository;
        this.contactRepository = contactRepository;
        this.templateService = templateService;
        this.templateRepository = templateRepository;
    }

    @PostMapping("/templates")
    public ResponseEntity<Template> createTemplate(@RequestBody Template template) throws URISyntaxException {
        // Si l'utilisateur n'est pas administrateur, renseigner le champ user_id

        try {
            String currentUserLogin = getCurrentUserLoginOrThrow();
            String effectiveUserLogin = determineEffectiveUserLogin(currentUserLogin);
            template.setUser_id(effectiveUserLogin);
        } catch (IllegalStateException e) {
            log.warn("Utilisateur non authentifié lors de la création du template: {}", e.getMessage());
            throw e; // Re-lancer pour que le contrôleur puisse gérer
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String currentDate = LocalDateTime.now().format(formatter);
        template.setCreated_at(currentDate);
        template.setStatus("PENDING");
        template.setApproved(false);
        Template result = templateService.save(template);
        return ResponseEntity.created(new URI("/api/templates/" + result.getId())).body(result);
    }

    private String determineEffectiveUserLogin(String currentUserLogin) {
        boolean isUser =
            SecurityUtils.hasCurrentUserAnyOfAuthorities("ROLE_USER") && !SecurityUtils.hasCurrentUserAnyOfAuthorities("ROLE_ADMIN");

        if (!isUser) {
            return currentUserLogin;
        }

        return userRepository.findOneByLogin(currentUserLogin).map(User::getExpediteur).filter(Objects::nonNull).orElse(currentUserLogin);
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PutMapping("/templates/{id}/approve")
    public ResponseEntity<Template> approveTemplate(@PathVariable Long id) {
        try {
            Template template = templateService.approveTemplate(id, true);
            return ResponseEntity.ok(template);
        } catch (EntityNotFoundException | JsonProcessingException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PutMapping("/templates/{id}/rejected")
    public ResponseEntity<Template> rejectedTemplate(@PathVariable Long id) {
        try {
            Template template = templateService.approveTemplate(id, false);
            return ResponseEntity.ok(template);
        } catch (EntityNotFoundException ex) {
            return ResponseEntity.notFound().build();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @PutMapping("/templates/{id}")
    public ResponseEntity<Template> updateTemplate(@PathVariable Long id, @RequestBody Template template) {
        // Vérifie que l'objet reçu possède bien un ID
        if (template.getId() == null) {
            throw new BadRequestAlertException("L'ID du template ne peut pas être nul", "Template", "idnull");
        }
        // Vérifie que l'ID dans l'URL correspond à celui du template
        if (!Objects.equals(id, template.getId())) {
            throw new BadRequestAlertException("L'ID de l'URL ne correspond pas à l'ID de l'objet", "Template", "idinvalid");
        }
        // Vérifier que le template existe avant la mise à jour
        if (!templateRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        template.setApproved(Boolean.FALSE);
        // Sauvegarde du template mis à jour
        Template result = templateRepository.save(template);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/templates/refresh")
    public ResponseEntity<Void> refreshTemplates() throws JsonProcessingException {
        sendWhatsappService.refreshUnapprovedTemplates();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/templates")
    public ResponseEntity<Page<Template>> getAllTemplates(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(required = false) String approved,
        @RequestParam(required = false) Boolean isWhatsapp,
        @RequestParam(required = false) String search
    ) {
        log.debug(
            "REST request to get templates with params: page={}, size={}, approved={}, isWhatsapp={}, search={}",
            page,
            size,
            approved,
            isWhatsapp,
            search
        );

        try {
            // Validation des paramètres
            validateTemplateParameters(page, size, search);

            Pageable pageable = PageRequest.of(page, size);

            String currentUserLogin = getCurrentUserLoginOrThrow();
            String effectiveUserLogin = determineEffectiveUserLogin(currentUserLogin);
            boolean isAdmin = SecurityUtils.hasCurrentUserAnyOfAuthorities("ROLE_ADMIN");

            TemplateSearchCriteria criteria = new TemplateSearchCriteria(approved, isWhatsapp, search);

            Page<Template> templates = getTemplatesPage(criteria, effectiveUserLogin, isAdmin, pageable);

            log.debug("Retrieved {} templates for user {}", templates.getTotalElements(), currentUserLogin);

            return ResponseEntity.ok(templates);
        } catch (IllegalArgumentException e) {
            log.warn("Paramètres invalides pour la recherche de templates: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (IllegalStateException e) {
            log.warn("État invalide lors de la recherche de templates: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (Exception e) {
            log.error("Erreur inattendue lors de la récupération des templates", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Méthodes extraites pour améliorer la lisibilité et la testabilité

    private String getCurrentUserLoginOrThrow() {
        return SecurityUtils.getCurrentUserLogin().orElseThrow(() -> new IllegalStateException("L'utilisateur n'est pas authentifié"));
    }

    private void validateTemplateParameters(int page, int size, String search) {
        if (page < 0) {
            throw new IllegalArgumentException("Le numéro de page ne peut pas être négatif");
        }

        if (size <= 0 || size > 100) {
            throw new IllegalArgumentException("La taille de page doit être entre 1 et 100");
        }

        if (search != null && search.trim().length() < 2) {
            throw new IllegalArgumentException("Le terme de recherche doit contenir au moins 2 caractères");
        }
    }

    private Page<Template> getTemplatesPage(TemplateSearchCriteria criteria, String userLogin, boolean isAdmin, Pageable pageable) {
        if (isAdmin) {
            return getAdminTemplatesPage(criteria, pageable);
        } else {
            return getUserTemplatesPage(criteria, userLogin, pageable);
        }
    }

    private Page<Template> getAdminTemplatesPage(TemplateSearchCriteria criteria, Pageable pageable) {
        log.debug("Admin requesting templates with criteria: {}", criteria);

        if (criteria.hasApproved() && criteria.hasWhatsapp()) {
            return templateService.findTemplatesByApprovedAndWhatsappAndSearch(
                criteria.getApproved(),
                criteria.getIsWhatsapp(),
                criteria.getSearch(),
                pageable
            );
        }

        if (criteria.hasApproved()) {
            return templateService.findTemplatesByApprovedAndSearch(criteria.getApproved(), criteria.getSearch(), pageable);
        }

        if (criteria.hasWhatsapp()) {
            return templateService.findTemplatesByWhatsappAndSearch(criteria.getIsWhatsapp(), criteria.getSearch(), pageable);
        }

        return templateService.findTemplatesBySearch(criteria.getSearch(), pageable);
    }

    private Page<Template> getUserTemplatesPage(TemplateSearchCriteria criteria, String userLogin, Pageable pageable) {
        log.debug("User {} requesting templates with criteria: {}", userLogin, criteria);

        if (criteria.hasApproved() && criteria.hasWhatsapp()) {
            return templateService.findTemplatesByUserAndApprovedAndWhatsappAndSearch(
                userLogin,
                criteria.getApproved(),
                criteria.getIsWhatsapp(),
                criteria.getSearch(),
                pageable
            );
        }

        if (criteria.hasApproved()) {
            return templateService.findTemplatesByUserAndApprovedAndSearch(
                userLogin,
                criteria.getApproved(),
                criteria.getSearch(),
                pageable
            );
        }

        if (criteria.hasWhatsapp()) {
            return templateService.findTemplatesByUserAndWhatsappAndSearch(
                userLogin,
                criteria.getIsWhatsapp(),
                criteria.getSearch(),
                pageable
            );
        }

        return templateService.findTemplatesByUserAndSearch(userLogin, criteria.getSearch(), pageable);
    }

    private static class TemplateSearchCriteria {

        private final String approved;
        private final Boolean isWhatsapp;
        private final String search;

        public TemplateSearchCriteria(String approved, Boolean isWhatsapp, String search) {
            this.approved = approved;
            this.isWhatsapp = isWhatsapp;
            this.search = search != null && !search.trim().isEmpty() ? search.trim() : null;
        }

        public String getApproved() {
            return approved;
        }

        public Boolean getIsWhatsapp() {
            return isWhatsapp;
        }

        public String getSearch() {
            return search;
        }

        public boolean hasApproved() {
            return approved != null;
        }

        public boolean hasWhatsapp() {
            return isWhatsapp != null;
        }

        public boolean hasSearch() {
            return search != null;
        }

        @Override
        public String toString() {
            return String.format("TemplateSearchCriteria{approved='%s', isWhatsapp=%s, search='%s'}", approved, isWhatsapp, search);
        }
    }

    @GetMapping("/templates/media/header/{id}/{userLogin}")
    public ResponseEntity<StreamingResponseBody> streamHeaderMedia(@PathVariable Long id, @PathVariable String userLogin) {
        Template tmpl = templateRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        Configuration cfg = configurationRepository
            .findOneByUserLogin(userLogin)
            .orElseThrow(() -> new BadRequestAlertException("Partner has no WhatsApp configuration", "whatsapp", "noconfig"));
        // 2.1) premier handle CSV
        String handle = Arrays.stream(tmpl.getCode().split("\\s*,\\s*"))
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NO_CONTENT));

        // 2.2) interroger Graph API pour obtenir l’URL “url”
        String graphUrl = GRAPH_API_PREFIX + handle + "?fields=url&phone_number_id=" + cfg.getPhoneNumberId();

        JsonNode mediaMeta = restTemplate.getForObject(graphUrl + "&access_token=" + cfg.getAccessToken(), JsonNode.class);
        if (mediaMeta == null || !mediaMeta.has("url")) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "media URL non retournée");
        }
        String cdnUrl = mediaMeta.get("url").asText();

        // 2.3) on prépare le StreamingResponseBody
        StreamingResponseBody body = outputStream -> {
            // on établit un InputStream direct vers le CDN (avec auth)
            HttpHeaders hdr = new HttpHeaders();
            hdr.setBearerAuth(cfg.getAccessToken());
            RequestEntity<Void> req = new RequestEntity<>(hdr, HttpMethod.GET, URI.create(cdnUrl));
            ResponseEntity<Resource> cdnResp = restTemplate.exchange(req, Resource.class);
            try (InputStream in = cdnResp.getBody().getInputStream()) {
                byte[] buf = new byte[8192];
                int r;
                while ((r = in.read(buf)) != -1) {
                    outputStream.write(buf, 0, r);
                }
            }
        };

        // 2.4) on déduit le type mime depuis Graph meta (optionnel, ici on fixe “video/mp4”)
        HttpHeaders respHdr = new HttpHeaders();
        respHdr.setContentType(MediaType.valueOf(mediaMeta.path("mime_type").asText("video/mp4")));
        // on ne fixe PAS Content-Length pour éviter l’erreur si on ne connais pas la taille à l’avance

        return ResponseEntity.ok().headers(respHdr).body(body);
    }

    @GetMapping("/templates/header/{id}")
    public ResponseEntity<byte[]> getHeaderMedia(@PathVariable Long id) throws IOException {
        // 1) Récupère le Template
        Template tmpl = templateRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        String userLogin = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new BadRequestAlertException("User login not found", "whatsapp", "nouser"));

        Configuration cfg = configurationRepository
            .findOneByUserLogin(userLogin)
            .orElseThrow(() -> new BadRequestAlertException("Partner has no WhatsApp configuration", "whatsapp", "noconfig"));
        // 2) Extrait le premier handle
        String firstHandle = Arrays.stream(tmpl.getCode().split("\\s*,\\s*"))
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NO_CONTENT));

        // 3) Récupère l’URL CDN via Graph API
        HttpHeaders graphHdr = new HttpHeaders();
        graphHdr.setBearerAuth(cfg.getAccessToken());
        String graphUrl = String.format(
            "https://graph.facebook.com/v22.0/%s?phone_number_id=%s&fields=url",
            firstHandle,
            cfg.getPhoneNumberId()
        );
        ResponseEntity<String> graphResp = restTemplate.exchange(graphUrl, HttpMethod.GET, new HttpEntity<>(graphHdr), String.class);
        String cdnUrl = new ObjectMapper().readTree(graphResp.getBody()).path("url").asText();

        // 4) Télécharge le binaire du CDN avec le même token
        ResponseEntity<byte[]> cdnResp = restTemplate.exchange(cdnUrl, HttpMethod.GET, new HttpEntity<>(graphHdr), byte[].class);

        // 5) Renvoie le flux binaire avec le bon content‐type
        HttpHeaders respHdr = new HttpHeaders();
        respHdr.setContentType(MediaType.valueOf(cdnResp.getHeaders().getContentType().toString()));
        respHdr.setContentLength(cdnResp.getBody().length);

        return new ResponseEntity<>(cdnResp.getBody(), respHdr, HttpStatus.OK);
    }

    /** Optionnel : dans votre GET /templates/{id} replacez header.mediaUrl par ce endpoint : */
    @GetMapping("/templates/{id}")
    public ResponseEntity<Template> getTemplate(@PathVariable Long id) throws JsonProcessingException {
        String login = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new IllegalStateException("L'utilisateur n'est pas authentifié."));

        Template tmpl = templateRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        // Si c'est un template WhatsApp importé de Meta (avec templateId)
        if (tmpl.getTemplateId() != null && tmpl.getContent() != null) {
            try {
                ObjectMapper om = new ObjectMapper();
                JsonNode contentNode = om.readTree(tmpl.getContent());

                // Vérifier si le contenu est déjà un tableau (cas d'import depuis Meta)
                if (contentNode.isArray()) {
                    ArrayNode components = (ArrayNode) contentNode;

                    // Chercher le composant header (type = HEADER)
                    for (JsonNode component : components) {
                        if (component.has("type") && "HEADER".equals(component.get("type").asText())) {
                            ObjectNode header = (ObjectNode) component;

                            // Remplacer l'URL du média si présent
                            if (
                                header.has("format") &&
                                (header.get("format").asText().equals("IMAGE") ||
                                    header.get("format").asText().equals("VIDEO") ||
                                    header.get("format").asText().equals("DOCUMENT"))
                            ) {
                                header.put("mediaUrl", "/api/templates/media/header/" + tmpl.getId() + "/" + login);
                            }
                            break;
                        }
                    }

                    tmpl.setContent(om.writeValueAsString(components));
                } else if (contentNode.isObject()) {
                    // Cas où le contenu est un objet avec une propriété "components"
                    ObjectNode root = (ObjectNode) contentNode;

                    if (root.has("components")) {
                        ArrayNode components = (ArrayNode) root.get("components");

                        if (components.size() > 0) {
                            JsonNode firstComponent = components.get(0);

                            if (
                                firstComponent.isObject() &&
                                firstComponent.has("type") &&
                                "HEADER".equals(firstComponent.get("type").asText())
                            ) {
                                ObjectNode header = (ObjectNode) firstComponent;
                                header.put("mediaUrl", "/api/templates/media/header/" + tmpl.getId() + "/" + login);
                            }
                        }

                        tmpl.setContent(om.writeValueAsString(root));
                    }
                }
            } catch (JsonProcessingException e) {
                log.error("Erreur lors du traitement du contenu JSON du template {}: {}", id, e.getMessage());
                // On continue avec le contenu original sans modification
            }
        }

        return ResponseEntity.ok(tmpl);
    }

    // Dans TemplateResource.java

    @PostMapping("/templates/{id}/variable-mapping")
    public ResponseEntity<Template> updateVariableMapping(@PathVariable Long id, @RequestBody VariableMappingRequest mappingRequest) {
        log.debug("REST request to update variable mapping for template: {}", id);

        try {
            Template template = templateRepository
                .findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Template non trouvé"));

            // Appliquer le mapping aux variables du template
            String updatedContent = templateService.applyVariableMapping(template.getContent(), mappingRequest.getMappings());

            template.setContent(updatedContent);
            Template savedTemplate = templateRepository.save(template);

            log.info("Variable mapping updated successfully for template {}", id);

            return ResponseEntity.ok(savedTemplate);
        } catch (Exception e) {
            log.error("Error updating variable mapping for template {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Dans ContactResource.java (ou créer un nouveau endpoint)

    @GetMapping("/templates/available-fields")
    public ResponseEntity<List<ContactFieldInfo>> getAvailableContactFields() {
        log.debug("REST request to get available contact fields");

        List<ContactFieldInfo> fields = new ArrayList<>();

        // Champs standards
        fields.add(new ContactFieldInfo("connom", "Nom", "standard", true));
        fields.add(new ContactFieldInfo("conprenom", "Prénom", "standard", true));
        fields.add(new ContactFieldInfo("contelephone", "Téléphone", "standard", true));
        fields.add(new ContactFieldInfo("conemail", "Email", "standard", false));
        fields.add(new ContactFieldInfo("conadresse", "Adresse", "standard", false));
        fields.add(new ContactFieldInfo("conville", "Ville", "standard", false));
        fields.add(new ContactFieldInfo("conpays", "Pays", "standard", false));
        fields.add(new ContactFieldInfo("concodpostal", "Code postal", "standard", false));
        fields.add(new ContactFieldInfo("conorganisation", "Organisation", "standard", false));
        fields.add(new ContactFieldInfo("conposte", "Poste", "standard", false));

        // Récupérer l'utilisateur connecté
        String currentUserLogin = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new IllegalStateException("Utilisateur non authentifié"));

        // Récupérer les champs personnalisés de l'utilisateur
        userRepository
            .findOneByLogin(currentUserLogin)
            .ifPresent(user -> {
                if (user.getCustomFields() != null && !user.getCustomFields().trim().isEmpty()) {
                    try {
                        ObjectMapper objectMapper = new ObjectMapper();
                        JsonNode customFieldsNode = objectMapper.readTree(user.getCustomFields());

                        Iterator<String> fieldNames = customFieldsNode.fieldNames();
                        while (fieldNames.hasNext()) {
                            String fieldName = fieldNames.next();
                            String displayName = formatFieldName(fieldName);
                            fields.add(new ContactFieldInfo(fieldName, displayName, "custom", false));
                        }
                    } catch (JsonProcessingException e) {
                        log.warn("Erreur lors du parsing des custom fields: {}", e.getMessage());
                    }
                }
            });

        return ResponseEntity.ok(fields);
    }

    private String formatFieldName(String fieldName) {
        if (fieldName == null || fieldName.isEmpty()) {
            return fieldName;
        }

        String formatted = fieldName.replace("_", " ").replace("-", " ");
        return formatted.substring(0, 1).toUpperCase() + formatted.substring(1);
    }

    // Classe DTO
    public static class ContactFieldInfo {

        private String fieldName;
        private String displayName;
        private String type; // "standard" ou "custom"
        private boolean required;

        public ContactFieldInfo(String fieldName, String displayName, String type, boolean required) {
            this.fieldName = fieldName;
            this.displayName = displayName;
            this.type = type;
            this.required = required;
        }

        // Getters and setters
        public String getFieldName() {
            return fieldName;
        }

        public void setFieldName(String fieldName) {
            this.fieldName = fieldName;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public boolean isRequired() {
            return required;
        }

        public void setRequired(boolean required) {
            this.required = required;
        }
    }

    @GetMapping("/templates/{id}/variables")
    public ResponseEntity<List<VariableInfo>> extractTemplateVariables(@PathVariable Long id) {
        log.debug("REST request to extract variables from template: {}", id);

        try {
            Template template = templateRepository
                .findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Template non trouvé"));

            List<VariableInfo> variables = templateService.extractVariableInfo(template.getContent());

            return ResponseEntity.ok(variables);
        } catch (Exception e) {
            log.error("Error extracting variables from template {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Classes DTO
    public static class VariableMappingRequest {

        private Map<String, String> mappings;

        public Map<String, String> getMappings() {
            return mappings;
        }

        public void setMappings(Map<String, String> mappings) {
            this.mappings = mappings;
        }
    }

    public static class VariableInfo {

        private String name;
        private String defaultValue;
        private String type; // HEADER, BODY, FOOTER, BUTTON
        private int index;

        public VariableInfo(String name, String defaultValue, String type, int index) {
            this.name = name;
            this.defaultValue = defaultValue;
            this.type = type;
            this.index = index;
        }

        // Getters and setters
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDefaultValue() {
            return defaultValue;
        }

        public void setDefaultValue(String defaultValue) {
            this.defaultValue = defaultValue;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }
    }

    @DeleteMapping("/templates/{id}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable Long id) {
        templateService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/templates/import-from-meta")
    public ResponseEntity<Template> importTemplateFromMeta(@RequestBody ImportTemplateRequest request) {
        log.debug("REST request to import template from Meta: {}", request.getTemplateName());

        try {
            String currentUserLogin = getCurrentUserLoginOrThrow();
            String effectiveUserLogin = determineEffectiveUserLogin(currentUserLogin);

            // Récupérer la configuration Meta de l'utilisateur
            Configuration cfg = configurationRepository
                .findOneByUserLogin(effectiveUserLogin)
                .orElseThrow(() -> new BadRequestAlertException("L'utilisateur n'a pas de configuration WhatsApp", "whatsapp", "noconfig"));

            // Appeler l'API Meta pour récupérer le template
            Template importedTemplate = templateService.importTemplateFromMeta(request.getTemplateName(), cfg, effectiveUserLogin);

            return ResponseEntity.ok(importedTemplate);
        } catch (IllegalStateException e) {
            log.warn("Utilisateur non authentifié lors de l'import: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (BadRequestAlertException e) {
            log.error("Erreur lors de l'import du template: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Erreur inattendue lors de l'import du template", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Classe pour la requête
    public static class ImportTemplateRequest {

        private String templateName;

        public String getTemplateName() {
            return templateName;
        }

        public void setTemplateName(String templateName) {
            this.templateName = templateName;
        }
    }

    @DeleteMapping("/templates/delete-all")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Void> deleteAllTemplates() {
        try {
            templateService.deleteAllTempaltesAsync();
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // Dans TemplateResource.java

    @PostMapping("/templates/{id}/upload-media")
    public ResponseEntity<MediaUploadResponse> uploadMediaForTemplate(
        @PathVariable Long id,
        @RequestParam("file") MultipartFile file,
        @RequestParam("mediaType") String mediaType // IMAGE, VIDEO, DOCUMENT
    ) {
        log.debug("REST request to upload media for template: {}", id);

        try {
            // Vérifier que le template existe
            Template template = templateRepository
                .findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Template non trouvé"));

            // Récupérer l'utilisateur connecté
            String currentUserLogin = SecurityUtils.getCurrentUserLogin()
                .orElseThrow(() -> new IllegalStateException("Utilisateur non authentifié"));

            String effectiveUserLogin = determineEffectiveUserLogin(currentUserLogin);

            // Récupérer la configuration WhatsApp
            Configuration cfg = configurationRepository
                .findOneByUserLogin(effectiveUserLogin)
                .orElseThrow(() -> new BadRequestAlertException("Configuration WhatsApp non trouvée", "whatsapp", "noconfig"));

            // Valider le fichier
            if (file.isEmpty()) {
                throw new BadRequestAlertException("Fichier vide", "media", "emptyfile");
            }

            // Valider le type de média
            if (!Arrays.asList("IMAGE", "VIDEO", "DOCUMENT").contains(mediaType.toUpperCase())) {
                throw new BadRequestAlertException("Type de média invalide", "media", "invalidtype");
            }

            // Upload le média via le service WhatsApp
            byte[] fileBytes = file.getBytes();
            String mimeType = file.getContentType();

            String mediaId = sendWhatsappService.uploadMediaAndGetId(fileBytes, mimeType, mediaType, cfg);

            // Mettre à jour le template avec le media_id
            template.setCode(mediaId);
            templateRepository.save(template);

            log.info("Media uploaded successfully for template {}: mediaId={}", id, mediaId);

            return ResponseEntity.ok(new MediaUploadResponse(true, mediaId, file.getOriginalFilename(), mediaType, file.getSize()));
        } catch (IOException e) {
            log.error("Error uploading media for template {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new MediaUploadResponse(false, null, null, null, 0L));
        }
    }

    // Classe DTO pour la réponse
    public static class MediaUploadResponse {

        private boolean success;
        private String mediaId;
        private String filename;
        private String mediaType;
        private Long fileSize;

        public MediaUploadResponse() {}

        public MediaUploadResponse(boolean success, String mediaId, String filename, String mediaType, Long fileSize) {
            this.success = success;
            this.mediaId = mediaId;
            this.filename = filename;
            this.mediaType = mediaType;
            this.fileSize = fileSize;
        }

        // Getters and setters
        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getMediaId() {
            return mediaId;
        }

        public void setMediaId(String mediaId) {
            this.mediaId = mediaId;
        }

        public String getFilename() {
            return filename;
        }

        public void setFilename(String filename) {
            this.filename = filename;
        }

        public String getMediaType() {
            return mediaType;
        }

        public void setMediaType(String mediaType) {
            this.mediaType = mediaType;
        }

        public Long getFileSize() {
            return fileSize;
        }

        public void setFileSize(Long fileSize) {
            this.fileSize = fileSize;
        }
    }
}
