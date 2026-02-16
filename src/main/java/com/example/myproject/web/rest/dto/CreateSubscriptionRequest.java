package com.example.myproject.web.rest.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * DTO pour la création d'un nouvel abonnement
 */
public class CreateSubscriptionRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "L'ID du plan est obligatoire")
    private Long planId;

    private Long userId; // Sera automatiquement défini par le contrôleur

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @Future(message = "La date de fin doit être dans le futur")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    @Pattern(regexp = "^(credit_card|debit_card|bank_transfer|paypal|stripe|mobile_money|cash)$", message = "Méthode de paiement invalide")
    private String paymentMethod;

    private String transactionId;

    private Boolean autoRenew = true;

    private Boolean isTrial = false;

    private LocalDate trialEndDate;

    // Informations de facturation optionnelles
    private String billingAddress;
    private String billingCity;
    private String billingCountry;
    private String billingPostalCode;
    private String taxId;

    // Informations de paiement supplémentaires
    private String paymentMethodId; // ID Stripe/PayPal
    private String couponCode;
    private String promoCode;

    // Métadonnées
    private String source; // "web", "mobile", "api"
    private String campaignId;
    private String referralCode;

    // ===== CONSTRUCTEURS =====

    public CreateSubscriptionRequest() {}

    public CreateSubscriptionRequest(Long planId, Long userId) {
        this.planId = planId;
        this.userId = userId;
        this.startDate = LocalDate.now();
        this.autoRenew = true;
        this.isTrial = false;
    }

    public CreateSubscriptionRequest(Long planId, Long userId, LocalDate startDate, LocalDate endDate) {
        this.planId = planId;
        this.userId = userId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.autoRenew = true;
        this.isTrial = false;
    }

    // ===== MÉTHODES UTILITAIRES =====

    /**
     * Vérifie si la demande concerne un abonnement d'essai
     */
    public boolean isTrialRequest() {
        return Boolean.TRUE.equals(isTrial) && trialEndDate != null;
    }

    /**
     * Vérifie si les dates sont cohérentes
     */
    public boolean isDateRangeValid() {
        if (startDate == null) {
            return true; // startDate sera définie automatiquement
        }
        if (endDate == null) {
            return true; // Peut être null pour abonnements illimités
        }
        return endDate.isAfter(startDate);
    }

    /**
     * Vérifie si un code promo est présent
     */
    public boolean hasPromoCode() {
        return (couponCode != null && !couponCode.trim().isEmpty()) || (promoCode != null && !promoCode.trim().isEmpty());
    }

    /**
     * Obtient le code promo (coupon ou promo)
     */
    public String getEffectivePromoCode() {
        if (couponCode != null && !couponCode.trim().isEmpty()) {
            return couponCode.trim();
        }
        if (promoCode != null && !promoCode.trim().isEmpty()) {
            return promoCode.trim();
        }
        return null;
    }

    /**
     * Vérifie si des informations de facturation sont fournies
     */
    public boolean hasBillingInfo() {
        return billingAddress != null || billingCity != null || billingCountry != null || billingPostalCode != null;
    }

    // ===== GETTERS & SETTERS =====

    public Long getPlanId() {
        return planId;
    }

    public void setPlanId(Long planId) {
        this.planId = planId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public Boolean getAutoRenew() {
        return autoRenew;
    }

    public void setAutoRenew(Boolean autoRenew) {
        this.autoRenew = autoRenew;
    }

    public Boolean getIsTrial() {
        return isTrial;
    }

    public void setIsTrial(Boolean isTrial) {
        this.isTrial = isTrial;
    }

    public LocalDate getTrialEndDate() {
        return trialEndDate;
    }

    public void setTrialEndDate(LocalDate trialEndDate) {
        this.trialEndDate = trialEndDate;
    }

    public String getBillingAddress() {
        return billingAddress;
    }

    public void setBillingAddress(String billingAddress) {
        this.billingAddress = billingAddress;
    }

    public String getBillingCity() {
        return billingCity;
    }

    public void setBillingCity(String billingCity) {
        this.billingCity = billingCity;
    }

    public String getBillingCountry() {
        return billingCountry;
    }

    public void setBillingCountry(String billingCountry) {
        this.billingCountry = billingCountry;
    }

    public String getBillingPostalCode() {
        return billingPostalCode;
    }

    public void setBillingPostalCode(String billingPostalCode) {
        this.billingPostalCode = billingPostalCode;
    }

    public String getTaxId() {
        return taxId;
    }

    public void setTaxId(String taxId) {
        this.taxId = taxId;
    }

    public String getPaymentMethodId() {
        return paymentMethodId;
    }

    public void setPaymentMethodId(String paymentMethodId) {
        this.paymentMethodId = paymentMethodId;
    }

    public String getCouponCode() {
        return couponCode;
    }

    public void setCouponCode(String couponCode) {
        this.couponCode = couponCode;
    }

    public String getPromoCode() {
        return promoCode;
    }

    public void setPromoCode(String promoCode) {
        this.promoCode = promoCode;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getCampaignId() {
        return campaignId;
    }

    public void setCampaignId(String campaignId) {
        this.campaignId = campaignId;
    }

    public String getReferralCode() {
        return referralCode;
    }

    public void setReferralCode(String referralCode) {
        this.referralCode = referralCode;
    }

    // ===== MÉTHODES STANDARD =====

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CreateSubscriptionRequest)) return false;
        CreateSubscriptionRequest that = (CreateSubscriptionRequest) o;
        return (
            java.util.Objects.equals(planId, that.planId) &&
            java.util.Objects.equals(userId, that.userId) &&
            java.util.Objects.equals(startDate, that.startDate) &&
            java.util.Objects.equals(endDate, that.endDate)
        );
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(planId, userId, startDate, endDate);
    }

    @Override
    public String toString() {
        return (
            "CreateSubscriptionRequest{" +
            "planId=" +
            planId +
            ", userId=" +
            userId +
            ", startDate=" +
            startDate +
            ", endDate=" +
            endDate +
            ", paymentMethod='" +
            paymentMethod +
            '\'' +
            ", autoRenew=" +
            autoRenew +
            ", isTrial=" +
            isTrial +
            ", source='" +
            source +
            '\'' +
            '}'
        );
    }
}
