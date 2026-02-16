package com.example.myproject.web.rest.dto;

import com.example.myproject.domain.Contact;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

/**
 * Réponse améliorée pour l'import de contacts avec distinction des types de doublons
 */
public class DuplicateContactsResponse {

    @JsonProperty("uniqueContacts")
    private List<Contact> uniqueContacts;

    @JsonProperty("duplicateContacts")
    private List<Contact> duplicateContacts; // Maintenu pour compatibilité - contiendra tous les doublons

    @JsonProperty("databaseDuplicates")
    private List<Contact> databaseDuplicates; // 🆕 Doublons côté base de données

    @JsonProperty("fileDuplicates")
    private List<Contact> fileDuplicates; // 🆕 Doublons côté fichier

    @JsonProperty("errorContacts")
    private List<Contact> errorContacts;

    @JsonProperty("totalFileLines")
    private int totalFileLines;

    @JsonProperty("totalInserted")
    private int totalInserted;

    @JsonProperty("totalDuplicates")
    private int totalDuplicates; // Maintenu pour compatibilité - total de tous les doublons

    @JsonProperty("totalDatabaseDuplicates")
    private int totalDatabaseDuplicates; // 🆕 Nombre de doublons DB

    @JsonProperty("totalFileDuplicates")
    private int totalFileDuplicates; // 🆕 Nombre de doublons fichier

    @JsonProperty("totalErrors")
    private int totalErrors;

    @JsonProperty("errorFileLocation")
    private String errorFileLocation;

    @JsonProperty("duplicateFileLocation")
    private String duplicateFileLocation; // Maintenu pour compatibilité

    @JsonProperty("databaseDuplicateFileLocation")
    private String databaseDuplicateFileLocation; // 🆕 Fichier doublons DB

    @JsonProperty("fileDuplicateFileLocation")
    private String fileDuplicateFileLocation; // 🆕 Fichier doublons fichier

    @JsonProperty("progressId")
    private String progressId;

    @JsonIgnore
    private List<Contact> allContactsToInsert; // Temporary for async insertion, not exposed in JSON

    public DuplicateContactsResponse() {
        this.databaseDuplicates = new ArrayList<>();
        this.fileDuplicates = new ArrayList<>();
        this.duplicateContacts = new ArrayList<>();
    }

    // 🆕 Constructeur principal avec distinction des doublons
    public DuplicateContactsResponse(
        List<Contact> uniqueContacts,
        List<Contact> databaseDuplicates,
        List<Contact> fileDuplicates,
        List<Contact> errorContacts,
        int totalFileLines,
        int totalInserted,
        int totalDatabaseDuplicates,
        int totalFileDuplicates,
        int totalErrors,
        String errorFileLocation,
        String databaseDuplicateFileLocation,
        String fileDuplicateFileLocation,
        String progressId
    ) {
        this.uniqueContacts = uniqueContacts;
        this.databaseDuplicates = databaseDuplicates != null ? databaseDuplicates : new ArrayList<>();
        this.fileDuplicates = fileDuplicates != null ? fileDuplicates : new ArrayList<>();
        this.errorContacts = errorContacts;
        this.totalFileLines = totalFileLines;
        this.totalInserted = totalInserted;
        this.totalDatabaseDuplicates = totalDatabaseDuplicates;
        this.totalFileDuplicates = totalFileDuplicates;
        this.totalErrors = totalErrors;
        this.errorFileLocation = errorFileLocation;
        this.databaseDuplicateFileLocation = databaseDuplicateFileLocation;
        this.fileDuplicateFileLocation = fileDuplicateFileLocation;
        this.progressId = progressId != null ? progressId : java.util.UUID.randomUUID().toString();

        // Pour compatibilité - combiner tous les doublons
        this.totalDuplicates = totalDatabaseDuplicates + totalFileDuplicates;
        this.duplicateContacts = new ArrayList<>();
        this.duplicateContacts.addAll(this.databaseDuplicates);
        this.duplicateContacts.addAll(this.fileDuplicates);
        this.duplicateFileLocation = databaseDuplicateFileLocation; // Par défaut, utiliser le fichier DB
    }

