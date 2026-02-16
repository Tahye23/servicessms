package com.example.myproject.web.rest;

import com.example.myproject.domain.Application;
import com.example.myproject.domain.TokensApp;
import com.example.myproject.service.ApplicationService;
import com.example.myproject.service.TokensAppService;
import com.example.myproject.service.dto.TokensAppDTO;
import com.example.myproject.web.rest.errors.BadRequestAlertException;
import jakarta.validation.Valid;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import tech.jhipster.web.util.HeaderUtil;

@RestController
@RequestMapping("/api/tokens-apps")
@Transactional
public class TokensAppResource {

    private final Logger log = LoggerFactory.getLogger(TokensAppResource.class);
    private static final String ENTITY_NAME = "tokensApp";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final TokensAppService tokensAppService;
    private final ApplicationService applicationService;

    public TokensAppResource(TokensAppService tokensAppService, ApplicationService applicationService) {
        this.tokensAppService = tokensAppService;
        this.applicationService = applicationService;
    }

    // =====================
    // CREATE TOKEN
    // =====================
    @PostMapping("/create-with-expiration")
    public ResponseEntity<TokensApp> createToken(@Valid @RequestBody CreateTokenRequest request) {
        TokensApp tokensApp = new TokensApp();

        // ✅ Application OPTIONNELLE
        Application application = null;
        if (request.getApplicationId() != null) {
            application = applicationService
                .findOne(request.getApplicationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application non trouvée"));
        }

        tokensApp.setApplication(application);

        tokensApp.setActive(request.getActive() != null ? request.getActive() : true);

        // Expiration
        if (Boolean.TRUE.equals(request.getNeverExpire())) {
            tokensApp.setDateExpiration(null);
        } else {
            if (request.getDateExpiration() == null || request.getDateExpiration().isBlank()) {
                throw new BadRequestAlertException(
                    "dateExpiration obligatoire si neverExpire = false",
                    ENTITY_NAME,
                    "dateexpirationrequired"
                );
            }
            tokensApp.setDateExpiration(ZonedDateTime.parse(request.getDateExpiration()));
        }

        TokensApp result = tokensAppService.generateAndSaveToken(tokensApp);

        return ResponseEntity.ok(result);
    }

    // =====================
    // READ
    // =====================
    @GetMapping
    public ResponseEntity<List<TokensAppDTO>> getAllTokens() {
        return ResponseEntity.ok(tokensAppService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TokensAppDTO> getTokenById(@PathVariable Integer id) {
        return ResponseEntity.ok(tokensAppService.findById(id));
    }

    @GetMapping("/by-value/{token}")
    public ResponseEntity<TokensAppDTO> getTokenByValue(@PathVariable String token) {
        return ResponseEntity.ok(tokensAppService.findByToken(token));
    }

    // =====================
    // ACTIONS
    // =====================
    @PostMapping("/{id}/regenerate")
    public ResponseEntity<TokensApp> regenerate(@PathVariable Integer id) {
        return ResponseEntity.ok(tokensAppService.regenerateToken(id));
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<TokensApp> activate(@PathVariable Integer id) {
        return ResponseEntity.ok(tokensAppService.activateToken(id));
    }

    @PostMapping("/{id}/deactivate")
    public ResponseEntity<TokensApp> deactivate(@PathVariable Integer id) {
        return ResponseEntity.ok(tokensAppService.deactivateToken(id));
    }

    // =====================
    // VALIDATION
    // =====================
    @PostMapping("/{id}/validate")
    public ResponseEntity<Map<String, Object>> validateToken(@PathVariable Integer id, @RequestBody ValidateTokenRequest request) {
        if (request.getToken() == null || request.getToken().isBlank()) {
            throw new BadRequestAlertException("Token requis", ENTITY_NAME, "tokenrequired");
        }

        boolean valid = tokensAppService.isTokenValid(request.getToken());
        if (valid) {
            tokensAppService.updateLastUsedAt(request.getToken());
        }

        Map<String, Object> response = new HashMap<>();
        response.put("valid", valid);
        return ResponseEntity.ok(response);
    }

    // =====================
    // DELETE
    // =====================
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        tokensAppService.deleteToken(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }

    // =====================
    // DTOs
    // =====================
    public static class CreateTokenRequest {

        private Integer applicationId;

        // ISO-8601
        private String dateExpiration;

        // NOUVEAU
        private Boolean neverExpire;

        private Boolean active;

        public Integer getApplicationId() {
            return applicationId;
        }

        public void setApplicationId(Integer applicationId) {
            this.applicationId = applicationId;
        }

        public String getDateExpiration() {
            return dateExpiration;
        }

        public void setDateExpiration(String dateExpiration) {
            this.dateExpiration = dateExpiration;
        }

        public Boolean getNeverExpire() {
            return neverExpire;
        }

        public void setNeverExpire(Boolean neverExpire) {
            this.neverExpire = neverExpire;
        }

        public Boolean getActive() {
            return active;
        }

        public void setActive(Boolean active) {
            this.active = active;
        }
    }

    public static class ValidateTokenRequest {

        private String token;

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }
    }
}
