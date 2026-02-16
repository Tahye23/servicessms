package com.example.myproject.service.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.Column;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

public class AbonnementDTO implements Serializable {

    private Long id;

    @NotNull
    private Long userId;

    private Long planId;

    @NotNull
    @Pattern(regexp = "^(ACTIVE|EXPIRED|SUSPENDED|CANCELLED|TRIAL|PENDING_PAYMENT)$")
    private String status = "ACTIVE";

    private ExtendedUserDTO user;
    private PlanabonnementDTO plan;

    private LocalDate startDate;

    private LocalDate endDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant createdDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant updatedDate;

    private Boolean canViewDashboard = false;

    private Boolean canManageAPI = false;
    private Integer smsUsed = 0;

    private Integer whatsappUsed = 0;

    private Integer apiCallsToday = 0;

    private Integer storageUsedMb = 0;

    private LocalDate lastApiCallDate;

    @Size(max = 50)
    private String paymentMethod;

    @Size(max = 255)
    private String transactionId;

    private Boolean autoRenew = true;

    private LocalDate trialEndDate;

    private Boolean isTrial = false;

    private Boolean isCustomPlan = false;

    private BigDecimal customPrice;

    private String customPeriod;
    private Boolean sidebarVisible = true;

    @Size(max = 255)
    private String customName;

    @Size(max = 1000)
    private String customDescription;

    private Integer customSmsLimit;

    private Integer customWhatsappLimit;

    private Integer customUsersLimit;

    private Integer customTemplatesLimit;

    private Integer customApiCallsLimit;

    private Integer customStorageLimitMb;

    private Boolean customCanManageUsers = false;

    private Boolean customCanManageTemplates = false;

    private Boolean customCanViewConversations = false;

    private Boolean customCanViewAnalytics = false;

    private Boolean customPrioritySupport = false;

    private Boolean bonusSmsEnabled = false;

    private Integer bonusSmsAmount;

    private Boolean bonusWhatsappEnabled = false;

    private Integer bonusWhatsappAmount;

    private Boolean allowSmsCarryover = false;

    private Boolean allowWhatsappCarryover = false;

    private Integer carriedOverSms;

    private Integer carriedOverWhatsapp;

    public Integer getCarriedOverWhatsapp() {
        return carriedOverWhatsapp;
    }

    public void setCarriedOverWhatsapp(Integer carriedOverWhatsapp) {
        this.carriedOverWhatsapp = carriedOverWhatsapp;
    }

    public @NotNull Long getUserId() {
        return userId;
    }

    public ExtendedUserDTO getUser() {
        return user;
    }

    public void setUser(ExtendedUserDTO user) {
        this.user = user;
    }

