package com.example.myproject.web.rest.errors;

/**
 * ✅ EXCEPTION PERSONNALISÉE avec code HTTP
 * (Sans Lombok pour compatibilité maximale)
 */
public class CustomException extends RuntimeException {

    private final int httpStatusCode;

    public CustomException(String message, int httpStatusCode) {
        super(message);
        this.httpStatusCode = httpStatusCode;
    }

    public CustomException(String message, int httpStatusCode, Throwable cause) {
        super(message, cause);
        this.httpStatusCode = httpStatusCode;
    }

    /**
     * ✅ GETTER pour httpStatusCode
     */
    public int getHttpStatusCode() {
        return httpStatusCode;
    }

    @Override
    public String toString() {
        return String.format("CustomException[status=%d, message=%s]", httpStatusCode, getMessage());
    }
}
