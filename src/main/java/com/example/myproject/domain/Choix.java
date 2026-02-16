package com.example.myproject.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.io.Serializable;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * A Choix.
 */
@Entity
@Table(name = "choix")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Choix implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @Column(name = "chovaleur")
    private String chovaleur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "qustenquette", "quesquestiontype" }, allowSetters = true)
    private Question choquestion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "qustenquette", "quesquestiontype" }, allowSetters = true)
    private Question choquestionSuivante;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public Choix id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getChovaleur() {
        return this.chovaleur;
    }

    public Choix chovaleur(String chovaleur) {
        this.setChovaleur(chovaleur);
        return this;
    }

    public void setChovaleur(String chovaleur) {
        this.chovaleur = chovaleur;
    }

    public Question getChoquestion() {
        return this.choquestion;
    }

    public void setChoquestion(Question question) {
        this.choquestion = question;
    }

    public Choix choquestion(Question question) {
        this.setChoquestion(question);
        return this;
    }

    public Question getChoquestionSuivante() {
        return this.choquestionSuivante;
    }

    public void setChoquestionSuivante(Question question) {
        this.choquestionSuivante = question;
    }

    public Choix choquestionSuivante(Question question) {
        this.setChoquestionSuivante(question);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Choix)) {
            return false;
        }
        return getId() != null && getId().equals(((Choix) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Choix{" +
            "id=" + getId() +
            ", chovaleur='" + getChovaleur() + "'" +
            "}";
    }
}
