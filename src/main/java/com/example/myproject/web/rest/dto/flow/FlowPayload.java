package com.example.myproject.web.rest.dto.flow;

// ================================
// DTOs PAYLOAD POUR RECEVOIR LES DONNÉES DU FRONTEND
// ================================

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

// FlowPayload.java - Payload principal reçu du frontend
public class FlowPayload {

    private Long partnerId;
    private String flowId;

    @NotBlank(message = "Le nom du flow est obligatoire")
    @Size(max = 100)
    private String name;

    private String description;
    private Boolean active;
    private String language = "fr";
    private List<FlowNodePayload> nodes;
    private List<VariablePayload> variables;
    private String createdAt;
    private String updatedAt;

    // Constructeurs
    public FlowPayload() {}

    // Getters et setters
    public Long getPartnerId() {
        return partnerId;
    }

    public void setPartnerId(Long partnerId) {
        this.partnerId = partnerId;
    }

    public String getFlowId() {
        return flowId;
    }

    public void setFlowId(String flowId) {
        this.flowId = flowId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public List<FlowNodePayload> getNodes() {
        return nodes;
    }

    public void setNodes(List<FlowNodePayload> nodes) {
        this.nodes = nodes;
    }

    public List<VariablePayload> getVariables() {
        return variables;
    }

    public void setVariables(List<VariablePayload> variables) {
        this.variables = variables;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}
