package com.example.myproject.web.rest;

import com.example.myproject.domain.Configuration;
import com.example.myproject.domain.Template;
import com.example.myproject.domain.TokensApp;
import com.example.myproject.repository.ConfigurationRepository;
import com.example.myproject.repository.TemplateRepository;
import com.example.myproject.repository.TokensAppRepository;
import com.example.myproject.service.ExternalApiCacheService;
import com.example.myproject.service.ExternalApiTrackingService;
import com.example.myproject.service.SendWhatsappService;
import com.example.myproject.web.rest.dto.SendMessageResult;
import com.example.myproject.web.rest.errors.ResourceNotFoundException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/external/v1/whatsapp")
public class ExternalWhatsAppResource {

    private static final Logger log = LoggerFactory.getLogger(ExternalWhatsAppResource.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final TemplateRepository templateRepository;
    private final ConfigurationRepository configurationRepository;
    private final SendWhatsappService sendWhatsappService;
    private final TokensAppRepository tokensAppRepository;
    private final ExternalApiTrackingService trackingService;
    private final ExternalApiCacheService cacheService; // ✅ CACHE

    public ExternalWhatsAppResource(
        TemplateRepository templateRepository,
        ConfigurationRepository configurationRepository,
        SendWhatsappService sendWhatsappService,
        TokensAppRepository tokensAppRepository,
        ExternalApiTrackingService trackingService,
        ExternalApiCacheService cacheService // ✅ CACHE
    ) {
        this.templateRepository = templateRepository;
        this.configurationRepository = configurationRepository;
        this.sendWhatsappService = sendWhatsappService;
        this.tokensAppRepository = tokensAppRepository;
        this.trackingService = trackingService;
        this.cacheService = cacheService;
    }

    @Transactional
    @PostMapping(value = "/send", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiSendResponse> sendTemplate(
        @RequestParam String businessId,
        @RequestParam String templateName,
        @RequestParam String phone,
        @RequestParam(required = false) String phoneId,
        @RequestParam(required = false) String variablesJson,
        @RequestPart(name = "media", required = false) MultipartFile media,
        @RequestHeader("X-Partner-Token") String partnerToken
    ) {
        log.info("[EXTERNAL-WA-API] Réception demande - business: {}, template: {}, phone: {}", businessId, templateName, phone);

        // Variables pour traçabilité
        TokensApp tokensApp = null;
        Template template = null;
        Configuration cfg = null;
        String messageId = null;
        boolean success = false;
        String errorMessage = null;

        try {
            // 1) Validation paramètres
            if (StringUtils.isAnyBlank(businessId, templateName, phone, partnerToken)) {
                errorMessage = "businessId, templateName, phone et X-Partner-Token sont requis";
                log.warn("[EXTERNAL-WA-API] Paramètres manquants");
                return ResponseEntity.badRequest().body(ApiSendResponse.error(errorMessage));
            }

            // 2) Validation token
            Optional<TokensApp> tokenOptional = tokensAppRepository.findByToken(partnerToken.trim());
            if (!tokenOptional.isPresent()) {
                errorMessage = "Token invalide ou inexistant";
                log.warn("[EXTERNAL-WA-API] Token invalide");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiSendResponse.error(errorMessage));
            }

            tokensApp = tokenOptional.get();
            log.debug("[EXTERNAL-WA-API] Token trouvé - ID: {}, User: {}", tokensApp.getId(), tokensApp.getUserLogin());

            // 3) Vérification validité token
            if (!tokensApp.isValid()) {
                String reason = !Boolean.TRUE.equals(tokensApp.getActive()) ? "Token désactivé" : "Token expiré";
                errorMessage = "Token non valide: " + reason;
                log.warn("[EXTERNAL-WA-API] {}", errorMessage);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiSendResponse.error(errorMessage));
            }

            // 4) Récupération userLogin
            String userLogin = tokensApp.getUserLogin();
            if (StringUtils.isBlank(userLogin)) {
                errorMessage = "Token invalide: utilisateur non identifié";
                log.error("[EXTERNAL-WA-API] Token sans userLogin: {}", tokensApp.getId());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiSendResponse.error(errorMessage));
            }

            // 5) Récupération configuration
            cfg = configurationRepository
                .findOneByBusinessIdAndUserLoginIgnoreCase(businessId.trim(), userLogin)
                .orElseThrow(() -> new ResourceNotFoundException("Configuration introuvable pour ce businessId et utilisateur"));

            if (StringUtils.isBlank(cfg.getUserLogin())) {
                errorMessage = "Configuration invalide: userLogin manquant";
                log.error("[EXTERNAL-WA-API] Configuration sans userLogin");
                return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).body(ApiSendResponse.error(errorMessage));
            }

            log.debug("[EXTERNAL-WA-API] Configuration trouvée - PhoneNumberId: {}", cfg.getPhoneNumberId());

            // 6) Récupération template
            template = templateRepository
                .findByNameAndUserLogin(templateName, cfg.getUserLogin())
                .orElseThrow(() -> new ResourceNotFoundException("Template introuvable pour cet utilisateur"));

            log.debug("[EXTERNAL-WA-API] Template trouvé - ID: {}, Name: {}", template.getId(), template.getName());

