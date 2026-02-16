package com.example.myproject.web.rest.dto.flow;

// ButtonPayload.java - Bouton
public class ButtonPayload {

    private String id;
    private String text;
    private String value;
    private String nextNodeId;
    private String storeInVariable;

    public ButtonPayload() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getNextNodeId() {
        return nextNodeId;
    }

    public void setNextNodeId(String nextNodeId) {
        this.nextNodeId = nextNodeId;
    }

    public String getStoreInVariable() {
        return storeInVariable;
    }

    public void setStoreInVariable(String storeInVariable) {
        this.storeInVariable = storeInVariable;
    }
}
