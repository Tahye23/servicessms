package com.example.myproject.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;

@Entity
@Table(name = "flow_variable")
public class FlowVariable extends AbstractAuditingEntity<Long> implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator", allocationSize = 1)
    private Long id;

    @NotNull
    @Size(max = 50)
    @Column(name = "name", length = 50, nullable = false)
    private String name;

    @Column(name = "default_value")
    private String defaultValue;

    @NotNull
    @Size(max = 20)
    @Column(name = "variable_type", length = 20, nullable = false)
    private String variableType;

    @Size(max = 200)
    @Column(name = "description", length = 200)
    private String description;

    @Column(name = "is_system")
    private Boolean isSystem = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flow_id")
    private ChatbotFlow flow;

    // Getters et setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String getVariableType() {
        return variableType;
    }

    public void setVariableType(String variableType) {
        this.variableType = variableType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getIsSystem() {
        return isSystem;
    }

    public void setIsSystem(Boolean isSystem) {
        this.isSystem = isSystem;
    }

    public ChatbotFlow getFlow() {
        return flow;
    }

    public void setFlow(ChatbotFlow flow) {
        this.flow = flow;
    }
}
