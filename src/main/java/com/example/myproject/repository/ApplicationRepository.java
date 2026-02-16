package com.example.myproject.repository;

import com.example.myproject.domain.Application;
import com.example.myproject.domain.Application.Environment;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Application entity.
 *
 * NOTE: Le champ apiToken a été supprimé en faveur de TokensApp.
 * Les tokens sont maintenant gérés par la classe TokensApp avec support
 * de multiples tokens par application et dates d'expiration.
 */
@Repository
public interface ApplicationRepository extends JpaRepository<Application, Integer> {
    /**
     * Trouve toutes les applications actives
     */
    List<Application> findByIsActiveTrue();

    /**
     * Trouve les applications par utilisateur
     */
    List<Application> findByUtilisateurId(Integer utilisateurId);

    /**
     * Trouve les applications par environnement
     */
    List<Application> findByEnvironment(Environment environment);

    /**
     * Trouve les applications par nom (recherche partielle)
     */
    @Query("SELECT a FROM Application a WHERE LOWER(a.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Application> findByNameContainingIgnoreCase(@Param("name") String name);

    /**
     * Compte le nombre d'applications actives
     */
    @Query("SELECT COUNT(a) FROM Application a WHERE a.isActive = true")
    Long countActiveApplications();

    /**
     * Trouve les applications qui ont atteint leur limite journalière
     */
    @Query("SELECT a FROM Application a WHERE a.dailyLimit IS NOT NULL AND a.currentDailyUsage >= a.dailyLimit")
    List<Application> findApplicationsAtDailyLimit();

    @Query(
        """
            select distinct a from Application a
            left join fetch a.tokens
        """
    )
    List<Application> findAllWithTokens();

    @EntityGraph(attributePaths = { "tokens", "allowedServices", "api", "planabonnement", "utilisateur" })
    Optional<Application> findWithAllById(Integer id);

    /**
     * Trouve les applications qui ont atteint leur limite mensuelle
     */
    @Query("SELECT a FROM Application a WHERE a.monthlyLimit IS NOT NULL AND a.currentMonthlyUsage >= a.monthlyLimit")
    List<Application> findApplicationsAtMonthlyLimit();

    /**
     * Calcule le total des appels API pour toutes les applications
     */
    @Query("SELECT COALESCE(SUM(a.totalApiCalls), 0) FROM Application a")
    Long getTotalApiCalls();

    /**
     * Calcule le total des appels API d'aujourd'hui
     */
    @Query("SELECT COALESCE(SUM(a.currentDailyUsage), 0) FROM Application a")
    Long getTodayApiCalls();

    /**
     * Calcule le total des appels API du mois
     */
    @Query("SELECT COALESCE(SUM(a.currentMonthlyUsage), 0) FROM Application a")
    Long getMonthlyApiCalls();

    /**
     * Trouve les applications créées après une date donnée
     */
    List<Application> findByCreatedAtAfter(Instant date);

    /**
     * Trouve les applications avec des webhooks configurés
     */
    @Query("SELECT a FROM Application a WHERE a.webhookUrl IS NOT NULL AND a.webhookUrl != ''")
    List<Application> findApplicationsWithWebhooks();

    /**
     * Met à jour le statut d'une application
     */
    @Modifying
    @Query("UPDATE Application a SET a.isActive = :isActive, a.updatedAt = :updatedAt WHERE a.id = :id")
    int updateApplicationStatus(@Param("id") Integer id, @Param("isActive") Boolean isActive, @Param("updatedAt") Instant updatedAt);

    /**
     * Remet à zéro les compteurs journaliers pour toutes les applications
     */
    @Modifying
    @Query("UPDATE Application a SET a.currentDailyUsage = 0")
    int resetAllDailyUsage();

    /**
     * Remet à zéro les compteurs mensuels pour toutes les applications
     */
    @Modifying
    @Query("UPDATE Application a SET a.currentMonthlyUsage = 0")
    int resetAllMonthlyUsage();

    /**
     * Remet à zéro les compteurs pour une application spécifique
     */
    @Modifying
    @Query("UPDATE Application a SET a.currentDailyUsage = 0, a.currentMonthlyUsage = 0 WHERE a.id = :id")
    int resetApplicationCounters(@Param("id") Integer id);

    /**
     * Incrémente le compteur d'appels API pour une application
     */
    @Modifying
    @Query(
        "UPDATE Application a SET a.totalApiCalls = a.totalApiCalls + 1, a.currentDailyUsage = a.currentDailyUsage + 1, a.currentMonthlyUsage = a.currentMonthlyUsage + 1, a.lastApiCall = :now WHERE a.id = :id"
    )
    int incrementApiCallCount(@Param("id") Integer id, @Param("now") Instant now);

    /**
     * Recherche avancée avec filtres multiples
     */
    @Query(
        "SELECT a FROM Application a WHERE " +
        "(:name IS NULL OR LOWER(a.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
        "(:environment IS NULL OR a.environment = :environment) AND " +
        "(:isActive IS NULL OR a.isActive = :isActive) AND " +
        "(:userId IS NULL OR a.utilisateur.id = :userId)"
    )
    List<Application> findWithFilters(
        @Param("name") String name,
        @Param("environment") Environment environment,
        @Param("isActive") Boolean isActive,
        @Param("userId") Integer userId
    );

    /**
     * Trouve les applications les plus utilisées
     */
    @Query("SELECT a FROM Application a ORDER BY a.totalApiCalls DESC")
    List<Application> findTopUsedApplications();

    /**
     * Vérifie si un nom d'application existe déjà pour un utilisateur
     */
    @Query(
        "SELECT COUNT(a) > 0 FROM Application a WHERE LOWER(a.name) = LOWER(:name) AND a.utilisateur.id = :userId AND (:id IS NULL OR a.id != :id)"
    )
    boolean existsByNameAndUtilisateurIdAndIdNot(@Param("name") String name, @Param("userId") Integer userId, @Param("id") Integer id);

    /**
     * Trouve une application par son token (via la relation TokensApp)
     * Alternative à findByApiToken()
     */
    @Query("SELECT t.application FROM TokensApp t WHERE t.token = :token AND t.active = true")
    Optional<Application> findByActiveToken(@Param("token") String token);
}
