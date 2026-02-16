package com.example.myproject.domain;

import jakarta.persistence.*;
import java.io.Serializable;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * A Service.
 */
@Entity
@Table(name = "service")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Service implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @Column(name = "serv_nom")
    private String servNom;

    @Column(name = "serv_description")
    private String servDescription;

    @Column(name = "nbrusage")
    private Integer nbrusage;

    @Column(name = "montant")
    private Double montant;

    @Column(name = "statut")
    private Boolean statut;

    @ManyToOne(fetch = FetchType.LAZY)
    private Referentiel accessType;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public Service id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getServNom() {
        return this.servNom;
    }

    public Service servNom(String servNom) {
        this.setServNom(servNom);
        return this;
    }

    public void setServNom(String servNom) {
        this.servNom = servNom;
    }

    public String getServDescription() {
        return this.servDescription;
    }

    public Service servDescription(String servDescription) {
        this.setServDescription(servDescription);
        return this;
    }

    public void setServDescription(String servDescription) {
        this.servDescription = servDescription;
    }

    public Integer getNbrusage() {
        return this.nbrusage;
    }

    public Service nbrusage(Integer nbrusage) {
        this.setNbrusage(nbrusage);
        return this;
    }

    public void setNbrusage(Integer nbrusage) {
        this.nbrusage = nbrusage;
    }

    public Double getMontant() {
        return this.montant;
    }

    public Service montant(Double montant) {
        this.setMontant(montant);
        return this;
    }

    public void setMontant(Double montant) {
        this.montant = montant;
    }

    public Boolean getStatut() {
        return this.statut;
    }

    public Service statut(Boolean statut) {
        this.setStatut(statut);
        return this;
    }

    public void setStatut(Boolean statut) {
        this.statut = statut;
    }

    public Referentiel getAccessType() {
        return this.accessType;
    }

    public void setAccessType(Referentiel referentiel) {
        this.accessType = referentiel;
    }

    public Service accessType(Referentiel referentiel) {
        this.setAccessType(referentiel);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Service)) {
            return false;
        }
        return getId() != null && getId().equals(((Service) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Service{" +
            "id=" + getId() +
            ", servNom='" + getServNom() + "'" +
            ", servDescription='" + getServDescription() + "'" +
            ", nbrusage=" + getNbrusage() +
            ", montant=" + getMontant() +
            ", statut='" + getStatut() + "'" +
            "}";
    }
}
