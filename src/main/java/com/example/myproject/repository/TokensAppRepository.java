package com.example.myproject.repository;

import com.example.myproject.domain.TokensApp;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Repository pour TokensApp
 *
 * Contient les méthodes de recherche personnalisées pour les tokens
 */
@Repository
public interface TokensAppRepository extends JpaRepository<TokensApp, Integer> {
    // -------------------- EntityGraph pour charger Application --------------------
    @Override
    @EntityGraph(attributePaths = "application")
    List<TokensApp> findAll();

    @Override
    @EntityGraph(attributePaths = "application")
    Optional<TokensApp> findById(Integer id);

    @EntityGraph(attributePaths = "application")
    Optional<TokensApp> findByToken(String token);

    // -------------------- Tokens d'une application --------------------

    @EntityGraph(attributePaths = "application")
    List<TokensApp> findByApplication_IdOrderByCreatedAtDesc(Integer applicationId);

    @EntityGraph(attributePaths = "application")
    List<TokensApp> findByApplication_IdAndActiveTrue(Integer applicationId);

    @EntityGraph(attributePaths = "application")
    List<TokensApp> findByApplication_IdAndActiveFalse(Integer applicationId);

    @EntityGraph(attributePaths = "application")
    Optional<TokensApp> findByApplication_IdAndToken(Integer applicationId, String token);

    // -------------------- Token actif --------------------

    @EntityGraph(attributePaths = "application")
    Optional<TokensApp> findFirstByApplication_IdAndActiveTrueAndIsExpiredFalseOrderByCreatedAtDesc(Integer applicationId);

    boolean existsByApplication_IdAndActiveTrueAndIsExpiredFalse(Integer applicationId);

    // -------------------- Tokens expirant bientôt --------------------

    @EntityGraph(attributePaths = "application")
    List<TokensApp> findByDateExpirationBeforeAndActiveTrueAndIsExpiredFalseOrderByDateExpirationAsc(ZonedDateTime expiringLimit);

    // -------------------- Statistiques --------------------

    long countByApplication_Id(Integer applicationId);

    long countByApplication_IdAndActiveTrue(Integer applicationId);

    long countByApplication_IdAndIsExpiredTrue(Integer applicationId);

    @Query("SELECT COUNT(t) FROM TokensApp t WHERE t.active = true AND t.isExpired = false")
    long countAllActiveTokens();

    // -------------------- Tokens utilisés --------------------

    @Query("SELECT t FROM TokensApp t WHERE t.lastUsedAt BETWEEN :startDate AND :endDate ORDER BY t.lastUsedAt DESC")
    @EntityGraph(attributePaths = "application")
    List<TokensApp> findTokensUsedBetween(@Param("startDate") ZonedDateTime startDate, @Param("endDate") ZonedDateTime endDate);

    @Query("SELECT t FROM TokensApp t WHERE t.createdAt BETWEEN :startDate AND :endDate ORDER BY t.createdAt DESC")
    @EntityGraph(attributePaths = "application")
    List<TokensApp> findTokensCreatedBetween(@Param("startDate") ZonedDateTime startDate, @Param("endDate") ZonedDateTime endDate);

    // -------------------- Tokens expirés --------------------

    @EntityGraph(attributePaths = "application")
    List<TokensApp> findByIsExpiredTrueOrderByDateExpirationDesc();

    @Query("SELECT t FROM TokensApp t WHERE t.lastUsedAt IS NULL ORDER BY t.createdAt DESC")
    @EntityGraph(attributePaths = "application")
    List<TokensApp> findUnusedTokens();

    @Transactional
    @Modifying
    @Query("DELETE FROM TokensApp t WHERE t.isExpired = true")
    void deleteAllExpiredTokens();
}