    // Constructeur de compatibilité (maintenu pour l'ancien code)
    public DuplicateContactsResponse(
        List<Contact> uniqueContacts,
        List<Contact> duplicateContacts,
        List<Contact> errorContacts,
        int totalFileLines,
        int totalInserted,
        int totalDuplicates,
        int totalErrors,
        String errorFileLocation,
        String duplicateFileLocation,
        String progressId
    ) {
        this.uniqueContacts = uniqueContacts;
        this.duplicateContacts = duplicateContacts != null ? duplicateContacts : new ArrayList<>();
        this.errorContacts = errorContacts;
        this.totalFileLines = totalFileLines;
        this.totalInserted = totalInserted;
        this.totalDuplicates = totalDuplicates;
        this.totalErrors = totalErrors;
        this.errorFileLocation = errorFileLocation;
        this.duplicateFileLocation = duplicateFileLocation;
        this.progressId = progressId != null ? progressId : java.util.UUID.randomUUID().toString();

        // Pour la compatibilité, traiter tous les doublons comme des doublons DB
        this.databaseDuplicates = new ArrayList<>(this.duplicateContacts);
        this.fileDuplicates = new ArrayList<>();
        this.totalDatabaseDuplicates = totalDuplicates;
        this.totalFileDuplicates = 0;
        this.databaseDuplicateFileLocation = duplicateFileLocation;
        this.fileDuplicateFileLocation = "";
    }

    // 📊 Méthodes utilitaires pour les statistiques

    /**
     * @return Pourcentage de réussite d'import
     */
    public double getSuccessRate() {
        if (totalFileLines == 0) return 0.0;
        return ((double) totalInserted / totalFileLines) * 100;
    }

    /**
     * @return Pourcentage de doublons
     */
    public double getDuplicateRate() {
        if (totalFileLines == 0) return 0.0;
        return ((double) totalDuplicates / totalFileLines) * 100;
    }

    /**
     * @return Pourcentage d'erreurs
     */
    public double getErrorRate() {
        if (totalFileLines == 0) return 0.0;
        return ((double) totalErrors / totalFileLines) * 100;
    }

    /**
     * @return Résumé textuel de l'import
     */
    public String getImportSummary() {
        return String.format(
            "Import terminé : %d lignes traitées | %d insérés (%.1f%%) | %d doublons DB | %d doublons fichier | %d erreurs (%.1f%%)",
            totalFileLines,
            totalInserted,
            getSuccessRate(),
            totalDatabaseDuplicates,
            totalFileDuplicates,
            totalErrors,
            getErrorRate()
        );
    }

    // 🔍 Méthodes de validation

    /**
     * Valide la cohérence des données de réponse
     */
    public boolean isValid() {
        int calculatedTotal = totalInserted + totalDuplicates + totalErrors;
        return calculatedTotal <= totalFileLines;
    }

    /**
     * Vérifie s'il y a des doublons à traiter
     */
    public boolean hasDuplicates() {
        return totalDuplicates > 0;
    }

    /**
     * Vérifie s'il y a des erreurs
     */
    public boolean hasErrors() {
        return totalErrors > 0;
    }

    /**
     * Vérifie s'il y a des doublons base de données
     */
    public boolean hasDatabaseDuplicates() {
        return totalDatabaseDuplicates > 0;
    }

    /**
     * Vérifie s'il y a des doublons fichier
     */
    public boolean hasFileDuplicates() {
        return totalFileDuplicates > 0;
    }

    // Getters and setters

    public List<Contact> getUniqueContacts() {
        return uniqueContacts;
    }

    public void setUniqueContacts(List<Contact> uniqueContacts) {
        this.uniqueContacts = uniqueContacts;
    }

    public List<Contact> getDuplicateContacts() {
        return duplicateContacts;
    }

    public void setDuplicateContacts(List<Contact> duplicateContacts) {
        this.duplicateContacts = duplicateContacts;
        // Maintenir la cohérence avec les nouveaux champs si possible
        if (
            duplicateContacts != null &&
            (databaseDuplicates == null || databaseDuplicates.isEmpty()) &&
            (fileDuplicates == null || fileDuplicates.isEmpty())
        ) {
            this.databaseDuplicates = new ArrayList<>(duplicateContacts);
        }
    }

