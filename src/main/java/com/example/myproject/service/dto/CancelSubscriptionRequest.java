package com.example.myproject.service.dto;

import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * DTO pour l'annulation d'un abonnement
 */
public class CancelSubscriptionRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "L'ID de l'abonnement est obligatoire")
    private Long subscriptionId;

    private String reason; // Raison de l'annulation
    private Boolean immediate = false; // Annulation immédiate ou à la fin de la période
    private LocalDate effectiveDate; // Date effective d'annulation
    private String feedback; // Commentaires de l'utilisateur

    // Constructeurs
    public CancelSubscriptionRequest() {}

    public CancelSubscriptionRequest(Long subscriptionId, String reason) {
        this.subscriptionId = subscriptionId;
        this.reason = reason;
    }

    // Getters & Setters
    public Long getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(Long subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Boolean getImmediate() {
        return immediate;
    }

    public void setImmediate(Boolean immediate) {
        this.immediate = immediate;
    }

    public LocalDate getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(LocalDate effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }

    @Override
    public String toString() {
        return (
            "CancelSubscriptionRequest{" +
            "subscriptionId=" +
            subscriptionId +
            ", reason='" +
            reason +
            '\'' +
            ", immediate=" +
            immediate +
            ", effectiveDate=" +
            effectiveDate +
            '}'
        );
    }
}
