package com.example.myproject.service.dto;

public class CoexistenceRequest {

    private String phoneNumberId;
    private String accessToken;

    public CoexistenceRequest() {}

    public CoexistenceRequest(String phoneNumberId, String accessToken) {
        this.phoneNumberId = phoneNumberId;
        this.accessToken = accessToken;
    }

    public String getPhoneNumberId() {
        return phoneNumberId;
    }

    public void setPhoneNumberId(String phoneNumberId) {
        this.phoneNumberId = phoneNumberId;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
}
