package com.example.myproject.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.io.Serializable;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * A Groupedecontact.
 */
@Entity
@Table(name = "groupedecontact")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Groupedecontact implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "extendedUser" }, allowSetters = true)
    private Groupe cgrgroupe;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "groupedecontacts" }, allowSetters = true)
    private Contact contact;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public Groupedecontact id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Groupe getCgrgroupe() {
        return this.cgrgroupe;
    }

    public void setCgrgroupe(Groupe groupe) {
        this.cgrgroupe = groupe;
    }

    public Groupedecontact cgrgroupe(Groupe groupe) {
        this.setCgrgroupe(groupe);
        return this;
    }

    public Contact getContact() {
        return this.contact;
    }

    public void setContact(Contact contact) {
        this.contact = contact;
    }

    public Groupedecontact contact(Contact contact) {
        this.setContact(contact);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Groupedecontact)) {
            return false;
        }
        return getId() != null && getId().equals(((Groupedecontact) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Groupedecontact{" +
            "id=" + getId() +
            "}";
    }
}
