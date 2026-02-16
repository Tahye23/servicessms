package com.example.myproject.service.dto;

import com.example.myproject.web.rest.dto.ApplicationDTO;
import java.io.Serializable;
import java.time.ZonedDateTime;

public class TokensAppDTO implements Serializable {

    private Integer id;
    private String token;
    private Boolean active;
    private ZonedDateTime dateExpiration;
    private String userLogin;
    private ZonedDateTime lastUsedAt;
    private ZonedDateTime createdAt;
    private Boolean isExpired;
    private Integer applicationId;
    private String applicationName;
    private ApplicationDTO application;

    public ApplicationDTO getApplication() {
        return application;
    }

    public void setApplication(ApplicationDTO application) {
        this.application = application;
    }

    public Integer getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(Integer applicationId) {
        this.applicationId = applicationId;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public String getUserLogin() {
        return userLogin;
    }

    public void setUserLogin(String userLogin) {
        this.userLogin = userLogin;
    }

    public ZonedDateTime getLastUsedAt() {
        return lastUsedAt;
    }

    public void setLastUsedAt(ZonedDateTime lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }

    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(ZonedDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Boolean getIsExpired() {
        return isExpired;
    }

    public void setIsExpired(Boolean expired) {
        isExpired = expired;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public ZonedDateTime getDateExpiration() {
        return dateExpiration;
    }

    public void setDateExpiration(ZonedDateTime dateExpiration) {
        this.dateExpiration = dateExpiration;
    }
}
