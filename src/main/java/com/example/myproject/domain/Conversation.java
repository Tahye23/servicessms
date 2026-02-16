package com.example.myproject.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.io.Serializable;
import java.time.ZonedDateTime;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * A Conversation.
 */
@Entity
@Table(name = "conversation")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Conversation implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @Column(name = "covdatedebut")
    private ZonedDateTime covdatedebut;

    @Column(name = "covdatefin")
    private ZonedDateTime covdatefin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "groupedecontacts" }, allowSetters = true)
    private Contact contact;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "qustenquette", "quesquestiontype" }, allowSetters = true)
    private Question question;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "camUser", "camstatus" }, allowSetters = true)
    private Company covenquette;

    @ManyToOne(fetch = FetchType.LAZY)
    private Referentiel covstate;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public Conversation id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ZonedDateTime getCovdatedebut() {
        return this.covdatedebut;
    }

    public Conversation covdatedebut(ZonedDateTime covdatedebut) {
        this.setCovdatedebut(covdatedebut);
        return this;
    }

    public void setCovdatedebut(ZonedDateTime covdatedebut) {
        this.covdatedebut = covdatedebut;
    }

    public ZonedDateTime getCovdatefin() {
        return this.covdatefin;
    }

    public Conversation covdatefin(ZonedDateTime covdatefin) {
        this.setCovdatefin(covdatefin);
        return this;
    }

    public void setCovdatefin(ZonedDateTime covdatefin) {
        this.covdatefin = covdatefin;
    }

    public Contact getContact() {
        return this.contact;
    }

    public void setContact(Contact contact) {
        this.contact = contact;
    }

    public Conversation contact(Contact contact) {
        this.setContact(contact);
        return this;
    }

    public Question getQuestion() {
        return this.question;
    }

    public void setQuestion(Question question) {
        this.question = question;
    }

    public Conversation question(Question question) {
        this.setQuestion(question);
        return this;
    }

    public Company getCovenquette() {
        return this.covenquette;
    }

    public void setCovenquette(Company company) {
        this.covenquette = company;
    }

    public Conversation covenquette(Company company) {
        this.setCovenquette(company);
        return this;
    }

    public Referentiel getCovstate() {
        return this.covstate;
    }

    public void setCovstate(Referentiel referentiel) {
        this.covstate = referentiel;
    }

    public Conversation covstate(Referentiel referentiel) {
        this.setCovstate(referentiel);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Conversation)) {
            return false;
        }
        return getId() != null && getId().equals(((Conversation) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Conversation{" +
            "id=" + getId() +
            ", covdatedebut='" + getCovdatedebut() + "'" +
            ", covdatefin='" + getCovdatefin() + "'" +
            "}";
    }
}
