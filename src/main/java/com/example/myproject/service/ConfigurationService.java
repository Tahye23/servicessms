package com.example.myproject.service;

import com.example.myproject.config.GraphApiProperties;
import com.example.myproject.domain.ChannelConfiguration;
import com.example.myproject.domain.Configuration;
import com.example.myproject.repository.ChannelConfigurationRepository;
import com.example.myproject.repository.ConfigurationRepository;
import com.example.myproject.security.SecurityUtils;
import com.example.myproject.service.dto.ConfigurationDTO;
import com.example.myproject.service.mapper.ConfigurationMapper;
import com.example.myproject.web.rest.dto.UnifiedConfigurationDTO;
import com.example.myproject.web.rest.errors.BadRequestAlertException;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@Transactional
public class ConfigurationService {

    private final Logger log = LoggerFactory.getLogger(ConfigurationService.class);
    private final GraphApiProperties graphApiProperties;
    private final ConfigurationRepository configurationRepository;
    private final ConfigurationMapper configurationMapper;
    private final RestTemplate restTemplate;
    private String baseUrl = "https://graph.facebook.com/v18.0";
    private final ChannelConfigurationRepository channelConfigurationRepository;

    public ConfigurationService(
        GraphApiProperties graphApiProperties,
        ConfigurationRepository configurationRepository,
        ConfigurationMapper configurationMapper,
        RestTemplateBuilder restTemplateBuilder,
        ChannelConfigurationRepository channelConfigurationRepository
    ) {
        this.graphApiProperties = graphApiProperties;
        this.configurationRepository = configurationRepository;
        this.configurationMapper = configurationMapper;
        this.restTemplate = restTemplateBuilder.build();
        this.channelConfigurationRepository = channelConfigurationRepository;
    }

    /**
     * Appelle l’API Graph pour activer la Coexistence sur le numéro donné.
     */
    public void enableCoexistence(String phoneNumberId, String accessToken) {
        String url = UriComponentsBuilder.fromHttpUrl(graphApiProperties.getBaseUrl() + "/" + phoneNumberId + "/coexistence")
            .queryParam("access_token", accessToken)
            .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Si besoin d’un body vide ou personnalisé, remplacez null par un Map<String,Object>
        HttpEntity<Void> entity = new HttpEntity<>(null, headers);

        ResponseEntity<CoexistenceResponse> response = restTemplate.exchange(url, HttpMethod.POST, entity, CoexistenceResponse.class);

        if (
            !response.getStatusCode().is2xxSuccessful() ||
            response.getBody() == null ||
            Boolean.FALSE.equals(response.getBody().getCoexistenceEnabled())
        ) {
            throw new IllegalStateException("Activation Coexistence échouée : " + response.getStatusCode());
        }
    }

    @Transactional(readOnly = true)
    public List<UnifiedConfigurationDTO> getAllForCurrentUser() {
        String login = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new BadRequestAlertException("User not found", "config", "nouser"));

        List<UnifiedConfigurationDTO> result = new ArrayList<>();

        // 1️⃣ WhatsApp (table configuration)
        configurationRepository
            .findOneByUserLogin(login)
            .ifPresent(cfg -> {
                UnifiedConfigurationDTO dto = new UnifiedConfigurationDTO();
                dto.setChannel("WHATSAPP");
                dto.setId(cfg.getId());
                dto.setVerified(cfg.isVerified());
                dto.setExtraInfo("BusinessId: " + cfg.getBusinessId());
                result.add(dto);
            });

        // 2️⃣ SMS + EMAIL (table channel_configuration)
        List<ChannelConfiguration> channels = channelConfigurationRepository
            .findAll()
            .stream()
            .filter(c -> login.equals(c.getUserLogin()))
            .toList();

        for (ChannelConfiguration c : channels) {
            UnifiedConfigurationDTO dto = new UnifiedConfigurationDTO();
            dto.setChannel(c.getChannelType().name());
            dto.setId(c.getId());
            dto.setVerified(c.getVerified());
            dto.setUsername(c.getUsername());
            dto.setHost(c.getHost());
            dto.setPort(c.getPort());

            if ("SMS".equals(c.getChannelType().name())) {
                dto.setExtraInfo("Opérateur: " + c.getSmsOperator());
            }

            result.add(dto);
        }

