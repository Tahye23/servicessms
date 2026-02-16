package com.example.myproject.service;

import com.example.myproject.web.rest.dto.SmsProgress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class ProgressService {

    // Map pour stocker la progression associée à chaque envoi (clé = ID parent)
    private final Map<Long, SmsProgress> progressMap = new ConcurrentHashMap<>();

    // Initialise la progression pour un envoi donné
    public void initProgress(Long id, int processed, int total) {
        SmsProgress progress = new SmsProgress(processed, total);
        progressMap.put(id, progress);
    }

    // Met à jour la progression en ajoutant le nombre traité
    public void updateProgress(Long id, int count) {
        SmsProgress progress = progressMap.get(id);
        if (progress != null) {
            progress.setProcessed(progress.getProcessed() + count);
        }
    }

    // Récupère la progression pour un envoi donné
    public SmsProgress getProgress(Long id) {
        return progressMap.get(id);
    }
}
