package com.example.myproject.service;

import com.example.myproject.domain.Contact;
import com.example.myproject.domain.Groupe;
import com.example.myproject.domain.Groupedecontact;
import com.example.myproject.repository.ContactRepository;
import com.example.myproject.repository.GroupedecontactRepository;
import com.example.myproject.repository.ImportHistoryRepository;
import com.example.myproject.service.dto.ProgressTracker;
import com.example.myproject.web.rest.ContactResource;
import com.example.myproject.web.rest.dto.CleanPreparationResult;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ContactCleaningService {

    private final Logger log = LoggerFactory.getLogger(ContactResource.class);
    private final ContactRepository contactRepository;
    private final GroupedecontactRepository groupedecontactRepository;
    private final ProgressTracker progressTracker;
    private final ImportHistoryRepository importHistoryRepository;
    private final TaskExecutor bulkInsertExecutor;

    public ContactCleaningService(
        ContactRepository contactRepository,
        GroupedecontactRepository groupedecontactRepository,
        ProgressTracker progressTracker,
        ImportHistoryRepository importHistoryRepository,
        @Qualifier("bulkInsertExecutor") TaskExecutor bulkInsertExecutor
    ) {
        this.contactRepository = contactRepository;
        this.groupedecontactRepository = groupedecontactRepository;
        this.progressTracker = progressTracker;
        this.importHistoryRepository = importHistoryRepository;
        this.bulkInsertExecutor = bulkInsertExecutor;
    }

    @Async("bulkInsertExecutor")
    public void insertContactsAsync(List<Contact> contacts, String login, String progressId) {
        int totalContacts = contacts.size();
        int batchSize = calculateOptimalBatchSize(totalContacts);

        log.info(
            "🚀 Starting async insertion (NO DUPLICATE CHECK) for {} contacts with progressId {} using batch size {}",
            totalContacts,
            progressId,
            batchSize
        );

        try {
            progressTracker.init(progressId, totalContacts);
            updateImportHistoryStatus(progressId, "PROCESSING");

            AtomicInteger processedCount = new AtomicInteger(0);
            AtomicInteger insertedCount = new AtomicInteger(0);
            AtomicInteger errorCount = new AtomicInteger(0);

            // 🚀 Traitement séquentiel des batches
            for (int i = 0; i < totalContacts; i += batchSize) {
                final int batchStart = i;
                final int batchEnd = Math.min(i + batchSize, totalContacts);
                final List<Contact> batch = contacts.subList(batchStart, batchEnd);

                try {
                    // Préparer les contacts pour l'insertion
                    batch.forEach(c -> {
                        c.setUser_login(login);
                        c.setProgressId(progressId);
                        if (c.getGroupedecontacts() != null) {
                            c.getGroupedecontacts().forEach(gc -> gc.setContact(c));
                        }
                    });

                    // ✅ Sauvegarde DIRECTE sans vérification
                    BatchResult result = saveBatchDirectly(batch);

                    insertedCount.addAndGet(result.inserted);
                    errorCount.addAndGet(result.errors);
                    processedCount.addAndGet(batch.size());

                    progressTracker.updateDetailedProgress(
                        progressId,
                        processedCount.get(),
                        insertedCount.get(),
                        0, // duplicates = 0
                        errorCount.get(),
                        false
                    );

                    log.info(
                        "📊 Batch {}-{}: inserted={}, errors={}, progress={}/{}",
                        batchStart + 1,
                        batchEnd,
                        result.inserted,
                        result.errors,
                        processedCount.get(),
                        totalContacts
                    );

                    Thread.sleep(10);
                } catch (Exception e) {
                    log.error("❌ Error in batch {}-{}: {}", batchStart, batchEnd, e.getMessage(), e);
                    errorCount.addAndGet(batch.size());
                    processedCount.addAndGet(batch.size());

                    progressTracker.updateDetailedProgress(
                        progressId,
                        processedCount.get(),
                        insertedCount.get(),
                        0,
                        errorCount.get(),
                        false
                    );
                }
            }

            // Finalisation
            if (totalContacts == 0 || processedCount.get() == 0) {
                progressTracker.markAsCompleted(progressId, "No contacts to process");
                updateImportHistoryStatus(progressId, "COMPLETED");
                return;
            }

            String reason = String.format(
                "All processed: %d inserted, %d errors (NO DUPLICATE CHECK)",
                insertedCount.get(),
                errorCount.get()
            );
            progressTracker.markAsCompleted(progressId, reason);
            updateImportHistoryStatus(progressId, "COMPLETED");

            log.info(
                "✅ Import completed: processed={}, inserted={}, errors={}",
                processedCount.get(),
                insertedCount.get(),
                errorCount.get()
            );

            updateFinalImportHistory(progressId, processedCount.get(), insertedCount.get(), 0, errorCount.get());
        } catch (Exception e) {
            log.error("❌ Fatal error in async insertion: {}", e.getMessage(), e);
            progressTracker.markAsCompleted(progressId, "Failed: " + e.getMessage());
            updateImportHistoryStatus(progressId, "FAILED");
            throw new RuntimeException("Insertion failed", e);
        }
    }

    /**
     * ✅ NOUVELLE MÉTHODE : Sauvegarde DIRECTE sans aucune vérification
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public BatchResult saveBatchDirectly(List<Contact> batch) {
        int inserted = 0;
        int errors = 0;

        try {
            // Validation minimale : téléphone non vide
            List<Contact> validContacts = batch
                .stream()
                .filter(contact -> contact.getContelephone() != null && !contact.getContelephone().trim().isEmpty())
                .collect(Collectors.toList());

            errors = batch.size() - validContacts.size();

            if (!validContacts.isEmpty()) {
                // ✅ INSERTION DIRECTE sans vérification de doublons
                List<Contact> savedContacts = contactRepository.saveAll(validContacts);
                contactRepository.flush();
                inserted = savedContacts.size();

                log.debug("✅ Inserted {} contacts directly (no duplicate check)", inserted);
            }

            return new BatchResult(inserted, 0, errors); // duplicates = 0
        } catch (Exception e) {
            log.error("❌ Error saving batch: {}", e.getMessage(), e);
            return new BatchResult(0, 0, batch.size());
        }
    }

    /**
     * Mise à jour finale de l'historique
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateFinalImportHistory(String bulkId, int processed, int inserted, int duplicates, int errors) {
        try {
            importHistoryRepository
                .findByBulkId(bulkId)
                .ifPresent(importHistory -> {
                    importHistory.setTotalLines(processed);
                    importHistory.setInsertedCount(inserted);
                    importHistory.setDuplicateCount(0); // ✅ Toujours 0
                    importHistory.setRejectedCount(errors);
                    importHistoryRepository.save(importHistory);

                    log.info("✅ Updated ImportHistory: processed={}, inserted={}, errors={}", processed, inserted, errors);
                });
        } catch (Exception e) {
            log.error("❌ Error updating ImportHistory: {}", e.getMessage(), e);
        }
    }

    /**
     * Classe pour les résultats de batch
     */
    public static class BatchResult {

        public final int inserted;
        public final int duplicates;
        public final int errors;

        public BatchResult(int inserted, int duplicates, int errors) {
            this.inserted = inserted;
            this.duplicates = duplicates;
            this.errors = errors;
        }
    }

    private int calculateOptimalBatchSize(int totalRecords) {
        if (totalRecords <= 1000) return 100;
        if (totalRecords <= 10000) return 500;
        if (totalRecords <= 100000) return 1000;
        return 2000;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateImportHistoryStatus(String bulkId, String status) {
        try {
            importHistoryRepository
                .findByBulkId(bulkId)
                .ifPresentOrElse(
                    importHistory -> {
                        importHistory.setStatus(status);
                        importHistoryRepository.save(importHistory);
                        log.info("✅ Updated status to {} for bulkId {}", status, bulkId);
                    },
                    () -> log.warn("⚠️ ImportHistory not found for bulkId {}", bulkId)
                );
        } catch (Exception e) {
            log.error("❌ Error updating status: {}", e.getMessage(), e);
        }
    }

    @Async
    @Transactional
    public void deleteAllContactsAsync() {
        try {
            long totalCount = contactRepository.count();
            groupedecontactRepository.deleteAllGroupedecontacts();
            log.info("Deleted all groupedecontacts");
            importHistoryRepository.deleteAllImports();
            contactRepository.deleteAllContacts();
            log.info("Deleted {} contacts", totalCount);
        } catch (Exception e) {
            log.error("Error during async deletion: {}", e.getMessage(), e);
            throw new RuntimeException("Deletion failed", e);
        }
    }
}
