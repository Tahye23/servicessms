package com.example.myproject.repository;

import com.example.myproject.domain.Template;
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
public interface TemplateRepository extends JpaRepository<Template, Long> {
    @Query("SELECT t FROM Template t WHERE t.user_id = :userId")
    List<Template> findAllByUser(@Param("userId") Long userId);

    @Modifying
    @Query(value = "DELETE FROM Template", nativeQuery = true)
    int deleteAllTemplates();

    List<Template> findByApprovedFalse();

    @Query("SELECT t FROM Template t WHERE t.status = :approved")
    Page<Template> findByApproved(@Param("approved") String approved, Pageable pageable);

    @Query("SELECT t FROM Template t WHERE t.status = :approved AND LOWER(t.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    Page<Template> findByApprovedAndNameContainingIgnoreCase(
        @Param("approved") String approved,
        @Param("name") String name,
        Pageable pageable
    );

    @Query("SELECT t FROM Template t WHERE t.user_id = :userId AND t.status = :approved")
    Page<Template> findByUserIdAndApproved(@Param("userId") String userId, @Param("approved") String approved, Pageable pageable);

    @Query(
        "SELECT t FROM Template t WHERE t.user_id = :userId AND t.status = :approved AND LOWER(t.name) LIKE LOWER(CONCAT('%', :name, '%'))"
    )
    Page<Template> findByUserIdAndApprovedAndNameContainingIgnoreCase(
        @Param("userId") String userId,
        @Param("approved") String approved,
        @Param("name") String name,
        Pageable pageable
    );