            // 7) Variables
            List<VariableDTO> vars = List.of();
            if (StringUtils.isNotBlank(variablesJson)) {
                vars = MAPPER.readValue(variablesJson, new TypeReference<List<VariableDTO>>() {});
                log.debug("[EXTERNAL-WA-API] Variables: {}", variablesJson);
            }

            // 8) Média
            MediaAttachment att = null;
            if (media != null && !media.isEmpty()) {
                String ct = Optional.ofNullable(media.getContentType()).orElse(MimeTypeUtils.APPLICATION_OCTET_STREAM_VALUE);
                att = new MediaAttachment(media.getOriginalFilename(), ct, media.getBytes());
                log.debug("[EXTERNAL-WA-API] Média attaché: {}", media.getOriginalFilename());
            }

            // 9) Mise à jour dernière utilisation token
            tokensApp.setLastUsedAt(java.time.ZonedDateTime.now());
            tokensAppRepository.save(tokensApp);

            // 10) ✅ ENVOI WHATSAPP
            log.info("[EXTERNAL-WA-API] Envoi WhatsApp - PhoneNumberId: {}, To: {}", cfg.getPhoneNumberId(), phone);

            SendMessageResult result = sendWhatsappService.sendTemplateWithMedia(phone, phoneId, template, vars, att, cfg.getUserLogin());

            success = result.isSuccess();
            messageId = result.getMessageId();
            errorMessage = result.getError();

            if (!success) {
                log.warn("[EXTERNAL-WA-API] Échec envoi: {}", errorMessage);
            } else {
                log.info("[EXTERNAL-WA-API] ✅ WhatsApp envoyé - MessageId: {}", messageId);
            }

            // 11) ✅ TRAÇABILITÉ : Enregistrer dans la table sms
            ExternalApiTrackingService.ExternalWhatsAppTrackingData trackingData =
                new ExternalApiTrackingService.ExternalWhatsAppTrackingData(
                    tokensApp.getId(),
                    userLogin,
                    tokensApp.getApplication() != null ? tokensApp.getApplication().getName() : "Unknown",
                    phone,
                    businessId,
                    cfg.getPhoneNumberId(),
                    template.getId(),
                    template.getName(),
                    variablesJson,
                    success,
                    messageId, // ✅ MESSAGE ID META (crucial pour webhook)
                    errorMessage
                );

            trackingService.trackExternalWhatsApp(trackingData);

            // 12) Réponse
            if (!success) {
                return ResponseEntity.ok(ApiSendResponse.failure(errorMessage));
            }

            return ResponseEntity.ok(ApiSendResponse.success(messageId));
        } catch (ResourceNotFoundException e) {
            errorMessage = e.getMessage();
            log.warn("[EXTERNAL-WA-API] Ressource non trouvée: {}", errorMessage);

            // ✅ Enregistrer même en cas d'erreur
            tryTrackError(tokensApp, template, cfg, phone, businessId, variablesJson, errorMessage);

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiSendResponse.error(errorMessage));
        } catch (Exception e) {
            errorMessage = "Erreur interne: " + e.getMessage();
            log.error("[EXTERNAL-WA-API] Erreur interne", e);

            // ✅ Enregistrer même en cas d'erreur
            tryTrackError(tokensApp, template, cfg, phone, businessId, variablesJson, errorMessage);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiSendResponse.error(errorMessage));
        }
    }

    /**
     * ✅ ENREGISTRER LES ERREURS (best effort)
     */
    private void tryTrackError(
        TokensApp tokensApp,
        Template template,
        Configuration cfg,
        String phone,
        String businessId,
        String variablesJson,
        String errorMessage
    ) {
        try {
            if (tokensApp != null && template != null && cfg != null) {
                ExternalApiTrackingService.ExternalWhatsAppTrackingData trackingData =
                    new ExternalApiTrackingService.ExternalWhatsAppTrackingData(
                        tokensApp.getId(),
                        tokensApp.getUserLogin(),
                        tokensApp.getApplication() != null ? tokensApp.getApplication().getName() : "Unknown",
                        phone,
                        businessId,
                        cfg.getPhoneNumberId(),
                        template.getId(),
                        template.getName(),
                        variablesJson,
                        false,
                        null, // Pas de messageId en cas d'erreur
                        errorMessage
                    );

                trackingService.trackExternalWhatsApp(trackingData);
            }
        } catch (Exception e) {
            log.error("[EXTERNAL-WA-API] Erreur traçabilité erreur", e);
        }
    }

    // ===== DTO =====

    public static class ApiSendResponse {

        public boolean success;
        public String messageId;
        public String error;

        public static ApiSendResponse success(String id) {
            var r = new ApiSendResponse();
            r.success = true;
            r.messageId = id;
            return r;
        }

        public static ApiSendResponse failure(String err) {
            var r = new ApiSendResponse();
            r.success = false;
            r.error = err;
            return r;
        }

        public static ApiSendResponse error(String err) {
            var r = new ApiSendResponse();
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

    public static class MediaAttachment {

        private final String filename;
        private final String contentType;
        private final byte[] bytes;

        public MediaAttachment(String filename, String contentType, byte[] bytes) {
            this.filename = filename;
            this.contentType = contentType;
            this.bytes = bytes;
        }

        public String getFilename() {
            return filename;
        }

        public String getContentType() {
            return contentType;
        }

        public byte[] getBytes() {
            return bytes;
        }
    }
}
