package com.example.myproject.repository;

import com.example.myproject.domain.Contact;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring Data JPA repository for the Contact entity.
 */
@SuppressWarnings("unused")
@Repository
public interface ContactRepository extends JpaRepository<Contact, Long> {
    boolean existsBycontelephone(String contelephone);
    List<Contact> findBycontelephone(String contelephone);
    List<Contact> findByStatuttraitement(Integer statutTraitement);
    Optional<Contact> findByContelephone(String contelephone);

    @Query(
        value = "SELECT DISTINCT jsonb_object_keys(custom_fields::jsonb) as field_name " +
        "FROM contact " +
        "WHERE custom_fields IS NOT NULL " +
        "AND custom_fields::text != '{}' " +
        "AND custom_fields::text != ''",
        nativeQuery = true
    )
    List<String> findDistinctCustomFields();

    @Query("SELECT c FROM Contact c WHERE c.contelephone = :contelephone AND c.user_login = :userLogin")
    Optional<Contact> findByContelephoneAndUser_login(@Param("contelephone") String contelephone, @Param("userLogin") String userLogin);

    // Récupérer les IDs des contacts d'un groupe
    @Query("SELECT DISTINCT c.id FROM Contact c JOIN c.groupedecontacts gc WHERE gc.cgrgroupe.id = :groupeId")
    List<Long> findContactIdsByGroupeId(@Param("groupeId") Long groupeId);

    // Supprimer les contacts par leurs IDs
    @Modifying
    @Transactional
    @Query("DELETE FROM Contact c WHERE c.id IN :ids")
    int deleteByIdIn(@Param("ids") List<Long> ids);

    @Query("SELECT c FROM Contact c WHERE c.progressId = :progressId ORDER BY c.id DESC LIMIT :limit")
    List<Contact> findByProgressIdOrderByIdDesc(@Param("progressId") String progressId, @Param("limit") int limit);

    @Query("SELECT c FROM Contact c WHERE c.user_login = :userLogin")
    List<Contact> findAllByUserLogin(@Param("userLogin") String userLogin);

    @Modifying
    @Query(value = "DELETE FROM contact", nativeQuery = true)
    void deleteAllContacts();

    boolean existsByProgressId(@Param("progressId") String progressId);

    @Query(
        "SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Contact c WHERE c.contelephone = :contelephone AND c.user_login = :userLogin"
    )
    boolean existsByContelephoneAndUserLogin(@Param("contelephone") String contelephone, @Param("userLogin") String userLogin);

    @Query("SELECT c.contelephone FROM Contact c WHERE c.user_login = :userLogin")
    Set<String> findAllPhoneNumbersByUserLogin(@Param("userLogin") String userLogin);

    List<Contact> findByGroupeId(Long groupeId);
    Page<Contact> findByProgressId(String progressId, Pageable pageable);

    @Query(
        "SELECT c FROM Contact c " +
        "WHERE LOWER(c.connom) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
        "   OR LOWER(c.conprenom) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
        "   OR LOWER(c.contelephone) LIKE LOWER(CONCAT('%', :searchTerm, '%'))"
    )
    Page<Contact> searchContacts(@Param("searchTerm") String searchTerm, Pageable pageable);

    // Récupérer les contacts appartenant à l'utilisateur connecté
    @Query("SELECT c FROM Contact c WHERE c.user_login = :userLogin")
    Page<Contact> findByUserLogin(@Param("userLogin") String userLogin, Pageable pageable);

    // Recherche avec filtrage par userLogin
    @Query(
        "SELECT c FROM Contact c " +
        "WHERE c.user_login = :userLogin " +
        "  AND (LOWER(c.connom) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
        "       OR LOWER(c.conprenom) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
        "       OR LOWER(c.contelephone) LIKE LOWER(CONCAT('%', :searchTerm, '%')))"
    )
    Page<Contact> findByUserLoginAndSearch(@Param("userLogin") String userLogin, @Param("searchTerm") String searchTerm, Pageable pageable);

    // ContactRepository.java - Méthodes ajoutées pour la performance

    /**
     * 🚀 OPTIMISATION : Charger contacts existants par téléphones (batch)
     */
    @Query("SELECT c FROM Contact c WHERE c.contelephone IN :phones AND c.user_login = :userLogin")
    List<Contact> findByContelephoneInAndUserLogin(@Param("phones") Set<String> phones, @Param("userLogin") String userLogin);

