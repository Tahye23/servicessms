package com.example.myproject.service;

import com.example.myproject.service.dto.ProgressStatus;
import com.example.myproject.service.dto.ProgressTracker;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
class ProgressTrackerImpl implements ProgressTracker {

    private static final Logger log = LoggerFactory.getLogger(ProgressTrackerImpl.class);
    private final Map<String, ProgressData> progressMap = new ConcurrentHashMap<>();

    private static class ProgressData {

        int total;
        int inserted; // 🎯 Renommé en 'processed' conceptuellement
        long startTime;
        boolean completed;

        // 🆕 Statistiques détaillées
        int actualInserted; // Vrais nouveaux contacts insérés
        int duplicates; // Contacts doublons
        int errors; // Erreurs de traitement

        ProgressData(int total) {
            this.total = total;
            this.inserted = 0;
            this.startTime = System.nanoTime();
            this.completed = false;
            this.actualInserted = 0;
            this.duplicates = 0;
            this.errors = 0;
        }
    }

    @Override
    public void init(String progressId, int total) {
        progressMap.put(progressId, new ProgressData(total));
        log.info("✅ Initialized progress for {} with total {}", progressId, total);
    }

    @Override
    public void increment(String progressId, int increment) {
        ProgressData data = progressMap.get(progressId);
        if (data != null) {
            synchronized (data) {
                data.inserted += increment;
                log.debug(
                    "📊 Progress for {}: {}/{} ({}%)",
                    progressId,
                    data.inserted,
                    data.total,
                    data.total > 0 ? ((data.inserted * 100.0) / data.total) : 0
                );
            }
        } else {
            log.warn("⚠️ Attempted to increment non-existent progress: {}", progressId);
        }
    }

    @Override
    public void setProgress(String progressId, int current, int total, boolean completed) {
        ProgressData data = progressMap.get(progressId);
        if (data != null) {
            synchronized (data) {
                data.inserted = Math.min(current, total);
                data.total = total;
                data.completed = completed;

                double percentage = total > 0 ? ((data.inserted * 100.0) / total) : 0;
                log.debug(
                    "📊 Progress set for {}: {}/{} ({}%) - Completed: {}",
                    progressId,
                    data.inserted,
                    total,
                    String.format("%.1f", percentage),
                    completed
                );
            }
        } else {
            log.warn("⚠️ Attempted to set progress for non-existent progressId: {}", progressId);
        }
    }

    @Override
    public void complete(String progressId) {
        ProgressData data = progressMap.get(progressId);
        if (data != null) {
            synchronized (data) {
                // 🚀 CORRECTION : Marquer comme terminé sans forcer inserted = total
                // Car dans le cas des doublons, inserted reste à 0 mais c'est terminé
                data.completed = true;
                log.info(
                    "✅ Completed progress for {} - Final: {}/{} (actualInserted: {}, duplicates: {}, errors: {})",
                    progressId,
                    data.inserted,
                    data.total,
                    data.actualInserted,
                    data.duplicates,
                    data.errors
                );
            }
        } else {
            log.warn("⚠️ Attempted to complete non-existent progress: {}", progressId);
        }
    }

    @Override
    public ProgressStatus getProgress(String progressId) {
        ProgressData data = progressMap.get(progressId);
        if (data == null) {
            log.debug("🔍 Progress not found for progressId: {}, returning default", progressId);
            return new ProgressStatus(0, 0, 0.0, 0.0, true);
        }

        synchronized (data) {
            double elapsedSeconds = (System.nanoTime() - data.startTime) / 1_000_000_000.0;

            // 🚀 CORRECTION : Taux de traitement basé sur les éléments traités (inserted = processed)
            double processingRate = elapsedSeconds > 0 ? data.inserted / elapsedSeconds : 0.0;

            // 🚀 CORRECTION : Calcul du temps restant
            double remainingItems = Math.max(0, data.total - data.inserted);
            double estimatedTimeRemaining = (processingRate > 0 && !data.completed && remainingItems > 0)
                ? remainingItems / processingRate
                : 0.0;

            // 🚀 CORRECTION : Déterminer si réellement terminé
            boolean actuallyCompleted = data.completed || (data.inserted >= data.total && data.total > 0);

            ProgressStatus status = new ProgressStatus(
                data.total,
                data.inserted, // 🎯 Ceci représente les éléments traités
                processingRate,
                actuallyCompleted ? 0.0 : estimatedTimeRemaining,
                actuallyCompleted
            );

            if (actuallyCompleted && !data.completed) {
                log.info("🔄 Auto-completing progress for {} ({}/{})", progressId, data.inserted, data.total);
                data.completed = true;
            }

            log.debug(
                "📊 Progress status for {}: total={}, processed={}, rate={:.2f}/s, remaining={:.1f}s, completed={}",
                progressId,
                data.total,
                data.inserted,
                processingRate,
                estimatedTimeRemaining,
                actuallyCompleted
            );

            return status;
        }
    }

