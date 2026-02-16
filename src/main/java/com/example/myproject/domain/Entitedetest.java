package com.example.myproject.domain;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * A Entitedetest.
 */
@Entity
@Table(name = "entitedetest")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Entitedetest implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @Column(name = "identite")
    private Integer identite;

    @Column(name = "nom")
    private String nom;

    @Column(name = "nombrec")
    private Integer nombrec;

    @Column(name = "chamb")
    private Boolean chamb;

    @Column(name = "champdate")
    private Instant champdate;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public Entitedetest id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getIdentite() {
        return this.identite;
    }

    public Entitedetest identite(Integer identite) {
        this.setIdentite(identite);
        return this;
    }

    public void setIdentite(Integer identite) {
        this.identite = identite;
    }

    public String getNom() {
        return this.nom;
    }

    public Entitedetest nom(String nom) {
        this.setNom(nom);
        return this;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public Integer getNombrec() {
        return this.nombrec;
    }

    public Entitedetest nombrec(Integer nombrec) {
        this.setNombrec(nombrec);
        return this;
    }

    public void setNombrec(Integer nombrec) {
        this.nombrec = nombrec;
    }

    public Boolean getChamb() {
        return this.chamb;
    }

    public Entitedetest chamb(Boolean chamb) {
        this.setChamb(chamb);
        return this;
    }

    public void setChamb(Boolean chamb) {
        this.chamb = chamb;
    }

    public Instant getChampdate() {
        return this.champdate;
    }

    public Entitedetest champdate(Instant champdate) {
        this.setChampdate(champdate);
        return this;
    }

    public void setChampdate(Instant champdate) {
        this.champdate = champdate;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Entitedetest)) {
            return false;
        }
        return getId() != null && getId().equals(((Entitedetest) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Entitedetest{" +
            "id=" + getId() +
            ", identite=" + getIdentite() +
            ", nom='" + getNom() + "'" +
            ", nombrec=" + getNombrec() +
            ", chamb='" + getChamb() + "'" +
            ", champdate='" + getChampdate() + "'" +
            "}";
    }
}
