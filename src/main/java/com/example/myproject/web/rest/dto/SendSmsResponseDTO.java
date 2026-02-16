package com.example.myproject.web.rest.dto;

public class SendSmsResponseDTO {

    private String bulkId;
    private int totalRecipients;
    private boolean templateExiste = true;
    private Boolean iSent;

    public SendSmsResponseDTO() {}

    public SendSmsResponseDTO(String bulkId, int totalRecipients, boolean templateExiste, Boolean iSent) {
        this.bulkId = bulkId;
        this.totalRecipients = totalRecipients;
        this.templateExiste = templateExiste;
        if (iSent != null) {
            this.iSent = iSent;
        }
    }

    public String getBulkId() {
        return bulkId;
    }

    public void setBulkId(String bulkId) {
        this.bulkId = bulkId;
    }

    public int getTotalRecipients() {
        return totalRecipients;
    }

    public void setTotalRecipients(int totalRecipients) {
        this.totalRecipients = totalRecipients;
    }

    public static SendSmsResponseDTO empty() {
        return new SendSmsResponseDTO(null, 0, true, false);
    }

    public Boolean isiSent() {
        return iSent;
    }

    public void setiSent(Boolean iSent) {
        this.iSent = iSent;
    }

    public boolean isTemplateExiste() {
        return templateExiste;
    }

    public void setTemplateExiste(boolean templateExiste) {
        this.templateExiste = templateExiste;
    }
}
