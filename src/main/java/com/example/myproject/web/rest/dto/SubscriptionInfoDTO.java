package com.example.myproject.web.rest.dto;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * DTO pour les informations d'abonnement utilisées dans le dashboard
 */
public class SubscriptionInfoDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    // Informations de base
    private String subscriptionType; // FREE, SMS, WHATSAPP, PREMIUM
    private String planName;

    // Capacités d'envoi
    private Boolean canSendSMS = false;
    private Boolean canSendWhatsApp = false;

    // Limites et utilisations
    private Integer smsLimit = 0;
    private Integer whatsappLimit = 0;
    private Integer smsRemaining = 0;
    private Integer whatsappRemaining = 0;

    // Permissions
    private Boolean canManageTemplates = false;
    private Boolean canViewAnalytics = false;
    private Boolean canManageUsers = false;

    // Informations d'expiration
    private Boolean isExpiringSoon = false;
    private Long daysUntilExpiration;
    private LocalDate endDate;

    // Constructeur par défaut
    public SubscriptionInfoDTO() {}

    // Getters et Setters
    public String getSubscriptionType() {
        return subscriptionType;
    }

    public void setSubscriptionType(String subscriptionType) {
        this.subscriptionType = subscriptionType;
    }

    public String getPlanName() {
        return planName;
    }

    public void setPlanName(String planName) {
        this.planName = planName;
    }

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

    public Boolean getCanManageTemplates() {
        return canManageTemplates;
    }

    public void setCanManageTemplates(Boolean canManageTemplates) {
        this.canManageTemplates = canManageTemplates;
    }

    public Boolean getCanViewAnalytics() {
        return canViewAnalytics;
    }

    public void setCanViewAnalytics(Boolean canViewAnalytics) {
        this.canViewAnalytics = canViewAnalytics;
    }

    public Boolean getCanManageUsers() {
        return canManageUsers;
    }

    public void setCanManageUsers(Boolean canManageUsers) {
        this.canManageUsers = canManageUsers;
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

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    @Override
    public String toString() {
        return (
            "SubscriptionInfoDTO{" +
            "subscriptionType='" +
            subscriptionType +
            '\'' +
            ", planName='" +
            planName +
            '\'' +
            ", canSendSMS=" +
            canSendSMS +
            ", canSendWhatsApp=" +
            canSendWhatsApp +
            ", smsRemaining=" +
            smsRemaining +
            ", whatsappRemaining=" +
            whatsappRemaining +
            ", isExpiringSoon=" +
            isExpiringSoon +
            '}'
        );
    }
}
