package com.example.myproject.web.rest.dto;

import java.util.List;

public class SmsRequest {

    private List<String> contacts;
    private String msgdata;
    private String sender;

    public SmsRequest() {}

    // Getters et setters
    public List<String> getContacts() {
        return contacts;
    }

    public void setContacts(List<String> contacts) {
        this.contacts = contacts;
    }

    public String getMsgdata() {
        return msgdata;
    }

    public void setMsgdata(String msgdata) {
        this.msgdata = msgdata;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    @Override
    public String toString() {
        return "SmsRequest{" + "contacts=" + contacts + ", msgdata='" + msgdata + '\'' + ", sender='" + sender + '\'' + '}';
    }
}
