package com.example.myproject.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * A Contact.
 */
@Entity
@Table(name = "contact")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Contact implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @Column(name = "conid")
    private Integer conid;

    @Column(name = "user_login")
    private String user_login;

    @Column(name = "connom")
    private String connom;

    @Column(name = "conprenom")
    private String conprenom;

    @Column(name = "contelephone")
    private String contelephone;

    @Column(name = "statuttraitement")
    private Integer statuttraitement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "extendedUser" }, allowSetters = true)
    private Groupe groupe;

    @Column(name = "custom_fields", columnDefinition = "TEXT")
    private String customFields;

    @Column(name = "progress_id")
    private String progressId;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "contact", cascade = CascadeType.ALL, orphanRemoval = true)
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonIgnoreProperties(value = { "cgrgroupe", "contact" }, allowSetters = true)
    private Set<Groupedecontact> groupedecontacts = new HashSet<>();

    // 🆕 CHAMPS STATISTIQUES DE MESSAGES
    @Column(name = "has_whatsapp")
    private Boolean hasWhatsapp;

    @Column(name = "total_sms_sent")
    private Integer totalSmsSent = 0;

    @Column(name = "total_sms_success")
    private Integer totalSmsSuccess = 0;

    @Column(name = "total_sms_failed")
    private Integer totalSmsFailed = 0;

    @Column(name = "total_whatsapp_sent")
    private Integer totalWhatsappSent = 0;

    @Column(name = "total_whatsapp_success")
    private Integer totalWhatsappSuccess = 0;

    @Column(name = "total_whatsapp_failed")
    private Integer totalWhatsappFailed = 0;

    // Getters et Setters
    public Boolean getHasWhatsapp() {
        return hasWhatsapp;
    }

    public void setHasWhatsapp(Boolean hasWhatsapp) {
        this.hasWhatsapp = hasWhatsapp;
    }

    public Integer getTotalSmsSent() {
        return totalSmsSent;
    }

    public void setTotalSmsSent(Integer totalSmsSent) {
        this.totalSmsSent = totalSmsSent;
    }

    public Integer getTotalSmsSuccess() {
        return totalSmsSuccess;
    }

    public void setTotalSmsSuccess(Integer totalSmsSuccess) {
        this.totalSmsSuccess = totalSmsSuccess;
    }

    public Integer getTotalSmsFailed() {
        return totalSmsFailed;
    }

    public void setTotalSmsFailed(Integer totalSmsFailed) {
        this.totalSmsFailed = totalSmsFailed;
    }

    public Integer getTotalWhatsappSent() {
        return totalWhatsappSent;
    }

    public void setTotalWhatsappSent(Integer totalWhatsappSent) {
        this.totalWhatsappSent = totalWhatsappSent;
    }

    public Integer getTotalWhatsappSuccess() {
        return totalWhatsappSuccess;
    }

    public void setTotalWhatsappSuccess(Integer totalWhatsappSuccess) {
        this.totalWhatsappSuccess = totalWhatsappSuccess;
    }

    public Integer getTotalWhatsappFailed() {
        return totalWhatsappFailed;
    }

    public void setTotalWhatsappFailed(Integer totalWhatsappFailed) {
        this.totalWhatsappFailed = totalWhatsappFailed;
    }

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public Contact id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getConid() {
        return this.conid;
    }

    public Contact conid(Integer conid) {
        this.setConid(conid);
        return this;
    }

    public void setConid(Integer conid) {
        this.conid = conid;
    }

    public String getConnom() {
        return this.connom;
    }

    public Contact connom(String connom) {
        this.setConnom(connom);
        return this;
    }

    public String getProgressId() {
        return progressId;
    }

    public void setProgressId(String progressId) {
        this.progressId = progressId;
    }

    public void setConnom(String connom) {
        this.connom = connom;
    }

    public String getConprenom() {
        return this.conprenom;
    }

    public Contact conprenom(String conprenom) {
        this.setConprenom(conprenom);
        return this;
    }

    public String getCustomFields() {
        return customFields;
    }

    public void setCustomFields(String customFields) {
        this.customFields = customFields;
    }

    public void setConprenom(String conprenom) {
        this.conprenom = conprenom;
    }

    public String getContelephone() {
        return this.contelephone;
    }

    public Contact contelephone(String contelephone) {
        this.setContelephone(contelephone);
        return this;
    }

    public void setContelephone(String contelephone) {
        this.contelephone = contelephone;
    }

    public Integer getStatuttraitement() {
        return this.statuttraitement;
    }

    public Contact statuttraitement(Integer statuttraitement) {
        this.setStatuttraitement(statuttraitement);
        return this;
    }

    public void setStatuttraitement(Integer statuttraitement) {
        this.statuttraitement = statuttraitement;
    }

    public Groupe getGroupe() {
        return this.groupe;
    }

    public void setGroupe(Groupe groupe) {
        this.groupe = groupe;
    }

    public Contact groupe(Groupe groupe) {
        this.setGroupe(groupe);
        return this;
    }

    public Set<Groupedecontact> getGroupedecontacts() {
        return this.groupedecontacts;
    }

    public void setGroupedecontacts(Set<Groupedecontact> groupedecontacts) {
        if (this.groupedecontacts != null) {
            this.groupedecontacts.forEach(i -> i.setContact(null));
        }
        if (groupedecontacts != null) {
            groupedecontacts.forEach(i -> i.setContact(this));
        }
        this.groupedecontacts = groupedecontacts;
    }

    public String getUser_login() {
        return user_login;
    }

    public void setUser_login(String user_login) {
        this.user_login = user_login;
    }

    public Contact groupedecontacts(Set<Groupedecontact> groupedecontacts) {
        this.setGroupedecontacts(groupedecontacts);
        return this;
    }

    public Contact addGroupedecontact(Groupedecontact groupedecontact) {
        this.groupedecontacts.add(groupedecontact);
        groupedecontact.setContact(this);
        return this;
    }

    public Contact removeGroupedecontact(Groupedecontact groupedecontact) {
        this.groupedecontacts.remove(groupedecontact);
        groupedecontact.setContact(null);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Contact)) {
            return false;
        }
        return getId() != null && getId().equals(((Contact) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Contact{" +
            "id=" + getId() +
            ", conid=" + getConid() +
            ", connom='" + getConnom() + "'" +
            ", conprenom='" + getConprenom() + "'" +
            ", contelephone='" + getContelephone() + "'" +
            ", statuttraitement=" + getStatuttraitement() +
            "}";
    }
}
