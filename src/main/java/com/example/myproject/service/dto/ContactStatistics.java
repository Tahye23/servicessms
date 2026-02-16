package com.example.myproject.service.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO pour les statistiques des contacts
 */
public class ContactStatistics {

    private Long totalContacts;
    private Long validContacts;
    private Long invalidContacts;
    private Long duplicateContacts;
    private Long whatsappContacts;

    private Long totalSmsCount;
    private Long totalWhatsappCount;
    private Long totalMessagesCount;

    private Double avgSuccessRate;

    private List<CarrierCount> topCarriers = new ArrayList<>();
    private List<CountryCount> topCountries = new ArrayList<>();

    // Constructeur pour la requête JPA
    public ContactStatistics(
        Long totalContacts,
        Long validContacts,
        Long invalidContacts,
        Long duplicateContacts,
        Long whatsappContacts,
        Long totalSmsCount,
        Long totalWhatsappCount,
        Long totalMessagesCount,
        Double avgSuccessRate
    ) {
        this.totalContacts = totalContacts;
        this.validContacts = validContacts;
        this.invalidContacts = invalidContacts;
        this.duplicateContacts = duplicateContacts;
        this.whatsappContacts = whatsappContacts;
        this.totalSmsCount = totalSmsCount;
        this.totalWhatsappCount = totalWhatsappCount;
        this.totalMessagesCount = totalMessagesCount;
        this.avgSuccessRate = avgSuccessRate;
    }

    // Constructeur par défaut
    public ContactStatistics() {}

    // Getters et Setters
    public Long getTotalContacts() {
        return totalContacts;
    }

    public void setTotalContacts(Long totalContacts) {
        this.totalContacts = totalContacts;
    }

    public Long getValidContacts() {
        return validContacts;
    }

    public void setValidContacts(Long validContacts) {
        this.validContacts = validContacts;
    }

    public Long getInvalidContacts() {
        return invalidContacts;
    }

    public void setInvalidContacts(Long invalidContacts) {
        this.invalidContacts = invalidContacts;
    }

    public Long getDuplicateContacts() {
        return duplicateContacts;
    }

    public void setDuplicateContacts(Long duplicateContacts) {
        this.duplicateContacts = duplicateContacts;
    }

    public Long getWhatsappContacts() {
        return whatsappContacts;
    }

    public void setWhatsappContacts(Long whatsappContacts) {
        this.whatsappContacts = whatsappContacts;
    }

    public Long getTotalSmsCount() {
        return totalSmsCount;
    }

    public void setTotalSmsCount(Long totalSmsCount) {
        this.totalSmsCount = totalSmsCount;
    }

    public Long getTotalWhatsappCount() {
        return totalWhatsappCount;
    }

    public void setTotalWhatsappCount(Long totalWhatsappCount) {
        this.totalWhatsappCount = totalWhatsappCount;
    }

    public Long getTotalMessagesCount() {
        return totalMessagesCount;
    }

    public void setTotalMessagesCount(Long totalMessagesCount) {
        this.totalMessagesCount = totalMessagesCount;
    }

    public Double getAvgSuccessRate() {
        return avgSuccessRate;
    }

    public void setAvgSuccessRate(Double avgSuccessRate) {
        this.avgSuccessRate = avgSuccessRate;
    }

    public List<CarrierCount> getTopCarriers() {
        return topCarriers;
    }

    public void setTopCarriers(List<CarrierCount> topCarriers) {
        this.topCarriers = topCarriers;
    }

    public List<CountryCount> getTopCountries() {
        return topCountries;
    }

    public void setTopCountries(List<CountryCount> topCountries) {
        this.topCountries = topCountries;
    }

    // Classes internes pour les compteurs
    public static class CarrierCount {

        private String carrier;
        private Integer count;

        public CarrierCount() {}

        public CarrierCount(String carrier, Integer count) {
            this.carrier = carrier;
            this.count = count;
        }

        public String getCarrier() {
            return carrier;
        }

        public void setCarrier(String carrier) {
            this.carrier = carrier;
        }

        public Integer getCount() {
            return count;
        }

        public void setCount(Integer count) {
            this.count = count;
        }
    }

    public static class CountryCount {

        private String country;
        private Integer count;

        public CountryCount() {}

        public CountryCount(String country, Integer count) {
            this.country = country;
            this.count = count;
        }

        public String getCountry() {
            return country;
        }

        public void setCountry(String country) {
            this.country = country;
        }

        public Integer getCount() {
            return count;
        }

        public void setCount(Integer count) {
            this.count = count;
        }
    }
}