    public PlanabonnementDTO getPlan() {
        return plan;
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

    public void setPlan(PlanabonnementDTO plan) {
        this.plan = plan;
    }

    public void setUserId(@NotNull Long userId) {
        this.userId = userId;
    }

    public @NotNull @Pattern(regexp = "^(ACTIVE|EXPIRED|SUSPENDED|CANCELLED|TRIAL|PENDING_PAYMENT)$") String getStatus() {
        return status;
    }

    public void setStatus(@NotNull @Pattern(regexp = "^(ACTIVE|EXPIRED|SUSPENDED|CANCELLED|TRIAL|PENDING_PAYMENT)$") String status) {
        this.status = status;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public Integer getCarriedOverSms() {
        return carriedOverSms;
    }

    public void setCarriedOverSms(Integer carriedOverSms) {
        this.carriedOverSms = carriedOverSms;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPlanId() {
        return planId;
    }

    public void setPlanId(Long planId) {
        this.planId = planId;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public Instant getUpdatedDate() {
        return updatedDate;
    }

    public void setUpdatedDate(Instant updatedDate) {
        this.updatedDate = updatedDate;
    }

    public Boolean getSidebarVisible() {
        return sidebarVisible;
    }

    public void setSidebarVisible(Boolean sidebarVisible) {
        this.sidebarVisible = sidebarVisible;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }

    public @Min(0) Integer getWhatsappUsed() {
        return whatsappUsed;
    }

    public void setWhatsappUsed(@Min(0) Integer whatsappUsed) {
        this.whatsappUsed = whatsappUsed;
    }

    public @Min(0) Integer getSmsUsed() {
        return smsUsed;
    }

    public void setSmsUsed(@Min(0) Integer smsUsed) {
        this.smsUsed = smsUsed;
    }

    public @Min(0) Integer getApiCallsToday() {
        return apiCallsToday;
    }

    public void setApiCallsToday(@Min(0) Integer apiCallsToday) {
        this.apiCallsToday = apiCallsToday;
    }

    public @Min(0) Integer getStorageUsedMb() {
        return storageUsedMb;
    }

    public void setStorageUsedMb(@Min(0) Integer storageUsedMb) {
        this.storageUsedMb = storageUsedMb;
    }

    public LocalDate getLastApiCallDate() {
        return lastApiCallDate;
    }

    public void setLastApiCallDate(LocalDate lastApiCallDate) {
        this.lastApiCallDate = lastApiCallDate;
    }

    public @Size(max = 50) String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(@Size(max = 50) String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public @Size(max = 255) String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(@Size(max = 255) String transactionId) {
        this.transactionId = transactionId;
    }

    public Boolean getAutoRenew() {
        return autoRenew;
    }

    public void setAutoRenew(Boolean autoRenew) {
        this.autoRenew = autoRenew;
    }

    public Boolean getTrial() {
        return isTrial;
    }

    public void setTrial(Boolean trial) {
        isTrial = trial;
    }

    public LocalDate getTrialEndDate() {
        return trialEndDate;
    }

    public void setTrialEndDate(LocalDate trialEndDate) {
        this.trialEndDate = trialEndDate;
    }

    public Boolean getCustomPlan() {
        return isCustomPlan;
    }

    public void setCustomPlan(Boolean customPlan) {
        isCustomPlan = customPlan;
    }

    public Integer getCustomStorageLimitMb() {
        return customStorageLimitMb;
    }

    public void setCustomStorageLimitMb(Integer customStorageLimitMb) {
        this.customStorageLimitMb = customStorageLimitMb;
    }

    public Boolean getCustomCanManageUsers() {
        return customCanManageUsers;
    }

    public void setCustomCanManageUsers(Boolean customCanManageUsers) {
        this.customCanManageUsers = customCanManageUsers;
    }

    public Boolean getCustomCanViewConversations() {
        return customCanViewConversations;
    }

    public void setCustomCanViewConversations(Boolean customCanViewConversations) {
        this.customCanViewConversations = customCanViewConversations;
    }

    public Boolean getBonusSmsEnabled() {
        return bonusSmsEnabled;
    }

    public void setBonusSmsEnabled(Boolean bonusSmsEnabled) {
        this.bonusSmsEnabled = bonusSmsEnabled;
    }

    public Boolean getBonusWhatsappEnabled() {
        return bonusWhatsappEnabled;
    }

    public void setBonusWhatsappEnabled(Boolean bonusWhatsappEnabled) {
        this.bonusWhatsappEnabled = bonusWhatsappEnabled;
    }

    public Integer getBonusWhatsappAmount() {
        return bonusWhatsappAmount;
    }

    public void setBonusWhatsappAmount(Integer bonusWhatsappAmount) {
        this.bonusWhatsappAmount = bonusWhatsappAmount;
    }

    public Integer getCustomWhatsappLimit() {
        return customWhatsappLimit;
    }

    public void setCustomWhatsappLimit(Integer customWhatsappLimit) {
        this.customWhatsappLimit = customWhatsappLimit;
    }

    public Integer getCustomSmsLimit() {
        return customSmsLimit;
    }

    public void setCustomSmsLimit(Integer customSmsLimit) {
        this.customSmsLimit = customSmsLimit;
    }

    public @Size(max = 255) String getCustomName() {
        return customName;
    }

    public void setCustomName(@Size(max = 255) String customName) {
        this.customName = customName;
    }

    public String getCustomPeriod() {
        return customPeriod;
    }

    public void setCustomPeriod(String customPeriod) {
        this.customPeriod = customPeriod;
    }

    public Boolean getAllowWhatsappCarryover() {
        return allowWhatsappCarryover;
    }

    public void setAllowWhatsappCarryover(Boolean allowWhatsappCarryover) {
        this.allowWhatsappCarryover = allowWhatsappCarryover;
    }

    public Boolean getAllowSmsCarryover() {
        return allowSmsCarryover;
    }

    public void setAllowSmsCarryover(Boolean allowSmsCarryover) {
        this.allowSmsCarryover = allowSmsCarryover;
    }

    public Integer getBonusSmsAmount() {
        return bonusSmsAmount;
    }

    public void setBonusSmsAmount(Integer bonusSmsAmount) {
        this.bonusSmsAmount = bonusSmsAmount;
    }

    public Boolean getCustomCanViewAnalytics() {
        return customCanViewAnalytics;
    }

    public void setCustomCanViewAnalytics(Boolean customCanViewAnalytics) {
        this.customCanViewAnalytics = customCanViewAnalytics;
    }

    public Boolean getCustomPrioritySupport() {
        return customPrioritySupport;
    }

    public void setCustomPrioritySupport(Boolean customPrioritySupport) {
        this.customPrioritySupport = customPrioritySupport;
    }

    public Integer getCustomUsersLimit() {
        return customUsersLimit;
    }

    public void setCustomUsersLimit(Integer customUsersLimit) {
        this.customUsersLimit = customUsersLimit;
    }

    public Integer getCustomApiCallsLimit() {
        return customApiCallsLimit;
    }

    public void setCustomApiCallsLimit(Integer customApiCallsLimit) {
        this.customApiCallsLimit = customApiCallsLimit;
    }

    public Integer getCustomTemplatesLimit() {
        return customTemplatesLimit;
    }

    public void setCustomTemplatesLimit(Integer customTemplatesLimit) {
        this.customTemplatesLimit = customTemplatesLimit;
    }

    public BigDecimal getCustomPrice() {
        return customPrice;
    }

    public void setCustomPrice(BigDecimal customPrice) {
        this.customPrice = customPrice;
    }

    public @Size(max = 1000) String getCustomDescription() {
        return customDescription;
    }

    public void setCustomDescription(@Size(max = 1000) String customDescription) {
        this.customDescription = customDescription;
    }

    public Boolean getCustomCanManageTemplates() {
        return customCanManageTemplates;
    }

    public void setCustomCanManageTemplates(Boolean customCanManageTemplates) {
        this.customCanManageTemplates = customCanManageTemplates;
    }
}
