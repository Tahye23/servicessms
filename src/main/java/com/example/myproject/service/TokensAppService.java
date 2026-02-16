package com.example.myproject.service;

import static com.example.myproject.security.SecurityUtils.JWT_ALGORITHM;

import com.example.myproject.domain.Application;
import com.example.myproject.domain.TokensApp;
import com.example.myproject.repository.TokensAppRepository;
import com.example.myproject.security.SecurityUtils;
import com.example.myproject.service.dto.TokensAppDTO;
import com.example.myproject.web.rest.dto.ApplicationDTO;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jose.jws.JwsAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class TokensAppService {

    private final Logger log = LoggerFactory.getLogger(TokensAppService.class);
    private final TokensAppRepository tokensAppRepository;
    private final JwtEncoder jwtEncoder;
    private final SecurityUtils securityUtils;

    @Value("${jhipster.security.authentication.jwt.token-validity-in-seconds:0}")
    private long tokenValidityInSeconds;

    public TokensAppService(SecurityUtils securityUtils, TokensAppRepository tokensAppRepository, JwtEncoder jwtEncoder) {
        this.tokensAppRepository = tokensAppRepository;
        this.jwtEncoder = jwtEncoder;
        this.securityUtils = securityUtils;
    }

    // =====================
    // GENERATION DE TOKEN
    // =====================
    private String generateTokenValue(Application application) {
        if (application == null) {
            // Token général → JWT
            return generateJwtToken();
        } else {
            // Token spécifique à une application → UUID opaque
            return "app_" + application.getId() + "_" + UUID.randomUUID().toString().replace("-", "");
        }
    }

    private String generateJwtToken() {
        Instant now = Instant.now();
        Instant expiry = now.plus(tokenValidityInSeconds, ChronoUnit.SECONDS);
        String roles = securityUtils.getCurrentUserRoles();

        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuedAt(now)
            .expiresAt(expiry)
            .subject("General API Token")
            .claim("tokenType", "general")
            .claim("roles", roles)
            .build();

        JwsHeader jwsHeader = JwsHeader.with(JWT_ALGORITHM).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).getTokenValue();
    }

    // =====================
    // CREATE
    // =====================
    public TokensApp generateAndSaveToken(TokensApp tokensApp) {
        if (tokensApp == null) throw new IllegalArgumentException("TokensApp invalide");

        tokensApp.setToken(generateTokenValue(tokensApp.getApplication()));
        tokensApp.setActive(tokensApp.getActive() != null ? tokensApp.getActive() : true);
        tokensApp.setIsExpired(false);
        tokensApp.setCreatedAt(ZonedDateTime.now());

        return tokensAppRepository.save(tokensApp);
    }

    // =====================
    // READ
    // =====================
    @Transactional(readOnly = true)
    public TokensAppDTO findById(Integer id) {
        TokensApp token = tokensAppRepository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Token non trouvé"));
        return mapToDTO(token);
    }

    @Transactional(readOnly = true)
    public TokensAppDTO findByToken(String tokenValue) {
        TokensApp token = tokensAppRepository
            .findByToken(tokenValue)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Token non trouvé"));
        return mapToDTO(token);
    }

    @Transactional(readOnly = true)
    public List<TokensAppDTO> findAll() {
        return tokensAppRepository.findAll().stream().map(this::mapToDTO).toList();
    }

    // =====================
    // UPDATE
    // =====================
    public TokensApp regenerateToken(Integer id) {
        TokensApp token = getTokenOrThrow(id);
        token.setToken(generateTokenValue(token.getApplication()));
        token.setIsExpired(false);
        token.setActive(true);
        return tokensAppRepository.save(token);
    }

    public TokensApp activateToken(Integer id) {
        TokensApp token = getTokenOrThrow(id);
        token.setActive(true);
        return tokensAppRepository.save(token);
    }

    public TokensApp deactivateToken(Integer id) {
        TokensApp token = getTokenOrThrow(id);
        token.setActive(false);
        return tokensAppRepository.save(token);
    }

    // =====================
    // VALIDATION
    // =====================
    @Transactional(readOnly = true)
    public boolean isTokenValid(String token) {
        return tokensAppRepository.findByToken(token).map(TokensApp::isValid).orElse(false);
    }

    public void updateLastUsedAt(String token) {
        tokensAppRepository
            .findByToken(token)
            .ifPresent(t -> {
                t.setLastUsedAt(ZonedDateTime.now());
                tokensAppRepository.save(t);
            });
    }

    // =====================
    // DELETE
    // =====================
    public void deleteToken(Integer id) {
        if (!tokensAppRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Token non trouvé");
        }
        tokensAppRepository.deleteById(id);
    }

    // =====================
    // HELPERS
    // =====================
    private TokensApp getTokenOrThrow(Integer id) {
        return tokensAppRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Token non trouvé"));
    }

    public TokensAppDTO mapToDTO(TokensApp token) {
        TokensAppDTO dto = new TokensAppDTO();
        dto.setId(token.getId());
        dto.setToken(token.getToken());
        dto.setActive(token.getActive());
        dto.setIsExpired(token.getIsExpired());
        dto.setCreatedAt(token.getCreatedAt());
        dto.setLastUsedAt(token.getLastUsedAt());

        if (token.getApplication() != null) {
            Application app = token.getApplication();
            ApplicationDTO appDTO = new ApplicationDTO();
            appDTO.setId(app.getId());
            appDTO.setName(app.getName());
            dto.setApplication(appDTO);
            dto.setApplicationId(app.getId());
            dto.setApplicationName(app.getName());
        }

        return dto;
    }
}
