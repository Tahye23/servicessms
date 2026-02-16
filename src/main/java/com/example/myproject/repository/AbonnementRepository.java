package com.example.myproject.repository;

import com.example.myproject.domain.Abonnement;
import com.example.myproject.domain.Abonnement.SubscriptionStatus;
import com.example.myproject.domain.PlanAbonnement;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Abonnement entity.
 */
@Repository
public interface AbonnementRepository extends JpaRepository<Abonnement, Long>, JpaSpecificationExecutor<Abonnement> {
    Optional<Abonnement> findByUser_Id(Long userId);

    @Query("SELECT a FROM Abonnement a WHERE a.user.id = :userId AND a.status = :status")
    Optional<Abonnement> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") Abonnement.SubscriptionStatus status);

    List<Abonnement> findAllByUserIdAndStatus(Long userId, Abonnement.SubscriptionStatus status);

    // ===== Recherche d’abonnement actif par utilisateur =====
    @Query(
        "SELECT a FROM Abonnement a WHERE a.user.id = :userId AND a.status = 'ACTIVE' AND (a.endDate IS NULL OR a.endDate > CURRENT_DATE)"
    )
    List<Abonnement> findActiveByUserId(@Param("userId") Long userId);

    List<Abonnement> findByUser_User_IdOrderByCreatedDateDesc(Long userId);

    @Query(
        "SELECT a FROM Abonnement a WHERE a.user.id = :userId AND a.status = 'ACTIVE' AND (a.endDate IS NULL OR a.endDate > CURRENT_DATE)"
    )
    Optional<Abonnement> findActivedByUserId(@Param("userId") Long userId);

    @Query("SELECT a FROM Abonnement a WHERE a.user.id = :userId AND a.status = 'ACTIVE' AND a.plan.planType = :planType")
    List<Abonnement> findActiveByUserIdAndPlanType(@Param("userId") Long userId, @Param("planType") PlanAbonnement.PlanType planType);

    @Query("SELECT a FROM Abonnement a WHERE a.endDate < CURRENT_DATE")
    List<Abonnement> findExpiredSubscriptions();

    // ===== Recherche avancée =====
    @Query("SELECT a FROM Abonnement a WHERE a.status = 'ACTIVE'")
    List<Abonnement> findAllActive();

    // Expirant bientôt
    @Query("SELECT a FROM Abonnement a WHERE a.endDate <= :expirationDate AND a.status = 'ACTIVE'")
    List<Abonnement> findExpiringSoon(@Param("expirationDate") LocalDate expirationDate);

    // Déjà expirés
    @Query("SELECT a FROM Abonnement a WHERE a.endDate < CURRENT_DATE AND a.status = 'ACTIVE'")
    List<Abonnement> findExpired();

    // Usage élevé (SMS > 90% quota, WhatsApp > 90% quota)
    @Query(
        "SELECT a FROM Abonnement a WHERE " +
        "((a.smsUsed IS NOT NULL AND a.plan.smsLimit > 0 AND a.smsUsed * 1.0 / a.plan.smsLimit >= 0.9) OR " +
        " (a.whatsappUsed IS NOT NULL AND a.plan.whatsappLimit > 0 AND a.whatsappUsed * 1.0 / a.plan.whatsappLimit >= 0.9))"
    )
    List<Abonnement> findHighUsageAbonnements();

    // Pour reporting
    @Query("SELECT a FROM Abonnement a WHERE a.endDate BETWEEN :startDate AND :endDate")
    List<Abonnement> findCreatedBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    // Abonnements nécessitant une attention (expiration < X jours)
    @Query("SELECT a FROM Abonnement a WHERE a.endDate <= :alertDate AND a.status = 'ACTIVE'")
    List<Abonnement> findRequiringAttention(@Param("alertDate") LocalDate alertDate);

    // Pour l’export complet
    @Query("SELECT a FROM Abonnement a JOIN FETCH a.user JOIN FETCH a.plan")
    List<Abonnement> findAllWithDetails();

    // Par email utilisateur (recherche avancée)
    @Query("SELECT a FROM Abonnement a WHERE LOWER(a.user.user.email) = LOWER(:email)")
    List<Abonnement> findByUserEmail(@Param("email") String email);

    // Par nom du plan (recherche avancée)
    @Query("SELECT a FROM Abonnement a WHERE LOWER(a.plan.abpName) = LOWER(:planName)")
    List<Abonnement> findByPlanName(@Param("planName") String planName);

    // ===== Méthodes d’incrément usage =====
    @Modifying
    @Query(
        "UPDATE Abonnement a SET a.smsUsed = COALESCE(a.smsUsed,0) + 1, a.updatedDate = CURRENT_TIMESTAMP WHERE a.user.id = :userId AND a.status = 'ACTIVE'"
    )
    int incrementSmsUsage(@Param("userId") Long userId);

    @Modifying
    @Query(
        "UPDATE Abonnement a SET a.whatsappUsed = COALESCE(a.whatsappUsed,0) + 1, a.updatedDate = CURRENT_TIMESTAMP WHERE a.user.id = :userId AND a.status = 'ACTIVE'"
    )
    int incrementWhatsappUsage(@Param("userId") Long userId);

