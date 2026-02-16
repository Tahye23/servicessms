package com.example.myproject.repository;

import com.example.myproject.domain.User;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the {@link User} entity.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    List<User> findByLastName(String lastName);

    String USERS_BY_LOGIN_CACHE = "usersByLogin";

    String USERS_BY_EMAIL_CACHE = "usersByEmail";
    Optional<User> findOneByActivationKey(String activationKey);
    List<User> findAllByActivatedIsFalseAndActivationKeyIsNotNullAndCreatedDateBefore(Instant dateTime);
    Optional<User> findOneByResetKey(String resetKey);
    Optional<User> findOneByEmailIgnoreCase(String email);
    Optional<User> findOneByLogin(String login);
    Page<User> findAllByExpediteurAndAuthorities_Name(String expediteur, String authority, Pageable pageable);

    @Query(
        value = """
            SELECT DISTINCT u.* FROM jhi_user u
            LEFT JOIN jhi_user_authority a ON u.id = a.user_id
            WHERE (:expediteur IS NULL OR u.expediteur = :expediteur)
              AND (:status IS NULL OR u.activated = :status)
              AND (:role IS NULL OR a.authority_name = :role)
            ORDER BY u.id
        """,
        nativeQuery = true
    )
    Page<User> findByFiltersWithoutSearch(
        @Param("expediteur") String expediteur,
        @Param("status") Boolean status,
        @Param("role") String role,
        Pageable pageable
    );

    @Query(
        value = """
            SELECT DISTINCT u.* FROM jhi_user u
            LEFT JOIN jhi_user_authority a ON u.id = a.user_id
            WHERE (:expediteur IS NULL OR u.expediteur = :expediteur)
              AND (:status IS NULL OR u.activated = :status)
              AND (:role IS NULL OR a.authority_name = :role)
              AND (
                  :search IS NULL OR
                  LOWER(u.login::text) LIKE LOWER(CONCAT('%', :search, '%')) OR
                  LOWER(u.email::text) LIKE LOWER(CONCAT('%', :search, '%')) OR
                  LOWER(u.first_name::text) LIKE LOWER(CONCAT('%', :search, '%')) OR
                  LOWER(u.last_name::text) LIKE LOWER(CONCAT('%', :search, '%'))
              )
            ORDER BY u.id
        """,
        nativeQuery = true
    )
    Page<User> findByFiltersWithSearch(
        @Param("expediteur") String expediteur,
        @Param("status") Boolean status,
        @Param("role") String role,
        @Param("search") String search,
        Pageable pageable
    );

    @EntityGraph(attributePaths = "authorities")
    @Cacheable(cacheNames = USERS_BY_LOGIN_CACHE)
    Optional<User> findOneWithAuthoritiesByLogin(String login);

    @EntityGraph(attributePaths = "authorities")
    @Cacheable(cacheNames = USERS_BY_EMAIL_CACHE)
    Optional<User> findOneWithAuthoritiesByEmailIgnoreCase(String email);

    @Query(
        value = "SELECT DISTINCT jsonb_object_keys(custom_fields::jsonb) as field_name " +
        "FROM jhi_user " +
        "WHERE custom_fields IS NOT NULL " +
        "AND custom_fields::text != '{}' " +
        "AND custom_fields::text != ''",
        nativeQuery = true
    )
    List<String> findDistinctCustomFields();

    Page<User> findAllByIdNotNullAndActivatedIsTrue(Pageable pageable);
}
