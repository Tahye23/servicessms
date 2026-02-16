package com.example.myproject.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.io.Serializable;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * A Groupe.
 */
@Entity
@Table(name = "groupe")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Groupe implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @Column(name = "grotitre")
    private String grotitre;

    @Column(name = "jhi_user")
    private String user;

    @Column(name = "user_id")
    private String user_id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "extuser", "groupes" }, allowSetters = true)
    private ExtendedUser extendedUser;

    @Column(name = "group_type")
    private String groupType;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public Groupe id(Long id) {
        this.setId(id);
        return this;
    }

    // Getter
    public String getGroupType() {
        return groupType;
    }

    // Setter
    public void setGroupType(String groupType) {
        this.groupType = groupType;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getGrotitre() {
        return this.grotitre;
    }

    public Groupe grotitre(String grotitre) {
        this.setGrotitre(grotitre);
        return this;
    }

    public void setGrotitre(String grotitre) {
        this.grotitre = grotitre;
    }

    public String getUser() {
        return this.user;
    }

    public Groupe user(String user) {
        this.setUser(user);
        return this;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public ExtendedUser getExtendedUser() {
        return this.extendedUser;
    }

    public void setExtendedUser(ExtendedUser extendedUser) {
        this.extendedUser = extendedUser;
    }

    public Groupe extendedUser(ExtendedUser extendedUser) {
        this.setExtendedUser(extendedUser);
        return this;
    }

    public String getUser_id() {
        return user_id;
    }

    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Groupe)) {
            return false;
        }
        return getId() != null && getId().equals(((Groupe) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Groupe{" +
            "id=" + getId() +
            ", grotitre='" + getGrotitre() + "'" +
            ", user='" + getUser() + "'" +
            "}";
    }
}
