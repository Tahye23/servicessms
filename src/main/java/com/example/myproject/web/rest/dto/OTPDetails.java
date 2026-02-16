package com.example.myproject.web.rest.dto;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class OTPDetails {

    private String otp;
    private String expirationDate; // Format: yyyy-MM-dd HH:mm:ss
    private String message;

    // Constructeur par défaut
    public OTPDetails() {}

    // Getters et Setters
    public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }

    public String getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(String expirationDate) {
        // Formatage de la date
        //DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        this.expirationDate = expirationDate;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    // toString
    @Override
    public String toString() {
        return "OTPDetails{" + "otp='" + otp + '\'' + ", expirationDate='" + expirationDate + '\'' + ", message='" + message + '\'' + '}';
    }
}
