package com.example.myproject.web.rest;

import com.example.myproject.domain.Application;
import com.example.myproject.domain.TokensApp;
import com.example.myproject.service.ApplicationService;
import com.example.myproject.service.TokensAppService;
import com.example.myproject.service.mapper.ApplicationMapper;
import com.example.myproject.web.rest.dto.ApplicationDTO;
import com.example.myproject.web.rest.errors.BadRequestAlertException;
import jakarta.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.ResponseUtil;

@RestController
@RequestMapping("/api/applications")
@Transactional
public class ApplicationResource {

    private final Logger log = LoggerFactory.getLogger(ApplicationResource.class);
    private static final String ENTITY_NAME = "application";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final ApplicationService applicationService;
    private final TokensAppService tokensAppService;
    private final ApplicationMapper applicationMapper;

    public ApplicationResource(
        ApplicationMapper applicationMapper,
        ApplicationService applicationService,
        TokensAppService tokensAppService
    ) {
        this.applicationMapper = applicationMapper;
        this.applicationService = applicationService;
        this.tokensAppService = tokensAppService;
    }

    // POST /api/applications : Créer une application

    @PostMapping
    public ResponseEntity<Application> createApplication(@Valid @RequestBody Map<String, Object> request) throws URISyntaxException {
        log.debug("Request to create Application: {}", request);

        String name = (String) request.get("name");
        Integer userId = (Integer) request.get("userId");
        String description = (String) request.get("description");

        Integer dailyLimit = (Integer) request.get("dailyLimit");
        Integer monthlyLimit = (Integer) request.get("monthlyLimit");
        @SuppressWarnings("unchecked")
        List<String> allowedServices = (List<String>) request.get("allowedServices");

        String dateExpirationStr = (String) request.get("dateExpiration");
        Boolean tokenNeverExpires = (Boolean) request.get("tokenNeverExpires");

        ZonedDateTime dateExpiration = null;

        if (Boolean.TRUE.equals(tokenNeverExpires)) {
            dateExpiration = null;
        } else {
            if (dateExpirationStr != null && !dateExpirationStr.isBlank()) {
                try {
                    dateExpiration = ZonedDateTime.parse(dateExpirationStr);
                } catch (Exception e) {
                    throw new BadRequestAlertException("Format de date invalide (ISO-8601 requis)", ENTITY_NAME, "invaliddate");
                }
            } else {
                dateExpiration = ZonedDateTime.now().plusMonths(6);
            }
        }

        Application app = new Application();
        app.setName(name);
        app.setDescription(description);
        app.setDailyLimit(dailyLimit);
        app.setMonthlyLimit(monthlyLimit);

        //    Conversion sécurisée avec gestion d'erreur
        if (allowedServices != null && !allowedServices.isEmpty()) {
            try {
                Set<Application.AllowedService> services = allowedServices
                    .stream()
                    .map(serviceName -> Application.AllowedService.valueOf(serviceName.toUpperCase().trim()))
                    .collect(Collectors.toSet());
                app.setAllowedServices(services);
            } catch (IllegalArgumentException e) {
                throw new BadRequestAlertException(
                    "Service non autorisé. Services valides: " + Arrays.toString(Application.AllowedService.values()),
                    ENTITY_NAME,
                    "invalidservice"
                );
            }
        }

        Application result = applicationService.saveWithTokenExpiration(app, dateExpiration);

        return ResponseEntity.created(new URI("/api/applications/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    // PUT /api/applications/{id} : Mettre à jour une application
    // PUT /api/applications/{id} : Mettre à jour une application
    @PutMapping("/{id:\\d+}")
    public ResponseEntity<ApplicationDTO> updateApplication(@PathVariable Integer id, @Valid @RequestBody Application application) {
        if (application.getId() == null || !Objects.equals(id, application.getId())) {
            throw new BadRequestAlertException("ID invalide", ENTITY_NAME, "idinvalid");
        }

        // Charger l'entité existante
        Application existingApp = applicationService
            .findOne(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application non trouvée"));

        // Mettre à jour uniquement les champs spécifiques
        existingApp.setName(application.getName());
        existingApp.setDescription(application.getDescription());
        existingApp.setDailyLimit(application.getDailyLimit());
        existingApp.setMonthlyLimit(application.getMonthlyLimit());
        existingApp.setEnvironment(application.getEnvironment());
        existingApp.setWebhookUrl(application.getWebhookUrl());
        existingApp.setWebhookSecret(application.getWebhookSecret());
        existingApp.setAllowedServices(application.getAllowedServices());

        // Préserver isActive si null
        if (application.getIsActive() != null) {
            existingApp.setIsActive(application.getIsActive());
        }

        Application result = applicationService.save(existingApp);

        // ✅ Retourner un DTO au lieu de l'entité complète
        ApplicationDTO dto = ApplicationMapper.toDto(result);

        return ResponseEntity.ok().headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, id.toString())).body(dto);
    }

    // GET /api/applications : Liste des applications
    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<List<ApplicationDTO>> getAllApplications() {
        List<Application> apps = applicationService.findAll();
        List<ApplicationDTO> dtos = apps.stream().map(ApplicationMapper::toDto).toList();

        return ResponseEntity.ok(dtos);
    }

    // GET /api/applications/stats : Statistiques globales
    @GetMapping("/stats")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getApplicationStats() {
        List<Application> apps = applicationService.findAll();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalApplications", apps.size());
        stats.put("todayApiCalls", apps.stream().mapToLong(a -> a.getCurrentDailyUsage() != null ? a.getCurrentDailyUsage() : 0).sum());
        stats.put(
            "monthlyApiCalls",
            apps.stream().mapToLong(a -> a.getCurrentMonthlyUsage() != null ? a.getCurrentMonthlyUsage() : 0).sum()
        );

        return ResponseEntity.ok(stats);
    }

    // GET /api/applications/{id}
    @GetMapping("/{id}")
    public ResponseEntity<ApplicationDTO> getApplication(@PathVariable Integer id) {
        ApplicationDTO dto = applicationService.findOne1(id);
        return ResponseEntity.ok(dto);
    }

    // DELETE /api/applications/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteApplication(@PathVariable Integer id) {
        if (applicationService.findOne(id).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Application non trouvée");
        }

        applicationService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }

    // TOKENS
    // GET /api/applications/{id}/tokens
    @GetMapping("/{id:\\d+}/tokens")
    @Transactional(readOnly = true)
    public ResponseEntity<List<TokensApp>> getApplicationTokens(@PathVariable Integer id) {
        if (applicationService.findOne(id).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Application non trouvée");
        }
        return ResponseEntity.ok(applicationService.getApplicationTokens(id));
    }

    // GET /api/applications/{id}/tokens/active
    @GetMapping("/{id:\\d+}/tokens/active")
    @Transactional(readOnly = true)
    public ResponseEntity<TokensApp> getActiveToken(@PathVariable Integer id) {
        return ResponseUtil.wrapOrNotFound(applicationService.getActiveToken(id));
    }

    // GET /api/applications/{id}/tokens/stats
    @GetMapping("/{id:\\d+}/tokens/stats")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getTokenStats(@PathVariable Integer id) {
        ApplicationService.TokenStatsDto stats = applicationService.getTokenStatistics(id);

        Map<String, Object> response = new HashMap<>();
        response.put("total", stats.getTotal());
        response.put("active", stats.getActive());
        response.put("inactive", stats.getInactive());
        response.put("expired", stats.getExpired());

        return ResponseEntity.ok(response);
    }

    // POST /api/applications/{id}/tokens/validate
    @PostMapping("/{id:\\d+}/tokens/validate")
    public ResponseEntity<Map<String, Object>> validateToken(@PathVariable Integer id, @RequestBody Map<String, String> request) {
        String token = request.get("token");
        if (token == null || token.isBlank()) {
            throw new BadRequestAlertException("Token requis", ENTITY_NAME, "tokenrequired");
        }

        Optional<TokensApp> result = applicationService.validateToken(token);
        Map<String, Object> response = new HashMap<>();

        if (result.isPresent() && result.get().getApplication().getId().equals(id)) {
            tokensAppService.updateLastUsedAt(token);
            response.put("valid", true);
        } else {
            response.put("valid", false);
        }

        return ResponseEntity.ok(response);
    }
}
