package com.example.myproject.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;

@Entity
@Table(name = "template")
public class Template implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "name", nullable = false)
    private String name;

    @NotNull
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "expediteur")
    private String expediteur;

    @Column(name = "created_at")
    private String created_at;

    @Column(name = "approved_at")
    private String approved_at;

    @Column(name = "status")
    private String status;

    @Column(name = "character_count")
    private Integer characterCount;

    @Column(name = "approved", nullable = false)
    private Boolean approved;

    @Column(name = "user_id")
    private String user_id;

    @Column(name = "template_id")
    private String templateId;

    @Column(name = "code", nullable = false, columnDefinition = "TEXT")
    private String code;

    @Column(name = "template_name")
    private String templateName;

    // Getters & Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public Template name(String name) {
        this.name = name;
        return this;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContent() {
        return content;
    }

    public Template content(String content) {
        this.content = content;
        return this;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getExpediteur() {
        return expediteur;
    }

    public Template expediteur(String expediteur) {
        this.expediteur = expediteur;
        return this;
    }

    public Integer getCharacterCount() {
        return characterCount;
    }

    public void setCharacterCount(Integer characterCount) {
        this.characterCount = characterCount;
    }

    public void setExpediteur(String expediteur) {
        this.expediteur = expediteur;
    }

    public Boolean getApproved() {
        return approved;
    }

    public Template approved(Boolean approved) {
        this.approved = approved;
        return this;
    }

    public void setApproved(Boolean approved) {
        this.approved = approved;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getUser_id() {
        return user_id;
    }

    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }

    public String getApproved_at() {
        return approved_at;
    }

    public void setApproved_at(String approved_at) {
        this.approved_at = approved_at;
    }

    public String getCreated_at() {
        return created_at;
    }

    public void setCreated_at(String created_at) {
        this.created_at = created_at;
    }

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }
}