    // 🆕 NOUVELLES MÉTHODES IMPLÉMENTÉES

    @Override
    public void markAsCompleted(String progressId, String reason) {
        ProgressData data = progressMap.get(progressId);
        if (data != null) {
            synchronized (data) {
                data.completed = true;
                log.info("✅ Marked progress {} as completed: {}/{} (reason: {})", progressId, data.inserted, data.total, reason);
            }
        } else {
            log.warn("⚠️ Attempted to mark non-existent progress as completed: {}", progressId);
        }
    }

    @Override
    public void updateDetailedProgress(String progressId, int processed, int inserted, int duplicates, int errors, boolean completed) {
        ProgressData data = progressMap.get(progressId);
        if (data != null) {
            synchronized (data) {
                // 🚀 CORRECTION : Mettre à jour toutes les statistiques
                data.inserted = processed; // Le champ 'inserted' représente maintenant 'processed'
                data.actualInserted = inserted; // Vrais nouveaux contacts insérés
                data.duplicates = duplicates; // Doublons
                data.errors = errors; // Erreurs
                data.completed = completed || (processed >= data.total);

                log.debug(
                    "📊 Detailed progress update for {}: processed={}/{}, inserted={}, duplicates={}, errors={}, completed={}",
                    progressId,
                    processed,
                    data.total,
                    inserted,
                    duplicates,
                    errors,
                    data.completed
                );
            }
        } else {
            log.warn("⚠️ Attempted to update non-existent detailed progress: {}", progressId);
        }
    }

    @Override
    public boolean exists(String progressId) {
        return progressMap.containsKey(progressId);
    }

    @Override
    public void remove(String progressId) {
        ProgressData removed = progressMap.remove(progressId);
        if (removed != null) {
            log.info("🗑️ Removed progress for progressId: {}", progressId);
        }
    }

    // 🛠️ MÉTHODES UTILITAIRES EXISTANTES

    /**
     * Nettoie les anciens progrès terminés
     */
    public void cleanup() {
        int removedCount = 0;
        long oneHourAgo = System.nanoTime() - 3600_000_000_000L; // 1 heure

        Iterator<Map.Entry<String, ProgressData>> iterator = progressMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, ProgressData> entry = iterator.next();
            ProgressData data = entry.getValue();

            if (data.completed && data.startTime < oneHourAgo) {
                iterator.remove();
                removedCount++;
                log.debug("🧹 Cleaned up old completed progress: {}", entry.getKey());
            }
        }

        if (removedCount > 0) {
            log.info("🧹 Cleanup completed: removed {} old progress entries", removedCount);
        }
    }

    /**
     * Obtient le nombre de progrès actifs
     */
    public int getActiveCount() {
        return (int) progressMap.values().stream().filter(data -> !data.completed).count();
    }

    /**
     * 🆕 Obtenir les statistiques détaillées d'un progress
     */
    public DetailedProgressStats getDetailedStats(String progressId) {
        ProgressData data = progressMap.get(progressId);
        if (data == null) return null;

        synchronized (data) {
            return new DetailedProgressStats(
                data.total,
                data.inserted, // processed
                data.actualInserted, // réellement insérés
                data.duplicates,
                data.errors,
                data.completed
            );
        }
    }

    /**
     * 🆕 Classe pour les statistiques détaillées
     */
    public static class DetailedProgressStats {

        public final int total;
        public final int processed;
        public final int inserted;
        public final int duplicates;
        public final int errors;
        public final boolean completed;

        public DetailedProgressStats(int total, int processed, int inserted, int duplicates, int errors, boolean completed) {
            this.total = total;
            this.processed = processed;
            this.inserted = inserted;
            this.duplicates = duplicates;
            this.errors = errors;
            this.completed = completed;
        }
    }
}
