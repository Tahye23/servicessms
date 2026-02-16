package com.example.myproject.repository;

import com.example.myproject.domain.PartnershipRequest;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PartnershipRequestRepository extends JpaRepository<PartnershipRequest, Long> {
    /**
     * Trouve toutes les demandes par statut
     */
    List<PartnershipRequest> findByStatus(PartnershipRequest.RequestStatus status);

    /**
     * Trouve toutes les demandes par statut avec pagination
     */
    Page<PartnershipRequest> findByStatus(PartnershipRequest.RequestStatus status, Pageable pageable);

    /**
     * Trouve les demandes par email
     */
    List<PartnershipRequest> findByEmailIgnoreCase(String email);

    /**
     * Trouve les demandes par nom de société
     */
    List<PartnershipRequest> findByCompanyNameContainingIgnoreCase(String companyName);

    /**
     * Trouve les demandes par secteur d'activité
     */
    List<PartnershipRequest> findByIndustryIgnoreCase(String industry);

    /**
     * Trouve les demandes créées après une date donnée
     */
    List<PartnershipRequest> findByCreatedDateAfter(Instant date);

    /**
     * Trouve les demandes créées entre deux dates
     */
    List<PartnershipRequest> findByCreatedDateBetween(Instant startDate, Instant endDate);

    /**
     * Trouve les demandes par plan sélectionné
     */
    List<PartnershipRequest> findBySelectedPlanId(Long planId);

    /**
     * Compte les demandes par statut
     */
    long countByStatus(PartnershipRequest.RequestStatus status);

    /**
     * Compte les demandes créées aujourd'hui
     */
    @Query("SELECT COUNT(pr) FROM PartnershipRequest pr WHERE pr.createdDate >= :startOfDay AND pr.createdDate < :endOfDay")
    long countTodayRequests(@Param("startOfDay") Instant startOfDay, @Param("endOfDay") Instant endOfDay);

    /**
     * Trouve les demandes en attente depuis plus de X jours
     */
    @Query("SELECT pr FROM PartnershipRequest pr WHERE pr.status = :status AND pr.createdDate < :beforeDate")
    List<PartnershipRequest> findPendingRequestsOlderThan(
        @Param("status") PartnershipRequest.RequestStatus status,
        @Param("beforeDate") Instant beforeDate
    );

    /**
     * Recherche avancée avec critères multiples
     */
    @Query(
        "SELECT pr FROM PartnershipRequest pr WHERE " +
        "(:status IS NULL OR pr.status = :status) AND " +
        "(:industry IS NULL OR pr.industry = :industry) AND " +
        "(:email IS NULL OR LOWER(pr.email) LIKE LOWER(CONCAT('%', :email, '%'))) AND " +
        "(:companyName IS NULL OR LOWER(pr.companyName) LIKE LOWER(CONCAT('%', :companyName, '%')))"
    )
    Page<PartnershipRequest> findByMultipleCriteria(
        @Param("status") PartnershipRequest.RequestStatus status,
        @Param("industry") String industry,
        @Param("email") String email,
        @Param("companyName") String companyName,
        Pageable pageable
    );

    /**
     * Statistiques par secteur d'activité
     */
    @Query("SELECT pr.industry, COUNT(pr) FROM PartnershipRequest pr GROUP BY pr.industry ORDER BY COUNT(pr) DESC")
    List<Object[]> getRequestCountByIndustry();

    /**
     * Statistiques par statut
     */
    @Query("SELECT pr.status, COUNT(pr) FROM PartnershipRequest pr GROUP BY pr.status")
    List<Object[]> getRequestCountByStatus();

    /**
     * Trouve les demandes récentes (dernières 24h)
     */
    @Query("SELECT pr FROM PartnershipRequest pr WHERE pr.createdDate >= :since ORDER BY pr.createdDate DESC")
    List<PartnershipRequest> findRecentRequests(@Param("since") Instant since);

    /**
     * Vérifie si une demande existe déjà pour cette email et ce plan
     */
    boolean existsByEmailIgnoreCaseAndSelectedPlanIdAndStatus(String email, Long planId, PartnershipRequest.RequestStatus status);

    /**
     * Trouve la dernière demande d'un utilisateur
     */
    Optional<PartnershipRequest> findFirstByEmailIgnoreCaseOrderByCreatedDateDesc(String email);

    /**
     * Met à jour le statut d'une demande
     */
    @Modifying
    @Query(
        "UPDATE PartnershipRequest pr SET pr.status = :status, pr.processedDate = :processedDate, pr.adminNotes = :notes WHERE pr.id = :id"
    )
    int updateStatus(
        @Param("id") Long id,
        @Param("status") PartnershipRequest.RequestStatus status,
        @Param("processedDate") Instant processedDate,
        @Param("notes") String notes
    );

    /**
     * Supprime les anciennes demandes rejetées (plus de 6 mois)
     */
    @Modifying
    @Query("DELETE FROM PartnershipRequest pr WHERE pr.status = :status AND pr.processedDate < :beforeDate")
    int deleteOldRejectedRequests(@Param("status") PartnershipRequest.RequestStatus status, @Param("beforeDate") Instant beforeDate);
}
