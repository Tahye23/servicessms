package com.example.myproject.service.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Future;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * DTO pour la modification d'un abonnement existant
 */
public class UpdateSubscriptionRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long subscriptionId;

    @Future(message = "La date de fin doit être dans le futur")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    private String paymentMethod;
    private String transactionId;
    private Boolean autoRenew;
    private String paymentMethodId;

    // Informations de facturation
    private String billingAddress;
    private String billingCity;
    private String billingCountry;
    private String billingPostalCode;
    private String taxId;

    // Constructeurs
    public UpdateSubscriptionRequest() {}

    public UpdateSubscriptionRequest(Long subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    // Getters & Setters
    public Long getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(Long subscriptionId) {
        this.subscriptionId = subscriptionId;
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

    public String getPaymentMethodId() {
        return paymentMethodId;
    }

    public void setPaymentMethodId(String paymentMethodId) {
        this.paymentMethodId = paymentMethodId;
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

    @Override
    public String toString() {
        return (
            "UpdateSubscriptionRequest{" +
            "subscriptionId=" +
            subscriptionId +
            ", endDate=" +
            endDate +
            ", paymentMethod='" +
            paymentMethod +
            '\'' +
            ", autoRenew=" +
            autoRenew +
            '}'
        );
    }
}
