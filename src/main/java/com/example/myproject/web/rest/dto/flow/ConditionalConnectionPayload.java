package com.example.myproject.web.rest.dto.flow;

// ConditionalConnectionPayload.java - Connexion conditionnelle
public class ConditionalConnectionPayload {

    private String id;
    private String condition;
    private String nextNodeId;
    private String label;
    private String operator;
    // Nouveau champ pour expressions custom
    private String customExpression;
    private String expressionDescription;

    // Getters et setters
    public String getCustomExpression() {
        return customExpression;
    }

    public void setCustomExpression(String customExpression) {
        this.customExpression = customExpression;
    }

    public String getExpressionDescription() {
        return expressionDescription;
    }

    public void setExpressionDescription(String expressionDescription) {
        this.expressionDescription = expressionDescription;
    }

    public ConditionalConnectionPayload() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public String getNextNodeId() {
        return nextNodeId;
    }

    public void setNextNodeId(String nextNodeId) {
        this.nextNodeId = nextNodeId;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }
}
