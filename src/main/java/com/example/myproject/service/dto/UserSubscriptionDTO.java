package com.example.myproject.service.dto;

import com.example.myproject.domain.Abonnement;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;

public class UserSubscriptionDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String planName;
    private String planType;
    private Boolean sidebarVisible;
    private String status;
    private LocalDate startDate;
    private LocalDate endDate;

    private Integer smsLimit;
    private Integer whatsappLimit;

    private Integer smsUsed;
    private Integer whatsappUsed;

    private Integer smsRemaining;
    private Integer whatsappRemaining;

    private BigDecimal price;
    private String currency;

    private Set<String> features = new HashSet<>();

    private Boolean isActive;
    private Boolean isExpiringSoon;
    private Long daysUntilExpiration;

    private Instant createdDate;

    // Permissions / droits
    private Boolean customCanManageUsers;
    private Boolean customCanManageTemplates;
    private Boolean customCanViewConversations;
    private Boolean customCanViewAnalytics;
    private Boolean customPrioritySupport;

    private Boolean canViewDashboard;
    private Boolean canManageAPI;

    // Bonus
    private Boolean bonusSmsEnabled;
    private Integer bonusSmsAmount;

    private Boolean bonusWhatsappEnabled;
    private Integer bonusWhatsappAmount;

    // Carryover
    private Boolean allowSmsCarryover;
    private Boolean allowWhatsappCarryover;

    private Integer carriedOverSms;
    private Integer carriedOverWhatsapp;

    public UserSubscriptionDTO() {}

    public UserSubscriptionDTO(Abonnement abonnement) {
        this.id = abonnement.getId();
        this.status = abonnement.getStatus() != null ? abonnement.getStatus().name() : null;
        this.startDate = abonnement.getStartDate();
        this.endDate = abonnement.getEndDate();

        this.smsUsed = abonnement.getSmsUsed() != null ? abonnement.getSmsUsed() : 0;
        this.whatsappUsed = abonnement.getWhatsappUsed() != null ? abonnement.getWhatsappUsed() : 0;

        if (abonnement.getPlan() != null) {
            this.planName = abonnement.getPlan().getAbpName();
            this.planType = abonnement.getPlan().getPlanType() != null ? abonnement.getPlan().getPlanType().name() : null;
            this.smsLimit = abonnement.getPlan().getSmsLimit() != null ? abonnement.getPlan().getSmsLimit() : 0;
            this.whatsappLimit = abonnement.getPlan().getWhatsappLimit() != null ? abonnement.getPlan().getWhatsappLimit() : 0;
            this.price = abonnement.getPlan().getAbpPrice();
            this.currency = abonnement.getPlan().getAbpCurrency();
        }

        // Calcul smsRemaining et whatsappRemaining en tenant compte des limites personnalisées
        int effectiveSmsLimit = abonnement.getCustomSmsLimit() != null
            ? abonnement.getCustomSmsLimit()
            : (this.smsLimit != null ? this.smsLimit : 0);
        int effectiveWhatsappLimit = abonnement.getCustomWhatsappLimit() != null
            ? abonnement.getCustomWhatsappLimit()
            : (this.whatsappLimit != null ? this.whatsappLimit : 0);

        this.smsRemaining = Math.max(0, effectiveSmsLimit - this.smsUsed);
        this.whatsappRemaining = Math.max(0, effectiveWhatsappLimit - this.whatsappUsed);

        // Dates et flags d’expiration
        if (this.endDate != null) {
            long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), this.endDate);
            this.daysUntilExpiration = daysLeft > 0 ? daysLeft : 0;
            this.isExpiringSoon = daysLeft > 0 && daysLeft <= 7;
        } else {
            this.daysUntilExpiration = null;
            this.isExpiringSoon = false;
        }

        this.isActive = "ACTIVE".equalsIgnoreCase(this.status) && (this.endDate == null || !LocalDate.now().isAfter(this.endDate));

        this.createdDate = abonnement.getCreatedDate();

        // Permissions / droits personnalisés
        this.customCanManageUsers = abonnement.getCustomCanManageUsers();
        this.customCanManageTemplates = abonnement.getCustomCanManageTemplates();
        this.customCanViewConversations = abonnement.getCustomCanViewConversations();
        this.customCanViewAnalytics = abonnement.getCustomCanViewAnalytics();
        this.customPrioritySupport = abonnement.getCustomPrioritySupport();

        // Permissions additionnelles
        this.canViewDashboard = abonnement.getCanViewDashboard();
        this.canManageAPI = abonnement.getCanManageAPI();

        // Bonus
        this.bonusSmsEnabled = abonnement.getBonusSmsEnabled();
        this.bonusSmsAmount = abonnement.getBonusSmsAmount();
        this.bonusWhatsappEnabled = abonnement.getBonusWhatsappEnabled();
        this.bonusWhatsappAmount = abonnement.getBonusWhatsappAmount();

        // Carryover
        this.allowSmsCarryover = abonnement.getAllowSmsCarryover();
        this.allowWhatsappCarryover = abonnement.getAllowWhatsappCarryover();
        this.carriedOverSms = abonnement.getCarriedOverSms();
        this.carriedOverWhatsapp = abonnement.getCarriedOverWhatsapp();

        // Construire le set des features (facultatif, pour UI simplifiée)
        if (Boolean.TRUE.equals(customCanManageUsers)) features.add("manage-users");
        if (Boolean.TRUE.equals(customCanManageTemplates)) features.add("manage-templates");
        if (Boolean.TRUE.equals(customCanViewConversations)) features.add("view-conversations");
        if (Boolean.TRUE.equals(customCanViewAnalytics)) features.add("view-analytics");
        if (Boolean.TRUE.equals(customPrioritySupport)) features.add("priority-support");
        if (Boolean.TRUE.equals(canViewDashboard)) features.add("view-dashboard");
        if (Boolean.TRUE.equals(canManageAPI)) features.add("manage-api");
        if (Boolean.TRUE.equals(bonusSmsEnabled)) features.add("bonus-sms");
        if (Boolean.TRUE.equals(bonusWhatsappEnabled)) features.add("bonus-whatsapp");
    }

    // Getters & Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPlanName() {
        return planName;
    }

    public void setPlanName(String planName) {
        this.planName = planName;
    }

    public String getPlanType() {
        return planType;
    }

    public void setPlanType(String planType) {
        this.planType = planType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
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

    public Integer getSmsUsed() {
        return smsUsed;
    }

    public void setSmsUsed(Integer smsUsed) {
        this.smsUsed = smsUsed;
    }

    public Integer getWhatsappUsed() {
        return whatsappUsed;
    }

    public void setWhatsappUsed(Integer whatsappUsed) {
        this.whatsappUsed = whatsappUsed;
    }

    public Integer getSmsRemaining() {
        return smsRemaining;
    }

    public void setSmsRemaining(Integer smsRemaining) {
        this.smsRemaining = smsRemaining;
    }

    public Integer getWhatsappRemaining() {
        return whatsappRemaining;
    }

    public void setWhatsappRemaining(Integer whatsappRemaining) {
        this.whatsappRemaining = whatsappRemaining;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Set<String> getFeatures() {
        return features;
    }

    public void setFeatures(Set<String> features) {
        this.features = features;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Boolean getIsExpiringSoon() {
        return isExpiringSoon;
    }

    public void setIsExpiringSoon(Boolean isExpiringSoon) {
        this.isExpiringSoon = isExpiringSoon;
    }

    public Long getDaysUntilExpiration() {
        return daysUntilExpiration;
    }

    public void setDaysUntilExpiration(Long daysUntilExpiration) {
        this.daysUntilExpiration = daysUntilExpiration;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }

    public Boolean getActive() {
        return isActive;
    }

    public void setActive(Boolean active) {
        isActive = active;
    }

    public Boolean getExpiringSoon() {
        return isExpiringSoon;
    }

    public void setExpiringSoon(Boolean expiringSoon) {
        isExpiringSoon = expiringSoon;
    }

    public Boolean getCustomCanManageUsers() {
        return customCanManageUsers;
    }

    public void setCustomCanManageUsers(Boolean customCanManageUsers) {
        this.customCanManageUsers = customCanManageUsers;
    }

    public Boolean getCustomCanManageTemplates() {
        return customCanManageTemplates;
    }

    public void setCustomCanManageTemplates(Boolean customCanManageTemplates) {
        this.customCanManageTemplates = customCanManageTemplates;
    }

    public Boolean getCustomCanViewConversations() {
        return customCanViewConversations;
    }

    public void setCustomCanViewConversations(Boolean customCanViewConversations) {
        this.customCanViewConversations = customCanViewConversations;
    }

    public Boolean getCustomCanViewAnalytics() {
        return customCanViewAnalytics;
    }

    public void setCustomCanViewAnalytics(Boolean customCanViewAnalytics) {
        this.customCanViewAnalytics = customCanViewAnalytics;
    }

    public Boolean getSidebarVisible() {
        return sidebarVisible;
    }

    public void setSidebarVisible(Boolean sidebarVisible) {
        this.sidebarVisible = sidebarVisible;
    }

    public Boolean getCustomPrioritySupport() {
        return customPrioritySupport;
    }

    public void setCustomPrioritySupport(Boolean customPrioritySupport) {
        this.customPrioritySupport = customPrioritySupport;
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

    public Boolean getBonusSmsEnabled() {
        return bonusSmsEnabled;
    }

    public void setBonusSmsEnabled(Boolean bonusSmsEnabled) {
        this.bonusSmsEnabled = bonusSmsEnabled;
    }

    public Integer getBonusSmsAmount() {
        return bonusSmsAmount;
    }

    public void setBonusSmsAmount(Integer bonusSmsAmount) {
        this.bonusSmsAmount = bonusSmsAmount;
    }

    public Integer getBonusWhatsappAmount() {
        return bonusWhatsappAmount;
    }

    public void setBonusWhatsappAmount(Integer bonusWhatsappAmount) {
        this.bonusWhatsappAmount = bonusWhatsappAmount;
    }

    public Boolean getBonusWhatsappEnabled() {
        return bonusWhatsappEnabled;
    }

    public void setBonusWhatsappEnabled(Boolean bonusWhatsappEnabled) {
        this.bonusWhatsappEnabled = bonusWhatsappEnabled;
    }

    public Boolean getAllowSmsCarryover() {
        return allowSmsCarryover;
    }

    public void setAllowSmsCarryover(Boolean allowSmsCarryover) {
        this.allowSmsCarryover = allowSmsCarryover;
    }

    public Boolean getAllowWhatsappCarryover() {
        return allowWhatsappCarryover;
    }

    public void setAllowWhatsappCarryover(Boolean allowWhatsappCarryover) {
        this.allowWhatsappCarryover = allowWhatsappCarryover;
    }

    public Integer getCarriedOverSms() {
        return carriedOverSms;
    }

    public void setCarriedOverSms(Integer carriedOverSms) {
        this.carriedOverSms = carriedOverSms;
    }

    public Integer getCarriedOverWhatsapp() {
        return carriedOverWhatsapp;
    }

    public void setCarriedOverWhatsapp(Integer carriedOverWhatsapp) {
        this.carriedOverWhatsapp = carriedOverWhatsapp;
    }

    @Override
    public String toString() {
        return (
            "UserSubscriptionDTO{" +
            "id=" +
            id +
            ", planName='" +
            planName +
            '\'' +
            ", planType='" +
            planType +
            '\'' +
            ", status='" +
            status +
            '\'' +
            ", smsRemaining=" +
            smsRemaining +
            ", whatsappRemaining=" +
            whatsappRemaining +
            '}'
        );
    }
}
