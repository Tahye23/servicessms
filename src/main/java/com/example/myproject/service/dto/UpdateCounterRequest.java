package com.example.myproject.service.dto;

import jakarta.validation.constraints.NotNull;
import java.io.Serializable;

public class UpdateCounterRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull
    private String type; // "SMS" ou "WHATSAPP"

    @NotNull
    private Integer count;

    @NotNull
    private String action; // "INCREMENT" ou "DECREMENT"

    private Long userId;
    private String reason; // Optionnel pour tracking

    // Constructeurs
    public UpdateCounterRequest() {}

    public UpdateCounterRequest(String type, Integer count, String action) {
        this.type = type;
        this.count = count;
        this.action = action;
    }

    // Getters & Setters
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    @Override
    public String toString() {
        return (
            "UpdateCounterRequest{" +
            "type='" +
            type +
            '\'' +
            ", count=" +
            count +
            ", action='" +
            action +
            '\'' +
            ", userId=" +
            userId +
            '}'
        );
    }
}