    /**
     * 🚀 OPTIMISATION : Version sans userLogin pour compatibilité
     */
    @Query("SELECT c FROM Contact c WHERE c.contelephone IN :phones")
    List<Contact> findByContelephoneIn(@Param("phones") Set<String> phones);

    /**
     * 🚀 STATISTIQUES : Compter par progressId et statut
     */
    @Query("SELECT c.statuttraitement, COUNT(c) FROM Contact c WHERE c.progressId = :progressId GROUP BY c.statuttraitement")
    List<Object[]> countByProgressIdAndStatuttraitement(@Param("progressId") String progressId);

    /**
     * 🚀 OPTIMISATION : Batch insert optimisé
     */
    @Modifying
    @Query(
        value = """
        INSERT INTO contact (connom, conprenom, contelephone, user_login, statuttraitement, progress_id, custom_fields)
        VALUES (:connom, :conprenom, :contelephone, :userLogin, :statuttraitement, :progressId, :customFields)
        """,
        nativeQuery = true
    )
    void batchInsertContact(
        @Param("connom") String connom,
        @Param("conprenom") String conprenom,
        @Param("contelephone") String contelephone,
        @Param("userLogin") String userLogin,
        @Param("statuttraitement") Integer statuttraitement,
        @Param("progressId") String progressId,
        @Param("customFields") String customFields
    );

    /**
     * 🆕 FILTRES AVANCÉS POUR GROUPES
     */
    @Query(
        value = """
        SELECT DISTINCT c.* FROM contact c
        INNER JOIN groupedecontact gc ON c.id = gc.contact_id
        WHERE gc.cgrgroupe_id = :groupeId
        AND (:nom IS NULL OR LOWER(c.connom) LIKE LOWER(CONCAT('%', :nom, '%')))
        AND (:prenom IS NULL OR LOWER(c.conprenom) LIKE LOWER(CONCAT('%', :prenom, '%')))
        AND (:telephone IS NULL OR c.contelephone LIKE CONCAT('%', :telephone, '%'))
        AND (:statut IS NULL OR c.statuttraitement = :statut)
        AND (:hasWhatsapp IS NULL OR c.has_whatsapp = :hasWhatsapp)
        AND (:minSmsSent IS NULL OR COALESCE(c.total_sms_sent, 0) >= :minSmsSent)
        AND (:maxSmsSent IS NULL OR COALESCE(c.total_sms_sent, 0) <= :maxSmsSent)
        AND (:minWhatsappSent IS NULL OR COALESCE(c.total_whatsapp_sent, 0) >= :minWhatsappSent)
        AND (:maxWhatsappSent IS NULL OR COALESCE(c.total_whatsapp_sent, 0) <= :maxWhatsappSent)
        AND (:hasReceivedMessages IS NULL OR
             CASE WHEN :hasReceivedMessages = true THEN
                  (COALESCE(c.total_sms_sent, 0) + COALESCE(c.total_whatsapp_sent, 0)) > 0
             ELSE (COALESCE(c.total_sms_sent, 0) + COALESCE(c.total_whatsapp_sent, 0)) = 0 END)
        ORDER BY c.id DESC
        """,
        nativeQuery = true
    )
    Page<Contact> findContactsByGroupeIdWithAdvancedFilters(
        @Param("groupeId") Long groupeId,
        @Param("nom") String nom,
        @Param("prenom") String prenom,
        @Param("telephone") String telephone,
        @Param("statut") Integer statut,
        @Param("hasWhatsapp") Boolean hasWhatsapp,
        @Param("minSmsSent") Integer minSmsSent,
        @Param("maxSmsSent") Integer maxSmsSent,
        @Param("minWhatsappSent") Integer minWhatsappSent,
        @Param("maxWhatsappSent") Integer maxWhatsappSent,
        @Param("hasReceivedMessages") Boolean hasReceivedMessages,
        Pageable pageable
    );

    /**
     * 🆕 STATISTIQUES DE MESSAGES POUR UN GROUPE
     */
    @Query(
        value = """

        SELECT
            COUNT(c.id) as totalContacts,
            SUM(CASE WHEN c.has_whatsapp = true THEN 1 ELSE 0 END) as contactsWithWhatsapp,
            SUM(COALESCE(c.total_sms_sent, 0)) as totalSmsSent,
            SUM(COALESCE(c.total_whatsapp_sent, 0)) as totalWhatsappSent,
            SUM(COALESCE(c.total_sms_success, 0)) as totalSmsSuccess,
            SUM(COALESCE(c.total_sms_failed, 0)) as totalSmsFailed,
            SUM(COALESCE(c.total_whatsapp_success, 0)) as totalWhatsappSuccess,
            SUM(COALESCE(c.total_whatsapp_failed, 0)) as totalWhatsappFailed
        FROM contact c
        INNER JOIN groupedecontact gc ON c.id = gc.contact_id
        WHERE gc.cgrgroupe_id = :groupeId
        """,
        nativeQuery = true
    )
    List<Object[]> getMessageStatisticsByGroupeId(@Param("groupeId") Long groupeId);

