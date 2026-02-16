package com.example.myproject.web.rest.dto;

public class ProcessResult {

    public final boolean success;
    public final Exception exception;
    public final String messageId;
    public final String error;

    public ProcessResult(boolean success, Exception exception, String messageId, String error) {
        this.success = success;
        this.exception = exception;
        this.messageId = messageId;
        this.error = error;
    }

    public String getError() {
        return error;
    }

    public String getMessageId() {
        return messageId;
    }

    public boolean isSuccess() {
        return success;
    }

    public Exception getException() {
        return exception;
    }
}
