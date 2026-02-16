package com.example.myproject.web.rest.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ComponentRequest {

    @NotBlank(message = "Le type de composant est requis")
    @Pattern(regexp = "^(HEADER|BODY|FOOTER|BUTTONS)$", message = "Type doit être HEADER, BODY, FOOTER ou BUTTONS")
    private String type;

    // Pour HEADER uniquement
    private String format; // TEXT, IMAGE, VIDEO, AUDIO, DOCUMENT

    // Contenu textuel (pour HEADER/TEXT, BODY, FOOTER)
    private String text;

    // Médias (pour HEADER avec format média)
    private String mediaUrl; // Base64 dataURL du frontend
    private String fileName; // Nom du fichier original
    private Long fileSize; // Taille en bytes
    private String mimeType; // Type MIME du fichier

    // Pour documents spécifiquement
    private String documentName;

    // Pour BUTTONS uniquement
    @Valid
    private List<ButtonRequest> buttons = new ArrayList<>();

    // Validation conditionnelle
    @AssertTrue(message = "Le texte est requis pour les composants TEXT")
    public boolean isTextValidForTextComponents() {
        if ("HEADER".equals(type) && "TEXT".equals(format)) {
            return text != null && !text.trim().isEmpty();
        }
        if ("BODY".equals(type) || "FOOTER".equals(type)) {
            return text != null && !text.trim().isEmpty();
        }
        return true;
    }

    @AssertTrue(message = "L'URL média est requise pour les composants média")
    public boolean isMediaValidForMediaComponents() {
        if ("HEADER".equals(type) && Set.of("IMAGE", "VIDEO", "AUDIO", "DOCUMENT").contains(format)) {
            return mediaUrl != null && !mediaUrl.trim().isEmpty();
        }
        return true;
    }

    @AssertTrue(message = "Au moins un bouton est requis pour le composant BUTTONS")
    public boolean isButtonsValidForButtonComponents() {
        if ("BUTTONS".equals(type)) {
            return buttons != null && !buttons.isEmpty();
        }
        return true;
    }

    public @NotBlank(message = "Le type de composant est requis") @Pattern(
        regexp = "^(HEADER|BODY|FOOTER|BUTTONS)$",
        message = "Type doit être HEADER, BODY, FOOTER ou BUTTONS"
    ) String getType() {
        return type;
    }

    public void setType(
        @NotBlank(message = "Le type de composant est requis") @Pattern(
            regexp = "^(HEADER|BODY|FOOTER|BUTTONS)$",
            message = "Type doit être HEADER, BODY, FOOTER ou BUTTONS"
        ) String type
    ) {
        this.type = type;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getMediaUrl() {
        return mediaUrl;
    }

    public void setMediaUrl(String mediaUrl) {
        this.mediaUrl = mediaUrl;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getDocumentName() {
        return documentName;
    }

    public void setDocumentName(String documentName) {
        this.documentName = documentName;
    }

    public @Valid List<ButtonRequest> getButtons() {
        return buttons;
    }

    public void setButtons(@Valid List<ButtonRequest> buttons) {
        this.buttons = buttons;
    }
}
