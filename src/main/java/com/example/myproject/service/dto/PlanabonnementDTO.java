// ===== 1. CORRIGER LE DTO =====
package com.example.myproject.service.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;

public class PlanabonnementDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    @NotNull(message = "Le nom du plan est obligatoire")
    @Size(min = 2, max = 255, message = "Le nom du plan doit contenir entre 2 et 255 caractères")
    private String abpName;

    @Size(max = 1000, message = "La description ne peut pas dépasser 1000 caractères")
    private String abpDescription;

    @DecimalMin(value = "0.0", message = "Le prix doit être positif")
    private BigDecimal abpPrice;

    @Pattern(regexp = "^[A-Z]{3}$", message = "La devise doit être un code ISO à 3 lettres")
    private String abpCurrency = "MRU";

    @Pattern(regexp = "^(MONTHLY|YEARLY|LIFETIME)$", message = "Période invalide")
    private String abpPeriod;

    private String abpFeatures;

    private String abpButtonText;

    private String buttonClass;

    private Boolean abpPopular = false;
    private Boolean customPlan = false;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant createdDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant updatedDate;

    private Boolean active = true; // Renommé pour correspondre à l'entité

    // ===== NOUVEAUX CHAMPS POUR LE SYSTÈME D'ABONNEMENT =====

    @NotNull(message = "Le type de plan est obligatoire")
    @Pattern(regexp = "^(FREE|SMS|WHATSAPP|PREMIUM|ENTERPRISE)$", message = "Type de plan invalide")
    private String planType;

    @Min(value = -1, message = "La limite SMS doit être -1 (illimité) ou positive")
    private Integer smsLimit = 0;

    @Min(value = -1, message = "La limite WhatsApp doit être -1 (illimité) ou positive")
    private Integer whatsappLimit = 0;

    @Min(value = -1, message = "La limite d'utilisateurs doit être -1 (illimité) ou au moins 1")
    private Integer usersLimit = 1;

    @Min(value = -1, message = "La limite de templates doit être -1 (illimité) ou positive")
    private Integer templatesLimit = 0;

    private Boolean canManageUsers = false;
    private Boolean canManageTemplates = false;
    private Boolean canViewConversations = false;
    private Boolean canViewAnalytics = false;
    private Boolean prioritySupport = false;

    @Min(value = -1, message = "Le nombre d'appels API doit être -1 (illimité) ou au moins 1")
    private Integer maxApiCallsPerDay = 100;

    @Min(value = 1, message = "La limite de stockage doit être au moins 1 MB")
    private Integer storageLimitMb = 100;

    @Min(value = 0, message = "L'ordre de tri doit être positif")
    private Integer sortOrder = 0;

    private Boolean canViewDashboard = false;

    private Boolean canManageAPI = false;

    // ===== GETTERS ET SETTERS =====

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

    public Boolean getCustomPlan() {
        return customPlan;
    }

    public void setCustomPlan(Boolean customPlan) {
        this.customPlan = customPlan;
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

    public String getPlanType() {
        return planType;
    }

    public void setPlanType(String planType) {
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
// ===== 2. CORRIGER L'ENTITÉ =====