    @Query(
        value = """
        SELECT DISTINCT c.*
        FROM contact c
        WHERE (CAST(:userLogin AS text) IS NULL OR c.user_login = CAST(:userLogin AS text))

          -- Filtres textuels en AND
          AND (:nomLike IS NULL OR c.connom ILIKE CAST(:nomLike AS text))
          AND (:prenomLike IS NULL OR c.conprenom ILIKE CAST(:prenomLike AS text))
          AND (:telephoneLike IS NULL OR
               REPLACE(REPLACE(REPLACE(c.contelephone,' ',''),'+',''),'-','')
               ILIKE REPLACE(REPLACE(REPLACE(CAST(:telephoneLike AS text),' ',''),'+',''),'-',''))

          -- Recherche globale (optionnelle, en OR)
          AND (
            CAST(:searchLike AS text) IS NULL
            OR c.connom ILIKE CAST(:searchLike AS text)
            OR c.conprenom ILIKE CAST(:searchLike AS text)
            OR REPLACE(REPLACE(REPLACE(c.contelephone,' ',''),'+',''),'-','')
               ILIKE REPLACE(REPLACE(REPLACE(CAST(:searchLike AS text),' ',''),'+',''),'-','')
          )

          -- Autres filtres
          AND (:statut IS NULL OR c.statuttraitement = :statut)
          AND (:hasWhatsapp IS NULL OR c.has_whatsapp = :hasWhatsapp)
          AND (:minSmsSent IS NULL OR COALESCE(c.total_sms_sent, 0) >= :minSmsSent)
          AND (:maxSmsSent IS NULL OR COALESCE(c.total_sms_sent, 0) <= :maxSmsSent)
          AND (:minWhatsappSent IS NULL OR COALESCE(c.total_whatsapp_sent, 0) >= :minWhatsappSent)
          AND (:maxWhatsappSent IS NULL OR COALESCE(c.total_whatsapp_sent, 0) <= :maxWhatsappSent)
          AND (
            CAST(:hasReceivedMessages AS boolean) IS NULL
            OR (CAST(:hasReceivedMessages AS boolean) = TRUE AND (COALESCE(c.total_sms_sent,0) + COALESCE(c.total_whatsapp_sent,0)) > 0)
            OR (CAST(:hasReceivedMessages AS boolean) = FALSE AND (COALESCE(c.total_sms_sent,0) + COALESCE(c.total_whatsapp_sent,0)) = 0)
          )
        ORDER BY c.id DESC
        """,
        countQuery = """
        SELECT COUNT(DISTINCT c.id)
        FROM contact c
        WHERE (CAST(:userLogin AS text) IS NULL OR c.user_login = CAST(:userLogin AS text))
          AND (:nomLike IS NULL OR c.connom ILIKE CAST(:nomLike AS text))
          AND (:prenomLike IS NULL OR c.conprenom ILIKE CAST(:prenomLike AS text))
          AND (:telephoneLike IS NULL OR
               REPLACE(REPLACE(REPLACE(c.contelephone,' ',''),'+',''),'-','')
               ILIKE REPLACE(REPLACE(REPLACE(CAST(:telephoneLike AS text),' ',''),'+',''),'-',''))
          AND (
            CAST(:searchLike AS text) IS NULL
            OR c.connom ILIKE CAST(:searchLike AS text)
            OR c.conprenom ILIKE CAST(:searchLike AS text)
            OR REPLACE(REPLACE(REPLACE(c.contelephone,' ',''),'+',''),'-','')
               ILIKE REPLACE(REPLACE(REPLACE(CAST(:searchLike AS text),' ',''),'+',''),'-','')
          )
          AND (:statut IS NULL OR c.statuttraitement = :statut)
          AND (:hasWhatsapp IS NULL OR c.has_whatsapp = :hasWhatsapp)
          AND (:minSmsSent IS NULL OR COALESCE(c.total_sms_sent, 0) >= :minSmsSent)
          AND (:maxSmsSent IS NULL OR COALESCE(c.total_sms_sent, 0) <= :maxSmsSent)
          AND (:minWhatsappSent IS NULL OR COALESCE(c.total_whatsapp_sent, 0) >= :minWhatsappSent)
          AND (:maxWhatsappSent IS NULL OR COALESCE(c.total_whatsapp_sent, 0) <= :maxWhatsappSent)
          AND (
            CAST(:hasReceivedMessages AS boolean) IS NULL
            OR (CAST(:hasReceivedMessages AS boolean) = TRUE AND (COALESCE(c.total_sms_sent,0) + COALESCE(c.total_whatsapp_sent,0)) > 0)
            OR (CAST(:hasReceivedMessages AS boolean) = FALSE AND (COALESCE(c.total_sms_sent,0) + COALESCE(c.total_whatsapp_sent,0)) = 0)
          )
        """,
        nativeQuery = true
    )
    Page<Contact> findAllWithAdvancedFilters_like(
        @Param("userLogin") String userLogin,
        @Param("nomLike") String nomLike,
        @Param("prenomLike") String prenomLike,
        @Param("telephoneLike") String telephoneLike,
        @Param("searchLike") String searchLike,
        @Param("statut") Integer statut,
        @Param("hasWhatsapp") Boolean hasWhatsapp,
        @Param("minSmsSent") Integer minSmsSent,
        @Param("maxSmsSent") Integer maxSmsSent,
        @Param("minWhatsappSent") Integer minWhatsappSent,
        @Param("maxWhatsappSent") Integer maxWhatsappSent,
        @Param("hasReceivedMessages") Boolean hasReceivedMessages,
        Pageable pageable
    );

