package com.example.myproject.config;

import java.time.Duration;
import org.ehcache.config.builders.*;
import org.ehcache.jsr107.Eh107Configuration;
import org.hibernate.cache.jcache.ConfigSettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.cache.JCacheManagerCustomizer;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.info.GitProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.*;
import tech.jhipster.config.JHipsterProperties;
import tech.jhipster.config.cache.PrefixedKeyGenerator;

@Configuration
@EnableCaching
public class CacheConfiguration {

    private GitProperties gitProperties;
    private BuildProperties buildProperties;
    private final javax.cache.configuration.Configuration<Object, Object> jcacheConfiguration;

    // ✅ NOUVEAUX CACHES POUR API EXTERNE (haute performance)
    private final javax.cache.configuration.Configuration<Object, Object> apiTokenCacheConfig;
    private final javax.cache.configuration.Configuration<Object, Object> apiTemplateCacheConfig;
    private final javax.cache.configuration.Configuration<Object, Object> apiConfigurationCacheConfig;
    private final javax.cache.configuration.Configuration<Object, Object> apiUserCacheConfig;

    public CacheConfiguration(JHipsterProperties jHipsterProperties) {
        JHipsterProperties.Cache.Ehcache ehcache = jHipsterProperties.getCache().getEhcache();

        // Configuration par défaut (existante)
        jcacheConfiguration = Eh107Configuration.fromEhcacheCacheConfiguration(
            CacheConfigurationBuilder.newCacheConfigurationBuilder(
                Object.class,
                Object.class,
                ResourcePoolsBuilder.heap(ehcache.getMaxEntries())
            )
                .withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofSeconds(ehcache.getTimeToLiveSeconds())))
                .build()
        );

        // ✅ CACHE API TOKEN (5 minutes, 5000 entrées max)
        apiTokenCacheConfig = Eh107Configuration.fromEhcacheCacheConfiguration(
            CacheConfigurationBuilder.newCacheConfigurationBuilder(
                Object.class,
                Object.class,
                ResourcePoolsBuilder.heap(5000) // Plus de tokens
            )
                .withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofMinutes(5)))
                .build()
        );

        // ✅ CACHE API TEMPLATE (10 minutes, 2000 entrées max)
        apiTemplateCacheConfig = Eh107Configuration.fromEhcacheCacheConfiguration(
            CacheConfigurationBuilder.newCacheConfigurationBuilder(Object.class, Object.class, ResourcePoolsBuilder.heap(2000))
                .withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofMinutes(10)))
                .build()
        );

        // ✅ CACHE API CONFIGURATION (10 minutes, 1000 entrées max)
        apiConfigurationCacheConfig = Eh107Configuration.fromEhcacheCacheConfiguration(
            CacheConfigurationBuilder.newCacheConfigurationBuilder(Object.class, Object.class, ResourcePoolsBuilder.heap(1000))
                .withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofMinutes(10)))
                .build()
        );

        // ✅ CACHE API USER (15 minutes, 10000 entrées max)
        apiUserCacheConfig = Eh107Configuration.fromEhcacheCacheConfiguration(
            CacheConfigurationBuilder.newCacheConfigurationBuilder(
                Object.class,
                Object.class,
                ResourcePoolsBuilder.heap(10000) // Beaucoup d'utilisateurs
            )
                .withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofMinutes(15)))
                .build()
        );
    }

    @Bean
    public HibernatePropertiesCustomizer hibernatePropertiesCustomizer(javax.cache.CacheManager cacheManager) {
        return hibernateProperties -> hibernateProperties.put(ConfigSettings.CACHE_MANAGER, cacheManager);
    }

    @Bean
    public JCacheManagerCustomizer cacheManagerCustomizer() {
        return cm -> {
            // ===== CACHES EXISTANTS (inchangés) =====
            createCache(cm, com.example.myproject.repository.UserRepository.USERS_BY_LOGIN_CACHE);
            createCache(cm, com.example.myproject.repository.UserRepository.USERS_BY_EMAIL_CACHE);
            createCache(cm, com.example.myproject.domain.User.class.getName());
            createCache(cm, com.example.myproject.domain.Authority.class.getName());
            createCache(cm, com.example.myproject.domain.User.class.getName() + ".authorities");
            createCache(cm, com.example.myproject.domain.Entitedetest.class.getName());
            createCache(cm, com.example.myproject.domain.SendSms.class.getName());
            createCache(cm, com.example.myproject.domain.TokensApp.class.getName());
            createCache(cm, com.example.myproject.domain.Company.class.getName());
            createCache(cm, com.example.myproject.domain.Company.class.getName() + ".services");
            createCache(cm, com.example.myproject.domain.Dialogue.class.getName());
            createCache(cm, com.example.myproject.domain.Contact.class.getName());
            createCache(cm, com.example.myproject.domain.Api.class.getName());
            createCache(cm, com.example.myproject.domain.Groupe.class.getName());
            createCache(cm, com.example.myproject.domain.Groupedecontact.class.getName());
            createCache(cm, com.example.myproject.domain.UserService.class.getName());
            createCache(cm, com.example.myproject.domain.UserTokenApi.class.getName());
            createCache(cm, com.example.myproject.domain.Conversation.class.getName());
            createCache(cm, com.example.myproject.domain.Participant.class.getName());
            createCache(cm, com.example.myproject.domain.Choix.class.getName());
            createCache(cm, com.example.myproject.domain.Question.class.getName());
            createCache(cm, com.example.myproject.domain.Reponse.class.getName());
            createCache(cm, com.example.myproject.domain.OTPStorage.class.getName());
            createCache(cm, com.example.myproject.domain.Abonnement.class.getName());
            createCache(cm, com.example.myproject.domain.PlanAbonnement.class.getName());
            createCache(cm, com.example.myproject.domain.ExtendedUser.class.getName());
            createCache(cm, com.example.myproject.domain.Groupe.class.getName() + ".extendedUsers");
            createCache(cm, com.example.myproject.domain.ExtendedUser.class.getName() + ".groupes");
            createCache(cm, com.example.myproject.domain.Groupedecontact.class.getName() + ".groupes");
            createCache(cm, com.example.myproject.domain.Groupe.class.getName() + ".groupedecontacts");
            createCache(cm, com.example.myproject.domain.Contact.class.getName() + ".groupedecontacts");
            createCache(cm, com.example.myproject.domain.Service.class.getName());
            createCache(cm, com.example.myproject.domain.Service.class.getName() + ".applications");
            createCache(cm, com.example.myproject.domain.UserService.class.getName() + ".extendedUsers");
            createCache(cm, com.example.myproject.domain.UserService.class.getName() + ".services");
            createCache(cm, com.example.myproject.domain.ExtendedUser.class.getName() + ".userservices");
            createCache(cm, com.example.myproject.domain.Service.class.getName() + ".userservices");
            createCache(cm, com.example.myproject.domain.Fileextrait.class.getName());
            createCache(cm, com.example.myproject.domain.Customer.class.getName());
            createCache(cm, com.example.myproject.domain.Referentiel.class.getName());

            // ✅ NOUVEAUX CACHES API EXTERNE (avec configs optimisées)
            createApiCache(cm, "api-tokens", apiTokenCacheConfig);
            createApiCache(cm, "api-templates", apiTemplateCacheConfig);
            createApiCache(cm, "api-configurations", apiConfigurationCacheConfig);
            createApiCache(cm, "api-users", apiUserCacheConfig);
            // jhipster-needle-ehcache-add-entry
        };
    }

    /**
     * ✅ Créer un cache avec configuration par défaut
     */
    private void createCache(javax.cache.CacheManager cm, String cacheName) {
        javax.cache.Cache<Object, Object> cache = cm.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        } else {
            cm.createCache(cacheName, jcacheConfiguration);
        }
    }

    /**
     * ✅ NOUVEAU : Créer un cache API avec configuration spécifique
     */
    private void createApiCache(
        javax.cache.CacheManager cm,
        String cacheName,
        javax.cache.configuration.Configuration<Object, Object> config
    ) {
        javax.cache.Cache<Object, Object> cache = cm.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        } else {
            cm.createCache(cacheName, config);
        }
    }

    @Autowired(required = false)
    public void setGitProperties(GitProperties gitProperties) {
        this.gitProperties = gitProperties;
    }

    @Autowired(required = false)
    public void setBuildProperties(BuildProperties buildProperties) {
        this.buildProperties = buildProperties;
    }

    @Bean
    public KeyGenerator keyGenerator() {
        return new PrefixedKeyGenerator(this.gitProperties, this.buildProperties);
    }
}
