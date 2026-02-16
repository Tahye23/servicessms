package com.example.myproject.service.dto.flow;

// ================================
// CORRECTION DE LA CLASSE ButtonOption
// ================================

import java.util.Map;

public class ButtonOption {

    private String id;
    private String text;
    private String value; // <-- AJOUTER CE CHAMP
    private String action;
    private String nextNodeId;
    private Map<String, Object> payload;
    private String style; // primary, secondary

    public ButtonOption() {}

    // Constructeur avec paramètres principaux
    public ButtonOption(String id, String text, String value) {
        this.id = id;
        this.text = text;
        this.value = value;
    }

    // ================================
    // GETTERS ET SETTERS
    // ================================

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

    // AJOUTER ce getter/setter pour 'value' :
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getNextNodeId() {
        return nextNodeId;
    }

    public void setNextNodeId(String nextNodeId) {
        this.nextNodeId = nextNodeId;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }

    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    // ================================
    // MÉTHODES UTILITAIRES
    // ================================

    @Override
    public String toString() {
        return String.format("ButtonOption{id='%s', text='%s', value='%s'}", id, text, value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ButtonOption that = (ButtonOption) o;
        return java.util.Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(id);
    }
}
