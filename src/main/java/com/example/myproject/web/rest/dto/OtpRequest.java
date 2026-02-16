package com.example.myproject.web.rest.dto;

public class OtpRequest {

    // Attributs de la classe
    private String sender;
    private String phoneNumber;
    private String codeotp;

    // Constructeur par défaut
    public OtpRequest() {}

    // Getters et Setters

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        if (!isValidPhoneNumber(phoneNumber)) {
            throw new IllegalArgumentException("Numéro de téléphone invalide");
        }

        this.phoneNumber = phoneNumber;
    }

    public String getCodeotp() {
        return codeotp;
    }

    public void setCodeotp(String codeotp) {
        this.codeotp = codeotp;
    }

    public static boolean isValidPhoneNumber(String conTelephone) {
        String phonePattern = "^(\\+?222)?([324])\\d{7}$"; // Ajustez l'expression régulière si nécessaire
        return conTelephone != null && conTelephone.matches(phonePattern);
    }

    // Méthode toString pour afficher les données de l'objet sous forme de chaîne
    @Override
    public String toString() {
        return "OtpRequest{" + "sender='" + sender + '\'' + ", phoneNumber='" + phoneNumber + '\'' + ", codeotp='" + codeotp + '\'' + '}';
    }
}
