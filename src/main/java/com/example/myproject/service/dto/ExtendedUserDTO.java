package com.example.myproject.service.dto;

import com.example.myproject.domain.ExtendedUser;
import java.io.Serializable;
import java.time.Instant;
import java.util.List;

public class ExtendedUserDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String phoneNumber;
    private String companyName;
    private String website;
    private String timezone;
    private String language;

    // Quotas et usage
    private Integer smsQuota;
    private Integer whatsappQuota;
    private Integer smsUsedThisMonth;
    private Integer whatsappUsedThisMonth;
    private Long totalMessagesSent;

    // Permissions calculées
    private Boolean canSendSms;
    private Boolean canSendWhatsapp;
    private Boolean canManageUsers;
    private Boolean canManageTemplates;
    private Boolean canViewConversations;

    // Abonnements
    private List<UserSubscriptionDTO> subscriptions;
    private SubscriptionAccessDTO access;

    // Métriques
    private Instant lastLogin;
    private Long loginCount;
    private Instant accountCreated;

    // Constructeurs
    public ExtendedUserDTO() {}

    public ExtendedUserDTO(ExtendedUser extendedUser) {
        this.id = extendedUser.getId();
        this.phoneNumber = extendedUser.getPhoneNumber();
        this.companyName = extendedUser.getCompanyName();
        this.website = extendedUser.getWebsite();
        this.timezone = extendedUser.getTimezone();
        this.language = extendedUser.getLanguage();
        this.smsQuota = extendedUser.getSmsQuota();
        this.whatsappQuota = extendedUser.getWhatsappQuota();
        this.smsUsedThisMonth = extendedUser.getSmsUsedThisMonth();
        this.whatsappUsedThisMonth = extendedUser.getWhatsappUsedThisMonth();
        this.totalMessagesSent = extendedUser.getTotalMessagesSent();
        this.lastLogin = extendedUser.getLastLogin();
        this.loginCount = extendedUser.getLoginCount();
        this.accountCreated = extendedUser.getAccountCreated();
    }

    // Getters & Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Integer getSmsQuota() {
        return smsQuota;
    }

    public void setSmsQuota(Integer smsQuota) {
        this.smsQuota = smsQuota;
    }

    public Integer getWhatsappQuota() {
        return whatsappQuota;
    }

    public void setWhatsappQuota(Integer whatsappQuota) {
        this.whatsappQuota = whatsappQuota;
    }

    public Integer getSmsUsedThisMonth() {
        return smsUsedThisMonth;
    }

    public void setSmsUsedThisMonth(Integer smsUsedThisMonth) {
        this.smsUsedThisMonth = smsUsedThisMonth;
    }

    public Integer getWhatsappUsedThisMonth() {
        return whatsappUsedThisMonth;
    }

    public void setWhatsappUsedThisMonth(Integer whatsappUsedThisMonth) {
        this.whatsappUsedThisMonth = whatsappUsedThisMonth;
    }

    public Long getTotalMessagesSent() {
        return totalMessagesSent;
    }

    public void setTotalMessagesSent(Long totalMessagesSent) {
        this.totalMessagesSent = totalMessagesSent;
    }

    public Boolean getCanSendSms() {
        return canSendSms;
    }

    public void setCanSendSms(Boolean canSendSms) {
        this.canSendSms = canSendSms;
    }

    public Boolean getCanSendWhatsapp() {
        return canSendWhatsapp;
    }

    public void setCanSendWhatsapp(Boolean canSendWhatsapp) {
        this.canSendWhatsapp = canSendWhatsapp;
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

    public List<UserSubscriptionDTO> getSubscriptions() {
        return subscriptions;
    }

    public void setSubscriptions(List<UserSubscriptionDTO> subscriptions) {
        this.subscriptions = subscriptions;
    }

    public SubscriptionAccessDTO getAccess() {
        return access;
    }

    public void setAccess(SubscriptionAccessDTO access) {
        this.access = access;
    }

    public Instant getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(Instant lastLogin) {
        this.lastLogin = lastLogin;
    }

    public Long getLoginCount() {
        return loginCount;
    }

    public void setLoginCount(Long loginCount) {
        this.loginCount = loginCount;
    }

    public Instant getAccountCreated() {
        return accountCreated;
    }

    public void setAccountCreated(Instant accountCreated) {
        this.accountCreated = accountCreated;
    }

    @Override
    public String toString() {
        return (
            "ExtendedUserDTO{" +
            "id=" +
            id +
            ", companyName='" +
            companyName +
            '\'' +
            ", canSendSms=" +
            canSendSms +
            ", canSendWhatsapp=" +
            canSendWhatsapp +
            ", totalMessagesSent=" +
            totalMessagesSent +
            '}'
        );
    }
}
