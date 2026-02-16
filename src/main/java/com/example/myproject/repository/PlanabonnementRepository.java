package com.example.myproject.repository;

import com.example.myproject.domain.PlanAbonnement;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the PlanAbonnement entity.
 */
@SuppressWarnings("unused")
@Repository
public interface PlanabonnementRepository extends JpaRepository<PlanAbonnement, Long> {
    // ===== MÉTHODES DE BASE PAR STATUT ACTIF =====
    // Dans PlanAbonnementRepository.java
    // Dans PlanAbonnementRepository.java
    Optional<PlanAbonnement> findByAbpNameIgnoreCase(String abpName);
    Optional<PlanAbonnement> findByPlanType(PlanAbonnement.PlanType planType);

    /**
     * Trouve tous les plans actifs triés par ordre de tri
     */
    List<PlanAbonnement> findByActiveTrueOrderBySortOrderAsc();

    /**
     * Trouve tous les plans inactifs triés par ordre de tri
     */
    List<PlanAbonnement> findByActiveFalseOrderBySortOrderAsc();

    // ===== MÉTHODES PAR TYPE DE PLAN =====

    /**
     * Trouve les plans par type et actifs
     */
    List<PlanAbonnement> findByPlanTypeAndActiveTrueOrderBySortOrderAsc(PlanAbonnement.PlanType planType);

    /**
     * Trouve tous les plans par type (actifs et inactifs)
     */
    List<PlanAbonnement> findByPlanTypeOrderBySortOrderAsc(PlanAbonnement.PlanType planType);

    // ===== MÉTHODES PAR POPULARITÉ =====

    /**
     * Trouve les plans populaires et actifs
     */
    List<PlanAbonnement> findByAbpPopularTrueAndActiveTrueOrderBySortOrderAsc();

    /**
     * Trouve le premier plan populaire
     */
    Optional<PlanAbonnement> findFirstByAbpPopularTrueAndActiveTrueOrderBySortOrderAsc();

    // ===== MÉTHODES PAR NOM =====

    /**
     * Trouve un plan par nom exact (actif)
     */
    Optional<PlanAbonnement> findByAbpNameAndActiveTrue(String abpName);

    /**
     * Vérifie si un nom de plan existe déjà (pour éviter les doublons)
     */
    boolean existsByAbpNameIgnoreCase(String abpName);

    // ===== MÉTHODES PAR PRIX AVEC REQUÊTES NATIVES =====

    /**
     * Trouve les plans gratuits (prix = 0 ou null)
     */
    @Query(
        value = "SELECT * FROM plan_abonnement WHERE (abp_price = 0 OR abp_price IS NULL) " + "AND active = true ORDER BY sort_order ASC",
        nativeQuery = true
    )
    List<PlanAbonnement> findFreePlansOrderBySortOrderAsc();

    /**
     * Trouve les plans payants (prix > 0)
     */
    @Query(
        value = "SELECT * FROM plan_abonnement WHERE abp_price > 0 AND active = true " + "ORDER BY abp_price ASC, sort_order ASC",
        nativeQuery = true
    )
    List<PlanAbonnement> findPaidPlansOrderByPriceAsc();

    /**
     * Trouve les plans par gamme de prix
     */
    @Query(
        value = "SELECT * FROM plan_abonnement WHERE abp_price >= :minPrice " +
        "AND abp_price <= :maxPrice AND active = true ORDER BY abp_price ASC",
        nativeQuery = true
    )
    List<PlanAbonnement> findByPriceRange(@Param("minPrice") BigDecimal minPrice, @Param("maxPrice") BigDecimal maxPrice);

    // ===== MÉTHODES PAR LIMITES =====

    /**
     * Trouve les plans avec limite SMS supérieure ou égale
     */
    @Query(
        value = "SELECT * FROM plan_abonnement WHERE (sms_limit >= :minSms OR sms_limit = -1) " +
        "AND active = true ORDER BY sms_limit DESC, sort_order ASC",
        nativeQuery = true
    )
    List<PlanAbonnement> findPlansWithSmsLimit(@Param("minSms") Integer minSms);

