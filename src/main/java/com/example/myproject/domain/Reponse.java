package com.example.myproject.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.io.Serializable;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * A Reponse.
 */
@Entity
@Table(name = "reponse")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Reponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @Column(name = "repvaleur")
    private String repvaleur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "qusenquette", "qustypequestion" }, allowSetters = true)
    private Question repquestion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "groupedecontacts" }, allowSetters = true)
    private Contact repcontact;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public Reponse id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRepvaleur() {
        return this.repvaleur;
    }

    public Reponse repvaleur(String repvaleur) {
        this.setRepvaleur(repvaleur);
        return this;
    }

    public void setRepvaleur(String repvaleur) {
        this.repvaleur = repvaleur;
    }

    public Question getRepquestion() {
        return this.repquestion;
    }

    public void setRepquestion(Question question) {
        this.repquestion = question;
    }

    public Reponse repquestion(Question question) {
        this.setRepquestion(question);
        return this;
    }

    public Contact getRepcontact() {
        return this.repcontact;
    }

    public void setRepcontact(Contact contact) {
        this.repcontact = contact;
    }

    public Reponse repcontact(Contact contact) {
        this.setRepcontact(contact);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Reponse)) {
            return false;
        }
        return getId() != null && getId().equals(((Reponse) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Reponse{" +
            "id=" + getId() +
            ", repvaleur='" + getRepvaleur() + "'" +
            "}";
    }
}
