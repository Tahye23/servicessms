package com.example.myproject.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.io.Serializable;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * A Participant.
 */
@Entity
@Table(name = "participant")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Participant implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "groupedecontacts" }, allowSetters = true)
    private Contact patcontact;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "camUser", "camstatus" }, allowSetters = true)
    private Company patenquette;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public Participant id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Contact getPatcontact() {
        return this.patcontact;
    }

    public void setPatcontact(Contact contact) {
        this.patcontact = contact;
    }

    public Participant patcontact(Contact contact) {
        this.setPatcontact(contact);
        return this;
    }

    public Company getPatenquette() {
        return this.patenquette;
    }

    public void setPatenquette(Company company) {
        this.patenquette = company;
    }

    public Participant patenquette(Company company) {
        this.setPatenquette(company);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Participant)) {
            return false;
        }
        return getId() != null && getId().equals(((Participant) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Participant{" +
            "id=" + getId() +
            "}";
    }
}
