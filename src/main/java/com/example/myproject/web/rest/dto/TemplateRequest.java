package com.example.myproject.web.rest.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public class TemplateRequest {

    @NotBlank(message = "Le nom du template est requis")
    @Pattern(regexp = "^[a-z0-9_]+$", message = "Le nom ne doit contenir que a-z, 0-9 et _")
    private String name;

    @NotBlank(message = "La langue est requise")
    private String language; // ex "en_US"

    @NotBlank(message = "La catégorie est requise")
    @Pattern(regexp = "^(MARKETING|AUTHENTICATION|UTILITY)$", message = "Catégorie doit être MARKETING, AUTHENTICATION ou UTILITY")
    private String category;

    @NotNull(message = "Les composants sont requis")
    @Size(min = 1, message = "Au moins un composant est requis")
    @Valid
    private List<ComponentRequest> components;

    public @NotBlank String getName() {
        return name;
    }

    public void setName(@NotBlank String name) {
        this.name = name;
    }

    public @NotBlank String getLanguage() {
        return language;
    }

    public void setLanguage(@NotBlank String language) {
        this.language = language;
    }

    public @NotBlank String getCategory() {
        return category;
    }

    public void setCategory(@NotBlank String category) {
        this.category = category;
    }

    public @NotNull @Size(min = 1) List<ComponentRequest> getComponents() {
        return components;
    }

    public void setComponents(@NotNull @Size(min = 1) List<ComponentRequest> components) {
        this.components = components;
    }
}
