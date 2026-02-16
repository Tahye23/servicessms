package com.example.myproject.domain;

import jakarta.persistence.*;
import java.io.Serializable;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * A Api.
 */
@Entity
@Table(name = "api")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Api implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @Column(name = "api_nom")
    private String apiNom;

    @Column(name = "api_url")
    private String apiUrl;

    @Column(name = "api_version")
    private Integer apiVersion;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public Api id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getApiNom() {
        return this.apiNom;
    }

    public Api apiNom(String apiNom) {
        this.setApiNom(apiNom);
        return this;
    }

    public void setApiNom(String apiNom) {
        this.apiNom = apiNom;
    }

    public String getApiUrl() {
        return this.apiUrl;
    }

    public Api apiUrl(String apiUrl) {
        this.setApiUrl(apiUrl);
        return this;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public Integer getApiVersion() {
        return this.apiVersion;
    }

    public Api apiVersion(Integer apiVersion) {
        this.setApiVersion(apiVersion);
        return this;
    }

    public void setApiVersion(Integer apiVersion) {
        this.apiVersion = apiVersion;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Api)) {
            return false;
        }
        return getId() != null && getId().equals(((Api) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Api{" +
            "id=" + getId() +
            ", apiNom='" + getApiNom() + "'" +
            ", apiUrl='" + getApiUrl() + "'" +
            ", apiVersion=" + getApiVersion() +
            "}";
    }
}
