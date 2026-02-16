package com.example.myproject.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.io.Serializable;
import java.time.ZonedDateTime;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * A OTPStorage.
 */
@Entity
@Table(name = "otpstorage")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class OTPStorage implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @Column(name = "ots_otp")
    private String otsOTP;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "otsdateexpir")
    private ZonedDateTime otsdateexpir;

    @Column(name = "is_otp_used")
    private Boolean isOtpUsed;

    @Column(name = "is_expired")
    private Boolean isExpired;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "extuser", "groupes", "otpusers" }, allowSetters = true)
    private ExtendedUser user;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public OTPStorage id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOtsOTP() {
        return this.otsOTP;
    }

    public OTPStorage otsOTP(String otsOTP) {
        this.setOtsOTP(otsOTP);
        return this;
    }

    public void setOtsOTP(String otsOTP) {
        this.otsOTP = otsOTP;
    }

    public String getPhoneNumber() {
        return this.phoneNumber;
    }

    public OTPStorage phoneNumber(String phoneNumber) {
        this.setPhoneNumber(phoneNumber);
        return this;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public ZonedDateTime getOtsdateexpir() {
        return this.otsdateexpir;
    }

    public OTPStorage otsdateexpir(ZonedDateTime otsdateexpir) {
        this.setOtsdateexpir(otsdateexpir);
        return this;
    }

    public void setOtsdateexpir(ZonedDateTime otsdateexpir) {
        this.otsdateexpir = otsdateexpir;
    }

    public Boolean getIsOtpUsed() {
        return this.isOtpUsed;
    }

    public OTPStorage isOtpUsed(Boolean isOtpUsed) {
        this.setIsOtpUsed(isOtpUsed);
        return this;
    }

    public void setIsOtpUsed(Boolean isOtpUsed) {
        this.isOtpUsed = isOtpUsed;
    }

    public Boolean getIsExpired() {
        return this.isExpired;
    }

    public OTPStorage isExpired(Boolean isExpired) {
        this.setIsExpired(isExpired);
        return this;
    }

    public void setIsExpired(Boolean isExpired) {
        this.isExpired = isExpired;
    }

    public ExtendedUser getUser() {
        return this.user;
    }

    public void setUser(ExtendedUser extendedUser) {
        this.user = extendedUser;
    }

    public OTPStorage user(ExtendedUser extendedUser) {
        this.setUser(extendedUser);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof OTPStorage)) {
            return false;
        }
        return getId() != null && getId().equals(((OTPStorage) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "OTPStorage{" +
            "id=" + getId() +
            ", otsOTP='" + getOtsOTP() + "'" +
            ", phoneNumber='" + getPhoneNumber() + "'" +
            ", otsdateexpir='" + getOtsdateexpir() + "'" +
            ", isOtpUsed='" + getIsOtpUsed() + "'" +
            ", isExpired='" + getIsExpired() + "'" +
            "}";
    }
}
