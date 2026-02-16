package com.example.myproject.service.dto;

import java.io.Serializable;
import java.time.LocalDate;

public class SubscriptionAccessDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    // Permissions fonctionnelles
    private Boolean canSendSMS = false;
    private Boolean canSendWhatsApp = false;
    private Boolean canManageUsers = false;
    private Boolean canManageTemplates = false;
    private Boolean canViewConversations = false;
    private Boolean canViewDashboard = true;

    // Compteurs de crédits
    private Integer smsRemaining = 0;
    private Integer whatsappRemaining = 0;

    // Informations d'abonnement
    private String subscriptionType = "FREE";
    private Boolean isSubscriptionExpiring = false;
    private Long daysUntilExpiration = 0L;
    private Boolean needsUpgrade = false;

    // Informations supplémentaires
    private Integer smsUsed = 0;
    private Integer whatsappUsed = 0;
    private LocalDate subscriptionEndDate;
    private String planName;
    private Boolean hasActiveSubscription = false;

    // Constructeurs
    public SubscriptionAccessDTO() {}

    public SubscriptionAccessDTO(Boolean canSendSMS, Boolean canSendWhatsApp, Integer smsRemaining, Integer whatsappRemaining) {
        this.canSendSMS = canSendSMS;
        this.canSendWhatsApp = canSendWhatsApp;
        this.smsRemaining = smsRemaining;
        this.whatsappRemaining = whatsappRemaining;
    }

    // Getters & Setters
    public Boolean getCanSendSMS() {
        return canSendSMS;
    }

    public void setCanSendSMS(Boolean canSendSMS) {
        this.canSendSMS = canSendSMS;
    }

    public Boolean getCanSendWhatsApp() {
        return canSendWhatsApp;
    }

    public void setCanSendWhatsApp(Boolean canSendWhatsApp) {
        this.canSendWhatsApp = canSendWhatsApp;
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

    public Boolean getCanViewDashboard() {
        return canViewDashboard;
    }

    public void setCanViewDashboard(Boolean canViewDashboard) {
        this.canViewDashboard = canViewDashboard;
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

    public String getSubscriptionType() {
        return subscriptionType;
    }

    public void setSubscriptionType(String subscriptionType) {
        this.subscriptionType = subscriptionType;
    }

    public Boolean getIsSubscriptionExpiring() {
        return isSubscriptionExpiring;
    }

    public void setIsSubscriptionExpiring(Boolean isSubscriptionExpiring) {
        this.isSubscriptionExpiring = isSubscriptionExpiring;
    }

    public Long getDaysUntilExpiration() {
        return daysUntilExpiration;
    }

    public void setDaysUntilExpiration(Long daysUntilExpiration) {
        this.daysUntilExpiration = daysUntilExpiration;
    }

    public Boolean getNeedsUpgrade() {
        return needsUpgrade;
    }

    public void setNeedsUpgrade(Boolean needsUpgrade) {
        this.needsUpgrade = needsUpgrade;
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

    public LocalDate getSubscriptionEndDate() {
        return subscriptionEndDate;
    }

    public void setSubscriptionEndDate(LocalDate subscriptionEndDate) {
        this.subscriptionEndDate = subscriptionEndDate;
    }

    public String getPlanName() {
        return planName;
    }

    public void setPlanName(String planName) {
        this.planName = planName;
    }

    public Boolean getHasActiveSubscription() {
        return hasActiveSubscription;
    }

    public void setHasActiveSubscription(Boolean hasActiveSubscription) {
        this.hasActiveSubscription = hasActiveSubscription;
    }

    @Override
    public String toString() {
        return (
            "SubscriptionAccessDTO{" +
            "canSendSMS=" +
            canSendSMS +
            ", canSendWhatsApp=" +
            canSendWhatsApp +
            ", smsRemaining=" +
            smsRemaining +
            ", whatsappRemaining=" +
            whatsappRemaining +
            ", subscriptionType='" +
            subscriptionType +
            '\'' +
            ", needsUpgrade=" +
            needsUpgrade +
            '}'
        );
    }
}
