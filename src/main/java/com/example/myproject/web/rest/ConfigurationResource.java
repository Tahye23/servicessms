package com.example.myproject.web.rest;

import com.example.myproject.domain.Configuration;
import com.example.myproject.repository.ConfigurationRepository;
import com.example.myproject.service.ConfigurationService;
import com.example.myproject.service.dto.CoexistenceRequest;
import com.example.myproject.service.dto.ConfigurationDTO;
import com.example.myproject.web.rest.dto.UnifiedConfigurationDTO;
import com.example.myproject.web.rest.errors.BadRequestAlertException;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/configuration")
public class ConfigurationResource {

    private final ConfigurationRepository configurationRepository;
    private final Logger log = LoggerFactory.getLogger(ConfigurationResource.class);
    private final ConfigurationService configurationService;

    public ConfigurationResource(ConfigurationRepository configurationRepository, ConfigurationService configurationService) {
        this.configurationRepository = configurationRepository;
        this.configurationService = configurationService;
    }

    @GetMapping("/all")
    public ResponseEntity<List<UnifiedConfigurationDTO>> getAllConfigurationsForCurrentUser() {
        return ResponseEntity.ok(configurationService.getAllForCurrentUser());
    }

    /**
     * POST /partner : crée ou met à jour la config du partner.
     */
    @PostMapping("/partner")
    public ResponseEntity<ConfigurationDTO> createForPartner(@RequestBody ConfigurationDTO config) {
        log.debug("REST request to create configuration for partner: {}", config.getUserLogin());
        ConfigurationDTO result = configurationService.createOrUpdateForPartner(config);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/meta/coexistence")
    public ResponseEntity<Void> activateCoexistence(@RequestBody CoexistenceRequest request) {
        configurationService.enableCoexistence(request.getPhoneNumberId(), request.getAccessToken());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/admin/app-id")
    public ResponseEntity<String> getAdminAppIdEndpoint() {
        String appId = configurationRepository
            .findOneByUserLogin("admin")
            .map(Configuration::getAppId)
            .orElseThrow(() -> new BadRequestAlertException("Admin configuration not found", "configuration", "noadminconfig"));
        return ResponseEntity.ok(appId);
    }

    /**
     * GET  /partner : Récupère la configuration du user connecté.
     */
    @GetMapping("/partner")
    public ResponseEntity<ConfigurationDTO> getConfigurationForCurrentUser() {
        log.debug("REST request to get configuration for current user");
        ConfigurationDTO dto = configurationService.getConfigurationForCurrentUser();
        return ResponseEntity.ok(dto);
    }

    /**
     * POST /partner/test : valide la configuration auprès de l’API Meta.
     */
    @PostMapping("/partner/test")
    public ResponseEntity<Void> testConfigurationForPartner(@RequestBody ConfigurationDTO config) {
        log.debug("REST request to test configuration for partner");
        configurationService.testConfigurationForPartner(config);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/meta-signup")
    public ResponseEntity<ConfigurationDTO> finalizeConfiguration(@RequestBody Map<String, String> body) {
        String token = body.get("accessToken");
        String webhookCallbackUrl = body.get("webhookCallbackUrl");

        if (token == null || token.isBlank()) {
            throw new BadRequestAlertException("Missing accessToken", "configuration", "notoken");
        }

        if (webhookCallbackUrl == null || webhookCallbackUrl.isBlank()) {
            throw new BadRequestAlertException("Missing webhookCallbackUrl", "configuration", "nocallback");
        }

        ConfigurationDTO dto = configurationService.finalizeConfiguration(token, webhookCallbackUrl);
        return ResponseEntity.ok(dto);
    }
    // + classe CoexistenceRequest { String phoneNumberId; String accessToken; }

}
