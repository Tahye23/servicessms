package com.example.myproject.web.rest.dto;

public class Otp {

    private String phoneNumber;

    public Otp() {}

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    @Override
    public String toString() {
        return ", phoneNumber='" + phoneNumber + '\'' + '}';
    }
}