    @Query(
        value = """
        SELECT DISTINCT c.*
        FROM contact c
        LEFT JOIN sms s
          ON s.receiver = c.contelephone
         AND (CAST(:campaignId AS bigint) IS NULL OR s.send_sms_id = CAST(:campaignId AS bigint))

        WHERE (CAST(:userLogin AS text) IS NULL OR c.user_login = CAST(:userLogin AS text))

          AND (
            (
              (CAST(:nomLike AS text)       IS NULL OR c.connom    ILIKE CAST(:nomLike AS text))
              AND (CAST(:prenomLike AS text)    IS NULL OR c.conprenom ILIKE CAST(:prenomLike AS text))
              AND (CAST(:telephoneLike AS text) IS NULL OR
                   REPLACE(REPLACE(REPLACE(c.contelephone,' ','') ,'+',''),'-','')
                   ILIKE REPLACE(REPLACE(REPLACE(CAST(:telephoneLike AS text),' ','') ,'+',''),'-','')
              )
            )
            OR (
              CAST(:searchLike AS text) IS NOT NULL AND (
                c.connom ILIKE CAST(:searchLike AS text)
                OR c.conprenom ILIKE CAST(:searchLike AS text)
                OR REPLACE(REPLACE(REPLACE(c.contelephone,' ','') ,'+',''),'-','')
                   ILIKE REPLACE(REPLACE(REPLACE(CAST(:searchLike AS text),' ','') ,'+',''),'-','')
              )
            )
          )

          AND (:statut IS NULL OR c.statuttraitement = :statut)
          AND (:hasWhatsapp IS NULL OR c.has_whatsapp = :hasWhatsapp)
          AND (:minSmsSent IS NULL OR COALESCE(c.total_sms_sent, 0) >= :minSmsSent)
          AND (:maxSmsSent IS NULL OR COALESCE(c.total_sms_sent, 0) <= :maxSmsSent)
          AND (:minWhatsappSent IS NULL OR COALESCE(c.total_whatsapp_sent, 0) >= :minWhatsappSent)
          AND (:maxWhatsappSent IS NULL OR COALESCE(c.total_whatsapp_sent, 0) <= :maxWhatsappSent)

          AND (
            CAST(:hasReceivedMessages AS boolean) IS NULL
            OR (CAST(:hasReceivedMessages AS boolean) = TRUE  AND (COALESCE(c.total_sms_sent,0) + COALESCE(c.total_whatsapp_sent,0)) > 0)
            OR (CAST(:hasReceivedMessages AS boolean) = FALSE AND (COALESCE(c.total_sms_sent,0) + COALESCE(c.total_whatsapp_sent,0)) = 0)
          )

          AND (CAST(:smsStatus AS text)      IS NULL OR s.status          = CAST(:smsStatus AS text))
          AND (CAST(:deliveryStatus AS text) IS NULL OR s.delivery_status = CAST(:deliveryStatus AS text))
          AND (CAST(:lastErrorLike AS text)  IS NULL OR s.last_error ILIKE CAST(:lastErrorLike AS text))
        """,
        countQuery = """
        SELECT COUNT(DISTINCT c.id)
        FROM contact c
        LEFT JOIN sms s
          ON s.receiver = c.contelephone
         AND (CAST(:campaignId AS bigint) IS NULL OR s.send_sms_id = CAST(:campaignId AS bigint))

        WHERE (CAST(:userLogin AS text) IS NULL OR c.user_login = CAST(:userLogin AS text))

          AND (
            (
              (CAST(:nomLike AS text)       IS NULL OR c.connom    ILIKE CAST(:nomLike AS text))
              AND (CAST(:prenomLike AS text)    IS NULL OR c.conprenom ILIKE CAST(:prenomLike AS text))
              AND (CAST(:telephoneLike AS text) IS NULL OR
                   REPLACE(REPLACE(REPLACE(c.contelephone,' ','') ,'+',''),'-','')
                   ILIKE REPLACE(REPLACE(REPLACE(CAST(:telephoneLike AS text),' ','') ,'+',''),'-','')
              )
            )
            OR (
              CAST(:searchLike AS text) IS NOT NULL AND (
                c.connom ILIKE CAST(:searchLike AS text)
                OR c.conprenom ILIKE CAST(:searchLike AS text)
                OR REPLACE(REPLACE(REPLACE(c.contelephone,' ','') ,'+',''),'-','')
                   ILIKE REPLACE(REPLACE(REPLACE(CAST(:searchLike AS text),' ','') ,'+',''),'-','')
              )
            )
          )

          AND (:statut IS NULL OR c.statuttraitement = :statut)
          AND (:hasWhatsapp IS NULL OR c.has_whatsapp = :hasWhatsapp)
          AND (:minSmsSent IS NULL OR COALESCE(c.total_sms_sent, 0) >= :minSmsSent)
          AND (:maxSmsSent IS NULL OR COALESCE(c.total_sms_sent, 0) <= :maxSmsSent)
          AND (:minWhatsappSent IS NULL OR COALESCE(c.total_whatsapp_sent, 0) >= :minWhatsappSent)
          AND (:maxWhatsappSent IS NULL OR COALESCE(c.total_whatsapp_sent, 0) <= :maxWhatsappSent)

          AND (
            CAST(:hasReceivedMessages AS boolean) IS NULL
            OR (CAST(:hasReceivedMessages AS boolean) = TRUE  AND (COALESCE(c.total_sms_sent,0) + COALESCE(c.total_whatsapp_sent,0)) > 0)
            OR (CAST(:hasReceivedMessages AS boolean) = FALSE AND (COALESCE(c.total_sms_sent,0) + COALESCE(c.total_whatsapp_sent,0)) = 0)
          )

          AND (CAST(:smsStatus AS text)      IS NULL OR s.status          = CAST(:smsStatus AS text))
          AND (CAST(:deliveryStatus AS text) IS NULL OR s.delivery_status = CAST(:deliveryStatus AS text))
          AND (CAST(:lastErrorLike AS text)  IS NULL OR s.last_error ILIKE CAST(:lastErrorLike AS text))
        """,
        nativeQuery = true
    )
    Page<Contact> findAllWithAdvancedFiltersAndCampaign_like(
        @Param("userLogin") String userLogin,
        @Param("nomLike") String nomLike,
        @Param("prenomLike") String prenomLike,
        @Param("telephoneLike") String telephoneLike,
        @Param("searchLike") String searchLike,
        @Param("statut") Integer statut,
        @Param("hasWhatsapp") Boolean hasWhatsapp,
        @Param("minSmsSent") Integer minSmsSent,
        @Param("maxSmsSent") Integer maxSmsSent,
        @Param("minWhatsappSent") Integer minWhatsappSent,
        @Param("maxWhatsappSent") Integer maxWhatsappSent,
        @Param("hasReceivedMessages") Boolean hasReceivedMessages,
        @Param("campaignId") Long campaignId,
        @Param("smsStatus") String smsStatus,
        @Param("deliveryStatus") String deliveryStatus,
        @Param("lastErrorLike") String lastErrorLike,
        Pageable pageable
    );

    /**
     * ✅ COMPTER LES CONTACTS D'UN GROUPE (via table de liaison)
     */
    @Query("SELECT COUNT(gc.contact.id) FROM Groupedecontact gc WHERE gc.cgrgroupe.id = :groupeId")
    int countByGroupeId(@Param("groupeId") Long groupeId);
}
