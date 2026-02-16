package com.example.myproject.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.io.Serializable;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * A Question.
 */
@Entity
@Table(name = "question")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Question implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @Column(name = "qusordre")
    private Integer qusordre;

    @Column(name = "qusmessage")
    private String qusmessage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "camUser", "camstatus" }, allowSetters = true)
    private Company qusenquette;

    @ManyToOne(fetch = FetchType.LAZY)
    private Referentiel qustypequestion;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public Question id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getQusordre() {
        return this.qusordre;
    }

    public Question qusordre(Integer qusordre) {
        this.setQusordre(qusordre);
        return this;
    }

    public void setQusordre(Integer qusordre) {
        this.qusordre = qusordre;
    }

    public String getQusmessage() {
        return this.qusmessage;
    }

    public Question qusmessage(String qusmessage) {
        this.setQusmessage(qusmessage);
        return this;
    }

    public void setQusmessage(String qusmessage) {
        this.qusmessage = qusmessage;
    }

    public Company getQusenquette() {
        return this.qusenquette;
    }

    public void setQusenquette(Company company) {
        this.qusenquette = company;
    }

    public Question qusenquette(Company company) {
        this.setQusenquette(company);
        return this;
    }

    public Referentiel getQustypequestion() {
        return this.qustypequestion;
    }

    public void setQustypequestion(Referentiel referentiel) {
        this.qustypequestion = referentiel;
    }

    public Question qustypequestion(Referentiel referentiel) {
        this.setQustypequestion(referentiel);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Question)) {
            return false;
        }
        return getId() != null && getId().equals(((Question) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Question{" +
            "id=" + getId() +
            ", qusordre=" + getQusordre() +
            ", qusmessage='" + getQusmessage() + "'" +
            "}";
    }
}
