package com.example.myproject.web.rest.dto;

public class SendSmsmini {

    private String sender;
    private String receiver;
    private String msgdata;

    // Getter et setter pour l'attribut sender
    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    // Getter et setter pour l'attribut receiver
    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    // Getter et setter pour l'attribut msgdata
    public String getMsgdata() {
        return msgdata;
    }

    public void setMsgdata(String msgdata) {
        this.msgdata = msgdata;
    }

    // Constructeur sans argument
    public SendSmsmini() {}

    @Override
    public String toString() {
        return "SendSmsmini{" + "sender='" + sender + '\'' + ", receiver='" + receiver + '\'' + ", msgdata='" + msgdata + '\'' + '}';
    }

    public static boolean isValidPhoneNumber(String receiver) {
        // Regex pour valider les numéros de téléphone en Tunisie avec le préfixe international optionnel
        String phonePattern = "^(\\+?222)?([324])\\d{7}$"; // Le +222 est optionnel, et les numéros commencent par 3, 2 ou 4

        // Vérifie que le numéro n'est pas nul et qu'il correspond au modèle regex
        return receiver != null && receiver.matches(phonePattern);
    }
}