    @Query("SELECT t FROM Template t WHERE LOWER(t.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    Page<Template> findByNameContainingIgnoreCase(@Param("name") String name, Pageable pageable);

    @Query("SELECT t FROM Template t WHERE t.user_id = :userId AND LOWER(t.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    Page<Template> findByUserIdAndNameContainingIgnoreCase(@Param("userId") String userId, @Param("name") String name, Pageable pageable);

    @Query("SELECT t FROM Template t")
    Page<Template> findAllTemplates(Pageable pageable);

    @Query("SELECT t FROM Template t WHERE t.user_id = :userId")
    Page<Template> findByUserId(@Param("userId") String userId, Pageable pageable);

    @Query("SELECT t.templateId FROM Template t WHERE t.templateId IS NOT NULL")
    List<String> findAllTemplateMetaIds();

    Optional<Template> findByTemplateId(String templateId);

    // Nouvelles méthodes pour le critère isWhatsapp

    @Query(
        """
          SELECT t
            FROM Template t
           WHERE ( :isWhatsapp = TRUE
                   AND t.templateId IS NOT NULL
                   AND t.templateId <> '' )
              OR ( :isWhatsapp = FALSE
                   AND (t.templateId IS NULL OR t.templateId = '') )
        """
    )
    Page<Template> findByWhatsapp(@Param("isWhatsapp") Boolean isWhatsapp, Pageable pageable);

    @Query(
        """
          SELECT t
            FROM Template t
           WHERE (
                  ( :isWhatsapp = TRUE
                    AND t.templateId IS NOT NULL
                    AND t.templateId <> '' )
               OR ( :isWhatsapp = FALSE
                    AND (t.templateId IS NULL OR t.templateId = '') )
           )
             AND LOWER(t.name) LIKE LOWER(CONCAT('%', :name, '%'))
        """
    )
    Page<Template> findByWhatsappAndNameContainingIgnoreCase(
        @Param("isWhatsapp") Boolean isWhatsapp,
        @Param("name") String name,
        Pageable pageable
    );

    @Query(
        """
          SELECT t
            FROM Template t
           WHERE (
                  ( :isWhatsapp = TRUE
                    AND t.templateId IS NOT NULL
                    AND t.templateId <> '' )
               OR ( :isWhatsapp = FALSE
                    AND (t.templateId IS NULL OR t.templateId = '') )
           )
             AND t.status = :approved
        """
    )
    Page<Template> findByWhatsappAndApproved(
        @Param("isWhatsapp") Boolean isWhatsapp,
        @Param("approved") String approved,
        Pageable pageable
    );

    @Query(
        """
          SELECT t
            FROM Template t
           WHERE (
                  ( :isWhatsapp = TRUE
                    AND t.templateId IS NOT NULL
                    AND t.templateId <> '' )
               OR ( :isWhatsapp = FALSE
                    AND (t.templateId IS NULL OR t.templateId = '') )
           )
             AND t.status = :approved
             AND LOWER(t.name) LIKE LOWER(CONCAT('%', :name, '%'))
        """
    )
    Page<Template> findByWhatsappAndApprovedAndNameContainingIgnoreCase(
        @Param("isWhatsapp") Boolean isWhatsapp,
        @Param("approved") String approved,
        @Param("name") String name,
        Pageable pageable
    );

    @Query(
        """
          SELECT t
            FROM Template t
           WHERE t.user_id = :userId
             AND (
                  ( :isWhatsapp = TRUE
                    AND t.templateId IS NOT NULL
                    AND t.templateId <> '' )
               OR ( :isWhatsapp = FALSE
                    AND (t.templateId IS NULL OR t.templateId = '') )
             )
        """
    )
    Page<Template> findByUserIdAndWhatsapp(@Param("userId") String userId, @Param("isWhatsapp") Boolean isWhatsapp, Pageable pageable);

    @Query(
        """
          SELECT t
            FROM Template t
           WHERE t.user_id = :userId
             AND (
                  ( :isWhatsapp = TRUE
                    AND t.templateId IS NOT NULL
                    AND t.templateId <> '' )
               OR ( :isWhatsapp = FALSE
                    AND (t.templateId IS NULL OR t.templateId = '') )
             )
             AND LOWER(t.name) LIKE LOWER(CONCAT('%', :name, '%'))
        """
    )
    Page<Template> findByUserIdAndWhatsappAndNameContainingIgnoreCase(
        @Param("userId") String userId,
        @Param("isWhatsapp") Boolean isWhatsapp,
        @Param("name") String name,
        Pageable pageable
    );

    @Query(
        """
          SELECT t
            FROM Template t
           WHERE t.user_id = :userId
             AND t.status = :approved
             AND (
                  ( :isWhatsapp = TRUE
                    AND t.templateId IS NOT NULL
                    AND t.templateId <> '' )
               OR ( :isWhatsapp = FALSE
                    AND (t.templateId IS NULL OR t.templateId = '') )
             )
        """
    )
    Page<Template> findByUserIdAndApprovedAndWhatsapp(
        @Param("userId") String userId,
        @Param("approved") String approved,
        @Param("isWhatsapp") Boolean isWhatsapp,
        Pageable pageable
    );

    @Query(
        """
          SELECT t
            FROM Template t
           WHERE t.user_id = :userId
             AND t.status = :approved
             AND (
                  ( :isWhatsapp = TRUE
                    AND t.templateId IS NOT NULL
                    AND t.templateId <> '' )
               OR ( :isWhatsapp = FALSE
                    AND (t.templateId IS NULL OR t.templateId = '') )
             )
             AND LOWER(t.name) LIKE LOWER(CONCAT('%', :name, '%'))
        """
    )
    Page<Template> findByUserIdAndApprovedAndWhatsappAndNameContainingIgnoreCase(
        @Param("userId") String userId,
        @Param("approved") String approved,
        @Param("isWhatsapp") Boolean isWhatsapp,
        @Param("name") String name,
        Pageable pageable
    );

    @Query("SELECT t FROM Template t WHERE t.name = :name AND t.user_id = :userLogin")
    Optional<Template> findByNameAndUserLogin(@Param("name") String name, @Param("userLogin") String userLogin);
}
