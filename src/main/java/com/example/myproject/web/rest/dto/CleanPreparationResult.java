package com.example.myproject.web.rest.dto;

import com.example.myproject.domain.Contact;
import java.util.List;

public class CleanPreparationResult {

    private List<Contact> contacts;
    private Long groupId;
    private String progressId;

    public CleanPreparationResult(List<Contact> contacts, Long groupId, String progressId) {
        this.contacts = contacts;
        this.groupId = groupId;
        this.progressId = progressId;
    }

    public List<Contact> getContacts() {
        return contacts;
    }

    public void setContacts(List<Contact> contacts) {
        this.contacts = contacts;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public String getProgressId() {
        return progressId;
    }

    public void setProgressId(String progressId) {
        this.progressId = progressId;
    }
}
