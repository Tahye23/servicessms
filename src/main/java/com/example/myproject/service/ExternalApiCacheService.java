package com.example.myproject.service;

import com.example.myproject.domain.Configuration;
import com.example.myproject.domain.Template;
import com.example.myproject.domain.TokensApp;
import com.example.myproject.domain.User;
import com.example.myproject.repository.ConfigurationRepository;
import com.example.myproject.repository.TemplateRepository;
import com.example.myproject.repository.TokensAppRepository;
import com.example.myproject.repository.UserRepository;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 *
 * Stratégie de cache :
 * - Token : 5 minutes (validation fréquente)
 * - Template : 10 minutes (rarement modifié)
 * - Configuration : 10 minutes (rarement modifiée)
 * - User : 15 minutes (très stable)
 *
 * Performance attendue :
 * - Avec cache : ~2-5ms par requête
 * - Sans cache : ~50-100ms par requête
 * - Gain : 10-20x plus rapide
 */
@Service
@EnableCaching
public class ExternalApiCacheService {

    private static final Logger log = LoggerFactory.getLogger(ExternalApiCacheService.class);

    @Autowired
    private TokensAppRepository tokensAppRepository;

    @Autowired
    private TemplateRepository templateRepository;

    @Autowired
    private ConfigurationRepository configurationRepository;

    @Autowired
    private UserRepository userRepository;

    // ===== CACHE TOKEN (5 minutes) =====

    /**
     * ✅ CACHE TOKEN AVEC TTL 5 MINUTES
     * Validation fréquente car les tokens peuvent être révoqués
     */
    @Cacheable(value = "api-tokens", key = "#token", unless = "#result == null || !#result.isPresent()")
    public Optional<TokensApp> findTokenWithCache(String token) {
        long start = System.currentTimeMillis();

        Optional<TokensApp> result = tokensAppRepository.findByToken(token);

        long duration = System.currentTimeMillis() - start;
        log.debug("[CACHE-TOKEN] DB lookup took {}ms for token: {}", duration, token.substring(0, Math.min(10, token.length())));

        return result;
    }

    /**
     * ✅ INVALIDER LE CACHE TOKEN (lors de modification)
     */
    @CacheEvict(value = "api-tokens", key = "#token")
    public void evictToken(String token) {
        log.info("[CACHE-TOKEN] Éviction cache pour token: {}", token.substring(0, Math.min(10, token.length())));
    }

    // ===== CACHE TEMPLATE (10 minutes) =====

    /**
     * ✅ CACHE TEMPLATE PAR NOM + USER
     */
    @Cacheable(value = "api-templates", key = "#templateName + '_' + #userLogin", unless = "#result == null || !#result.isPresent()")
    public Optional<Template> findTemplateWithCache(String templateName, String userLogin) {
        long start = System.currentTimeMillis();

        Optional<Template> result = templateRepository.findByNameAndUserLogin(templateName, userLogin);

        long duration = System.currentTimeMillis() - start;
        log.debug("[CACHE-TEMPLATE] DB lookup took {}ms for template: {} (user: {})", duration, templateName, userLogin);

        return result;
    }

    /**
     * ✅ INVALIDER CACHE TEMPLATE
     */
    @CacheEvict(value = "api-templates", key = "#templateName + '_' + #userLogin")
    public void evictTemplate(String templateName, String userLogin) {
        log.info("[CACHE-TEMPLATE] Éviction cache pour template: {} (user: {})", templateName, userLogin);
    }

    /**
     * ✅ INVALIDER TOUS LES TEMPLATES D'UN USER
     */
    @CacheEvict(value = "api-templates", allEntries = true)
    public void evictAllTemplatesForUser(String userLogin) {
        log.info("[CACHE-TEMPLATE] Éviction complète des templates pour user: {}", userLogin);
    }

    // ===== CACHE CONFIGURATION (10 minutes) =====

