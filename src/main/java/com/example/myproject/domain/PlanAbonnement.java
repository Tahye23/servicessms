package com.example.myproject.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@Table(name = "plan_abonnement")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class PlanAbonnement implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull
    @Column(name = "abp_name", nullable = false)
    private String abpName;

    @Column(name = "abp_description")
    private String abpDescription;

    @Column(name = "abp_price", precision = 21, scale = 2)
    private BigDecimal abpPrice;

    @Column(name = "abp_currency", length = 3)
    private String abpCurrency = "MRU";

    @Column(name = "abp_period")
    private String abpPeriod;

    @Column(name = "abp_features", columnDefinition = "TEXT")
    private String abpFeatures;

    @Column(name = "abp_button_text")
    private String abpButtonText;

    @Column(name = "button_class")
    private String buttonClass;

    @Column(name = "abp_popular")
    private Boolean abpPopular = false;

    @Column(name = "custom_plan")
    private Boolean customPlan = false;

    @Column(name = "created_date")
    private Instant createdDate = Instant.now();

    @Column(name = "updated_date")
    private Instant updatedDate = Instant.now();

    @Column(name = "active") // Renommé pour correspondre au DTO
    private Boolean active = true;

    // ===== NOUVEAUX CHAMPS POUR LE SYSTÈME D'ABONNEMENT =====

    @Enumerated(EnumType.STRING)
    @Column(name = "plan_type", nullable = false)
    private PlanType planType;

    @Column(name = "sms_limit")
    private Integer smsLimit = 0;

    @Column(name = "whatsapp_limit")
    private Integer whatsappLimit = 0;

    @Column(name = "users_limit")
    private Integer usersLimit = 1;

    @Column(name = "templates_limit")
    private Integer templatesLimit = 0;

    @Column(name = "can_view_dashboard", nullable = false)
    private Boolean canViewDashboard = false;

    @Column(name = "can_manage_api", nullable = false)
    private Boolean canManageAPI = false;

    @Column(name = "can_manage_users")
    private Boolean canManageUsers = false;

    @Column(name = "can_manage_templates")
    private Boolean canManageTemplates = false;

    @Column(name = "can_view_conversations")
    private Boolean canViewConversations = false;

    @Column(name = "can_view_analytics")
    private Boolean canViewAnalytics = false;

    @Column(name = "priority_support")
    private Boolean prioritySupport = false;

    @Column(name = "max_api_calls_per_day")
    private Integer maxApiCallsPerDay = 100;

    @Column(name = "storage_limit_mb")
    private Integer storageLimitMb = 100;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    // ===== HOOKS POUR GESTION AUTOMATIQUE DES DATES =====
    @PrePersist
    protected void onCreate() {
        createdDate = Instant.now();
        updatedDate = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedDate = Instant.now();
    }

    // ===== ENUMS =====
    public enum PlanType {
        FREE("Gratuit"),
        SMS("SMS"),
        WHATSAPP("WhatsApp"),
        PREMIUM("Premium"),
        ENTERPRISE("Entreprise");

        private final String displayName;

        PlanType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    // ===== GETTERS ET SETTERS =====
    // (Générer tous les getters/setters pour tous les champs)

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAbpName() {
        return abpName;
    }

    public void setAbpName(String abpName) {
        this.abpName = abpName;
    }

    public String getAbpDescription() {
        return abpDescription;
    }

    public void setAbpDescription(String abpDescription) {
        this.abpDescription = abpDescription;
    }

    public Boolean getCustomPlan() {
        return customPlan;
    }

    public Boolean getCanViewDashboard() {
        return canViewDashboard;
    }

    public void setCanViewDashboard(Boolean canViewDashboard) {
        this.canViewDashboard = canViewDashboard;
    }

    public Boolean getCanManageAPI() {
        return canManageAPI;
    }

    public void setCanManageAPI(Boolean canManageAPI) {
        this.canManageAPI = canManageAPI;
    }

    public void setCustomPlan(Boolean customPlan) {
        this.customPlan = customPlan;
    }

    public BigDecimal getAbpPrice() {
        return abpPrice;
    }

    public void setAbpPrice(BigDecimal abpPrice) {
        this.abpPrice = abpPrice;
    }

    public String getAbpCurrency() {
        return abpCurrency;
    }

    public void setAbpCurrency(String abpCurrency) {
        this.abpCurrency = abpCurrency;
    }

    public String getAbpPeriod() {
        return abpPeriod;
    }

    public void setAbpPeriod(String abpPeriod) {
        this.abpPeriod = abpPeriod;
    }

    public String getAbpFeatures() {
        return abpFeatures;
    }

    public void setAbpFeatures(String abpFeatures) {
        this.abpFeatures = abpFeatures;
    }

    public String getAbpButtonText() {
        return abpButtonText;
    }

    public void setAbpButtonText(String abpButtonText) {
        this.abpButtonText = abpButtonText;
    }

    public String getButtonClass() {
        return buttonClass;
    }

    public void setButtonClass(String buttonClass) {
        this.buttonClass = buttonClass;
    }

    public Boolean getAbpPopular() {
        return abpPopular;
    }

    public void setAbpPopular(Boolean abpPopular) {
        this.abpPopular = abpPopular;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }

    public Instant getUpdatedDate() {
        return updatedDate;
    }

    public void setUpdatedDate(Instant updatedDate) {
        this.updatedDate = updatedDate;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public PlanType getPlanType() {
        return planType;
    }

    public void setPlanType(PlanType planType) {
        this.planType = planType;
    }

    public Integer getSmsLimit() {
        return smsLimit;
    }

    public void setSmsLimit(Integer smsLimit) {
        this.smsLimit = smsLimit;
    }

    public Integer getWhatsappLimit() {
        return whatsappLimit;
    }

    public void setWhatsappLimit(Integer whatsappLimit) {
        this.whatsappLimit = whatsappLimit;
    }

    public Integer getUsersLimit() {
        return usersLimit;
    }

    public void setUsersLimit(Integer usersLimit) {
        this.usersLimit = usersLimit;
    }

    public Integer getTemplatesLimit() {
        return templatesLimit;
    }

    public void setTemplatesLimit(Integer templatesLimit) {
        this.templatesLimit = templatesLimit;
    }

    public Boolean getCanManageUsers() {
        return canManageUsers;
    }

    public void setCanManageUsers(Boolean canManageUsers) {
        this.canManageUsers = canManageUsers;
    }

    public Boolean getCanManageTemplates() {
        return canManageTemplates;
    }

    public void setCanManageTemplates(Boolean canManageTemplates) {
        this.canManageTemplates = canManageTemplates;
    }

    public Boolean getCanViewConversations() {
        return canViewConversations;
    }

    public void setCanViewConversations(Boolean canViewConversations) {
        this.canViewConversations = canViewConversations;
    }

    public Boolean getCanViewAnalytics() {
        return canViewAnalytics;
    }

    public void setCanViewAnalytics(Boolean canViewAnalytics) {
        this.canViewAnalytics = canViewAnalytics;
    }

    public Boolean getPrioritySupport() {
        return prioritySupport;
    }

    public void setPrioritySupport(Boolean prioritySupport) {
        this.prioritySupport = prioritySupport;
    }

    public Integer getMaxApiCallsPerDay() {
        return maxApiCallsPerDay;
    }

    public void setMaxApiCallsPerDay(Integer maxApiCallsPerDay) {
        this.maxApiCallsPerDay = maxApiCallsPerDay;
    }

    public Integer getStorageLimitMb() {
        return storageLimitMb;
    }

    public void setStorageLimitMb(Integer storageLimitMb) {
        this.storageLimitMb = storageLimitMb;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