    public List<Contact> getDatabaseDuplicates() {
        return databaseDuplicates;
    }

    public void setDatabaseDuplicates(List<Contact> databaseDuplicates) {
        this.databaseDuplicates = databaseDuplicates;
        updateCombinedDuplicates();
    }

    public List<Contact> getFileDuplicates() {
        return fileDuplicates;
    }

    public void setFileDuplicates(List<Contact> fileDuplicates) {
        this.fileDuplicates = fileDuplicates;
        updateCombinedDuplicates();
    }

    public List<Contact> getErrorContacts() {
        return errorContacts;
    }

    public void setErrorContacts(List<Contact> errorContacts) {
        this.errorContacts = errorContacts;
    }

    public int getTotalFileLines() {
        return totalFileLines;
    }

    public void setTotalFileLines(int totalFileLines) {
        this.totalFileLines = totalFileLines;
    }

    public int getTotalInserted() {
        return totalInserted;
    }

    public void setTotalInserted(int totalInserted) {
        this.totalInserted = totalInserted;
    }

    public int getTotalDuplicates() {
        return totalDuplicates;
    }

    public void setTotalDuplicates(int totalDuplicates) {
        this.totalDuplicates = totalDuplicates;
    }

    public int getTotalDatabaseDuplicates() {
        return totalDatabaseDuplicates;
    }

    public void setTotalDatabaseDuplicates(int totalDatabaseDuplicates) {
        this.totalDatabaseDuplicates = totalDatabaseDuplicates;
        updateTotalDuplicates();
    }

    public int getTotalFileDuplicates() {
        return totalFileDuplicates;
    }

    public void setTotalFileDuplicates(int totalFileDuplicates) {
        this.totalFileDuplicates = totalFileDuplicates;
        updateTotalDuplicates();
    }

    public int getTotalErrors() {
        return totalErrors;
    }

    public void setTotalErrors(int totalErrors) {
        this.totalErrors = totalErrors;
    }

    public String getErrorFileLocation() {
        return errorFileLocation;
    }

    public void setErrorFileLocation(String errorFileLocation) {
        this.errorFileLocation = errorFileLocation;
    }

    public String getDuplicateFileLocation() {
        return duplicateFileLocation;
    }

    public void setDuplicateFileLocation(String duplicateFileLocation) {
        this.duplicateFileLocation = duplicateFileLocation;
    }

    public String getDatabaseDuplicateFileLocation() {
        return databaseDuplicateFileLocation;
    }

    public void setDatabaseDuplicateFileLocation(String databaseDuplicateFileLocation) {
        this.databaseDuplicateFileLocation = databaseDuplicateFileLocation;
    }

    public String getFileDuplicateFileLocation() {
        return fileDuplicateFileLocation;
    }

    public void setFileDuplicateFileLocation(String fileDuplicateFileLocation) {
        this.fileDuplicateFileLocation = fileDuplicateFileLocation;
    }

    public String getProgressId() {
        return progressId;
    }

    public void setProgressId(String progressId) {
        this.progressId = progressId;
    }

    public List<Contact> getAllContactsToInsert() {
        return allContactsToInsert;
    }

    public void setAllContactsToInsert(List<Contact> allContactsToInsert) {
        this.allContactsToInsert = allContactsToInsert;
    }

    // 🔧 Méthodes utilitaires privées

    /**
     * Met à jour la liste combinée des doublons pour la compatibilité
     */
    private void updateCombinedDuplicates() {
        if (duplicateContacts == null) {
            duplicateContacts = new ArrayList<>();
        } else {
            duplicateContacts.clear();
        }

        if (databaseDuplicates != null) {
            duplicateContacts.addAll(databaseDuplicates);
        }
        if (fileDuplicates != null) {
            duplicateContacts.addAll(fileDuplicates);
        }
    }

    /**
     * Met à jour le total des doublons
     */
    private void updateTotalDuplicates() {
        this.totalDuplicates = this.totalDatabaseDuplicates + this.totalFileDuplicates;
    }
}