    /**
     * Trouve les plans avec limite WhatsApp supérieure ou égale
     */
    @Query(
        value = "SELECT * FROM plan_abonnement WHERE (whatsapp_limit >= :minWhatsapp OR whatsapp_limit = -1) " +
        "AND active = true ORDER BY whatsapp_limit DESC, sort_order ASC",
        nativeQuery = true
    )
    List<PlanAbonnement> findPlansWithWhatsappLimit(@Param("minWhatsapp") Integer minWhatsapp);

    /**
     * Trouve les plans avec des limites illimitées
     */
    @Query(
        value = "SELECT * FROM plan_abonnement WHERE (sms_limit = -1 OR whatsapp_limit = -1 OR " +
        "users_limit = -1 OR templates_limit = -1 OR max_api_calls_per_day = -1) " +
        "AND active = true ORDER BY sort_order ASC",
        nativeQuery = true
    )
    List<PlanAbonnement> findUnlimitedPlansOrderBySortOrderAsc();

    // ===== MÉTHODES PAR PERMISSIONS =====

    /**
     * Trouve les plans permettant la gestion d'utilisateurs
     */
    List<PlanAbonnement> findByCanManageUsersTrueAndActiveTrueOrderBySortOrderAsc();

    /**
     * Trouve les plans permettant la gestion de templates
     */
    List<PlanAbonnement> findByCanManageTemplatesTrueAndActiveTrueOrderBySortOrderAsc();

    /**
     * Trouve les plans avec accès aux analytics
     */
    List<PlanAbonnement> findByCanViewAnalyticsTrueAndActiveTrueOrderBySortOrderAsc();

    /**
     * Trouve les plans avec support prioritaire
     */
    List<PlanAbonnement> findByPrioritySupportTrueAndActiveTrueOrderBySortOrderAsc();

    // ===== MÉTHODES DE RECHERCHE AVANCÉE =====

    /**
     * Trouve les plans recommandés selon les besoins
     */
    @Query(
        value = "SELECT * FROM plan_abonnement WHERE " +
        "(sms_limit = -1 OR sms_limit >= :smsNeeded) AND " +
        "(whatsapp_limit = -1 OR whatsapp_limit >= :whatsappNeeded) AND " +
        "(users_limit = -1 OR users_limit >= :usersNeeded) AND " +
        "active = true ORDER BY abp_price ASC, sort_order ASC",
        nativeQuery = true
    )
    List<PlanAbonnement> findRecommendedPlans(
        @Param("smsNeeded") Integer smsNeeded,
        @Param("whatsappNeeded") Integer whatsappNeeded,
        @Param("usersNeeded") Integer usersNeeded
    );

    /**
     * Recherche par nom (contient le texte)
     */
    @Query(
        value = "SELECT * FROM plan_abonnement WHERE LOWER(abp_name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
        "ORDER BY sort_order ASC",
        nativeQuery = true
    )
    List<PlanAbonnement> findByNameContainingIgnoreCase(@Param("searchTerm") String searchTerm);

    // ===== MÉTHODES STATISTIQUES =====

    /**
     * Compte les plans actifs par type
     */
    @Query(value = "SELECT COUNT(*) FROM plan_abonnement WHERE plan_type = :planType AND active = true", nativeQuery = true)
    Long countActiveByPlanType(@Param("planType") String planType);

    /**
     * Compte les plans actifs
     */
    @Query(value = "SELECT COUNT(*) FROM plan_abonnement WHERE active = true", nativeQuery = true)
    Long countActivePlans();

    // ===== MÉTHODES DE MISE À JOUR =====

    /**
     * Met à jour le statut actif d'un plan
     */
    @Modifying
    @Query(value = "UPDATE plan_abonnement SET active = :active WHERE id = :id", nativeQuery = true)
    int updateActiveStatus(@Param("id") Long id, @Param("active") Boolean active);

