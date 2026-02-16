package com.example.myproject.web.rest.dto;

public class CleanResponseDTO {

    private String progressId;

    public CleanResponseDTO(String progressId) {
        this.progressId = progressId;
    }

    public String getProgressId() {
        return progressId;
    }

    public void setProgressId(String progressId) {
        this.progressId = progressId;
    }
}
