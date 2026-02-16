package com.example.myproject.domain;

import com.example.myproject.domain.enumeration.Channel;
import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "channel_configuration")
public class ChannelConfiguration implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator", sequenceName = "sequenceGenerator", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @Column(name = "user_login", nullable = false, length = 50)
    private String userLogin;

    @Column(name = "channel_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private Channel channelType;

    @Column(name = "host", length = 255)
    private String host;

    @Column(name = "port")
    private Integer port;

    @Column(name = "username", length = 255)
    private String username;

    @Column(name = "password", length = 255)
    private String encryptedPassword;

    @Column(name = "extra_config_json", columnDefinition = "TEXT")
    private String extraConfigJson;

    @Column(name = "verified", nullable = false)
    private Boolean verified = false;

    public String getSmsOperator() {
        return smsOperator;
    }

    public void setSmsOperator(String smsOperator) {
        this.smsOperator = smsOperator;
    }

    @Column(name = "sms_operator", length = 50)
    private String smsOperator;

    // ============================================
    // Getters and Setters
    // ============================================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserLogin() {
        return userLogin;
    }

    public void setUserLogin(String userLogin) {
        this.userLogin = userLogin;
    }

    public Channel getChannelType() {
        return channelType;
    }

    public void setChannelType(Channel channelType) {
        this.channelType = channelType;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEncryptedPassword() {
        return encryptedPassword;
    }

    public void setEncryptedPassword(String encryptedPassword) {
        this.encryptedPassword = encryptedPassword;
    }

    public String getExtraConfigJson() {
        return extraConfigJson;
    }

    public void setExtraConfigJson(String extraConfigJson) {
        this.extraConfigJson = extraConfigJson;
    }

    public Boolean getVerified() {
        return verified;
    }

    public void setVerified(Boolean verified) {
        this.verified = verified;
    }

    @Override
    public String toString() {
        return (
            "ChannelConfiguration{" +
            "id=" +
            id +
            ", userLogin='" +
            userLogin +
            '\'' +
            ", channelType=" +
            channelType +
            ", host='" +
            host +
            '\'' +
            ", port=" +
            port +
            ", username='" +
            username +
            '\'' +
            ", verified=" +
            verified +
            '}'
        );
    }
}