    @Modifying
    @Query(
        "UPDATE Abonnement a SET a.apiCallsToday = COALESCE(a.apiCallsToday,0) + 1, a.lastApiCallDate = CURRENT_DATE, a.updatedDate = CURRENT_TIMESTAMP WHERE a.user.id = :userId AND a.status = 'ACTIVE'"
    )
    int incrementApiCallsToday(@Param("userId") Long userId);

    // ===== Méthodes de contrôle d’usage (vérification quota) =====
    @Query(
        "SELECT CASE WHEN (a.plan.smsLimit IS NULL OR a.plan.smsLimit = 0 OR a.plan.smsLimit > a.smsUsed) THEN true ELSE false END FROM Abonnement a WHERE a.user.id = :userId AND a.status = 'ACTIVE'"
    )
    Optional<Boolean> canUserSendSms(@Param("userId") Long userId);

    @Query(
        "SELECT CASE WHEN (a.plan.whatsappLimit IS NULL OR a.plan.whatsappLimit = 0 OR a.plan.whatsappLimit > a.whatsappUsed) THEN true ELSE false END FROM Abonnement a WHERE a.user.id = :userId AND a.status = 'ACTIVE'"
    )
    Optional<Boolean> canUserSendWhatsapp(@Param("userId") Long userId);

    @Query(
        "SELECT CASE WHEN (a.plan.maxApiCallsPerDay IS NULL OR a.plan.maxApiCallsPerDay = 0 OR a.plan.maxApiCallsPerDay > a.apiCallsToday) THEN true ELSE false END FROM Abonnement a WHERE a.user.id = :userId AND a.status = 'ACTIVE'"
    )
    Optional<Boolean> canUserMakeApiCall(@Param("userId") Long userId);

    // ===== Statistiques =====
    @Query("SELECT COUNT(a) FROM Abonnement a WHERE a.status = 'ACTIVE'")
    long countActiveAbonnements();

    @Query("SELECT COUNT(a) FROM Abonnement a WHERE a.status = 'ACTIVE' AND a.createdDate >= FUNCTION('date_trunc', 'month', CURRENT_DATE)")
    long countNewSubscriptionsThisMonth();

    @Query("SELECT a.status, COUNT(a) FROM Abonnement a GROUP BY a.status")
    List<Object[]> countByStatus();

    @Query("SELECT a.plan.abpName, COUNT(a) FROM Abonnement a GROUP BY a.plan.abpName")
    List<Object[]> countByPlan();

    // Revenus par plan (simplifié : à adapter selon ton schéma)
    @Query(
        "SELECT a.plan.abpName, FUNCTION('SUM', a.plan.abpPrice) FROM Abonnement a WHERE a.createdDate >= :startDate AND a.createdDate < :endDate GROUP BY a.plan.abpName"
    )
    List<Object[]> getMonthlyRevenueByPlan(@Param("startDate") java.time.Instant startDate, @Param("endDate") java.time.Instant endDate);

    // Usage SMS par plan
    @Query("SELECT a.plan.abpName, SUM(a.smsUsed) FROM Abonnement a GROUP BY a.plan.abpName")
    List<Object[]> getSmsUsageStatsByPlan();

    // Usage WhatsApp par plan
    @Query("SELECT a.plan.abpName, SUM(a.whatsappUsed) FROM Abonnement a GROUP BY a.plan.abpName")
    List<Object[]> getWhatsappUsageStatsByPlan();

    // ===== Expiration automatique =====
    @Modifying
    @Query("UPDATE Abonnement a SET a.status = 'EXPIRED' WHERE a.status = 'ACTIVE' AND a.endDate < CURRENT_DATE")
    int expirePastDueAbonnements();

    // Expirer les trials
    @Modifying
    @Query("UPDATE Abonnement a SET a.status = 'EXPIRED' WHERE a.status = 'TRIAL' AND a.trialEndDate < CURRENT_DATE")
    int expireTrials();

    // Suspendre/réactiver un abonnement
    @Modifying
    @Query("UPDATE Abonnement a SET a.status = 'SUSPENDED' WHERE a.id = :abonnementId")
    int suspendAbonnement(@Param("abonnementId") Long abonnementId);

    @Modifying
    @Query("UPDATE Abonnement a SET a.status = 'ACTIVE' WHERE a.id = :abonnementId")
    int reactivateAbonnement(@Param("abonnementId") Long abonnementId);

    // Reset mensuel
    @Modifying
    @Query(
        "UPDATE Abonnement a SET a.smsUsed = 0, a.whatsappUsed = 0, a.apiCallsToday = 0, a.updatedDate = CURRENT_TIMESTAMP WHERE a.status = 'ACTIVE'"
    )
    int resetMonthlyUsage();

    // Auto-renouvellement : sélection des abonnements à renouveler (exemple pour 3 jours avant expiration)
    @Query("SELECT a FROM Abonnement a WHERE a.autoRenew = true AND a.status = 'ACTIVE' AND a.endDate <= :renewalDate")
    List<Abonnement> findForAutoRenewal(@Param("renewalDate") LocalDate renewalDate);
}
