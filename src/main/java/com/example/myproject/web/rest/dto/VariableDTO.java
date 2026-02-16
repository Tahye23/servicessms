package com.example.myproject.web.rest.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

// DTO utilitaire pour stocker vos variables
public class VariableDTO {

    private int ordre;
    private String valeur;
    private String type;

    // Constructeur sans-argument requis par Jackson pour la désérialisation
    public VariableDTO() {}

    @JsonCreator
    public VariableDTO(@JsonProperty("ordre") int ordre, @JsonProperty("valeur") String valeur, @JsonProperty("type") String type) {
        this.ordre = ordre;
        this.valeur = valeur;
        this.type = type;
    }

    public int getOrdre() {
        return ordre;
    }

    public void setOrdre(int ordre) {
        this.ordre = ordre;
    }

    public String getValeur() {
        return valeur;
    }

    public void setValeur(String valeur) {
        this.valeur = valeur;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "VariableDTO{" + "ordre=" + ordre + ", valeur='" + valeur + '\'' + ", type='" + type + '\'' + '}';
    }
}
