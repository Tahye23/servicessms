package com.example.myproject.web.rest.dto;

import com.example.myproject.domain.Application;
import com.example.myproject.service.dto.TokensAppDTO;
import java.io.Serializable;
import java.time.Instant;
import java.util.Set;

public class ApplicationDTO implements Serializable {

    private Integer id;
    private String name;
    private String description;
    private Integer userId;
    private Boolean isActive;
    private String environment;
    private Integer dailyLimit;
    private Integer monthlyLimit;
    private Integer currentDailyUsage;
    private Integer currentMonthlyUsage;
    private Long totalApiCalls;
    private Instant lastApiCall;
    private Instant createdAt;
    private Instant updatedAt;

    private Set<Application.AllowedService> allowedServices;

    public Set<Application.AllowedService> getAllowedServices() {
        return allowedServices;
    }

    public void setAllowedServices(Set<Application.AllowedService> allowedServices) {
        this.allowedServices = allowedServices;
    }

    // relations
    private Set<TokensAppDTO> tokens;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Boolean getIsActive() {
        return this.isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public Integer getDailyLimit() {
        return dailyLimit;
    }

    public void setDailyLimit(Integer dailyLimit) {
        this.dailyLimit = dailyLimit;
    }

    public Integer getMonthlyLimit() {
        return monthlyLimit;
    }

    public void setMonthlyLimit(Integer monthlyLimit) {
        this.monthlyLimit = monthlyLimit;
    }

    public Integer getCurrentDailyUsage() {
        return currentDailyUsage;
    }

    public void setCurrentDailyUsage(Integer currentDailyUsage) {
        this.currentDailyUsage = currentDailyUsage;
    }

    public Integer getCurrentMonthlyUsage() {
        return currentMonthlyUsage;
    }

    public void setCurrentMonthlyUsage(Integer currentMonthlyUsage) {
        this.currentMonthlyUsage = currentMonthlyUsage;
    }

    public Long getTotalApiCalls() {
        return totalApiCalls;
    }

    public void setTotalApiCalls(Long totalApiCalls) {
        this.totalApiCalls = totalApiCalls;
    }

    public Instant getLastApiCall() {
        return lastApiCall;
    }

    public void setLastApiCall(Instant lastApiCall) {
        this.lastApiCall = lastApiCall;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Set<TokensAppDTO> getTokens() {
        return tokens;
    }

    public void setTokens(Set<TokensAppDTO> tokens) {
        this.tokens = tokens;
    }
}
