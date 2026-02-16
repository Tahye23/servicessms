package com.example.myproject.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "chatbot_flow")
public class ChatbotFlow extends AbstractAuditingEntity<Long> implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator", allocationSize = 1)
    private Long id;

    @NotNull
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @NotNull
    @Size(max = 100)
    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Size(max = 500)
    @Column(name = "description", length = 500)
    private String description;

    @NotNull
    @Column(name = "active", nullable = false)
    private Boolean active = false;

    @Size(max = 10)
    @Column(name = "language", length = 10)
    private String language = "fr";

    @Column(name = "version")
    private Integer version = 1;

    @Column(name = "status")
    private String status = "DRAFT";

    // Relations avec les nouvelles entités
    @OneToMany(mappedBy = "flow", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("nodeOrder ASC")
    private List<FlowNode> nodes = new ArrayList<>();

    @OneToMany(mappedBy = "flow", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<FlowVariable> variables = new ArrayList<>();

    // Getters et setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<FlowNode> getNodes() {
        return nodes;
    }

    public void setNodes(List<FlowNode> nodes) {
        this.nodes = nodes;
    }

    public List<FlowVariable> getVariables() {
        return variables;
    }

    public void setVariables(List<FlowVariable> variables) {
        this.variables = variables;
    }

    // Méthodes utilitaires
    public void addNode(FlowNode node) {
        nodes.add(node);
        node.setFlow(this);
    }

    public void removeNode(FlowNode node) {
        nodes.remove(node);
        node.setFlow(null);
    }

    public void addVariable(FlowVariable variable) {
        variables.add(variable);
        variable.setFlow(this);
    }

    public void removeVariable(FlowVariable variable) {
        variables.remove(variable);
        variable.setFlow(null);
    }
}
