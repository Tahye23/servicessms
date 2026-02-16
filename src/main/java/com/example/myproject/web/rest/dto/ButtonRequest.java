package com.example.myproject.web.rest.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

public class ButtonRequest {

    @NotBlank(message = "Le type de bouton est requis")
    @Pattern(regexp = "^(QUICK_REPLY|URL|PHONE_NUMBER)$", message = "Type doit être QUICK_REPLY, URL ou PHONE_NUMBER")
    private String type;

    @NotBlank(message = "Le texte du bouton est requis")
    @Size(max = 25, message = "Le texte du bouton ne peut pas dépasser 25 caractères")
    private String text;

    // Pour type URL
    @URL(message = "L'URL doit être valide")
    private String url;

    // Pour type PHONE_NUMBER
    @Pattern(regexp = "^\\+[1-9]\\d{1,14}$", message = "Le numéro doit être au format international (+221...)")
    private String phoneNumber;

    // Validation conditionnelle
    @AssertTrue(message = "L'URL est requise pour les boutons URL")
    public boolean isUrlValidForUrlButtons() {
        if ("URL".equals(type)) {
            return url != null && !url.trim().isEmpty();
        }
        return true;
    }

    @AssertTrue(message = "Le numéro est requis pour les boutons PHONE_NUMBER")
    public boolean isPhoneValidForPhoneButtons() {
        if ("PHONE_NUMBER".equals(type)) {
            return phoneNumber != null && !phoneNumber.trim().isEmpty();
        }
        return true;
    }

    public @NotBlank String getType() {
        return type;
    }

    public void setType(@NotBlank String type) {
        this.type = type;
    }

    public @NotBlank String getText() {
        return text;
    }

    public void setText(@NotBlank String text) {
        this.text = text;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
    // getters/setters
}
