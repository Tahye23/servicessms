package com.example.myproject.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "chatbot_session")
public class ChatbotSession {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator", allocationSize = 1)
    private Long id;

    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;

    @Column(name = "user_login", nullable = false)
    private String userLogin;

    @Column(name = "current_node_id")
    private String currentNodeId;

    @Lob
    @Column(name = "variables", columnDefinition = "TEXT")
    private String variables; // JSON des variables utilisateur

    @Column(name = "last_interaction")
    private Instant lastInteraction;

    @Column(name = "is_active")
    private Boolean isActive = true;

    // ================================
    // AJOUTS MINIMAUX RECOMMANDÉS (3 champs seulement)
    // ================================

    @Column(name = "session_start")
    private Instant sessionStart; // Pour savoir quand la conversation a commencé

    @Column(name = "last_message_type")
    private String lastMessageType; // text, image, document, etc. (pour validation)

    @Column(name = "error_count")
    private Integer errorCount = 0; // Pour éviter les boucles infinies

    // Constructeurs
    public ChatbotSession() {
        this.sessionStart = Instant.now();
        this.lastInteraction = Instant.now();
        this.variables = "{}";
        this.errorCount = 0;
    }

    public ChatbotSession(String phoneNumber, String userLogin, String currentNodeId) {
        this.phoneNumber = phoneNumber;
        this.userLogin = userLogin;
        this.currentNodeId = currentNodeId;
        this.variables = "{}"; // JSON vide par défaut
        this.lastInteraction = Instant.now();
        this.sessionStart = Instant.now();
        this.isActive = true;
        this.errorCount = 0;
    }

    // Méthodes utilitaires simples
    public void incrementErrorCount() {
        this.errorCount = (this.errorCount != null ? this.errorCount : 0) + 1;
    }

    public void resetErrorCount() {
        this.errorCount = 0;
    }

    // Getters et setters pour les nouveaux champs
    public Instant getSessionStart() {
        return sessionStart;
    }

    public void setSessionStart(Instant sessionStart) {
        this.sessionStart = sessionStart;
    }

    public String getLastMessageType() {
        return lastMessageType;
    }

    public void setLastMessageType(String lastMessageType) {
        this.lastMessageType = lastMessageType;
    }

    public Integer getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(Integer errorCount) {
        this.errorCount = errorCount;
    }

    // Vos getters/setters existants restent inchangés...
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getUserLogin() {
        return userLogin;
    }

    public void setUserLogin(String userLogin) {
        this.userLogin = userLogin;
    }

    public String getCurrentNodeId() {
        return currentNodeId;
    }

    public void setCurrentNodeId(String currentNodeId) {
        this.currentNodeId = currentNodeId;
    }

    public String getVariables() {
        return variables;
    }

    public void setVariables(String variables) {
        this.variables = variables;
    }

    public Instant getLastInteraction() {
        return lastInteraction;
    }

    public void setLastInteraction(Instant lastInteraction) {
        this.lastInteraction = lastInteraction;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}
