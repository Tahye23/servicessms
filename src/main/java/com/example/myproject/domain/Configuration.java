package com.example.myproject.domain;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.Instant;

@Entity
@Table(name = "configuration")
public class Configuration implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator", allocationSize = 1)
    private Long id;

    @Column(name = "user_login", length = 50, nullable = false, unique = true)
    private String userLogin;

    @Column(name = "access_token", columnDefinition = "TEXT", nullable = false)
    private String accessToken;

    @Column(name = "business_id", length = 50, nullable = false)
    private String businessId;

    @Column(name = "phone_number_id", length = 50, nullable = false)
    private String phoneNumberId;

    @Column(name = "app_id", length = 50, nullable = false)
    private String appId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "custom_fields", columnDefinition = "TEXT")
    private String customFields;

    @Column(name = "verified")
    private boolean verified = false;

    @Column(name = "is_valid")
    private Boolean isValid = false;

    @Column(name = "coexistence_enabled")
    private Boolean coexistenceEnabled;

    @Column(name = "application", columnDefinition = "TEXT")
    private String application;

    @Column(name = "updated_at")
    private Instant updatedAt;

    // Getters & Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserLogin() {
        return userLogin;
    }

    public void setUserLogin(String userLogin) {
        this.userLogin = userLogin;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getBusinessId() {
        return businessId;
    }

    public void setBusinessId(String businessId) {
        this.businessId = businessId;
    }

    public String getPhoneNumberId() {
        return phoneNumberId;
    }

    public void setPhoneNumberId(String phoneNumberId) {
        this.phoneNumberId = phoneNumberId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getCustomFields() {
        return customFields;
    }

    public void setCustomFields(String customFields) {
        this.customFields = customFields;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public Boolean isValid() {
        return isValid;
    }

    public void setValid(Boolean valid) {
        isValid = valid;
    }

    public String getApplication() {
        return application;
    }

    public void setApplication(String application) {
        this.application = application;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Boolean getValid() {
        return isValid;
    }

    public Boolean getCoexistenceEnabled() {
        return coexistenceEnabled;
    }

    public void setCoexistenceEnabled(Boolean coexistenceEnabled) {
        this.coexistenceEnabled = coexistenceEnabled;
    }
}
