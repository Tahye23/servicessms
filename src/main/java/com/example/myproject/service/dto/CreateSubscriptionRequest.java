package com.example.myproject.service.dto;

import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDate;

public class CreateSubscriptionRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull
    private Long planId;

    @NotNull
    private Long userId;

    private LocalDate startDate;
    private LocalDate endDate;
    private String paymentMethod;
    private String transactionId;
    private Boolean autoRenew = true;

    // Constructeurs
    public CreateSubscriptionRequest() {}

    public CreateSubscriptionRequest(Long planId, Long userId) {
        this.planId = planId;
        this.userId = userId;
        this.startDate = LocalDate.now();
    }

    // Getters & Setters
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
            '}'
        );
    }
}
