package com.example.myproject.web.rest.dto;

public class OtpRetour {

    private String code;
    private String phoneNumber;
    private String message;
    private Long otp_id;

    // Constructeur par défaut
    public OtpRetour() {}

    // Constructeur avec tous les champs

    // Getter et Setter pour le champ 'code'
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    // Getter et Setter pour le champ 'message'
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    // Getter et Setter pour le champ 'otp_id'
    public Long getOtp_id() {
        return otp_id;
    }

    public void setOtp_id(Long otp_id) {
        this.otp_id = otp_id;
    }

    // Méthode toString pour afficher les valeurs de l'objet
    @Override
    public String toString() {
        return (
            "OtpRetour{" +
            "code='" +
            code +
            '\'' +
            ", phoneNumber=" +
            phoneNumber +
            ", message='" +
            message +
            '\'' +
            ", otp_id=" +
            otp_id +
            '}'
        );
    }
}
