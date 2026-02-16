package com.example.myproject.repository;

import com.example.myproject.domain.ExtendedUser;
import com.example.myproject.domain.User;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the ExtendedUser entity.
 */
@Repository
public interface ExtendedUserRepository extends JpaRepository<ExtendedUser, Integer> {
    Optional<ExtendedUser> findById(Long id);

    @Query("select extendedUser from ExtendedUser extendedUser where extendedUser.user.login = ?#{authentication.name}")
    List<ExtendedUser> findByExtuserIsCurrentUser();

    @Query("SELECT e FROM ExtendedUser e WHERE e.user.login = :userLogin")
    Optional<ExtendedUser> findByUserLogin(@Param("userLogin") String userLogin);

    default Optional<ExtendedUser> findOneWithEagerRelationships(Integer id) {
        return this.findOneWithToOneRelationships(id);
    }

    Optional<ExtendedUser> findByUser(User user);

    default List<ExtendedUser> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<ExtendedUser> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select extendedUser from ExtendedUser extendedUser left join fetch extendedUser.user",
        countQuery = "select count(extendedUser) from ExtendedUser extendedUser"
    )
    Page<ExtendedUser> findAllWithToOneRelationships(Pageable pageable);

    @Query("select extendedUser from ExtendedUser extendedUser left join fetch extendedUser.user")
    List<ExtendedUser> findAllWithToOneRelationships();

    @Query("select extendedUser from ExtendedUser extendedUser left join fetch extendedUser.user where extendedUser.id =:id")
    Optional<ExtendedUser> findOneWithToOneRelationships(@Param("id") Integer id);

    @Query("SELECT e FROM ExtendedUser e WHERE e.user.login = :login")
    Page<ExtendedUser> findByUserLogin(@Param("login") String login, Pageable pageable);

    @Query("SELECT e FROM ExtendedUser e WHERE e.user.login = :login")
    Optional<ExtendedUser> findOneByUserLogin(@Param("login") String login);

    /**
     * Trouve un ExtendedUser par l'ID de l'utilisateur
     */
    @Query("SELECT eu FROM ExtendedUser eu WHERE eu.user.id = :userId")
    ExtendedUser findByUserId(@Param("userId") Long userId);

    @Query("SELECT eu FROM ExtendedUser eu WHERE eu.user.id = :userId")
    Optional<ExtendedUser> findOneByUserId(@Param("userId") Long userId);

    /**
     * Trouve un ExtendedUser par clé API
     */
    ExtendedUser findByApiKey(String apiKey);

    /**
     * Trouve les utilisateurs par nom d'entreprise
     */
    List<ExtendedUser> findByCompanyNameContainingIgnoreCase(String companyName);

    /**
     * Trouve les utilisateurs par pays
     */
    List<ExtendedUser> findByCountry(String country);

    /**
     * Trouve les utilisateurs par ville
     */
    List<ExtendedUser> findByCity(String city);

    /**
     * Trouve les utilisateurs avec des quotas SMS spécifiques
     */
    @Query("SELECT eu FROM ExtendedUser eu WHERE eu.smsQuota >= :minQuota")
    List<ExtendedUser> findUsersWithSmsQuota(@Param("minQuota") Integer minQuota);

    /**
     * Trouve les utilisateurs avec des quotas WhatsApp spécifiques
     */
    @Query("SELECT eu FROM ExtendedUser eu WHERE eu.whatsappQuota >= :minQuota")
    List<ExtendedUser> findUsersWithWhatsappQuota(@Param("minQuota") Integer minQuota);

    /**
     * Trouve les utilisateurs actifs (connectés récemment)
     */
    @Query("SELECT eu FROM ExtendedUser eu WHERE eu.lastLogin >= :since")
    List<ExtendedUser> findActiveUsers(@Param("since") Instant since);

    /**
     * Trouve les utilisateurs par timezone
     */
    List<ExtendedUser> findByTimezone(String timezone);

    /**
     * Trouve les utilisateurs par langue
     */
    List<ExtendedUser> findByLanguage(String language);

    /**
     * Trouve les utilisateurs qui acceptent les emails marketing
     */
    List<ExtendedUser> findByMarketingEmailsTrue();

    /**
     * Trouve les utilisateurs qui acceptent les notifications SMS
     */
    List<ExtendedUser> findByNotificationsSmsTrue();

    /**
     * Statistiques d'usage pour une période
     */
    @Query("SELECT COUNT(eu) FROM ExtendedUser eu WHERE eu.accountCreated >= :startDate " + "AND eu.accountCreated <= :endDate")
    Long countUsersCreatedInPeriod(@Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

    /**
     * Met à jour la dernière connexion
     */
    @Modifying
    @Query("UPDATE ExtendedUser eu SET eu.lastLogin = :lastLogin, eu.loginCount = eu.loginCount + 1 " + "WHERE eu.user.id = :userId")
    void updateLastLogin(@Param("userId") Long userId, @Param("lastLogin") Instant lastLogin);

    /**
     * Met à jour l'usage SMS mensuel
     */
    @Modifying
    @Query("UPDATE ExtendedUser eu SET eu.smsUsedThisMonth = eu.smsUsedThisMonth + :count " + "WHERE eu.user.id = :userId")
    void incrementSmsUsage(@Param("userId") Long userId, @Param("count") Integer count);

    /**
     * Met à jour l'usage WhatsApp mensuel
     */
    @Modifying
    @Query("UPDATE ExtendedUser eu SET eu.whatsappUsedThisMonth = eu.whatsappUsedThisMonth + :count " + "WHERE eu.user.id = :userId")
    void incrementWhatsappUsage(@Param("userId") Long userId, @Param("count") Integer count);

    /**
     * Reset les quotas mensuels pour tous les utilisateurs
     */
    @Modifying
    @Query("UPDATE ExtendedUser eu SET eu.smsUsedThisMonth = 0, eu.whatsappUsedThisMonth = 0, " + "eu.lastQuotaReset = :resetDate")
    void resetAllMonthlyQuotas(@Param("resetDate") java.time.LocalDate resetDate);

    /**
     * Trouve les utilisateurs avec une clé API expirée
     */
    @Query("SELECT eu FROM ExtendedUser eu WHERE eu.apiKeyCreatedDate < :expirationDate " + "AND eu.apiKey IS NOT NULL")
    List<ExtendedUser> findUsersWithExpiredApiKey(@Param("expirationDate") Instant expirationDate);

    /**
     * Trouve les top utilisateurs par nombre de messages envoyés
     */
    @Query("SELECT eu FROM ExtendedUser eu ORDER BY eu.totalMessagesSent DESC")
    List<ExtendedUser> findTopUsersByMessagesSent(org.springframework.data.domain.Pageable pageable);

    /**
     * Trouve les utilisateurs inactifs
     */
    @Query("SELECT eu FROM ExtendedUser eu WHERE eu.lastLogin < :threshold OR eu.lastLogin IS NULL")
    List<ExtendedUser> findInactiveUsers(@Param("threshold") Instant threshold);
}
