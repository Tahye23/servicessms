package com.example.myproject.web.rest.dto.flow;

import java.util.List;

public class ApiResponse<T> {

    private boolean success;
    private T data;
    private String message;
    private List<String> errors;

    // Constructeurs
    public ApiResponse() {}

    public ApiResponse(boolean success, T data, String message) {
        this.success = success;
        this.data = data;
        this.message = message;
    }

    public ApiResponse(boolean success, T data, String message, List<String> errors) {
        this.success = success;
        this.data = data;
        this.message = message;
        this.errors = errors;
    }

    // Méthodes statiques pour faciliter la création
    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, data, message);
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, "Succès");
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, null, message);
    }

    public static <T> ApiResponse<T> error(String message, List<String> errors) {
        return new ApiResponse<>(false, null, message, errors);
    }

    // Getters et Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }
}
