package com.example.myproject.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.io.Serializable;
import java.time.ZonedDateTime;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@Table(name = "tokens_app")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class TokensApp implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @Column(name = "date_expiration")
    private ZonedDateTime dateExpiration;

    @Column(name = "token", length = 1000) // Augmenter la taille pour les JWT
    private String token;

    @Column(name = "is_expired")
    private Boolean isExpired;

    @Column(name = "active", nullable = false)
    private Boolean active = true; // Par défaut actif

    @Column(name = "created_at")
    private ZonedDateTime createdAt;

    @Column(name = "last_used_at")
    private ZonedDateTime lastUsedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    private Application application;

    @Column(name = "user_login", length = 50)
    private String userLogin;

    // Getter et Setter
    public String getUserLogin() {
        return this.userLogin;
    }

    public TokensApp userLogin(String userLogin) {
        this.setUserLogin(userLogin);
        return this;
    }

    public void setUserLogin(String userLogin) {
        this.userLogin = userLogin;
    }

    // Constructeurs
    public TokensApp() {
        this.active = true;
        this.isExpired = false;
        this.createdAt = ZonedDateTime.now();
    }

    // Getters et Setters
    public Integer getId() {
        return this.id;
    }

    public TokensApp id(Integer id) {
        this.setId(id);
        return this;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public ZonedDateTime getDateExpiration() {
        return this.dateExpiration;
    }

    public TokensApp dateExpiration(ZonedDateTime dateExpiration) {
        this.setDateExpiration(dateExpiration);
        return this;
    }

    public void setDateExpiration(ZonedDateTime dateExpiration) {
        this.dateExpiration = dateExpiration;
        // Vérifier si le token est expiré
        if (dateExpiration != null) {
            this.isExpired = ZonedDateTime.now().isAfter(dateExpiration);
        }
    }

    public String getToken() {
        return this.token;
    }

    public TokensApp token(String token) {
        this.setToken(token);
        return this;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Boolean getActive() {
        return this.active;
    }

    public TokensApp active(Boolean active) {
        this.setActive(active);
        return this;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Application getApplication() {
        return this.application;
    }

    public void setApplication(Application application) {
        this.application = application;
    }

    public TokensApp application(Application application) {
        this.setApplication(application);
        return this;
    }

    public Boolean getIsExpired() {
        return this.isExpired;
    }

    public TokensApp isExpired(Boolean isExpired) {
        this.setIsExpired(isExpired);
        return this;
    }

    public void setIsExpired(Boolean isExpired) {
        this.isExpired = isExpired;
    }

    public ZonedDateTime getCreatedAt() {
        return this.createdAt;
    }

    public TokensApp createdAt(ZonedDateTime createdAt) {
        this.setCreatedAt(createdAt);
        return this;
    }

    public void setCreatedAt(ZonedDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public ZonedDateTime getLastUsedAt() {
        return this.lastUsedAt;
    }

    public TokensApp lastUsedAt(ZonedDateTime lastUsedAt) {
        this.setLastUsedAt(lastUsedAt);
        return this;
    }

    public void setLastUsedAt(ZonedDateTime lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }

    // Méthodes utilitaires
    public boolean isExpiredNow() {
        //  Si dateExpiration est null, le token n'expire jamais
        if (dateExpiration == null) {
            return false;
        }
        return ZonedDateTime.now().isAfter(dateExpiration);
    }

    public boolean isValid() {
        return Boolean.TRUE.equals(active) && !isExpiredNow();
    }

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = ZonedDateTime.now();
        }
        if (this.active == null) {
            this.active = true;
        }
        if (this.isExpired == null) {
            this.isExpired = false;
        }
    }

    @PreUpdate
    public void preUpdate() {
        // Mettre à jour automatiquement le statut d'expiration
        if (this.dateExpiration != null) {
            this.isExpired = ZonedDateTime.now().isAfter(this.dateExpiration);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TokensApp)) {
            return false;
        }
        return getId() != null && getId().equals(((TokensApp) o).getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return (
            "TokensApp{" +
            "id=" +
            getId() +
            ", dateExpiration='" +
            getDateExpiration() +
            "'" +
            ", token='" +
            (getToken() != null ? "***MASKED***" : null) +
            "'" +
            ", isExpired='" +
            getIsExpired() +
            "'" +
            ", active='" +
            getActive() +
            "'" +
            ", createdAt='" +
            getCreatedAt() +
            "'" +
            ", lastUsedAt='" +
            getLastUsedAt() +
            "'" +
            "}"
        );
    }
}
