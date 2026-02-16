package com.example.myproject.service;

import com.example.myproject.domain.PartnershipRequest;
import com.example.myproject.repository.PartnershipRequestRepository;
import com.example.myproject.service.dto.PartnershipRequestDTO;
import com.example.myproject.service.mapper.PartnershipRequestMapper;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PartnershipRequestService {

    private final Logger log = LoggerFactory.getLogger(PartnershipRequestService.class);

    private final PartnershipRequestRepository partnershipRequestRepository;
    private final PartnershipRequestMapper partnershipRequestMapper;

    // Injection optionnelle pour l'envoi d'emails
    // private final MailService mailService;

    public PartnershipRequestService(
        PartnershipRequestRepository partnershipRequestRepository,
        PartnershipRequestMapper partnershipRequestMapper
    ) {
        this.partnershipRequestRepository = partnershipRequestRepository;
        this.partnershipRequestMapper = partnershipRequestMapper;
    }

    /**
     * Sauvegarde une demande de partenariat.
     *
     * @param partnershipRequestDTO l'entité à sauvegarder.
     * @return l'entité persistée.
     */
    public PartnershipRequestDTO save(PartnershipRequestDTO partnershipRequestDTO) {
        log.debug("Demande de sauvegarde PartnershipRequest : {}", partnershipRequestDTO);

        // Vérification des doublons
        if (isDuplicateRequest(partnershipRequestDTO)) {
            throw new RuntimeException("Une demande similaire existe déjà pour cette adresse email et ce plan");
        }

        PartnershipRequest partnershipRequest = partnershipRequestMapper.toEntity(partnershipRequestDTO);
        partnershipRequest.setCreatedDate(Instant.now());
        partnershipRequest.setStatus(PartnershipRequest.RequestStatus.PENDING);

        partnershipRequest = partnershipRequestRepository.save(partnershipRequest);

        // Envoi de notification email (optionnel)
        sendNotificationEmails(partnershipRequest);

        log.info(
            "Nouvelle demande de partenariat créée : ID {}, Email {}, Société {}",
            partnershipRequest.getId(),
            partnershipRequest.getEmail(),
            partnershipRequest.getCompanyName()
        );

        return partnershipRequestMapper.toDto(partnershipRequest);
    }

    /**
     * Met à jour une demande de partenariat.
     */
    public PartnershipRequestDTO update(PartnershipRequestDTO partnershipRequestDTO) {
        log.debug("Demande de mise à jour PartnershipRequest : {}", partnershipRequestDTO);

        PartnershipRequest partnershipRequest = partnershipRequestMapper.toEntity(partnershipRequestDTO);
        partnershipRequest = partnershipRequestRepository.save(partnershipRequest);

        return partnershipRequestMapper.toDto(partnershipRequest);
    }

    /**
     * Récupère toutes les demandes avec pagination.
     */
    @Transactional(readOnly = true)
    public Page<PartnershipRequestDTO> findAll(Pageable pageable) {
        log.debug("Demande de récupération de toutes les PartnershipRequest");
        return partnershipRequestRepository.findAll(pageable).map(partnershipRequestMapper::toDto);
    }

    /**
     * Récupère une demande par son ID.
     */
    @Transactional(readOnly = true)
    public Optional<PartnershipRequestDTO> findOne(Long id) {
        log.debug("Demande de récupération PartnershipRequest : {}", id);
        return partnershipRequestRepository.findById(id).map(partnershipRequestMapper::toDto);
    }

    /**
     * Supprime une demande par son ID.
     */
    public void delete(Long id) {
        log.debug("Demande de suppression PartnershipRequest : {}", id);
        partnershipRequestRepository.deleteById(id);
    }

    /**
     * Trouve les demandes par statut.
     */
    @Transactional(readOnly = true)
    public Page<PartnershipRequestDTO> findByStatus(PartnershipRequest.RequestStatus status, Pageable pageable) {
        log.debug("Demande de récupération PartnershipRequest par statut : {}", status);
        return partnershipRequestRepository.findByStatus(status, pageable).map(partnershipRequestMapper::toDto);
    }

    /**
     * Recherche avancée avec critères multiples.
     */
    @Transactional(readOnly = true)
    public Page<PartnershipRequestDTO> findByMultipleCriteria(
        PartnershipRequest.RequestStatus status,
        String industry,
        String email,
        String companyName,
        Pageable pageable
    ) {
        log.debug(
            "Recherche PartnershipRequest avec critères : status={}, industry={}, email={}, company={}",
            status,
            industry,
            email,
            companyName
        );
        return partnershipRequestRepository
            .findByMultipleCriteria(status, industry, email, companyName, pageable)
            .map(partnershipRequestMapper::toDto);
    }

    /**
     * Approuve une demande.
     */
    public PartnershipRequestDTO approveRequest(Long id, String adminNotes) {
        log.debug("Approbation PartnershipRequest : {}", id);

        return partnershipRequestRepository
            .findById(id)
            .map(request -> {
                request.setStatus(PartnershipRequest.RequestStatus.APPROVED);
                request.setProcessedDate(Instant.now());
                request.setAdminNotes(adminNotes);

                PartnershipRequest savedRequest = partnershipRequestRepository.save(request);

                // Envoi email d'approbation
                sendApprovalEmail(savedRequest);

                log.info("Demande approuvée : ID {}, Email {}", id, request.getEmail());
                return partnershipRequestMapper.toDto(savedRequest);
            })
            .orElseThrow(() -> new RuntimeException("Demande non trouvée avec l'ID : " + id));
    }

    /**
     * Rejette une demande.
     */
    public PartnershipRequestDTO rejectRequest(Long id, String adminNotes) {
        log.debug("Rejet PartnershipRequest : {}", id);

        return partnershipRequestRepository
            .findById(id)
            .map(request -> {
                request.setStatus(PartnershipRequest.RequestStatus.REJECTED);
                request.setProcessedDate(Instant.now());
                request.setAdminNotes(adminNotes);

                PartnershipRequest savedRequest = partnershipRequestRepository.save(request);

                // Envoi email de rejet
                sendRejectionEmail(savedRequest);

                log.info("Demande rejetée : ID {}, Email {}", id, request.getEmail());
                return partnershipRequestMapper.toDto(savedRequest);
            })
            .orElseThrow(() -> new RuntimeException("Demande non trouvée avec l'ID : " + id));
    }

    /**
     * Récupère les statistiques des demandes.
     */
    @Transactional(readOnly = true)
    public PartnershipRequestStats getStatistics() {
        long totalRequests = partnershipRequestRepository.count();
        long pendingRequests = partnershipRequestRepository.countByStatus(PartnershipRequest.RequestStatus.PENDING);
        long approvedRequests = partnershipRequestRepository.countByStatus(PartnershipRequest.RequestStatus.APPROVED);
        long rejectedRequests = partnershipRequestRepository.countByStatus(PartnershipRequest.RequestStatus.REJECTED);

        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        Instant startOfDay = now.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant endOfDay = now.toLocalDate().plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        long todayRequests = partnershipRequestRepository.countTodayRequests(startOfDay, endOfDay);

        return new PartnershipRequestStats(totalRequests, pendingRequests, approvedRequests, rejectedRequests, todayRequests);
    }

    /**
     * Trouve les demandes en attente anciennes.
     */
    @Transactional(readOnly = true)
    public List<PartnershipRequestDTO> findOldPendingRequests(int daysOld) {
        Instant beforeDate = Instant.now().minus(daysOld, ChronoUnit.DAYS);
        return partnershipRequestRepository
            .findPendingRequestsOlderThan(PartnershipRequest.RequestStatus.PENDING, beforeDate)
            .stream()
            .map(partnershipRequestMapper::toDto)
            .toList();
    }

    /**
     * Vérifie s'il s'agit d'une demande en double.
     */
    private boolean isDuplicateRequest(PartnershipRequestDTO dto) {
        return partnershipRequestRepository.existsByEmailIgnoreCaseAndSelectedPlanIdAndStatus(
            dto.getEmail(),
            dto.getSelectedPlanId(),
            PartnershipRequest.RequestStatus.PENDING
        );
    }

    /**
     * Envoie les emails de notification (à implémenter selon vos besoins).
     */
    private void sendNotificationEmails(PartnershipRequest request) {
        // TODO: Implémenter l'envoi d'emails
        // - Email de confirmation au demandeur
        // - Email de notification aux admins
        log.info("Envoi des emails de notification pour la demande ID: {}", request.getId());
    }

    private void sendApprovalEmail(PartnershipRequest request) {
        // TODO: Implémenter l'envoi d'email d'approbation
        log.info("Envoi email d'approbation pour la demande ID: {}", request.getId());
    }

    private void sendRejectionEmail(PartnershipRequest request) {
        // TODO: Implémenter l'envoi d'email de rejet
        log.info("Envoi email de rejet pour la demande ID: {}", request.getId());
    }

    /**
     * Classe pour les statistiques.
     */
    public static class PartnershipRequestStats {

        private final long totalRequests;
        private final long pendingRequests;
        private final long approvedRequests;
        private final long rejectedRequests;
        private final long todayRequests;

        public PartnershipRequestStats(
            long totalRequests,
            long pendingRequests,
            long approvedRequests,
            long rejectedRequests,
            long todayRequests
        ) {
            this.totalRequests = totalRequests;
            this.pendingRequests = pendingRequests;
            this.approvedRequests = approvedRequests;
            this.rejectedRequests = rejectedRequests;
            this.todayRequests = todayRequests;
        }

        // Getters
        public long getTotalRequests() {
            return totalRequests;
        }

        public long getPendingRequests() {
            return pendingRequests;
        }

        public long getApprovedRequests() {
            return approvedRequests;
        }

        public long getRejectedRequests() {
            return rejectedRequests;
        }

        public long getTodayRequests() {
            return todayRequests;
        }
    }
}
