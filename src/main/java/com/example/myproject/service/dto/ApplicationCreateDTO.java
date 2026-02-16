package com.example.myproject.service.dto;

import java.time.ZonedDateTime;

public class ApplicationCreateDTO {

    private String name;
    private String description;

    // uniquement pour créer le token initial
    private ZonedDateTime tokenExpirationDate;

    // getters / setters

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

    public ZonedDateTime getTokenExpirationDate() {
        return tokenExpirationDate;
    }

    public void setTokenExpirationDate(ZonedDateTime tokenExpirationDate) {
        this.tokenExpirationDate = tokenExpirationDate;
    }
}
