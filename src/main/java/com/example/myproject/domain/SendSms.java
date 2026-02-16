package com.example.myproject.domain;

import com.example.myproject.domain.enumeration.MessageType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * A SendSms.
 */
@Entity
@Table(name = "send_sms")
public class SendSms implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @Column(name = "sender")
    private String sender;

    @Column(name = "namereceiver")
    private String namereceiver;

    @Column(name = "message_id")
    private String messageId;

    @Column(name = "receiver")
    private String receiver;

    @Column(name = "msgdata", nullable = false, columnDefinition = "TEXT")
    private String msgdata;

    @Column(name = "vars", columnDefinition = "TEXT")
    private String vars;

    @Column(name = "sendate_envoi")
    private ZonedDateTime sendateEnvoi;

    @Column(name = "reaction_counters", columnDefinition = "TEXT")
    private String reactionCounters;

    @Column(name = "dialogue")
    private String dialogue;

    @Column(name = "is_sent")
    private Boolean isSent;

    @Column(name = "isbulk")
    private Boolean isbulk;

    @Column(name = "inprocess")
    private Boolean inprocess;

    @Column(name = "character_count")
    private Integer characterCount;

    @Column(name = "delivery_status")
    private String deliveryStatus;

    @Column(name = "titre")
    private String titre;

    @Column(name = "total_message")
    private Integer totalMessage;

    @Column(name = "template_id")
    private Long template_id;

    // Dans votre entité SendSms, ajouter seulement ces champs :
    @Column(name = "retry_count")
    private Integer retryCount = 0;

    @Column(name = "last_retry_date")
    private Instant lastRetryDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "extuser", "groupes", "otpusers" }, allowSetters = true)
    private ExtendedUser user;

    @Column(name = "bulk_id")
    private String bulkId; // Identifiant unique pour chaque traitement

    @Column(name = "bulk_status")
    private String bulkStatus; // Enum: PENDING, SENT, FAILED

    @Column(name = "bulk_created_at")
    private Instant bulkCreatedAt;

    @Column(name = "total_recipients", nullable = false)
    private Integer totalRecipients;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private MessageType type;

    @Column(name = "grotitre")
    private String grotitre;

    @ManyToOne(fetch = FetchType.EAGER)
    @JsonIgnoreProperties(value = { "groupedecontacts" }, allowSetters = true)
    private Contact destinateur;

    @Column(name = "connom")
    private String connom;

    @Column(name = "total_success")
    private Integer totalSuccess;

    @Column(name = "total_failure")
    private Integer totalFailure;

    @Column(name = "success_rate")
    private Double successRate;

    @Column(name = "failure_rate")
    private Double failureRate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "extendedUser", "groupedecontacts" }, allowSetters = true)
    private Groupe destinataires;

    @Column(name = "total_delivered")
    private Integer totalDelivered = 0;

    @Column(name = "total_sent")
    private Integer totalSent = 0;

    @Column(name = "total_failed")
    private Integer totalFailed = 0;

    @Column(name = "total_pending")
    private Integer totalPending = 0;

    @Column(name = "total_read")
    private Integer totalRead = 0;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String last_error;

    @ManyToOne(fetch = FetchType.LAZY)
    private Referentiel statut;

    @OneToMany(mappedBy = "batch", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "batch" }, allowSetters = true)
    private List<Sms> smsList = new ArrayList<>();

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public SendSms id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSender() {
        return this.sender;
    }

    public SendSms sender(String sender) {
        this.setSender(sender);
        return this;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getReceiver() {
        return this.receiver;
    }

    public SendSms receiver(String receiver) {
        this.setReceiver(receiver);
        return this;
    }

    public String getLast_error() {
        return last_error;
    }

    public void setLast_error(String last_error) {
        this.last_error = last_error;
    }

    public Boolean getInprocess() {
        return inprocess;
    }

    public void setInprocess(Boolean inprocess) {
        this.inprocess = inprocess;
    }

    public Integer getTotalRecipients() {
        return totalRecipients;
    }

    public void setTotalRecipients(Integer totalRecipients) {
        this.totalRecipients = totalRecipients;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public String getMsgdata() {
        return this.msgdata;
    }

    public SendSms msgdata(String msgdata) {
        this.setMsgdata(msgdata);
        return this;
    }

    public String getNamereceiver() {
        return namereceiver;
    }

    public void setNamereceiver(String namereceiver) {
        this.namereceiver = namereceiver;
    }

    public String getVars() {
        return vars;
    }

    public void setVars(String vars) {
        this.vars = vars;
    }

    public Integer getCharacterCount() {
        return characterCount;
    }

    public void setCharacterCount(Integer characterCount) {
        this.characterCount = characterCount;
    }

    public void setMsgdata(String msgdata) {
        this.msgdata = msgdata;
    }

    public ZonedDateTime getSendateEnvoi() {
        return this.sendateEnvoi;
    }

    public SendSms sendateEnvoi(ZonedDateTime sendateEnvoi) {
        this.setSendateEnvoi(sendateEnvoi);
        return this;
    }

    public String getDeliveryStatus() {
        return deliveryStatus;
    }

    public void setDeliveryStatus(String deliveryStatus) {
        this.deliveryStatus = deliveryStatus;
    }

    public void setSendateEnvoi(ZonedDateTime sendateEnvoi) {
        this.sendateEnvoi = sendateEnvoi;
    }

    public String getDialogue() {
        return this.dialogue;
    }

    public SendSms dialogue(String dialogue) {
        this.setDialogue(dialogue);
        return this;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    // Getters et setters
    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public Instant getLastRetryDate() {
        return lastRetryDate;
    }

    public void setLastRetryDate(Instant lastRetryDate) {
        this.lastRetryDate = lastRetryDate;
    }

    public String getReactionCounters() {
        return reactionCounters;
    }

    public void setReactionCounters(String reactionCounters) {
        this.reactionCounters = reactionCounters;
    }

    public void setDialogue(String dialogue) {
        this.dialogue = dialogue;
    }

    public Boolean getIsSent() {
        return this.isSent;
    }

    public Integer getTotalMessage() {
        return totalMessage;
    }

    public void setTotalMessage(Integer totalMessage) {
        this.totalMessage = totalMessage;
    }

    public void setTemplate_id(Long template_id) {
        this.template_id = template_id;
    }

    public String getGrotitre() {
        return grotitre;
    }

    public void setGrotitre(String grotitre) {
        this.grotitre = grotitre;
    }

    public String getConnom() {
        return connom;
    }

    public void setConnom(String connom) {
        this.connom = connom;
    }

    public String getBulkId() {
        return bulkId;
    }

    public void setBulkId(String bulkId) {
        this.bulkId = bulkId;
    }

    public String getBulkStatus() {
        return bulkStatus;
    }

    public void setBulkStatus(String bulkStatus) {
        this.bulkStatus = bulkStatus;
    }

    public Instant getBulkCreatedAt() {
        return bulkCreatedAt;
    }

    public void setBulkCreatedAt(Instant bulkCreatedAt) {
        this.bulkCreatedAt = bulkCreatedAt;
    }

    public SendSms isSent(Boolean isSent) {
        this.setIsSent(isSent);
        return this;
    }

    public Boolean getSent() {
        return isSent;
    }

    public void setSent(Boolean sent) {
        isSent = sent;
    }

    public Long getTemplate_id() {
        return template_id;
    }

    public void setTemlate_id(Long temlate_id) {
        this.template_id = temlate_id;
    }

    public void setIsSent(Boolean isSent) {
        this.isSent = isSent;
    }

    public Boolean getIsbulk() {
        return this.isbulk;
    }

    public SendSms isbulk(Boolean isbulk) {
        this.setIsbulk(isbulk);
        return this;
    }

    public void setIsbulk(Boolean isbulk) {
        this.isbulk = isbulk;
    }

    public String getTitre() {
        return this.titre;
    }

    public SendSms Titre(String Titre) {
        this.setTitre(Titre);
        return this;
    }

    public void setTitre(String Titre) {
        this.titre = Titre;
    }

    public ExtendedUser getUser() {
        return this.user;
    }

    public void setUser(ExtendedUser extendedUser) {
        this.user = extendedUser;
    }

    public SendSms user(ExtendedUser extendedUser) {
        this.setUser(extendedUser);
        return this;
    }

    public Contact getDestinateur() {
        return this.destinateur;
    }

    public void setDestinateur(Contact contact) {
        this.destinateur = contact;
    }

    public SendSms destinateur(Contact contact) {
        this.setDestinateur(contact);
        return this;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public Groupe getDestinataires() {
        return this.destinataires;
    }

    public void setDestinataires(Groupe groupe) {
        this.destinataires = groupe;
    }

    public SendSms destinataires(Groupe groupe) {
        this.setDestinataires(groupe);
        return this;
    }

    public Referentiel getStatut() {
        return this.statut;
    }

    public void setStatut(Referentiel referentiel) {
        this.statut = referentiel;
    }

    public SendSms statut(Referentiel referentiel) {
        this.setStatut(referentiel);
        return this;
    }

    public Integer getTotalSuccess() {
        return totalSuccess;
    }

    public void setTotalSuccess(Integer totalSuccess) {
        this.totalSuccess = totalSuccess;
    }

    public Integer getTotalFailure() {
        return totalFailure;
    }

    public void setTotalFailure(Integer totalFailure) {
        this.totalFailure = totalFailure;
    }

    public Double getSuccessRate() {
        return successRate;
    }

    public void setSuccessRate(Double successRate) {
        this.successRate = successRate;
    }

    public Double getFailureRate() {
        return failureRate;
    }

    public void setFailureRate(Double failureRate) {
        this.failureRate = failureRate;
    }

    public List<Sms> getSmsList() {
        return smsList;
    }

    public void setSmsList(List<Sms> smsList) {
        this.smsList = smsList;
    }

    public Integer getTotalDelivered() {
        return totalDelivered;
    }

    public void setTotalDelivered(Integer totalDelivered) {
        this.totalDelivered = totalDelivered;
    }

    public Integer getTotalSent() {
        return totalSent;
    }

    public void setTotalSent(Integer totalSent) {
        this.totalSent = totalSent;
    }

    public Integer getTotalFailed() {
        return totalFailed;
    }

    public void setTotalFailed(Integer totalFailed) {
        this.totalFailed = totalFailed;
    }

    public Integer getTotalPending() {
        return totalPending;
    }

    public void setTotalPending(Integer totalPending) {
        this.totalPending = totalPending;
    }

    public Integer getTotalRead() {
        return totalRead;
    }

    public void setTotalRead(Integer totalRead) {
        this.totalRead = totalRead;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SendSms)) {
            return false;
        }
        return getId() != null && getId().equals(((SendSms) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "SendSms{" +
            "id=" + getId() +
            ", sender='" + getSender() + "'" +
            ", receiver='" + getReceiver() + "'" +
            ", msgdata='" + getMsgdata() + "'" +
            ", sendateEnvoi='" + getSendateEnvoi() + "'" +
            ", dialogue='" + getDialogue() + "'" +
            ", isSent='" + getIsSent() + "'" +
            ", isbulk='" + getIsbulk() + "'" +
            ", Titre='" + getTitre() + "'" +
            "}";
    }
}
