package com.example.myproject.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(name = "abonnement")
public class Abonnement implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private ExtendedUser user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id")
    @JsonIgnoreProperties(value = { "abonnements" }, allowSetters = true)
    private PlanAbonnement plan;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SubscriptionStatus status = SubscriptionStatus.ACTIVE;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "created_date", nullable = false)
    private Instant createdDate = Instant.now();

    @Column(name = "updated_date", nullable = false)
    private Instant updatedDate = Instant.now();

    // Usage counts
    @Column(name = "sms_used", nullable = false)
    private Integer smsUsed = 0;

    @Column(name = "whatsapp_used", nullable = false)
    private Integer whatsappUsed = 0;

    @Column(name = "api_calls_today", nullable = false)
    private Integer apiCallsToday = 0;

    @Column(name = "storage_used_mb", nullable = false)
    private Integer storageUsedMb = 0;

    @Column(name = "last_api_call_date")
    private LocalDate lastApiCallDate;

    // Payment info
    @Size(max = 50)
    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Size(max = 255)
    @Column(name = "transaction_id", length = 255)
    private String transactionId;

    @Column(name = "auto_renew", nullable = false)
    private Boolean autoRenew = true;

    // Trial info
    @Column(name = "trial_end_date")
    private LocalDate trialEndDate;

    @Column(name = "is_trial", nullable = false)
    private Boolean isTrial = false;

    // Custom plan fields
    @Column(name = "is_custom_plan", nullable = false)
    private Boolean isCustomPlan = false;

    @Column(name = "custom_price", precision = 10, scale = 2)
    private BigDecimal customPrice;

    @Column(name = "custom_period", length = 20)
    private String customPeriod;

    @Size(max = 255)
    @Column(name = "custom_name", length = 255)
    private String customName;

    @Size(max = 1000)
    @Column(name = "custom_description", length = 1000)
    private String customDescription;

    @Column(name = "custom_sms_limit")
    private Integer customSmsLimit;

    @Column(name = "custom_whatsapp_limit")
    private Integer customWhatsappLimit;

    @Column(name = "custom_users_limit")
    private Integer customUsersLimit;

    @Column(name = "custom_templates_limit")
    private Integer customTemplatesLimit;

    @Column(name = "custom_api_calls_limit")
    private Integer customApiCallsLimit;

    @Column(name = "sidebar_visible")
    private Boolean sidebarVisible = true;

    @Column(name = "custom_storage_limit_mb")
    private Integer customStorageLimitMb;

    // Permissions
    @Column(name = "custom_can_manage_users", nullable = false)
    private Boolean customCanManageUsers = false;

    @Column(name = "custom_can_manage_templates", nullable = false)
    private Boolean customCanManageTemplates = false;

    @Column(name = "custom_can_view_conversations", nullable = false)
    private Boolean customCanViewConversations = false;

    @Column(name = "custom_can_view_analytics", nullable = false)
    private Boolean customCanViewAnalytics = false;

    @Column(name = "custom_priority_support", nullable = false)
    private Boolean customPrioritySupport = false;

    // Bonus
    @Column(name = "bonus_sms_enabled", nullable = false)
    private Boolean bonusSmsEnabled = false;

    @Column(name = "bonus_sms_amount")
    private Integer bonusSmsAmount;

    @Column(name = "bonus_whatsapp_enabled", nullable = false)
    private Boolean bonusWhatsappEnabled = false;

    @Column(name = "bonus_whatsapp_amount")
    private Integer bonusWhatsappAmount;

    // Carryover
    @Column(name = "allow_sms_carryover", nullable = false)
    private Boolean allowSmsCarryover = false;

    @Column(name = "allow_whatsapp_carryover", nullable = false)
    private Boolean allowWhatsappCarryover = false;

    @Column(name = "carried_over_sms")
    private Integer carriedOverSms;

    @Column(name = "carried_over_whatsapp")
    private Integer carriedOverWhatsapp;

    @Column(name = "can_view_dashboard", nullable = false)
    private Boolean canViewDashboard = false;

    @Column(name = "can_manage_api", nullable = false)
    private Boolean canManageAPI = false;

    // Enum status
    public enum SubscriptionStatus {
        ACTIVE,
        EXPIRED,
        SUSPENDED,
        CANCELLED,
        TRIAL,
        PENDING_PAYMENT,
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public ExtendedUser getUser() {
        return user;
    }

    public void setUser(ExtendedUser user) {
        this.user = user;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public SubscriptionStatus getStatus() {
        return status;
    }

    public void setStatus(SubscriptionStatus status) {
        this.status = status;
    }

    public PlanAbonnement getPlan() {
        return plan;
    }

    public void setPlan(PlanAbonnement plan) {
        this.plan = plan;
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

    public Instant getCreatedDate() {
        return createdDate;
    }

    public Boolean getSidebarVisible() {
        return sidebarVisible;
    }

    public void setSidebarVisible(Boolean sidebarVisible) {
        this.sidebarVisible = sidebarVisible;
    }

    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
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

    public Integer getApiCallsToday() {
        return apiCallsToday;
    }

    public void setApiCallsToday(Integer apiCallsToday) {
        this.apiCallsToday = apiCallsToday;
    }

    public Integer getStorageUsedMb() {
        return storageUsedMb;
    }

    public void setStorageUsedMb(Integer storageUsedMb) {
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

    public LocalDate getTrialEndDate() {
        return trialEndDate;
    }

    public void setTrialEndDate(LocalDate trialEndDate) {
        this.trialEndDate = trialEndDate;
    }

    public Boolean getTrial() {
        return isTrial;
    }

    public void setTrial(Boolean trial) {
        isTrial = trial;
    }

    public Boolean getCustomPlan() {
        return isCustomPlan;
    }

    public void setCustomPlan(Boolean customPlan) {
        isCustomPlan = customPlan;
    }

    public BigDecimal getCustomPrice() {
        return customPrice;
    }

    public void setCustomPrice(BigDecimal customPrice) {
        this.customPrice = customPrice;
    }

    public String getCustomPeriod() {
        return customPeriod;
    }

    public void setCustomPeriod(String customPeriod) {
        this.customPeriod = customPeriod;
    }

    public @Size(max = 255) String getCustomName() {
        return customName;
    }

    public void setCustomName(@Size(max = 255) String customName) {
        this.customName = customName;
    }

    public @Size(max = 1000) String getCustomDescription() {
        return customDescription;
    }

    public void setCustomDescription(@Size(max = 1000) String customDescription) {
        this.customDescription = customDescription;
    }

    public Integer getCustomSmsLimit() {
        return customSmsLimit;
    }

    public void setCustomSmsLimit(Integer customSmsLimit) {
        this.customSmsLimit = customSmsLimit;
    }

    public Integer getCustomWhatsappLimit() {
        return customWhatsappLimit;
    }

    public void setCustomWhatsappLimit(Integer customWhatsappLimit) {
        this.customWhatsappLimit = customWhatsappLimit;
    }

    public Integer getCustomUsersLimit() {
        return customUsersLimit;
    }

    public void setCustomUsersLimit(Integer customUsersLimit) {
        this.customUsersLimit = customUsersLimit;
    }

    public Integer getCustomTemplatesLimit() {
        return customTemplatesLimit;
    }

    public void setCustomTemplatesLimit(Integer customTemplatesLimit) {
        this.customTemplatesLimit = customTemplatesLimit;
    }

    public Integer getCustomApiCallsLimit() {
        return customApiCallsLimit;
    }

    public void setCustomApiCallsLimit(Integer customApiCallsLimit) {
        this.customApiCallsLimit = customApiCallsLimit;
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

    public Boolean getCustomCanManageTemplates() {
        return customCanManageTemplates;
    }

    public void setCustomCanManageTemplates(Boolean customCanManageTemplates) {
        this.customCanManageTemplates = customCanManageTemplates;
    }

    public Integer getCarriedOverWhatsapp() {
        return carriedOverWhatsapp;
    }

    public void setCarriedOverWhatsapp(Integer carriedOverWhatsapp) {
        this.carriedOverWhatsapp = carriedOverWhatsapp;
    }

    public Integer getCarriedOverSms() {
        return carriedOverSms;
    }

    public void setCarriedOverSms(Integer carriedOverSms) {
        this.carriedOverSms = carriedOverSms;
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

    public Integer getBonusSmsAmount() {
        return bonusSmsAmount;
    }

    public void setBonusSmsAmount(Integer bonusSmsAmount) {
        this.bonusSmsAmount = bonusSmsAmount;
    }

    public Boolean getBonusSmsEnabled() {
        return bonusSmsEnabled;
    }

    public void setBonusSmsEnabled(Boolean bonusSmsEnabled) {
        this.bonusSmsEnabled = bonusSmsEnabled;
    }

    public Boolean getCustomPrioritySupport() {
        return customPrioritySupport;
    }

    public void setCustomPrioritySupport(Boolean customPrioritySupport) {
        this.customPrioritySupport = customPrioritySupport;
    }

    public Boolean getCustomCanViewAnalytics() {
        return customCanViewAnalytics;
    }

    public void setCustomCanViewAnalytics(Boolean customCanViewAnalytics) {
        this.customCanViewAnalytics = customCanViewAnalytics;
    }

    public Boolean getCustomCanViewConversations() {
        return customCanViewConversations;
    }

    public void setCustomCanViewConversations(Boolean customCanViewConversations) {
        this.customCanViewConversations = customCanViewConversations;
    }
}
