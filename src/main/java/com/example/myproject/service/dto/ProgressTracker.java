package com.example.myproject.service.dto;

public interface ProgressTracker {
    void init(String progressId, int total);

    void increment(String progressId, int increment);

    void setProgress(String progressId, int current, int total, boolean completed);

    void complete(String progressId);

    ProgressStatus getProgress(String progressId);

    // 🆕 NOUVELLES MÉTHODES

    /**
     * Marquer un progress comme terminé avec une raison
     */
    void markAsCompleted(String progressId, String reason);

    /**
     * Mise à jour détaillée avec toutes les statistiques
     */
    void updateDetailedProgress(String progressId, int processed, int inserted, int duplicates, int errors, boolean completed);

    /**
     * Vérifier si un progress existe
     */
    boolean exists(String progressId);

    /**
     * Supprimer un progress terminé
     */
    void remove(String progressId);
}
