package com.example.myproject.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.io.Serializable;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * A UserService.
 */
@Entity
@Table(name = "user_service")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class UserService implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @Column(name = "ur_s_service")
    private String urSService;

    @Column(name = "ur_s_user")
    private String urSUser;

    @ManyToOne(fetch = FetchType.LAZY)
    private Service service;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "user", "groupes" }, allowSetters = true)
    private ExtendedUser user;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public UserService id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUrSService() {
        return this.urSService;
    }

    public UserService urSService(String urSService) {
        this.setUrSService(urSService);
        return this;
    }

    public void setUrSService(String urSService) {
        this.urSService = urSService;
    }

    public String getUrSUser() {
        return this.urSUser;
    }

    public UserService urSUser(String urSUser) {
        this.setUrSUser(urSUser);
        return this;
    }

    public void setUrSUser(String urSUser) {
        this.urSUser = urSUser;
    }

    public Service getService() {
        return this.service;
    }

    public void setService(Service service) {
        this.service = service;
    }

    public UserService service(Service service) {
        this.setService(service);
        return this;
    }

    public ExtendedUser getUser() {
        return this.user;
    }

    public void setUser(ExtendedUser extendedUser) {
        this.user = extendedUser;
    }

    public UserService user(ExtendedUser extendedUser) {
        this.setUser(extendedUser);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UserService)) {
            return false;
        }
        return getId() != null && getId().equals(((UserService) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "UserService{" +
            "id=" + getId() +
            ", urSService='" + getUrSService() + "'" +
            ", urSUser='" + getUrSUser() + "'" +
            "}";
    }
}
