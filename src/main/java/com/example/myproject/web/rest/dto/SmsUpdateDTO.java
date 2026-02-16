package com.example.myproject.web.rest.dto;

/**
 * ✅ DTO POUR MISE À JOUR DES SMS AVEC DELIVERY STATUS
 */
public class SmsUpdateDTO {

    public final Long smsId;
    public final boolean success;
    public final String messageId;
    public final String deliveryStatus; // ✅ NOUVEAU CHAMP

    public SmsUpdateDTO(Long smsId, boolean success, String messageId, String deliveryStatus) {
        this.smsId = smsId;
        this.success = success;
        this.messageId = messageId;
        this.deliveryStatus = deliveryStatus;
    }

    // ✅ CONSTRUCTEUR DE COMPATIBILITÉ (pour l'ancien code qui n'utilise pas deliveryStatus)
    public SmsUpdateDTO(Long smsId, boolean success, String messageId) {
        this.smsId = smsId;
        this.success = success;
        this.messageId = messageId;
        this.deliveryStatus = success ? "SENT" : "FAILED"; // ✅ VALEUR PAR DÉFAUT BASÉE SUR SUCCESS
    }

    @Override
    public String toString() {
        return String.format(
            "SmsUpdateDTO{smsId=%d, success=%b, messageId='%s', deliveryStatus='%s'}",
            smsId,
            success,
            messageId,
            deliveryStatus
        );
    }
}
