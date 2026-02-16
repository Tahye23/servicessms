package com.example.myproject.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.io.Serializable;
import java.time.ZonedDateTime;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@Table(name = "send_whatsapp")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class SendWhatsapp implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @Column(name = "template_id")
    private Long templateId; // ID du template WhatsApp utilisé

    @Column(name = "send_date")
    private ZonedDateTime sendDate;

    @Column(name = "is_sent")
    private Boolean isSent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "extuser", "groupes", "otpusers" }, allowSetters = true)
    private ExtendedUser senderUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "extendedUser", "groupedecontacts" }, allowSetters = true)
    private Groupe recipientGroup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "groupedecontacts" }, allowSetters = true)
    private Contact recipientContact;

    @ManyToOne(fetch = FetchType.LAZY)
    private Referentiel statut;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTemplateId() {
        return templateId;
    }

    public void setTemplateId(Long templateId) {
        this.templateId = templateId;
    }

    public ZonedDateTime getSendDate() {
        return sendDate;
    }

    public void setSendDate(ZonedDateTime sendDate) {
        this.sendDate = sendDate;
    }

    public Boolean getIsSent() {
        return isSent;
    }

    public void setIsSent(Boolean sent) {
        isSent = sent;
    }

    public ExtendedUser getSenderUser() {
        return senderUser;
    }

    public void setSenderUser(ExtendedUser senderUser) {
        this.senderUser = senderUser;
    }

    public Groupe getRecipientGroup() {
        return recipientGroup;
    }

    public void setRecipientGroup(Groupe recipientGroup) {
        this.recipientGroup = recipientGroup;
    }

    public Contact getRecipientContact() {
        return recipientContact;
    }

    public void setRecipientContact(Contact recipientContact) {
        this.recipientContact = recipientContact;
    }

    public Referentiel getStatut() {
        return statut;
    }

    public void setStatut(Referentiel statut) {
        this.statut = statut;
    }
}