    /**
     * ✅ CACHE CONFIGURATION PAR BUSINESS ID + USER
     */
    @Cacheable(value = "api-configurations", key = "#businessId + '_' + #userLogin", unless = "#result == null || !#result.isPresent()")
    public Optional<Configuration> findConfigurationWithCache(String businessId, String userLogin) {
        long start = System.currentTimeMillis();

        Optional<Configuration> result = configurationRepository.findOneByBusinessIdAndUserLoginIgnoreCase(businessId, userLogin);

        long duration = System.currentTimeMillis() - start;
        log.debug("[CACHE-CONFIG] DB lookup took {}ms for businessId: {} (user: {})", duration, businessId, userLogin);

        return result;
    }

    /**
     * ✅ INVALIDER CACHE CONFIGURATION
     */
    @CacheEvict(value = "api-configurations", key = "#businessId + '_' + #userLogin")
    public void evictConfiguration(String businessId, String userLogin) {
        log.info("[CACHE-CONFIG] Éviction cache pour config: {} (user: {})", businessId, userLogin);
    }

    // ===== CACHE USER (15 minutes) =====

    /**
     * ✅ CACHE USER PAR LOGIN
     */
    @Cacheable(value = "api-users", key = "#userLogin", unless = "#result == null || !#result.isPresent()")
    public Optional<User> findUserWithCache(String userLogin) {
        long start = System.currentTimeMillis();

        Optional<User> result = userRepository.findOneByLogin(userLogin);

        long duration = System.currentTimeMillis() - start;
        log.debug("[CACHE-USER] DB lookup took {}ms for user: {}", duration, userLogin);

        return result;
    }

    /**
     * ✅ INVALIDER CACHE USER
     */
    @CacheEvict(value = "api-users", key = "#userLogin")
    public void evictUser(String userLogin) {
        log.info("[CACHE-USER] Éviction cache pour user: {}", userLogin);
    }

    // ===== NETTOYAGE AUTOMATIQUE (toutes les 5 minutes) =====

    /**
     * ✅ PURGE AUTOMATIQUE CACHE TOKEN (5 minutes)
     */
    @Scheduled(fixedRate = 5, timeUnit = TimeUnit.MINUTES)
    @CacheEvict(value = "api-tokens", allEntries = true)
    public void evictAllTokensCachePeriodically() {
        log.info("[CACHE-PURGE] ♻️ Purge automatique cache tokens (5 min)");
    }

    /**
     * ✅ PURGE AUTOMATIQUE CACHE TEMPLATE (10 minutes)
     */
    @Scheduled(fixedRate = 10, timeUnit = TimeUnit.MINUTES)
    @CacheEvict(value = "api-templates", allEntries = true)
    public void evictAllTemplatesCachePeriodically() {
        log.info("[CACHE-PURGE] ♻️ Purge automatique cache templates (10 min)");
    }

    /**
     * ✅ PURGE AUTOMATIQUE CACHE CONFIGURATION (10 minutes)
     */
    @Scheduled(fixedRate = 10, timeUnit = TimeUnit.MINUTES)
    @CacheEvict(value = "api-configurations", allEntries = true)
    public void evictAllConfigurationsCachePeriodically() {
        log.info("[CACHE-PURGE] ♻️ Purge automatique cache configurations (10 min)");
    }

    /**
     * ✅ PURGE AUTOMATIQUE CACHE USER (15 minutes)
     */
    @Scheduled(fixedRate = 15, timeUnit = TimeUnit.MINUTES)
    @CacheEvict(value = "api-users", allEntries = true)
    public void evictAllUsersCachePeriodically() {
        log.info("[CACHE-PURGE] ♻️ Purge automatique cache users (15 min)");
    }

    // ===== MÉTHODES DE MAINTENANCE =====

    /**
     * ✅ VIDER TOUT LE CACHE (en cas de problème)
     */
    @CacheEvict(value = { "api-tokens", "api-templates", "api-configurations", "api-users" }, allEntries = true)
    public void evictAllCaches() {
        log.warn("[CACHE-PURGE] 🔥 PURGE COMPLÈTE de tous les caches API externe");
    }

    /**
     * ✅ STATISTIQUES DE CACHE (pour monitoring)
     */
    public CacheStats getCacheStats() {
        // À implémenter avec Spring Cache Manager
        return new CacheStats("Cache statistics available via /actuator/metrics/cache.*");
    }

    // ===== CLASSE INTERNE =====

    public static class CacheStats {

        private final String message;

        public CacheStats(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }
}
