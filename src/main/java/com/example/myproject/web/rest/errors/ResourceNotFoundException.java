package com.example.myproject.web.rest.errors;

import org.springframework.http.HttpStatus;

/**
 * ✅ EXCEPTION pour ressource introuvable (404)
 */
public class ResourceNotFoundException extends CustomException {

    public ResourceNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND.value());
    }

    public ResourceNotFoundException(String resourceName, Long id) {
        super(String.format("%s avec ID %d introuvable", resourceName, id), HttpStatus.NOT_FOUND.value());
    }
}
