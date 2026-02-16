package com.example.myproject.web.rest.dto;

public class OtpVerify {

    private String code;
    private String phoneNumber;

    // Constructeur
    public OtpVerify() {}

    // Getter et Setter pour code
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    // Getter et Setter pour phoneNumber
    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    @Override
    public String toString() {
        return "OtpVerify{" + "code='" + code + '\'' + ", phoneNumber='" + phoneNumber + '\'' + '}';
    }
}
