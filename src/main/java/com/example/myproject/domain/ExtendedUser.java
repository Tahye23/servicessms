package com.example.myproject.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@Table(name = "extended_user")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class ExtendedUser implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @OneToOne
    @JoinColumn(unique = true)
    private User user;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "address")
    private String address;

    @Column(name = "city")
    private String city;

    @Column(name = "country")
    private String country;

    @Column(name = "postal_code")
    private String postalCode;

    @Column(name = "company_name")
    private String companyName;

    @Column(name = "website")
    private String website;

    @Column(name = "created_date")
    private Instant createdDate = Instant.now();

    @Column(name = "updated_date")
    private Instant updatedDate = Instant.now();

    // ===== RELATIONS AVEC LE SYSTÈME D'ABONNEMENT =====

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "user" }, allowSetters = true)
    private Set<Abonnement> abonnements = new HashSet<>();

    // ===== QUOTAS ET COMPTEURS =====

    @Column(name = "sms_quota")
    private Integer smsQuota = 0;

    @Column(name = "whatsapp_quota")
    private Integer whatsappQuota = 0;

    @Column(name = "sms_used_this_month")
    private Integer smsUsedThisMonth = 0;

    @Column(name = "whatsapp_used_this_month")
    private Integer whatsappUsedThisMonth = 0;

    @Column(name = "last_quota_reset")
    private LocalDate lastQuotaReset;

    // ===== PRÉFÉRENCES ET PARAMÈTRES =====

    @Column(name = "timezone")
    private String timezone = "UTC";

    @Column(name = "language")
    private String language = "fr";

    @Column(name = "notifications_email")
    private Boolean notificationsEmail = true;

    @Column(name = "notifications_sms")
    private Boolean notificationsSms = false;

    @Column(name = "marketing_emails")
    private Boolean marketingEmails = true;

    @Column(name = "api_key")
    private String apiKey;

    @Column(name = "api_key_created_date")
    private Instant apiKeyCreatedDate;

    @Column(name = "api_key_last_used")
    private Instant apiKeyLastUsed;

    // ===== INFORMATIONS DE BILLING =====

    @Column(name = "billing_address")
    private String billingAddress;

    @Column(name = "billing_city")
    private String billingCity;

    @Column(name = "billing_country")
    private String billingCountry;

    @Column(name = "billing_postal_code")
    private String billingPostalCode;

    @Column(name = "tax_id")
    private String taxId;

    @Column(name = "payment_method_id")
    private String paymentMethodId; // ID Stripe/PayPal

    // ===== MÉTRIQUES ET ANALYTICS =====

    @Column(name = "last_login")
    private Instant lastLogin;

    @Column(name = "login_count")
    private Long loginCount = 0L;

    @Column(name = "total_messages_sent")
    private Long totalMessagesSent = 0L;

    @Column(name = "account_created")
    private Instant accountCreated = Instant.now();

    @Column(name = "subscription_start_date")
    private LocalDate subscriptionStartDate;

    // ===== CONSTRUCTEURS =====

    public ExtendedUser() {}

    public ExtendedUser(User user) {
        this.user = user;
        this.createdDate = Instant.now();
        this.updatedDate = Instant.now();
        this.accountCreated = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedDate = Instant.now();
    }

    // ===== GETTERS & SETTERS =====

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
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

    public Set<Abonnement> getAbonnements() {
        return abonnements;
    }

    public void setAbonnements(Set<Abonnement> abonnements) {
        this.abonnements = abonnements;
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

    public LocalDate getLastQuotaReset() {
        return lastQuotaReset;
    }

    public void setLastQuotaReset(LocalDate lastQuotaReset) {
        this.lastQuotaReset = lastQuotaReset;
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

    public Boolean getNotificationsEmail() {
        return notificationsEmail;
    }

    public void setNotificationsEmail(Boolean notificationsEmail) {
        this.notificationsEmail = notificationsEmail;
    }

    public Boolean getNotificationsSms() {
        return notificationsSms;
    }

    public void setNotificationsSms(Boolean notificationsSms) {
        this.notificationsSms = notificationsSms;
    }

    public Boolean getMarketingEmails() {
        return marketingEmails;
    }

    public void setMarketingEmails(Boolean marketingEmails) {
        this.marketingEmails = marketingEmails;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public Instant getApiKeyCreatedDate() {
        return apiKeyCreatedDate;
    }

    public void setApiKeyCreatedDate(Instant apiKeyCreatedDate) {
        this.apiKeyCreatedDate = apiKeyCreatedDate;
    }

    public Instant getApiKeyLastUsed() {
        return apiKeyLastUsed;
    }

    public void setApiKeyLastUsed(Instant apiKeyLastUsed) {
        this.apiKeyLastUsed = apiKeyLastUsed;
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

    public Long getTotalMessagesSent() {
        return totalMessagesSent;
    }

    public void setTotalMessagesSent(Long totalMessagesSent) {
        this.totalMessagesSent = totalMessagesSent;
    }

    public Instant getAccountCreated() {
        return accountCreated;
    }

    public void setAccountCreated(Instant accountCreated) {
        this.accountCreated = accountCreated;
    }

    public LocalDate getSubscriptionStartDate() {
        return subscriptionStartDate;
    }

    public void setSubscriptionStartDate(LocalDate subscriptionStartDate) {
        this.subscriptionStartDate = subscriptionStartDate;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }

    public Instant getUpdatedDate() {
        return updatedDate;
    }

    public void setUpdatedDate(Instant updatedDate) {
        this.updatedDate = updatedDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ExtendedUser)) return false;
        ExtendedUser that = (ExtendedUser) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return (
            "ExtendedUser{" +
            "id=" +
            id +
            ", phoneNumber='" +
            phoneNumber +
            '\'' +
            ", companyName='" +
            companyName +
            '\'' +
            ", smsQuota=" +
            smsQuota +
            ", whatsappQuota=" +
            whatsappQuota +
            '}'
        );
    }
}