    /**
     * Met à jour l'ordre de tri d'un plan
     */
    @Modifying
    @Query(value = "UPDATE plan_abonnement SET sort_order = :sortOrder WHERE id = :id", nativeQuery = true)
    int updateSortOrder(@Param("id") Long id, @Param("sortOrder") Integer sortOrder);

    /**
     * Met à jour le statut populaire d'un plan
     */
    @Modifying
    @Query(value = "UPDATE plan_abonnement SET abp_popular = :popular WHERE id = :id", nativeQuery = true)
    int updatePopularStatus(@Param("id") Long id, @Param("popular") Boolean popular);

    /**
     * Désactive tous les plans populaires
     */
    @Modifying
    @Query(value = "UPDATE plan_abonnement SET abp_popular = false", nativeQuery = true)
    int resetAllPopularStatus();

    // ===== MÉTHODES DE GESTION DES ORDRES =====

    /**
     * Trouve le prochain ordre d'affichage disponible
     */
    @Query(value = "SELECT COALESCE(MAX(sort_order), 0) + 1 FROM plan_abonnement", nativeQuery = true)
    Integer findNextSortOrder();

    /**
     * Réorganise les ordres après suppression
     */
    @Modifying
    @Query(value = "UPDATE plan_abonnement SET sort_order = sort_order - 1 WHERE sort_order > :deletedOrder", nativeQuery = true)
    int reorderAfterDeletion(@Param("deletedOrder") Integer deletedOrder);

    /**
     * Trouve les plans avec un ordre d'affichage supérieur
     */
    List<PlanAbonnement> findBySortOrderGreaterThanOrderBySortOrderAsc(Integer sortOrder);

    // ===== MÉTHODES UTILITAIRES =====

    /**
     * Trouve les plans par période de facturation
     */
    List<PlanAbonnement> findByAbpPeriodAndActiveTrueOrderByAbpPriceAscSortOrderAsc(String period);

    /**
     * Trouve les plans par devise
     */
    List<PlanAbonnement> findByAbpCurrencyAndActiveTrueOrderBySortOrderAsc(String currency);

    /**
     * Recherche full-text dans nom et description
     */
    @Query(
        value = "SELECT * FROM plan_abonnement WHERE " +
        "(LOWER(abp_name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
        "LOWER(abp_description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) AND " +
        "active = true ORDER BY sort_order ASC",
        nativeQuery = true
    )
    List<PlanAbonnement> searchInNameAndDescription(@Param("searchTerm") String searchTerm);

    // ===== MÉTHODES SPÉCIALES =====

    /**
     * Trouve le plan gratuit par défaut
     */
    @Query(
        value = "SELECT * FROM plan_abonnement WHERE plan_type = 'FREE' " + "AND active = true ORDER BY sort_order ASC LIMIT 1",
        nativeQuery = true
    )
    Optional<PlanAbonnement> findDefaultFreePlan();

    /**
     * Vérifie si un nom de plan existe, excluant un ID spécifique
     */
    @Query(
        value = "SELECT CASE WHEN COUNT(*) > 0 THEN true ELSE false END FROM plan_abonnement " +
        "WHERE LOWER(abp_name) = LOWER(:name) AND id != :excludeId",
        nativeQuery = true
    )
    boolean existsByAbpNameIgnoreCaseAndIdNot(@Param("name") String abpName, @Param("excludeId") Long excludeId);

    /**
     * Trouve le plan le moins cher actif
     */
    @Query(
        value = "SELECT * FROM plan_abonnement WHERE abp_price > 0 AND active = true " + "ORDER BY abp_price ASC LIMIT 5",
        nativeQuery = true
    )
    List<PlanAbonnement> findCheapestPlans();

    /**
     * Statistiques sur les prix
     */
    @Query(
        value = "SELECT MIN(abp_price), MAX(abp_price), AVG(abp_price) FROM plan_abonnement " + "WHERE abp_price > 0 AND active = true",
        nativeQuery = true
    )
    Object[] getPriceStatistics();
}
