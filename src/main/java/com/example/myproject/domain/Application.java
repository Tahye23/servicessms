package com.example.myproject.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * A Application.
 */
@Entity
@Table(name = "application")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Application implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @NotNull
    @Size(min = 2, max = 100)
    @Column(name = "name", nullable = false)
    private String name;

    @Size(max = 500)
    @Column(name = "description")
    private String description;

    @Column(name = "user_id")
    private Integer userId;

    // Webhooks
    @Column(name = "webhook_url")
    private String webhookUrl;

    @Column(name = "webhook_secret")
    private String webhookSecret;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "environment")
    private Environment environment = Environment.DEVELOPMENT;

    // Services autorisés
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "application_allowed_services", joinColumns = @JoinColumn(name = "application_id"))
    @Column(name = "service")
    @Enumerated(EnumType.STRING)
    private Set<AllowedService> allowedServices = new HashSet<>();

    // Limites d'utilisation
    @Column(name = "daily_limit")
    private Integer dailyLimit;

    @Column(name = "monthly_limit")
    private Integer monthlyLimit;

    @Column(name = "current_daily_usage")
    private Integer currentDailyUsage = 0;

    @Column(name = "current_monthly_usage")
    private Integer currentMonthlyUsage = 0;

    // Statistiques
    @Column(name = "total_api_calls")
    private Long totalApiCalls = 0L;

    @Column(name = "last_api_call")
    private Instant lastApiCall;

    // Métadonnées
    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    // Relations
    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JsonIgnoreProperties(value = { "application" }, allowSetters = true)
    @JsonIgnore
    private Set<TokensApp> tokens = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    private Api api;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "service" }, allowSetters = true)
    private PlanAbonnement planabonnement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "extuser", "groupes", "otpusers" }, allowSetters = true)
    private ExtendedUser utilisateur;

    // Enums
    public enum Environment {
        @JsonProperty("development")
        DEVELOPMENT,

        @JsonProperty("staging")
        STAGING,

        @JsonProperty("production")
        PRODUCTION,
    }

    public enum AllowedService {
        @JsonProperty("sms")
        SMS,

        @JsonProperty("whatsapp")
        WHATSAPP,

        @JsonProperty("email")
        EMAIL,

        @JsonProperty("voice")
        VOICE,
    }

    // Lifecycle callbacks
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    // Méthodes utilitaires
    public void incrementApiCallCount() {
        this.totalApiCalls = (this.totalApiCalls != null ? this.totalApiCalls : 0L) + 1;
        this.currentDailyUsage = (this.currentDailyUsage != null ? this.currentDailyUsage : 0) + 1;
        this.currentMonthlyUsage = (this.currentMonthlyUsage != null ? this.currentMonthlyUsage : 0) + 1;
        this.lastApiCall = Instant.now();
    }

    public void resetDailyUsage() {
        this.currentDailyUsage = 0;
    }

    public void resetMonthlyUsage() {
        this.currentMonthlyUsage = 0;
    }

    public boolean hasReachedDailyLimit() {
        return dailyLimit != null && currentDailyUsage != null && currentDailyUsage >= dailyLimit;
    }

    public boolean hasReachedMonthlyLimit() {
        return monthlyLimit != null && currentMonthlyUsage != null && currentMonthlyUsage >= monthlyLimit;
    }

    public boolean isServiceAllowed(AllowedService service) {
        return allowedServices != null && allowedServices.contains(service);
    }

    /**
     * Récupère le token actif de l'application
     * (le plus récent qui est actif et non expiré)
     */
    public Optional<TokensApp> getActiveToken() {
        return this.tokens.stream().filter(t -> Boolean.TRUE.equals(t.getActive()) && !t.isExpiredNow()).findFirst();
    }

    /**
     * Récupère tous les tokens valides de l'application
     */
    public Set<TokensApp> getValidTokens() {
        Set<TokensApp> validTokens = new HashSet<>();
        for (TokensApp token : this.tokens) {
            if (token.isValid()) {
                validTokens.add(token);
            }
        }
        return validTokens;
    }

    // Getters et Setters

    public Integer getId() {
        return this.id;
    }

    public Application id(Integer id) {
        this.setId(id);
        return this;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public Application name(String name) {
        this.setName(name);
        return this;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return this.description;
    }

    public Application description(String description) {
        this.setDescription(description);
        return this;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getUserId() {
        return this.userId;
    }

    public Application userId(Integer userId) {
        this.setUserId(userId);
        return this;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getWebhookUrl() {
        return this.webhookUrl;
    }

    public Application webhookUrl(String webhookUrl) {
        this.setWebhookUrl(webhookUrl);
        return this;
    }

    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    public String getWebhookSecret() {
        return this.webhookSecret;
    }

    public Application webhookSecret(String webhookSecret) {
        this.setWebhookSecret(webhookSecret);
        return this;
    }

    public void setWebhookSecret(String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }

    public Boolean getIsActive() {
        return this.isActive;
    }

    public Application isActive(Boolean isActive) {
        this.setIsActive(isActive);
        return this;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Environment getEnvironment() {
        return this.environment;
    }

    public Application environment(Environment environment) {
        this.setEnvironment(environment);
        return this;
    }

    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    public Set<AllowedService> getAllowedServices() {
        return this.allowedServices;
    }

    public Application allowedServices(Set<AllowedService> allowedServices) {
        this.setAllowedServices(allowedServices);
        return this;
    }

    public void setAllowedServices(Set<AllowedService> allowedServices) {
        this.allowedServices = allowedServices != null ? allowedServices : new HashSet<>();
    }

    public Integer getDailyLimit() {
        return this.dailyLimit;
    }

    public Application dailyLimit(Integer dailyLimit) {
        this.setDailyLimit(dailyLimit);
        return this;
    }

    public void setDailyLimit(Integer dailyLimit) {
        this.dailyLimit = dailyLimit;
    }

    public Integer getMonthlyLimit() {
        return this.monthlyLimit;
    }

    public Application monthlyLimit(Integer monthlyLimit) {
        this.setMonthlyLimit(monthlyLimit);
        return this;
    }

    public void setMonthlyLimit(Integer monthlyLimit) {
        this.monthlyLimit = monthlyLimit;
    }

    public Integer getCurrentDailyUsage() {
        return this.currentDailyUsage;
    }

    public Application currentDailyUsage(Integer currentDailyUsage) {
        this.setCurrentDailyUsage(currentDailyUsage);
        return this;
    }

    public void setCurrentDailyUsage(Integer currentDailyUsage) {
        this.currentDailyUsage = currentDailyUsage;
    }

    public Integer getCurrentMonthlyUsage() {
        return this.currentMonthlyUsage;
    }

    public Application currentMonthlyUsage(Integer currentMonthlyUsage) {
        this.setCurrentMonthlyUsage(currentMonthlyUsage);
        return this;
    }

    public void setCurrentMonthlyUsage(Integer currentMonthlyUsage) {
        this.currentMonthlyUsage = currentMonthlyUsage;
    }

    public Long getTotalApiCalls() {
        return this.totalApiCalls;
    }

    public Application totalApiCalls(Long totalApiCalls) {
        this.setTotalApiCalls(totalApiCalls);
        return this;
    }

    public void setTotalApiCalls(Long totalApiCalls) {
        this.totalApiCalls = totalApiCalls;
    }

    public Instant getLastApiCall() {
        return this.lastApiCall;
    }

    public Application lastApiCall(Instant lastApiCall) {
        this.setLastApiCall(lastApiCall);
        return this;
    }

    public void setLastApiCall(Instant lastApiCall) {
        this.lastApiCall = lastApiCall;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }

    public Application createdAt(Instant createdAt) {
        this.setCreatedAt(createdAt);
        return this;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return this.updatedAt;
    }

    public Application updatedAt(Instant updatedAt) {
        this.setUpdatedAt(updatedAt);
        return this;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Set<TokensApp> getTokens() {
        return this.tokens;
    }

    public Application tokens(Set<TokensApp> tokens) {
        this.setTokens(tokens);
        return this;
    }

    public void setTokens(Set<TokensApp> tokens) {
        this.tokens = tokens;
    }

    public Api getApi() {
        return this.api;
    }

    public void setApi(Api api) {
        this.api = api;
    }

    public Application api(Api api) {
        this.setApi(api);
        return this;
    }

    public PlanAbonnement getPlanabonnement() {
        return this.planabonnement;
    }

    public void setPlanabonnement(PlanAbonnement planabonnement) {
        this.planabonnement = planabonnement;
    }

    public Application planabonnement(PlanAbonnement planabonnement) {
        this.setPlanabonnement(planabonnement);
        return this;
    }

    public ExtendedUser getUtilisateur() {
        return this.utilisateur;
    }

    public void setUtilisateur(ExtendedUser extendedUser) {
        this.utilisateur = extendedUser;
    }

    public Application utilisateur(ExtendedUser extendedUser) {
        this.setUtilisateur(extendedUser);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Application)) {
            return false;
        }
        return getId() != null && getId().equals(((Application) o).getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return (
            "Application{" +
            "id=" +
            getId() +
            ", name='" +
            getName() +
            "'" +
            ", description='" +
            getDescription() +
            "'" +
            ", userId=" +
            getUserId() +
            ", isActive=" +
            getIsActive() +
            ", environment='" +
            getEnvironment() +
            "'" +
            ", dailyLimit=" +
            getDailyLimit() +
            ", monthlyLimit=" +
            getMonthlyLimit() +
            ", currentDailyUsage=" +
            getCurrentDailyUsage() +
            ", currentMonthlyUsage=" +
            getCurrentMonthlyUsage() +
            ", totalApiCalls=" +
            getTotalApiCalls() +
            "}"
        );
    }
}
