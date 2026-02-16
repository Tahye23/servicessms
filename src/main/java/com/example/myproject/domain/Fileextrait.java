package com.example.myproject.domain;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.UUID;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * A Fileextrait.
 */
@Entity
@Table(name = "fileextrait")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Fileextrait implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @Column(name = "fexidfile")
    private UUID fexidfile;

    @Column(name = "fexparent")
    private String fexparent;

    @Lob
    @Column(name = "fexdata")
    private byte[] fexdata;

    @Column(name = "fexdata_content_type")
    private String fexdataContentType;

    @Column(name = "fextype")
    private String fextype;

    @Column(name = "fexname")
    private String fexname;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public Fileextrait id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UUID getFexidfile() {
        return this.fexidfile;
    }

    public Fileextrait fexidfile(UUID fexidfile) {
        this.setFexidfile(fexidfile);
        return this;
    }

    public void setFexidfile(UUID fexidfile) {
        this.fexidfile = fexidfile;
    }

    public String getFexparent() {
        return this.fexparent;
    }

    public Fileextrait fexparent(String fexparent) {
        this.setFexparent(fexparent);
        return this;
    }

    public void setFexparent(String fexparent) {
        this.fexparent = fexparent;
    }

    public byte[] getFexdata() {
        return this.fexdata;
    }

    public Fileextrait fexdata(byte[] fexdata) {
        this.setFexdata(fexdata);
        return this;
    }

    public void setFexdata(byte[] fexdata) {
        this.fexdata = fexdata;
    }

    public String getFexdataContentType() {
        return this.fexdataContentType;
    }

    public Fileextrait fexdataContentType(String fexdataContentType) {
        this.fexdataContentType = fexdataContentType;
        return this;
    }

    public void setFexdataContentType(String fexdataContentType) {
        this.fexdataContentType = fexdataContentType;
    }

    public String getFextype() {
        return this.fextype;
    }

    public Fileextrait fextype(String fextype) {
        this.setFextype(fextype);
        return this;
    }

    public void setFextype(String fextype) {
        this.fextype = fextype;
    }

    public String getFexname() {
        return this.fexname;
    }

    public Fileextrait fexname(String fexname) {
        this.setFexname(fexname);
        return this;
    }

    public void setFexname(String fexname) {
        this.fexname = fexname;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Fileextrait)) {
            return false;
        }
        return getId() != null && getId().equals(((Fileextrait) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Fileextrait{" +
            "id=" + getId() +
            ", fexidfile='" + getFexidfile() + "'" +
            ", fexparent='" + getFexparent() + "'" +
            ", fexdata='" + getFexdata() + "'" +
            ", fexdataContentType='" + getFexdataContentType() + "'" +
            ", fextype='" + getFextype() + "'" +
            ", fexname='" + getFexname() + "'" +
            "}";
    }
}
