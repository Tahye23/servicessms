package com.example.myproject.domain;

import com.example.myproject.domain.enumeration.ContentType;
import com.example.myproject.domain.enumeration.Direction;
import com.example.myproject.domain.enumeration.MessageType;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "sms")
public class Sms {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "send_sms_id")
    private SendSms batch;

    @Column(name = "message_id", unique = true, length = 255)
    private String messageId;

    @Column(name = "template_id")
    private Long template_id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_id")
    private Chat chat;

    @Column(name = "user_login")
    private String user_login;

    @Column(name = "delivery_status")
    private String deliveryStatus;

    @Column(name = "sender")
    private String sender;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", length = 20, nullable = false)
    private ContentType contentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private MessageType type;

    @Column(name = "receiver")
    private String receiver;

    @Column(name = "msgdata", nullable = false, columnDefinition = "TEXT")
    private String msgdata;

    @Column(name = "total_message")
    private Integer totalMessage;

    @Column(name = "is_sent")
    private Boolean isSent;

    @Column(name = "send_date")
    private Instant sendDate;

    @Column(name = "status")
    private String status;

    @Column(name = "bulk_id")
    private String bulkId;

    @Column(name = "namereceiver")
    private String namereceiver;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String last_error;

    @Column(name = "bulk_created_at")
    private Instant bulkCreatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", length = 10)
    private Direction direction;

    @Column(name = "vars", columnDefinition = "TEXT")
    private String vars;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getVars() {
        return vars;
    }

    public void setVars(String vars) {
        this.vars = vars;
    }

    public SendSms getBatch() {
        return batch;
    }

    public void setBatch(SendSms batch) {
        this.batch = batch;
    }

    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public String getMsgdata() {
        return msgdata;
    }

    public void setMsgdata(String msgdata) {
        this.msgdata = msgdata;
    }

    public Integer getTotalMessage() {
        return totalMessage;
    }

    public void setTotalMessage(Integer totalMessage) {
        this.totalMessage = totalMessage;
    }

    public Boolean getSent() {
        return isSent;
    }

    public void setSent(Boolean sent) {
        isSent = sent;
    }

    public Instant getSendDate() {
        return sendDate;
    }

    public String getBulkId() {
        return bulkId;
    }

    public void setBulkId(String bulkId) {
        this.bulkId = bulkId;
    }

    public Instant getBulkCreatedAt() {
        return bulkCreatedAt;
    }

    public void setBulkCreatedAt(Instant bulkCreatedAt) {
        this.bulkCreatedAt = bulkCreatedAt;
    }

    public void setSendDate(Instant sendDate) {
        this.sendDate = sendDate;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getTemplate_id() {
        return template_id;
    }

    public MessageType getType() {
        return type;
    }

    public String getNamereceiver() {
        return namereceiver;
    }

    public void setNamereceiver(String namereceiver) {
        this.namereceiver = namereceiver;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public void setTemplate_id(Long template_id) {
        this.template_id = template_id;
    }

    public String getUser_login() {
        return user_login;
    }

    public void setUser_login(String user_login) {
        this.user_login = user_login;
    }

    public String getDeliveryStatus() {
        return deliveryStatus;
    }

    public void setDeliveryStatus(String deliveryStatus) {
        this.deliveryStatus = deliveryStatus;
    }

    public Chat getChat() {
        return chat;
    }

    public void setChat(Chat chat) {
        this.chat = chat;
    }

    public ContentType getContentType() {
        return contentType;
    }

    public void setContentType(ContentType contentType) {
        this.contentType = contentType;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public Direction getDirection() {
        return direction;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    public String getLast_error() {
        return last_error;
    }

    public void setLast_error(String last_error) {
        this.last_error = last_error;
    }
}
