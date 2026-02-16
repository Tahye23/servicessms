package com.example.myproject.service.dto;

// dto/AdvancedFiltersPayload.java
public class AdvancedFiltersPayload {

    public String nom;
    public String prenom;
    public String telephone;
    public Integer statut;
    public Boolean hasWhatsapp;
    public Integer minSmsSent;
    public Integer maxSmsSent;
    public Integer minWhatsappSent;
    public Integer maxWhatsappSent;
    public Boolean hasReceivedMessages;

    public String nomFilterType;
    public String prenomFilterType;
    public String telephoneFilterType;

    public Long campaignId;
    public String smsStatus;
    public String deliveryStatus;
    public String lastErrorContains;
    private String search;

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public Integer getStatut() {
        return statut;
    }

    public void setStatut(Integer statut) {
        this.statut = statut;
    }

    public Boolean getHasWhatsapp() {
        return hasWhatsapp;
    }

    public void setHasWhatsapp(Boolean hasWhatsapp) {
        this.hasWhatsapp = hasWhatsapp;
    }

    public Integer getMinSmsSent() {
        return minSmsSent;
    }

    public void setMinSmsSent(Integer minSmsSent) {
        this.minSmsSent = minSmsSent;
    }

    public Integer getMaxSmsSent() {
        return maxSmsSent;
    }

    public void setMaxSmsSent(Integer maxSmsSent) {
        this.maxSmsSent = maxSmsSent;
    }

    public Integer getMinWhatsappSent() {
        return minWhatsappSent;
    }

    public void setMinWhatsappSent(Integer minWhatsappSent) {
        this.minWhatsappSent = minWhatsappSent;
    }

    public Integer getMaxWhatsappSent() {
        return maxWhatsappSent;
    }

    public void setMaxWhatsappSent(Integer maxWhatsappSent) {
        this.maxWhatsappSent = maxWhatsappSent;
    }

    public Boolean getHasReceivedMessages() {
        return hasReceivedMessages;
    }

    public void setHasReceivedMessages(Boolean hasReceivedMessages) {
        this.hasReceivedMessages = hasReceivedMessages;
    }

    public String getNomFilterType() {
        return nomFilterType;
    }

    public void setNomFilterType(String nomFilterType) {
        this.nomFilterType = nomFilterType;
    }

    public String getPrenomFilterType() {
        return prenomFilterType;
    }

    public void setPrenomFilterType(String prenomFilterType) {
        this.prenomFilterType = prenomFilterType;
    }

    public String getTelephoneFilterType() {
        return telephoneFilterType;
    }

    public void setTelephoneFilterType(String telephoneFilterType) {
        this.telephoneFilterType = telephoneFilterType;
    }

    public Long getCampaignId() {
        return campaignId;
    }

    public void setCampaignId(Long campaignId) {
        this.campaignId = campaignId;
    }

    public String getSmsStatus() {
        return smsStatus;
    }

    public void setSmsStatus(String smsStatus) {
        this.smsStatus = smsStatus;
    }

    public String getDeliveryStatus() {
        return deliveryStatus;
    }

    public void setDeliveryStatus(String deliveryStatus) {
        this.deliveryStatus = deliveryStatus;
    }

    public String getLastErrorContains() {
        return lastErrorContains;
    }

    public void setLastErrorContains(String lastErrorContains) {
        this.lastErrorContains = lastErrorContains;
    }

    public String getSearch() {
        return search;
    }

    public void setSearch(String search) {
        this.search = search;
    }
}
