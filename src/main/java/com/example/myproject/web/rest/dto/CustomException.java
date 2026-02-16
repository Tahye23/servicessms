package com.example.myproject.web.rest.dto;

public class CustomException extends RuntimeException {

    private final String message;
    private final int httpStatusCode;

    public CustomException(String message, int httpStatusCode) {
        super(message);
        this.message = message;
        this.httpStatusCode = httpStatusCode;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public int getHttpStatusCode() {
        return httpStatusCode;
    }
}
