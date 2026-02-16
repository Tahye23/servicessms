package com.example.myproject.repository;

import com.example.myproject.domain.Groupe;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Groupe entity.
 */
@SuppressWarnings("unused")
@Repository
public interface GroupeRepository extends JpaRepository<Groupe, Long> {
    Optional<Groupe> findByGrotitre(String nom);

    @Query("SELECT g FROM Groupe g WHERE g.user_id = :userLogin")
    Page<Groupe> findByUserLogin(@Param("userLogin") String userLogin, Pageable pageable);

    @Query("SELECT g FROM Groupe g WHERE LOWER(g.grotitre) LIKE LOWER(CONCAT('%', :searchTerm, '%')) AND g.user_id = :userLogin")
    Page<Groupe> findByGrotitreContainingIgnoreCaseAndUserLogin(
        @Param("searchTerm") String searchTerm,
        @Param("userLogin") String userLogin,
        Pageable pageable
    );

    // Méthode combinée avec recherche, userLogin et groupType optionnel
    @Query(
        value = """
        SELECT * FROM groupe g
        WHERE (:searchTerm IS NULL OR g.grotitre ILIKE CONCAT('%', :searchTerm, '%'))
          AND (:userLogin IS NULL OR g.user_id = :userLogin)
          AND (:groupType IS NULL OR g.group_type = :groupType)
        ORDER BY g.id DESC
        """,
        countQuery = """
        SELECT count(*) FROM groupe g
        WHERE (:searchTerm IS NULL OR g.grotitre ILIKE CONCAT('%', :searchTerm, '%'))
          AND (:userLogin IS NULL OR g.user_id = :userLogin)
          AND (:groupType IS NULL OR g.group_type = :groupType)
        """,
        nativeQuery = true
    )
    Page<Groupe> findBySearchNative(
        @Param("searchTerm") String searchTerm,
        @Param("userLogin") String userLogin,
        @Param("groupType") String groupType,
        Pageable pageable
    );

    // Recherche avec titre et groupType (admin)
    Page<Groupe> findByGrotitreContainingIgnoreCaseAndGroupType(String searchTerm, String groupType, Pageable pageable);

    // Recherche par titre (admin)
    Page<Groupe> findByGrotitreContainingIgnoreCase(String searchTerm, Pageable pageable);

    @Query("SELECT g FROM Groupe g JOIN Groupedecontact gc ON gc.cgrgroupe = g WHERE gc.contact.id = :contactId")
    List<Groupe> findGroupesByContactId(@Param("contactId") Long contactId);

    // Recherche par groupType (admin)
    Page<Groupe> findByGroupType(String groupType, Pageable pageable);
}
