package com.example.myproject.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.io.Serializable;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * A UserTokenApi.
 */
@Entity
@Table(name = "user_token_api")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class UserTokenApi implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Api api;

    @ManyToOne(fetch = FetchType.LAZY)
    private TokensApp token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "extuser", "groupes", "otpusers" }, allowSetters = true)
    private ExtendedUser user;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public UserTokenApi id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Api getApi() {
        return this.api;
    }

    public void setApi(Api api) {
        this.api = api;
    }

    public UserTokenApi api(Api api) {
        this.setApi(api);
        return this;
    }

    public TokensApp getToken() {
        return this.token;
    }

    public void setToken(TokensApp tokensApp) {
        this.token = tokensApp;
    }

    public UserTokenApi token(TokensApp tokensApp) {
        this.setToken(tokensApp);
        return this;
    }

    public ExtendedUser getUser() {
        return this.user;
    }

    public void setUser(ExtendedUser extendedUser) {
        this.user = extendedUser;
    }

    public UserTokenApi user(ExtendedUser extendedUser) {
        this.setUser(extendedUser);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UserTokenApi)) {
            return false;
        }
        return getId() != null && getId().equals(((UserTokenApi) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "UserTokenApi{" +
            "id=" + getId() +
            "}";
    }
}
