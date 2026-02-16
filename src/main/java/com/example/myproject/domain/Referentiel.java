package com.example.myproject.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * A Referentiel.
 */
@Entity
@Table(name = "referentiel")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Referentiel implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @NotNull
    @Column(name = "ref_code", nullable = false)
    private String refCode;

    @NotNull
    @Column(name = "ref_radical", nullable = false)
    private String refRadical;

    @Column(name = "ref_fr_title")
    private String refFrTitle;

    @Column(name = "ref_ar_title")
    private String refArTitle;

    @Column(name = "ref_en_title")
    private String refEnTitle;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public Referentiel id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRefCode() {
        return this.refCode;
    }

    public Referentiel refCode(String refCode) {
        this.setRefCode(refCode);
        return this;
    }

    public void setRefCode(String refCode) {
        this.refCode = refCode;
    }

    public String getRefRadical() {
        return this.refRadical;
    }

    public Referentiel refRadical(String refRadical) {
        this.setRefRadical(refRadical);
        return this;
    }

    public void setRefRadical(String refRadical) {
        this.refRadical = refRadical;
    }

    public String getRefFrTitle() {
        return this.refFrTitle;
    }

    public Referentiel refFrTitle(String refFrTitle) {
        this.setRefFrTitle(refFrTitle);
        return this;
    }

    public void setRefFrTitle(String refFrTitle) {
        this.refFrTitle = refFrTitle;
    }

    public String getRefArTitle() {
        return this.refArTitle;
    }

    public Referentiel refArTitle(String refArTitle) {
        this.setRefArTitle(refArTitle);
        return this;
    }

    public void setRefArTitle(String refArTitle) {
        this.refArTitle = refArTitle;
    }

    public String getRefEnTitle() {
        return this.refEnTitle;
    }

    public Referentiel refEnTitle(String refEnTitle) {
        this.setRefEnTitle(refEnTitle);
        return this;
    }

    public void setRefEnTitle(String refEnTitle) {
        this.refEnTitle = refEnTitle;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Referentiel)) {
            return false;
        }
        return getId() != null && getId().equals(((Referentiel) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Referentiel{" +
            "id=" + getId() +
            ", refCode='" + getRefCode() + "'" +
            ", refRadical='" + getRefRadical() + "'" +
            ", refFrTitle='" + getRefFrTitle() + "'" +
            ", refArTitle='" + getRefArTitle() + "'" +
            ", refEnTitle='" + getRefEnTitle() + "'" +
            "}";
    }
}
