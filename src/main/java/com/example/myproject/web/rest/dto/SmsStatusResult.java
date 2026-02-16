package com.example.myproject.web.rest.dto;

public class SmsStatusResult {

    private String status;
    private Byte stateCode;
    private String errorCode;

    public SmsStatusResult() {}

    public SmsStatusResult(String status, Byte stateCode, String errorCode) {
        this.status = status;
        this.stateCode = stateCode;
        this.errorCode = errorCode;
    }

    public static SmsStatusResult unknown(String error) {
        return new SmsStatusResult("UNKNOWN", null, error);
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Byte getStateCode() {
        return stateCode;
    }

    public void setStateCode(Byte stateCode) {
        this.stateCode = stateCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }
}