        return result;
    }

    /**
     * Crée ou met à jour la config (vous l’avez déjà)…
     */
    public ConfigurationDTO createOrUpdateForPartner(ConfigurationDTO config) {
        String userLogin = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new BadRequestAlertException("User login not found", "configuration", "nouser"));

        Configuration cfg = configurationRepository
            .findOneByUserLogin(userLogin)
            .orElseGet(() -> {
                Configuration c = new Configuration();
                c.setUserLogin(userLogin);
                c.setCreatedAt(Instant.now());
                return c;
            });

        // mise à jour des champs
        cfg.setBusinessId(config.getBusinessId());
        cfg.setAccessToken(config.getAccessToken());
        cfg.setPhoneNumberId(config.getPhoneNumberId());
        cfg.setAppId(config.getAppId());

        Configuration saved = configurationRepository.save(cfg);
        return configurationMapper.toDto(saved);
    }

    /**
     * Valide auprès de Meta que businessId et phoneNumberId sont corrects.
     * Lève BadRequestAlertException en cas d’erreur.
     */
    /**
     * Valide auprès de Meta que businessId et phoneNumberId sont corrects.
     * Lève BadRequestAlertException en cas d’erreur 400 ou si l’ID n’est pas trouvé.
     */
    public void testConfigurationForPartner(ConfigurationDTO dto) {
        String login = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new BadRequestAlertException("User login not found", "configuration", "nouser"));
        Configuration cfg = configurationRepository
            .findOneByUserLogin(login)
            .orElseThrow(() -> new BadRequestAlertException("No configuration", "configuration", "noconfig"));

        String baseUrl = graphApiProperties.getBaseUrl();
        String token = dto.getAccessToken();
        String businessId = dto.getBusinessId();
        String phoneNumberId = dto.getPhoneNumberId();
        String appId = dto.getAppId();

        // 0️⃣ Vérification App ID
        try {
            String url = String.format("%s/%s?fields=name&access_token=%s", baseUrl, appId, token);
            BusinessCheckResponse resp = restTemplate.getForObject(url, BusinessCheckResponse.class);
            if (resp == null || resp.getName() == null) {
                throw new BadRequestAlertException("App ID invalide", "configuration", "invalidAppId");
            }
        } catch (HttpClientErrorException ex) {
            throw new BadRequestAlertException("App ID invalide", "configuration", "invalidAppId");
        }

        // 1️⃣ Vérification Business ID
        try {
            String url = String.format("%s/%s?fields=name&access_token=%s", baseUrl, businessId, token);
            BusinessCheckResponse resp = restTemplate.getForObject(url, BusinessCheckResponse.class);
            if (resp == null || resp.getName() == null) {
                throw new BadRequestAlertException("Business ID invalide", "configuration", "invalidBusinessId");
            }
        } catch (HttpClientErrorException ex) {
            throw new BadRequestAlertException("Business ID invalide", "configuration", "invalidBusinessId");
        }

        // 2️⃣ Vérification Phone Number ID
        try {
            String url = String.format("%s/%s/phone_numbers?access_token=%s", baseUrl, businessId, token);
            PhoneNumbersResponse resp = restTemplate.getForObject(url, PhoneNumbersResponse.class);
            boolean ok = resp != null && resp.getData() != null && resp.getData().stream().anyMatch(p -> p.getId().equals(phoneNumberId));
            if (!ok) {
                throw new BadRequestAlertException("Phone Number ID invalide", "configuration", "invalidPhoneNumberId");
            }
        } catch (HttpClientErrorException ex) {
            throw new BadRequestAlertException("Phone Number ID invalide", "configuration", "invalidPhoneNumberId");
        }

        // 3️⃣ Si tout est OK, on marque verified=true
        cfg.setVerified(true);
        cfg.setValid(true);
        configurationRepository.save(cfg);
    }

    public ConfigurationDTO finalizeConfiguration(String longLivedToken, String webhookCallbackUrl) {
        String userLogin = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new BadRequestAlertException("User login not found", "configuration", "nouser"));

        Configuration cfg = configurationRepository
            .findOneByUserLogin(userLogin)
            .orElseGet(() -> {
                Configuration c = new Configuration();
                c.setUserLogin(userLogin);
                c.setCreatedAt(Instant.now());
                return c;
            });

        String baseUrl = graphApiProperties.getBaseUrl(); // ex: https://graph.facebook.com/v22.0

        try {
            // 1️⃣ Debug token pour récupérer le WABA ID depuis granular_scopes
            String debugUrl = baseUrl + "/debug_token" + "?input_token=" + longLivedToken + "&access_token=" + longLivedToken;

            DebugTokenResponse debugResp = callGraphApi(debugUrl, DebugTokenResponse.class);

            // Extraire le WABA ID depuis granular_scopes
            String wabaId = debugResp
                .getData()
                .getGranularScopes()
                .stream()
                .filter(
                    scope ->
                        "whatsapp_business_management".equals(scope.getScope()) || "whatsapp_business_messaging".equals(scope.getScope())
                )
                .map(GranularScope::getTargetIds)
                .flatMap(List::stream)
                .findFirst()
                .orElseThrow(() -> new BadRequestAlertException("No WhatsApp Business Account found in token", "configuration", "nowaba"));
            String appId = debugResp.getData().getAppId();
            log.info("Found WABA ID: {}", wabaId);

            // 2️⃣ Récupérer le Phone Number ID
            String phonesUrl = baseUrl + "/" + wabaId + "/phone_numbers" + "?access_token=" + longLivedToken;

            PhoneNumbersResponse phoneResp = callGraphApi(phonesUrl, PhoneNumbersResponse.class);

            String phoneId = phoneResp
                .getData()
                .stream()
                .findFirst()
                .map(PhoneNumberData::getId)
                .orElseThrow(() -> new BadRequestAlertException("No phone number found for WABA: " + wabaId, "configuration", "nophone"));

            log.info("Found Phone Number ID: {}", phoneId);

            // 3️⃣ Optionnel: Récupérer les détails du compte pour validation
            String wabaDetailsUrl = baseUrl + "/" + wabaId + "?fields=id,name,currency,timezone_id" + "&access_token=" + longLivedToken;

            WhatsAppBusinessAccountDetails wabaDetails = callGraphApi(wabaDetailsUrl, WhatsAppBusinessAccountDetails.class);
            log.info("WABA Details: {} - {}", wabaDetails.getName(), wabaDetails.getId());

            // 4️⃣ Mise à jour et sauvegarde
            cfg.setAccessToken(longLivedToken);
            cfg.setBusinessId(wabaId);
            cfg.setPhoneNumberId(phoneId);
            cfg.setAppId(appId);
            cfg.setApplication(debugResp.getData().getApplication()); // Optionnel
            cfg.setVerified(debugResp.getData().getIsValid());
            cfg.setValid(debugResp.getData().getIsValid());
            cfg.setUpdatedAt(Instant.now());

            Configuration saved = configurationRepository.save(cfg);
            log.info("Configuration saved for user: {}", userLogin);
            try {
                String url = graphApiProperties.getBaseUrl() + "/" + wabaId + "/subscribed_apps";

                String systemToken = getConfigurationForCurrentUser().getAccessToken();

                MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
                params.add("subscribed_fields", "messages");

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
                headers.setBearerAuth(systemToken);

                HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

                restTemplate.postForEntity(url, request, String.class);
            } catch (Exception ex) {
                log.error("Webhook subscription failed", ex);
                // Optionnel : ne bloque pas la configuration, juste un log ou une alerte
            }

            return configurationMapper.toDto(saved);
        } catch (Exception ex) {
            log.error("Error finalizing configuration for user: {}", userLogin, ex);
            throw new BadRequestAlertException("Failed to configure WhatsApp: " + ex.getMessage(), "configuration", "config-error");
        }
    }

    /**
     * Méthode de test pour vérifier la configuration
     */
    public boolean testWhatsAppConnection(String wabaId, String phoneId, String accessToken) {
        try {
            String baseUrl = graphApiProperties.getBaseUrl();
            String testUrl =
                baseUrl + "/" + phoneId + "?fields=id,display_phone_number,verified_name,status" + "&access_token=" + accessToken;

            PhoneNumberDetails details = callGraphApi(testUrl, PhoneNumberDetails.class);
            log.info("Phone details: {} - {} ({})", details.getDisplayPhoneNumber(), details.getVerifiedName(), details.getStatus());

            return "CONNECTED".equals(details.getStatus());
        } catch (Exception ex) {
            log.error("Connection test failed", ex);
            return false;
        }
    }

    @Transactional(readOnly = true)
    public ConfigurationDTO getConfigurationForCurrentUser() {
        return SecurityUtils.getCurrentUserLogin()
            .flatMap(configurationRepository::findFullByUserLogin)
            .map(configurationMapper::toDto)
            .orElse(null);
    }

    /**
     * Generic Graph API GET helper avec gestion d'erreurs améliorée
     */
    private <T> T callGraphApi(String uri, Class<T> responseType) {
        try {
            log.debug("Calling Graph API: {}", uri.replaceAll("access_token=[^&]*", "access_token=***"));

            ResponseEntity<T> response = restTemplate.getForEntity(uri, responseType);

            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            } else {
                throw new BadRequestAlertException(
                    "Graph API returned status: " + response.getStatusCode(),
                    "configuration",
                    "graph-error"
                );
            }
        } catch (HttpClientErrorException ex) {
            log.error("Graph API client error: {} - Response: {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new BadRequestAlertException("Graph API error: " + ex.getMessage(), "configuration", "graph-client-error");
        } catch (Exception ex) {
            log.error("Graph API call failed: {}", uri, ex);
            throw new BadRequestAlertException("Graph API error: " + ex.getMessage(), "configuration", "graph-error");
        }
    }

    // ===== CLASSES DTO POUR LA RÉPONSE =====

    public static class DebugTokenResponse {

        @JsonProperty("data")
        private DebugTokenData data;

        public DebugTokenData getData() {
            return data;
        }

        public void setData(DebugTokenData data) {
            this.data = data;
        }
    }

    public static class DebugTokenData {

        @JsonProperty("app_id")
        private String appId;

        @JsonProperty("user_id")
        private String userId;

        @JsonProperty("application")
        private String application;

        @JsonProperty("granular_scopes")
        private List<GranularScope> granularScopes;

        private List<String> scopes;

        @JsonProperty("is_valid")
        private Boolean isValid;

        public String getApplication() {
            return application;
        }

        public Boolean getValid() {
            return isValid;
        }

        // Getters/Setters
        public String getAppId() {
            return appId;
        }

        public void setAppId(String appId) {
            this.appId = appId;
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public List<GranularScope> getGranularScopes() {
            return granularScopes;
        }

        public void setGranularScopes(List<GranularScope> granularScopes) {
            this.granularScopes = granularScopes;
        }

        public List<String> getScopes() {
            return scopes;
        }

        public void setScopes(List<String> scopes) {
            this.scopes = scopes;
        }

        public Boolean getIsValid() {
            return isValid;
        }

        public void setIsValid(Boolean isValid) {
            this.isValid = isValid;
        }
    }

    public static class GranularScope {

        private String scope;

        @JsonProperty("target_ids")
        private List<String> targetIds;

        public String getScope() {
            return scope;
        }

        public void setScope(String scope) {
            this.scope = scope;
        }

        public List<String> getTargetIds() {
            return targetIds;
        }

        public void setTargetIds(List<String> targetIds) {
            this.targetIds = targetIds;
        }
    }

    public static class PhoneNumbersResponse {

        @JsonProperty("data")
        private List<PhoneNumberData> data;

        public List<PhoneNumberData> getData() {
            return data;
        }

        public void setData(List<PhoneNumberData> data) {
            this.data = data;
        }
    }

    public static class PhoneNumberData {

        private String id;

        @JsonProperty("display_phone_number")
        private String displayPhoneNumber;

        @JsonProperty("verified_name")
        private String verifiedName;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getDisplayPhoneNumber() {
            return displayPhoneNumber;
        }

        public void setDisplayPhoneNumber(String displayPhoneNumber) {
            this.displayPhoneNumber = displayPhoneNumber;
        }

        public String getVerifiedName() {
            return verifiedName;
        }

        public void setVerifiedName(String verifiedName) {
            this.verifiedName = verifiedName;
        }
    }

    public static class CoexistenceResponse {

        @JsonProperty("coexistence_enabled")
        private Boolean coexistenceEnabled;

        public Boolean getCoexistenceEnabled() {
            return coexistenceEnabled;
        }

        public void setCoexistenceEnabled(Boolean coexistenceEnabled) {
            this.coexistenceEnabled = coexistenceEnabled;
        }
    }

    public static class WhatsAppBusinessAccountDetails {

        private String id;
        private String name;
        private String currency;

        @JsonProperty("timezone_id")
        private String timezoneId;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getCurrency() {
            return currency;
        }

        public void setCurrency(String currency) {
            this.currency = currency;
        }

        public String getTimezoneId() {
            return timezoneId;
        }

        public void setTimezoneId(String timezoneId) {
            this.timezoneId = timezoneId;
        }
    }

    public static class PhoneNumberDetails {

        private String id;

        @JsonProperty("display_phone_number")
        private String displayPhoneNumber;

        @JsonProperty("verified_name")
        private String verifiedName;

        private String status;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getDisplayPhoneNumber() {
            return displayPhoneNumber;
        }

        public void setDisplayPhoneNumber(String displayPhoneNumber) {
            this.displayPhoneNumber = displayPhoneNumber;
        }

        public String getVerifiedName() {
            return verifiedName;
        }

        public void setVerifiedName(String verifiedName) {
            this.verifiedName = verifiedName;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }

    public static class BusinessCheckResponse {

        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
