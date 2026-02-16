package com.example.myproject.service;

import com.example.myproject.domain.Application;
import com.example.myproject.domain.TokensApp;
import com.example.myproject.repository.ApplicationRepository;
import com.example.myproject.repository.TokensAppRepository;
import com.example.myproject.security.SecurityUtils;
import com.example.myproject.service.mapper.ApplicationMapper;
import com.example.myproject.web.rest.dto.ApplicationDTO;
import com.example.myproject.web.rest.errors.BadRequestAlertException;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class ApplicationService {

    private final Logger log = LoggerFactory.getLogger(ApplicationService.class);

    private final ApplicationRepository applicationRepository;
    private final TokensAppRepository tokensAppRepository;
    private final TokensAppService tokensAppService;

    public ApplicationService(
        ApplicationRepository applicationRepository,
        TokensAppRepository tokensAppRepository,
        TokensAppService tokensAppService
    ) {
        this.applicationRepository = applicationRepository;
        this.tokensAppRepository = tokensAppRepository;
        this.tokensAppService = tokensAppService;
    }

    /**
     * Sauvegarde ou met à jour une application (méthode standard).
     * Crée automatiquement un token initial avec expiration à 6 mois.
     */
    public Application save(Application application) {
        log.debug("Request to save Application : {}", application);

        // Validation du nom unique par utilisateur
        if (application.getUtilisateur() != null) {
            boolean nameExists = applicationRepository.existsByNameAndUtilisateurIdAndIdNot(
                application.getName(),
                application.getUtilisateur().getId().intValue(),
                application.getId()
            );

            if (nameExists) {
                throw new BadRequestAlertException("Une application avec ce nom existe déjà", "Application", "nameexists");
            }
        }

        boolean isNewApplication = application.getId() == null;
        Application savedApplication = applicationRepository.save(application);

        // Créer un token initial si c'est une nouvelle application (avec expiration par défaut)
        if (isNewApplication) {
            String currentUserLogin = SecurityUtils.getCurrentUserLogin()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated"));

            // ✅ Par défaut, 6 mois d'expiration
            createInitialToken(savedApplication, currentUserLogin, ZonedDateTime.now().plusMonths(6));
            log.info("Initial token created for new application: {}", savedApplication.getId());
        }

        return savedApplication;
    }

    /**
     * ✅ NOUVELLE MÉTHODE: Sauvegarde une application avec date d'expiration personnalisée du token
     */
    public Application saveWithTokenExpiration(Application application, ZonedDateTime tokenExpiration) {
        log.debug("Request to save Application with custom token expiration: {}", application);

        // Validation du nom unique par utilisateur
        if (application.getUtilisateur() != null) {
            boolean nameExists = applicationRepository.existsByNameAndUtilisateurIdAndIdNot(
                application.getName(),
                application.getUtilisateur().getId().intValue(),
                application.getId()
            );

            if (nameExists) {
                throw new BadRequestAlertException("Une application avec ce nom existe déjà", "Application", "nameexists");
            }
        }

        boolean isNewApplication = application.getId() == null;
        Application savedApplication = applicationRepository.save(application);

        // Créer un token initial si c'est une nouvelle application
        if (isNewApplication) {
            String currentUserLogin = SecurityUtils.getCurrentUserLogin()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated"));

            // ✅ Utiliser la date d'expiration fournie (peut être null pour jamais expirer)
            createInitialToken(savedApplication, currentUserLogin, tokenExpiration);
            log.info(
                "Initial token created for new application: {} with expiration: {}",
                savedApplication.getId(),
                tokenExpiration != null ? tokenExpiration : "NEVER"
            );
        }

        return savedApplication;
    }

    /**
     * ✅ MODIFIÉ: Crée un token initial avec date d'expiration paramétrable
     */
    private void createInitialToken(Application application, String userLogin, ZonedDateTime dateExpiration) {
        TokensApp initialToken = new TokensApp();
        initialToken.setApplication(application);
        initialToken.setUserLogin(userLogin);
        initialToken.setActive(true);
        initialToken.setCreatedAt(ZonedDateTime.now());

        // ✅ Définir la date d'expiration (peut être null)
        initialToken.setDateExpiration(dateExpiration);

        tokensAppService.generateAndSaveToken(initialToken);
    }

    @Transactional(readOnly = true)
    public List<Application> findAll() {
        return applicationRepository.findAllWithTokens();
    }

    @Transactional(readOnly = true)
    public ApplicationDTO findOne1(Integer id) {
        return applicationRepository
            .findWithAllById(id)
            .map(ApplicationMapper::toDto)
            .orElseThrow(() -> new RuntimeException("Application not found"));
    }

    @Transactional(readOnly = true)
    public Optional<Application> findOne(Integer id) {
        return applicationRepository.findById(id);
    }

    /**
     * Supprime une application par son ID.
     * Les tokens associés seront supprimés automatiquement via cascade.
     */
    public void delete(Integer id) {
        log.debug("Request to delete Application : {}", id);
        applicationRepository.deleteById(id);
    }

    /**
     * Récupère le token actif d'une application
     */
    @Transactional(readOnly = true)
    public Optional<TokensApp> getActiveToken(Integer applicationId) {
        return tokensAppRepository.findFirstByApplication_IdAndActiveTrueAndIsExpiredFalseOrderByCreatedAtDesc(applicationId);
    }

    /**
     * Récupère tous les tokens d'une application
     */
    @Transactional(readOnly = true)
    public List<TokensApp> getApplicationTokens(Integer applicationId) {
        return tokensAppRepository.findByApplication_IdOrderByCreatedAtDesc(applicationId);
    }

    /**
     * Vérifie si une application a un token valide
     */
    @Transactional(readOnly = true)
    public boolean hasValidToken(Integer applicationId) {
        return tokensAppRepository.existsByApplication_IdAndActiveTrueAndIsExpiredFalse(applicationId);
    }

    /**
     * Valide un token par sa valeur
     */
    @Transactional(readOnly = true)
    public Optional<TokensApp> validateToken(String tokenValue) {
        Optional<TokensApp> tokensApp = tokensAppRepository.findByToken(tokenValue);

        if (tokensApp.isPresent() && tokensApp.get().isValid()) {
            return tokensApp;
        }

        return Optional.empty();
    }

    /**
     * Obtient les statistiques des tokens pour une application
     */
    @Transactional(readOnly = true)
    public TokenStatsDto getTokenStatistics(Integer applicationId) {
        List<TokensApp> tokens = tokensAppRepository.findByApplication_IdOrderByCreatedAtDesc(applicationId);

        long total = tokens.size();
        long active = tokens.stream().filter(t -> Boolean.TRUE.equals(t.getActive()) && !t.isExpiredNow()).count();
        long inactive = tokens.stream().filter(t -> !Boolean.TRUE.equals(t.getActive())).count();
        long expired = tokens.stream().filter(t -> t.isExpiredNow()).count();

        return new TokenStatsDto(total, active, inactive, expired);
    }

    /**
     * DTO pour les statistiques des tokens
     */
    public static class TokenStatsDto {

        private long total;
        private long active;
        private long inactive;
        private long expired;

        public TokenStatsDto(long total, long active, long inactive, long expired) {
            this.total = total;
            this.active = active;
            this.inactive = inactive;
            this.expired = expired;
        }

        public long getTotal() {
            return total;
        }

        public long getActive() {
            return active;
        }

        public long getInactive() {
            return inactive;
        }

        public long getExpired() {
            return expired;
        }
    }
}
