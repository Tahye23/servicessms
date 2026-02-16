package com.example.myproject.web.rest;

import com.example.myproject.SMSService;
import com.example.myproject.domain.Template;
import com.example.myproject.domain.TokensApp;
import com.example.myproject.domain.User;
import com.example.myproject.repository.TemplateRepository;
import com.example.myproject.repository.TokensAppRepository;
import com.example.myproject.repository.UserRepository;
import com.example.myproject.service.ExternalApiCacheService;
import com.example.myproject.service.ExternalApiTrackingService;
import com.example.myproject.web.rest.dto.SmsSendResult;
import com.example.myproject.web.rest.errors.ResourceNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import java.util.regex.Matcher;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/external/v1/sms")
@CrossOrigin(origins = "*")
public class ExternalSmsResource {

    private static final Logger log = LoggerFactory.getLogger(ExternalSmsResource.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final TemplateRepository templateRepository;
    private final SMSService smsService;
    private final TokensAppRepository tokensAppRepository;
    private final UserRepository userRepository;
    private final ExternalApiTrackingService trackingService;
    private final ExternalApiCacheService cacheService; //  CACHE

    public ExternalSmsResource(
        TemplateRepository templateRepository,
        SMSService smsService,
        TokensAppRepository tokensAppRepository,
        UserRepository userRepository,
        ExternalApiTrackingService trackingService,
        ExternalApiCacheService cacheService //  CACHE
    ) {
        this.templateRepository = templateRepository;
        this.smsService = smsService;
        this.tokensAppRepository = tokensAppRepository;
        this.userRepository = userRepository;
        this.trackingService = trackingService;
        this.cacheService = cacheService;
    }

    @GetMapping("/test")
    public ResponseEntity<String> testEndpoint() {
        return ResponseEntity.ok("External SMS API is working!");
    }

    @Transactional
    @PostMapping(
        value = "/send",
        consumes = { MediaType.APPLICATION_FORM_URLENCODED_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE },
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ApiSmsResponse> sendSmsTemplate(
        @RequestParam String templateName,
        @RequestParam String phone,
        @RequestParam(required = false) String variablesJson,
        @RequestHeader("X-Partner-Token") String partnerToken
    ) {
        log.info("[EXTERNAL-SMS-API] Réception demande - template: {}, phone: {}", templateName, phone);

        // Variables pour traçabilité
        TokensApp tokensApp = null;
        Template template = null;
        User user = null;
        String messageContent = null;
        boolean success = false;
        String errorMessage = null;
        String messageId = null;
        try {
            // 1) Validation des paramètres requis
            if (StringUtils.isAnyBlank(templateName, phone, partnerToken)) {
                errorMessage = "templateName, phone et X-Partner-Token sont requis";
                log.warn("[EXTERNAL-SMS-API] Paramètres manquants");
                return ResponseEntity.badRequest().body(ApiSmsResponse.error(errorMessage));
            }

            // 2) Validation et récupération du token
            Optional<TokensApp> tokenOptional = tokensAppRepository.findByToken(partnerToken.trim());
            if (!tokenOptional.isPresent()) {
                errorMessage = "Token invalide ou inexistant";
                log.warn("[EXTERNAL-SMS-API] Token invalide");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiSmsResponse.error(errorMessage));
            }

            tokensApp = tokenOptional.get();
            log.debug("[EXTERNAL-SMS-API] Token trouvé - ID: {}, User: {}", tokensApp.getId(), tokensApp.getUserLogin());

            // 3) Vérification de la validité du token
            if (!tokensApp.isValid()) {
                String reason = !Boolean.TRUE.equals(tokensApp.getActive()) ? "Token désactivé" : "Token expiré";
                errorMessage = "Token non valide: " + reason;
                log.warn("[EXTERNAL-SMS-API] {}", errorMessage);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiSmsResponse.error(errorMessage));
            }

            // 4) Récupération du userLogin
            String userLogin = tokensApp.getUserLogin();
            if (StringUtils.isBlank(userLogin)) {
                errorMessage = "Token invalide: utilisateur non identifié";
                log.error("[EXTERNAL-SMS-API] Token sans userLogin: {}", tokensApp.getId());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiSmsResponse.error(errorMessage));
            }

            // 5) Récupération de l'utilisateur
            user = userRepository
                .findOneByLogin(userLogin)
                .orElseThrow(() -> {
                    log.warn("[EXTERNAL-SMS-API] Utilisateur introuvable: {}", userLogin);
                    return new ResourceNotFoundException("Utilisateur introuvable");
                });

            // 6) Récupération du template
            template = templateRepository
                .findByNameAndUserLogin(templateName, userLogin)
                .orElseThrow(() -> {
                    log.warn("[EXTERNAL-SMS-API] Template '{}' introuvable pour '{}'", templateName, userLogin);
                    return new ResourceNotFoundException("Template introuvable pour cet utilisateur");
                });

            log.debug("[EXTERNAL-SMS-API] Template trouvé - ID: {}, Name: {}", template.getId(), template.getName());

            // 7) Validation template SMS
            validateSmsTemplate(template);

            // 8) Traitement des variables
            messageContent = template.getContent();
            if (StringUtils.isNotBlank(variablesJson)) {
                log.debug("[EXTERNAL-SMS-API] Traitement variables: {}", variablesJson);
                messageContent = processTemplateVariables(template.getContent(), variablesJson);
            }

            // 9) Expéditeur
            String sender = user.getExpediteur() != null ? user.getExpediteur() : "SMS-API";
            log.debug("[EXTERNAL-SMS-API] Expéditeur: {}", sender);

            // 10) Nettoyage numéro
            String cleanPhone = validateAndCleanPhone(phone);
            log.debug("[EXTERNAL-SMS-API] Numéro nettoyé: {}", cleanPhone);

            // 11) Mise à jour dernière utilisation token
            tokensApp.setLastUsedAt(java.time.ZonedDateTime.now());
            tokensAppRepository.save(tokensApp);

            // 12)  ENVOI DU SMS
            log.info("[EXTERNAL-SMS-API] Envoi SMS - From: {}, To: {}", sender, cleanPhone);
            // 12) ENVOI du SMS
            SmsSendResult sendResult = smsService.send(sender, cleanPhone, messageContent);

            success = sendResult.isSuccess();
            messageId = sendResult.getMessageId();
            errorMessage = sendResult.getError();

            if (!success) {
                log.warn("[SMS-API] Échec d'envoi vers {}, erreur: {}", cleanPhone, errorMessage);
            } else {
                log.info("[SMS-API] SMS envoyé vers {}, MsgId={}", cleanPhone, messageId);
            }

            // 13)  TRAÇABILITÉ : Enregistrer dans la table sms
            ExternalApiTrackingService.ExternalSmsTrackingData trackingData = new ExternalApiTrackingService.ExternalSmsTrackingData(
                tokensApp.getId(),
                userLogin,
                tokensApp.getApplication() != null ? tokensApp.getApplication().getName() : "Unknown",
                cleanPhone,
                sender,
                messageContent,
                template.getId(),
                template.getName(),
                variablesJson,
                success,
                errorMessage,
                messageId
            );

            trackingService.trackExternalSms(trackingData);

            // 14) Réponse
            if (!success) {
                return ResponseEntity.ok(ApiSmsResponse.failure(errorMessage));
            }

            return ResponseEntity.ok(ApiSmsResponse.success("SMS envoyé avec succès"));
        } catch (ResourceNotFoundException e) {
            errorMessage = e.getMessage();
            log.warn("[EXTERNAL-SMS-API] Ressource non trouvée: {}", errorMessage);

            //  Enregistrer même en cas d'erreur si on a les infos minimum
            tryTrackError(tokensApp, template, user, phone, null, variablesJson, errorMessage, messageId);

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiSmsResponse.error(errorMessage));
        } catch (IllegalArgumentException e) {
            errorMessage = e.getMessage();
            log.warn("[EXTERNAL-SMS-API] Argument invalide: {}", errorMessage);

            //  Enregistrer même en cas d'erreur
            tryTrackError(tokensApp, template, user, phone, messageContent, variablesJson, errorMessage, messageId);

            return ResponseEntity.badRequest().body(ApiSmsResponse.error(errorMessage));
        } catch (Exception e) {
            errorMessage = "Erreur interne: " + e.getMessage();
            log.error("[EXTERNAL-SMS-API] Erreur interne", e);

            //  Enregistrer même en cas d'erreur
            tryTrackError(tokensApp, template, user, phone, messageContent, variablesJson, errorMessage, messageId);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiSmsResponse.error(errorMessage));
        }
    }

    /**
     *  ENREGISTRER LES ERREURS (best effort)
     */
    private void tryTrackError(
        TokensApp tokensApp,
        Template template,
        User user,
        String phone,
        String messageContent,
        String variablesJson,
        String errorMessage,
        String messageId
    ) {
        try {
            if (tokensApp != null && user != null && template != null) {
                ExternalApiTrackingService.ExternalSmsTrackingData trackingData = new ExternalApiTrackingService.ExternalSmsTrackingData(
                    tokensApp.getId(),
                    user.getLogin(),
                    tokensApp.getApplication() != null ? tokensApp.getApplication().getName() : "Unknown",
                    phone,
                    user.getExpediteur() != null ? user.getExpediteur() : "SMS-API",
                    messageContent != null ? messageContent : template.getContent(),
                    template.getId(),
                    template.getName(),
                    variablesJson,
                    false,
                    errorMessage,
                    messageId
                );

                trackingService.trackExternalSms(trackingData);
            }
        } catch (Exception e) {
            log.error("[EXTERNAL-SMS-API] Erreur traçabilité erreur", e);
        }
    }

    // ===== MÉTHODES DE VALIDATION (inchangées) =====

    private void validateSmsTemplate(Template template) {
        String content = template.getContent();
        if (StringUtils.isBlank(content)) {
            throw new IllegalArgumentException("Template vide");
        }

        String trimmed = content.trim();
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            try {
                MAPPER.readTree(content);
                throw new IllegalArgumentException(
                    "Ce template WhatsApp ne peut pas être utilisé pour envoyer des SMS. " + "Veuillez utiliser un template SMS."
                );
            } catch (com.fasterxml.jackson.core.JsonProcessingException ignored) {
                // Si parsing échoue, c'est du texte normal
            }
        }
    }

    private String processTemplateVariables(String templateContent, String variablesJson) {
        try {
            VariableDTO[] variables = MAPPER.readValue(variablesJson, VariableDTO[].class);

            String result = templateContent;

            // 1️⃣ Remplacement des variables valides uniquement
            for (VariableDTO variable : variables) {
                if (variable.getIndex() != null && StringUtils.isNotBlank(variable.getText())) {
                    String placeholder = "\\{\\{" + variable.getIndex() + "\\}\\}";
                    result = result.replaceAll(placeholder, Matcher.quoteReplacement(variable.getText().trim()));
                    log.debug("Variable remplacée: {{}} -> {}", variable.getIndex(), variable.getText());
                }
            }

            // 2️⃣ Supprimer les lignes contenant encore des placeholders {{x}}
            result = result.replaceAll("(?m)^.*\\{\\{\\d+\\}\\}.*\\R?", "");

            // 3️⃣ Nettoyage final : lignes vides multiples
            result = result
                .replaceAll("(?m)^[ \\t]*\\r?\\n", "") // lignes vides
                .trim();

            return result;
        } catch (Exception e) {
            throw new IllegalArgumentException("Format des variables invalide: " + e.getMessage());
        }
    }

    private String validateAndCleanPhone(String phone) {
        if (StringUtils.isBlank(phone)) {
            throw new IllegalArgumentException("Numéro de téléphone requis");
        }

        String cleaned = phone.trim().replaceAll("[^\\d+]", "");

        if (!cleaned.matches("^\\+?[1-9]\\d{7,14}$")) {
            throw new IllegalArgumentException("Format de numéro de téléphone invalide: " + phone);
        }

        return cleaned;
    }

    // ===== DTO de réponse =====
    public static class ApiSmsResponse {

        public boolean success;
        public String messageId;
        public String error;

        public static ApiSmsResponse success(String message) {
            var r = new ApiSmsResponse();
            r.success = true;
            r.messageId = message;
            return r;
        }

        public static ApiSmsResponse failure(String err) {
            var r = new ApiSmsResponse();
            r.success = false;
            r.error = err;
            return r;
        }

        public static ApiSmsResponse error(String err) {
            var r = new ApiSmsResponse();
            r.success = false;
            r.error = err;
            return r;
        }
    }

    public static class VariableDTO {

        private Integer index;
        private String text;

        public Integer getIndex() {
            return index;
        }

        public void setIndex(Integer index) {
            this.index = index;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }
}
