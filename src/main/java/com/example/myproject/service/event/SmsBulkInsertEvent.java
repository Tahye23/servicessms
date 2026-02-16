package com.example.myproject.service.event;

import com.example.myproject.domain.Sms;
import java.util.List;

public class SmsBulkInsertEvent {

    private final List<Sms> smsList;
    private final Long sendSmsId;

    public SmsBulkInsertEvent(List<Sms> smsList, Long sendSmsId) {
        this.smsList = smsList;
        this.sendSmsId = sendSmsId;
    }

    public List<Sms> getSmsList() {
        return smsList;
    }

    public Long getSendSmsId() {
        return sendSmsId;
    }
}
