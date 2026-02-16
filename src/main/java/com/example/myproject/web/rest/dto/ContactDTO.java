package com.example.myproject.web.rest.dto;

import com.example.myproject.domain.Groupe;
import java.util.List;

public class ContactDTO {

    private Long id;
    private String connom;
    private String conprenom;
    private String contelephone;
    private Integer statuttraitement;
    // Liste des groupes sélectionnés (pour l'association multiple)
    private List<Groupe> groupes;
    private String customFields;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getConnom() {
        return connom;
    }

    public void setConnom(String connom) {
        this.connom = connom;
    }

    public String getConprenom() {
        return conprenom;
    }

    public void setConprenom(String conprenom) {
        this.conprenom = conprenom;
    }

    public String getContelephone() {
        return contelephone;
    }

    public void setContelephone(String contelephone) {
        this.contelephone = contelephone;
    }

    public Integer getStatuttraitement() {
        return statuttraitement;
    }

    public void setStatuttraitement(Integer statuttraitement) {
        this.statuttraitement = statuttraitement;
    }

    public String getCustomFields() {
        return customFields;
    }

    public void setCustomFields(String customFields) {
        this.customFields = customFields;
    }

    public List<Groupe> getGroupes() {
        return groupes;
    }

    public void setGroupes(List<Groupe> groupes) {
        this.groupes = groupes;
    }
    // Getters et setters
    // ...
}
