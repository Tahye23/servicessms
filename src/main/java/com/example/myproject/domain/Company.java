package com.example.myproject.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * A Company.
 */
@Entity
@Table(name = "company")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Company implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @Column(name = "name")
    private String name;

    @Column(name = "activity")
    private String activity;

    @Column(name = "camtitre")
    private String camtitre;

    @Column(name = "camdatecreation")
    private LocalDate camdatecreation;

    @Column(name = "camdatefin")
    private ZonedDateTime camdatefin;

    @Column(name = "camispub")
    private Boolean camispub;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "extuser", "groupes", "otpusers" }, allowSetters = true)
    private ExtendedUser camUser;

    @ManyToOne(fetch = FetchType.LAZY)
    private Referentiel camstatus;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Integer getId() {
        return this.id;
    }

    public Company id(Integer id) {
        this.setId(id);
        return this;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public Company name(String name) {
        this.setName(name);
        return this;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getActivity() {
        return this.activity;
    }

    public Company activity(String activity) {
        this.setActivity(activity);
        return this;
    }

    public void setActivity(String activity) {
        this.activity = activity;
    }

    public String getCamtitre() {
        return this.camtitre;
    }

    public Company camtitre(String camtitre) {
        this.setCamtitre(camtitre);
        return this;
    }

    public void setCamtitre(String camtitre) {
        this.camtitre = camtitre;
    }

    public LocalDate getCamdatecreation() {
        return this.camdatecreation;
    }

    public Company camdatecreation(LocalDate camdatecreation) {
        this.setCamdatecreation(camdatecreation);
        return this;
    }

    public void setCamdatecreation(LocalDate camdatecreation) {
        this.camdatecreation = camdatecreation;
    }

    public ZonedDateTime getCamdatefin() {
        return this.camdatefin;
    }

    public Company camdatefin(ZonedDateTime camdatefin) {
        this.setCamdatefin(camdatefin);
        return this;
    }

    public void setCamdatefin(ZonedDateTime camdatefin) {
        this.camdatefin = camdatefin;
    }

    public Boolean getCamispub() {
        return this.camispub;
    }

    public Company camispub(Boolean camispub) {
        this.setCamispub(camispub);
        return this;
    }

    public void setCamispub(Boolean camispub) {
        this.camispub = camispub;
    }

    public ExtendedUser getCamUser() {
        return this.camUser;
    }

    public void setCamUser(ExtendedUser extendedUser) {
        this.camUser = extendedUser;
    }

    public Company camUser(ExtendedUser extendedUser) {
        this.setCamUser(extendedUser);
        return this;
    }

    public Referentiel getCamstatus() {
        return this.camstatus;
    }

    public void setCamstatus(Referentiel referentiel) {
        this.camstatus = referentiel;
    }

    public Company camstatus(Referentiel referentiel) {
        this.setCamstatus(referentiel);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Company)) {
            return false;
        }
        return getId() != null && getId().equals(((Company) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Company{" +
            "id=" + getId() +
            ", name='" + getName() + "'" +
            ", activity='" + getActivity() + "'" +
            ", camtitre='" + getCamtitre() + "'" +
            ", camdatecreation='" + getCamdatecreation() + "'" +
            ", camdatefin='" + getCamdatefin() + "'" +
            ", camispub='" + getCamispub() + "'" +
            "}";
    }
}
