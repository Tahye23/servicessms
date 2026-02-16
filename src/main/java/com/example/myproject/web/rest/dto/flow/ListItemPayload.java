package com.example.myproject.web.rest.dto.flow;

// ListItemPayload.java - Item de liste
public class ListItemPayload {

    private String id;
    private String title;
    private String description;
    private String value;
    private String nextNodeId;
    private String storeInVariable;

    public ListItemPayload() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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
