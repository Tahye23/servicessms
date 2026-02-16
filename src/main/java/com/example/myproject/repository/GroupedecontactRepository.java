package com.example.myproject.repository;

import com.example.myproject.domain.Contact;
import com.example.myproject.domain.Groupe;
import com.example.myproject.domain.Groupedecontact;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring Data JPA repository for the Groupedecontact entity.
 */
@SuppressWarnings("unused")
@Repository
public interface GroupedecontactRepository extends JpaRepository<Groupedecontact, Long> {
    //List<Groupedecontact> findByGroupe(Groupe groupe);
    @Query(
        "SELECT gc.cgrgroupe FROM Groupedecontact gc " +
        "WHERE gc.contact.id = :contactId " +
        "AND (:search IS NULL OR LOWER(gc.cgrgroupe.grotitre) LIKE LOWER(concat('%', :search, '%')))"
    )
    Page<Groupe> findGroupesByContactIdAndSearch(@Param("contactId") Long contactId, @Param("search") String search, Pageable pageable);

    // Supprimer les associations d'un groupe et retourner le nombre supprimé
    @Modifying
    @Transactional
    @Query("DELETE FROM Groupedecontact gc WHERE gc.cgrgroupe.id = :groupeId")
    int deleteByGroupeId(@Param("groupeId") Long groupeId);

    @Query("SELECT gc.contact FROM Groupedecontact gc WHERE gc.cgrgroupe.id = :groupeId")
    List<Contact> findContactsByGroupeId(@Param("groupeId") Long groupeId);

    boolean existsByCgrgroupeAndContact(Groupe groupe, Contact contact);

    @Modifying
    @Query(value = "DELETE FROM groupedecontact", nativeQuery = true)
    void deleteAllGroupedecontacts();

    @Query(
        "SELECT gc.contact FROM Groupedecontact gc " +
        "WHERE gc.cgrgroupe.id = :groupeId " +
        "AND (:search IS NULL OR LOWER(gc.contact.connom) LIKE LOWER(CONCAT('%', :search, '%')) " +
        "     OR LOWER(gc.contact.conprenom) LIKE LOWER(CONCAT('%', :search, '%')))"
    )
    Page<Contact> findContactsByGroupeIdAndSearch(@Param("groupeId") Long groupeId, @Param("search") String search, Pageable pageable);

    @Query(
        value = "SELECT c.* FROM groupedecontact g " +
        "JOIN contact c ON c.id = g.contact_id " +
        "WHERE g.cgrgroupe_id = :groupeId " +
        "AND (:search IS NULL OR LOWER(c.connom::text) LIKE LOWER(CONCAT('%', :search, '%')) " +
        "     OR LOWER(c.conprenom::text) LIKE LOWER(CONCAT('%', :search, '%')))",
        nativeQuery = true
    )
    Page<Contact> findContactsByGroupeIdAndSearchNative(
        @Param("groupeId") Long groupeId,
        @Param("search") String search,
        Pageable pageable
    );

    @Query(
        value = "SELECT c.* FROM groupedecontact g " + "JOIN contact c ON c.id = g.contact_id " + "WHERE g.cgrgroupe_id = :groupeId",
        nativeQuery = true
    )
    List<Contact> findAllContactsByGroupeId(@Param("groupeId") Long groupeId);

    @Modifying
    @Query("DELETE FROM Groupedecontact g WHERE g.cgrgroupe.id = :groupeId AND g.contact.id IN :contactIds")
    int deleteByGroupeIdAndContactIdIn(@Param("groupeId") Long groupeId, @Param("contactIds") List<Long> contactIds);

    /**
     * 🆕 RECHERCHE AVANCÉE AVEC FILTRES
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
        countQuery = """
        SELECT COUNT(DISTINCT c.id) FROM contact c
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

    // ContactRepository.java
    @Query(
        value = """
        SELECT DISTINCT c.*
        FROM contact c
        INNER JOIN groupedecontact gc ON c.id = gc.contact_id
        /* on rattache les SMS de la campagne au contact par numéro */
        LEFT JOIN sms s
          ON s.receiver = c.contelephone
         AND (:campaignId IS NULL OR s.send_sms_id = :campaignId)
        WHERE gc.cgrgroupe_id = :groupeId

          /* --- filtres contact existants --- */
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

          /* --- filtres campagne / sms --- */
          AND (:campaignId IS NULL OR s.send_sms_id = :campaignId)
          AND (:smsStatus IS NULL OR s.status = :smsStatus)
          AND (:deliveryStatus IS NULL OR s.delivery_status = :deliveryStatus)
          AND (:lastErrorContains IS NULL OR s.last_error ILIKE CONCAT('%', :lastErrorContains, '%'))

        ORDER BY c.id DESC
        """,
        countQuery = """
        SELECT COUNT(DISTINCT c.id)
        FROM contact c
        INNER JOIN groupedecontact gc ON c.id = gc.contact_id
        LEFT JOIN sms s
          ON s.receiver = c.contelephone
         AND (:campaignId IS NULL OR s.send_sms_id = :campaignId)
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

          AND (:campaignId IS NULL OR s.send_sms_id = :campaignId)
          AND (:smsStatus IS NULL OR s.status = :smsStatus)
          AND (:deliveryStatus IS NULL OR s.delivery_status = :deliveryStatus)
          AND (:lastErrorContains IS NULL OR s.last_error ILIKE CONCAT('%', :lastErrorContains, '%'))
        """,
        nativeQuery = true
    )
    Page<Contact> findContactsByGroupeIdWithAdvancedFiltersAndCampaign(
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
        // 👇 nouveaux
        @Param("campaignId") Long campaignId,
        @Param("smsStatus") String smsStatus,
        @Param("deliveryStatus") String deliveryStatus,
        @Param("lastErrorContains") String lastErrorContains,
        Pageable pageable
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query(
        value = """
        INSERT INTO groupedecontact (id, cgrgroupe_id, contact_id)
        SELECT nextval('sequence_generator'), :targetGroupeId, c.id
        FROM contact c
        JOIN groupedecontact gc
          ON gc.contact_id = c.id
         AND gc.cgrgroupe_id = :sourceGroupeId

        LEFT JOIN sms s
          ON s.receiver = c.contelephone
         AND (:campaignId IS NULL OR s.send_sms_id = :campaignId)

        /* éviter de recréer une liaison déjà existante */
        LEFT JOIN groupedecontact already
          ON already.cgrgroupe_id = :targetGroupeId
         AND already.contact_id   = c.id

        WHERE already.id IS NULL

        /* ====== FILTRES CONTACT ====== */
        AND (:nom IS NULL OR LOWER(c.connom) LIKE LOWER(CONCAT('%', :nom, '%')))
        AND (:prenom IS NULL OR LOWER(c.conprenom) LIKE LOWER(CONCAT('%', :prenom, '%')))
        AND (:telephone IS NULL OR c.contelephone LIKE CONCAT('%', :telephone, '%'))
        AND (:statut IS NULL OR c.statuttraitement = :statut)
        AND (:hasWhatsapp IS NULL OR c.has_whatsapp = :hasWhatsapp)
        AND (:minSmsSent IS NULL OR COALESCE(c.total_sms_sent, 0) >= :minSmsSent)
        AND (:maxSmsSent IS NULL OR COALESCE(c.total_sms_sent, 0) <= :maxSmsSent)
        AND (:minWhatsappSent IS NULL OR COALESCE(c.total_whatsapp_sent, 0) >= :minWhatsappSent)
        AND (:maxWhatsappSent IS NULL OR COALESCE(c.total_whatsapp_sent, 0) <= :maxWhatsappSent)
        AND (
          :hasReceivedMessages IS NULL
          OR (:hasReceivedMessages = TRUE  AND (COALESCE(c.total_sms_sent,0) + COALESCE(c.total_whatsapp_sent,0)) > 0)
          OR (:hasReceivedMessages = FALSE AND (COALESCE(c.total_sms_sent,0) + COALESCE(c.total_whatsapp_sent,0)) = 0)
        )

        /* ====== FILTRES CAMPAGNE / SMS ====== */
        AND (:smsStatus IS NULL OR LOWER(s.status) = LOWER(:smsStatus))
        AND (:deliveryStatus IS NULL OR LOWER(s.delivery_status) = LOWER(:deliveryStatus))
        AND (:lastErrorContains IS NULL OR LOWER(s.last_error) LIKE LOWER(CONCAT('%', :lastErrorContains, '%')))
        /* éviter les doublons liés aux LEFT JOIN */
        GROUP BY c.id
        """,
        nativeQuery = true
    )
    int bulkLinkFilteredContactsAndCampaign(
        @Param("sourceGroupeId") Long sourceGroupeId,
        @Param("targetGroupeId") Long targetGroupeId,
        // filtres contact
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
        // filtres campagne / sms
        @Param("campaignId") Long campaignId,
        @Param("smsStatus") String smsStatus,
        @Param("deliveryStatus") String deliveryStatus,
        @Param("lastErrorContains") String lastErrorContains
    );

    @Query(
        value = """
        SELECT DISTINCT c.* FROM contact c
        JOIN groupedecontact gc ON c.id = gc.contact_id
        WHERE gc.cgrgroupe_id = :groupeId

        AND (
          :nom IS NULL OR
          (:nomOp = 'contains'        AND LOWER(c.connom) LIKE LOWER(CONCAT('%', :nom, '%'))) OR
          (:nomOp = 'starts_with'     AND LOWER(c.connom) LIKE LOWER(CONCAT(:nom, '%'))) OR
          (:nomOp = 'ends_with'       AND LOWER(c.connom) LIKE LOWER(CONCAT('%', :nom))) OR
          (:nomOp = 'exact'           AND LOWER(c.connom) = LOWER(:nom)) OR
          (:nomOp = 'not_contains'    AND LOWER(c.connom) NOT LIKE LOWER(CONCAT('%', :nom, '%'))) OR
          (:nomOp = 'not_starts_with' AND LOWER(c.connom) NOT LIKE LOWER(CONCAT(:nom, '%'))) OR
          (:nomOp = 'not_ends_with'   AND LOWER(c.connom) NOT LIKE LOWER(CONCAT('%', :nom))) OR
          (:nomOp = 'not_exact'       AND LOWER(c.connom) <> LOWER(:nom))
        )

        AND (
          :prenom IS NULL OR
          (:prenomOp = 'contains'        AND LOWER(c.conprenom) LIKE LOWER(CONCAT('%', :prenom, '%'))) OR
          (:prenomOp = 'starts_with'     AND LOWER(c.conprenom) LIKE LOWER(CONCAT(:prenom, '%'))) OR
          (:prenomOp = 'ends_with'       AND LOWER(c.conprenom) LIKE LOWER(CONCAT('%', :prenom))) OR
          (:prenomOp = 'exact'           AND LOWER(c.conprenom) = LOWER(:prenom)) OR
          (:prenomOp = 'not_contains'    AND LOWER(c.conprenom) NOT LIKE LOWER(CONCAT('%', :prenom, '%'))) OR
          (:prenomOp = 'not_starts_with' AND LOWER(c.conprenom) NOT LIKE LOWER(CONCAT(:prenom, '%'))) OR
          (:prenomOp = 'not_ends_with'   AND LOWER(c.conprenom) NOT LIKE LOWER(CONCAT('%', :prenom))) OR
          (:prenomOp = 'not_exact'       AND LOWER(c.conprenom) <> LOWER(:prenom))
        )

        AND (
          :telephone IS NULL OR
          (:telephoneOp = 'contains'        AND c.contelephone LIKE CONCAT('%', :telephone, '%')) OR
          (:telephoneOp = 'starts_with'     AND c.contelephone LIKE CONCAT(:telephone, '%')) OR
          (:telephoneOp = 'ends_with'       AND c.contelephone LIKE CONCAT('%', :telephone)) OR
          (:telephoneOp = 'exact'           AND c.contelephone = :telephone) OR
          (:telephoneOp = 'not_contains'    AND c.contelephone NOT LIKE CONCAT('%', :telephone, '%')) OR
          (:telephoneOp = 'not_starts_with' AND c.contelephone NOT LIKE CONCAT(:telephone, '%')) OR
          (:telephoneOp = 'not_ends_with'   AND c.contelephone NOT LIKE CONCAT('%', :telephone)) OR
          (:telephoneOp = 'not_exact'       AND c.contelephone <> :telephone)
        )

        AND (:statut IS NULL OR c.statuttraitement = :statut)
        AND (:hasWhatsapp IS NULL OR c.has_whatsapp = :hasWhatsapp)
        AND (:minSmsSent IS NULL OR COALESCE(c.total_sms_sent, 0) >= :minSmsSent)
        AND (:maxSmsSent IS NULL OR COALESCE(c.total_sms_sent, 0) <= :maxSmsSent)
        AND (:minWhatsappSent IS NULL OR COALESCE(c.total_whatsapp_sent, 0) >= :minWhatsappSent)
        AND (:maxWhatsappSent IS NULL OR COALESCE(c.total_whatsapp_sent, 0) <= :maxWhatsappSent)
        AND (
          :hasReceivedMessages IS NULL OR
          (:hasReceivedMessages = TRUE  AND (COALESCE(c.total_sms_sent,0) + COALESCE(c.total_whatsapp_sent,0)) > 0) OR
          (:hasReceivedMessages = FALSE AND (COALESCE(c.total_sms_sent,0) + COALESCE(c.total_whatsapp_sent,0)) = 0)
        )
        ORDER BY c.id DESC
        """,
        countQuery = """
        SELECT COUNT(*) FROM (
          SELECT DISTINCT c.id FROM contact c
          JOIN groupedecontact gc ON c.id = gc.contact_id
          WHERE gc.cgrgroupe_id = :groupeId

          AND (
            :nom IS NULL OR
            (:nomOp = 'contains'        AND LOWER(c.connom) LIKE LOWER(CONCAT('%', :nom, '%'))) OR
            (:nomOp = 'starts_with'     AND LOWER(c.connom) LIKE LOWER(CONCAT(:nom, '%'))) OR
            (:nomOp = 'ends_with'       AND LOWER(c.connom) LIKE LOWER(CONCAT('%', :nom))) OR
            (:nomOp = 'exact'           AND LOWER(c.connom) = LOWER(:nom)) OR
            (:nomOp = 'not_contains'    AND LOWER(c.connom) NOT LIKE LOWER(CONCAT('%', :nom, '%'))) OR
            (:nomOp = 'not_starts_with' AND LOWER(c.connom) NOT LIKE LOWER(CONCAT(:nom, '%'))) OR
            (:nomOp = 'not_ends_with'   AND LOWER(c.connom) NOT LIKE LOWER(CONCAT('%', :nom))) OR
            (:nomOp = 'not_exact'       AND LOWER(c.connom) <> LOWER(:nom))
          )

          AND (
            :prenom IS NULL OR
            (:prenomOp = 'contains'        AND LOWER(c.conprenom) LIKE LOWER(CONCAT('%', :prenom, '%'))) OR
            (:prenomOp = 'starts_with'     AND LOWER(c.conprenom) LIKE LOWER(CONCAT(:prenom, '%'))) OR
            (:prenomOp = 'ends_with'       AND LOWER(c.conprenom) LIKE LOWER(CONCAT('%', :prenom))) OR
            (:prenomOp = 'exact'           AND LOWER(c.conprenom) = LOWER(:prenom)) OR
            (:prenomOp = 'not_contains'    AND LOWER(c.conprenom) NOT LIKE LOWER(CONCAT('%', :prenom, '%'))) OR
            (:prenomOp = 'not_starts_with' AND LOWER(c.conprenom) NOT LIKE LOWER(CONCAT(:prenom, '%'))) OR
            (:prenomOp = 'not_ends_with'   AND LOWER(c.conprenom) NOT LIKE LOWER(CONCAT('%', :prenom))) OR
            (:prenomOp = 'not_exact'       AND LOWER(c.conprenom) <> LOWER(:prenom))
          )

          AND (
            :telephone IS NULL OR
            (:telephoneOp = 'contains'        AND c.contelephone LIKE CONCAT('%', :telephone, '%')) OR
            (:telephoneOp = 'starts_with'     AND c.contelephone LIKE CONCAT(:telephone, '%')) OR
            (:telephoneOp = 'ends_with'       AND c.contelephone LIKE CONCAT('%', :telephone)) OR
            (:telephoneOp = 'exact'           AND c.contelephone = :telephone) OR
            (:telephoneOp = 'not_contains'    AND c.contelephone NOT LIKE CONCAT('%', :telephone, '%')) OR
            (:telephoneOp = 'not_starts_with' AND c.contelephone NOT LIKE CONCAT(:telephone, '%')) OR
            (:telephoneOp = 'not_ends_with'   AND c.contelephone NOT LIKE CONCAT('%', :telephone)) OR
            (:telephoneOp = 'not_exact'       AND c.contelephone <> :telephone)
          )

          AND (:statut IS NULL OR c.statuttraitement = :statut)
          AND (:hasWhatsapp IS NULL OR c.has_whatsapp = :hasWhatsapp)
          AND (:minSmsSent IS NULL OR COALESCE(c.total_sms_sent, 0) >= :minSmsSent)
          AND (:maxSmsSent IS NULL OR COALESCE(c.total_sms_sent, 0) <= :maxSmsSent)
          AND (:minWhatsappSent IS NULL OR COALESCE(c.total_whatsapp_sent, 0) >= :minWhatsappSent)
          AND (:maxWhatsappSent IS NULL OR COALESCE(c.total_whatsapp_sent, 0) <= :maxWhatsappSent)
          AND (
            :hasReceivedMessages IS NULL OR
            (:hasReceivedMessages = TRUE  AND (COALESCE(c.total_sms_sent,0) + COALESCE(c.total_whatsapp_sent,0)) > 0) OR
            (:hasReceivedMessages = FALSE AND (COALESCE(c.total_sms_sent,0) + COALESCE(c.total_whatsapp_sent,0)) = 0)
          )
        ) t
        """,
        nativeQuery = true
    )
    Page<Contact> findContactsByGroupeIdWithAdvancedFiltersOps(
        @Param("groupeId") Long groupeId,
        @Param("nom") String nom,
        @Param("nomOp") String nomOp,
        @Param("prenom") String prenom,
        @Param("prenomOp") String prenomOp,
        @Param("telephone") String telephone,
        @Param("telephoneOp") String telephoneOp,
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
        SELECT DISTINCT c.* FROM contact c
        JOIN groupedecontact gc ON c.id = gc.contact_id
        LEFT JOIN sms s
          ON s.receiver = c.contelephone
         AND (:campaignId IS NULL OR s.send_sms_id = :campaignId)
        WHERE gc.cgrgroupe_id = :groupeId

        /* mêmes blocs nom/prenom/telephone avec opérateurs */
        AND (
          :nom IS NULL OR
          (:nomOp = 'contains'        AND LOWER(c.connom) LIKE LOWER(CONCAT('%', :nom, '%'))) OR
          (:nomOp = 'starts_with'     AND LOWER(c.connom) LIKE LOWER(CONCAT(:nom, '%'))) OR
          (:nomOp = 'ends_with'       AND LOWER(c.connom) LIKE LOWER(CONCAT('%', :nom))) OR
          (:nomOp = 'exact'           AND LOWER(c.connom) = LOWER(:nom)) OR
          (:nomOp = 'not_contains'    AND LOWER(c.connom) NOT LIKE LOWER(CONCAT('%', :nom, '%'))) OR
          (:nomOp = 'not_starts_with' AND LOWER(c.connom) NOT LIKE LOWER(CONCAT(:nom, '%'))) OR
          (:nomOp = 'not_ends_with'   AND LOWER(c.connom) NOT LIKE LOWER(CONCAT('%', :nom))) OR
          (:nomOp = 'not_exact'       AND LOWER(c.connom) <> LOWER(:nom))
        )
        AND (
          :prenom IS NULL OR
          (:prenomOp = 'contains'        AND LOWER(c.conprenom) LIKE LOWER(CONCAT('%', :prenom, '%'))) OR
          (:prenomOp = 'starts_with'     AND LOWER(c.conprenom) LIKE LOWER(CONCAT(:prenom, '%'))) OR
          (:prenomOp = 'ends_with'       AND LOWER(c.conprenom) LIKE LOWER(CONCAT('%', :prenom))) OR
          (:prenomOp = 'exact'           AND LOWER(c.conprenom) = LOWER(:prenom)) OR
          (:prenomOp = 'not_contains'    AND LOWER(c.conprenom) NOT LIKE LOWER(CONCAT('%', :prenom, '%'))) OR
          (:prenomOp = 'not_starts_with' AND LOWER(c.conprenom) NOT LIKE LOWER(CONCAT(:prenom, '%'))) OR
          (:prenomOp = 'not_ends_with'   AND LOWER(c.conprenom) NOT LIKE LOWER(CONCAT('%', :prenom))) OR
          (:prenomOp = 'not_exact'       AND LOWER(c.conprenom) <> LOWER(:prenom))
        )
        AND (
          :telephone IS NULL OR
          (:telephoneOp = 'contains'        AND c.contelephone LIKE CONCAT('%', :telephone, '%')) OR
          (:telephoneOp = 'starts_with'     AND c.contelephone LIKE CONCAT(:telephone, '%')) OR
          (:telephoneOp = 'ends_with'       AND c.contelephone LIKE CONCAT('%', :telephone)) OR
          (:telephoneOp = 'exact'           AND c.contelephone = :telephone) OR
          (:telephoneOp = 'not_contains'    AND c.contelephone NOT LIKE CONCAT('%', :telephone, '%')) OR
          (:telephoneOp = 'not_starts_with' AND c.contelephone NOT LIKE CONCAT(:telephone, '%')) OR
          (:telephoneOp = 'not_ends_with'   AND c.contelephone NOT LIKE CONCAT('%', :telephone)) OR
          (:telephoneOp = 'not_exact'       AND c.contelephone <> :telephone)
        )

        AND (:statut IS NULL OR c.statuttraitement = :statut)
        AND (:hasWhatsapp IS NULL OR c.has_whatsapp = :hasWhatsapp)
        AND (:minSmsSent IS NULL OR COALESCE(c.total_sms_sent, 0) >= :minSmsSent)
        AND (:maxSmsSent IS NULL OR COALESCE(c.total_sms_sent, 0) <= :maxSmsSent)
        AND (:minWhatsappSent IS NULL OR COALESCE(c.total_whatsapp_sent, 0) >= :minWhatsappSent)
        AND (:maxWhatsappSent IS NULL OR COALESCE(c.total_whatsapp_sent, 0) <= :maxWhatsappSent)
        AND (
          :hasReceivedMessages IS NULL OR
          (:hasReceivedMessages = TRUE  AND (COALESCE(c.total_sms_sent,0) + COALESCE(c.total_whatsapp_sent,0)) > 0) OR
          (:hasReceivedMessages = FALSE AND (COALESCE(c.total_sms_sent,0) + COALESCE(c.total_whatsapp_sent,0)) = 0)
        )

        /* filtres campagne / sms */
        AND (:smsStatus IS NULL OR s.status = :smsStatus)
        AND (:deliveryStatus IS NULL OR s.delivery_status = :deliveryStatus)
        AND (:lastErrorContains IS NULL OR s.last_error ILIKE CONCAT('%', :lastErrorContains, '%'))

        ORDER BY c.id DESC
        """,
        countQuery = """
        SELECT COUNT(*) FROM (
          SELECT DISTINCT c.id
          FROM contact c
          JOIN groupedecontact gc ON c.id = gc.contact_id
          LEFT JOIN sms s
            ON s.receiver = c.contelephone
           AND (:campaignId IS NULL OR s.send_sms_id = :campaignId)
          WHERE gc.cgrgroupe_id = :groupeId

          /* mêmes blocs nom/prenom/telephone + filtres divers */
          AND (
            :nom IS NULL OR
            (:nomOp = 'contains'        AND LOWER(c.connom) LIKE LOWER(CONCAT('%', :nom, '%'))) OR
            (:nomOp = 'starts_with'     AND LOWER(c.connom) LIKE LOWER(CONCAT(:nom, '%'))) OR
            (:nomOp = 'ends_with'       AND LOWER(c.connom) LIKE LOWER(CONCAT('%', :nom))) OR
            (:nomOp = 'exact'           AND LOWER(c.connom) = LOWER(:nom)) OR
            (:nomOp = 'not_contains'    AND LOWER(c.connom) NOT LIKE LOWER(CONCAT('%', :nom, '%'))) OR
            (:nomOp = 'not_starts_with' AND LOWER(c.connom) NOT LIKE LOWER(CONCAT(:nom, '%'))) OR
            (:nomOp = 'not_ends_with'   AND LOWER(c.connom) NOT LIKE LOWER(CONCAT('%', :nom))) OR
            (:nomOp = 'not_exact'       AND LOWER(c.connom) <> LOWER(:nom))
          )
          AND (
            :prenom IS NULL OR
            (:prenomOp = 'contains'        AND LOWER(c.conprenom) LIKE LOWER(CONCAT('%', :prenom, '%'))) OR
            (:prenomOp = 'starts_with'     AND LOWER(c.conprenom) LIKE LOWER(CONCAT(:prenom, '%'))) OR
            (:prenomOp = 'ends_with'       AND LOWER(c.conprenom) LIKE LOWER(CONCAT('%', :prenom))) OR
            (:prenomOp = 'exact'           AND LOWER(c.conprenom) = LOWER(:prenom)) OR
            (:prenomOp = 'not_contains'    AND LOWER(c.conprenom) NOT LIKE LOWER(CONCAT('%', :prenom, '%'))) OR
            (:prenomOp = 'not_starts_with' AND LOWER(c.conprenom) NOT LIKE LOWER(CONCAT(:prenom, '%'))) OR
            (:prenomOp = 'not_ends_with'   AND LOWER(c.conprenom) NOT LIKE LOWER(CONCAT('%', :prenom))) OR
            (:prenomOp = 'not_exact'       AND LOWER(c.conprenom) <> LOWER(:prenom))
          )
          AND (
            :telephone IS NULL OR
            (:telephoneOp = 'contains'        AND c.contelephone LIKE CONCAT('%', :telephone, '%')) OR
            (:telephoneOp = 'starts_with'     AND c.contelephone LIKE CONCAT(:telephone, '%')) OR
            (:telephoneOp = 'ends_with'       AND c.contelephone LIKE CONCAT('%', :telephone)) OR
            (:telephoneOp = 'exact'           AND c.contelephone = :telephone) OR
            (:telephoneOp = 'not_contains'    AND c.contelephone NOT LIKE CONCAT('%', :telephone, '%')) OR
            (:telephoneOp = 'not_starts_with' AND c.contelephone NOT LIKE CONCAT(:telephone, '%')) OR
            (:telephoneOp = 'not_ends_with'   AND c.contelephone NOT LIKE CONCAT('%', :telephone)) OR
            (:telephoneOp = 'not_exact'       AND c.contelephone <> :telephone)
          )

          AND (:statut IS NULL OR c.statuttraitement = :statut)
          AND (:hasWhatsapp IS NULL OR c.has_whatsapp = :hasWhatsapp)
          AND (:minSmsSent IS NULL OR COALESCE(c.total_sms_sent, 0) >= :minSmsSent)
          AND (:maxSmsSent IS NULL OR COALESCE(c.total_sms_sent, 0) <= :maxSmsSent)
          AND (:minWhatsappSent IS NULL OR COALESCE(c.total_whatsapp_sent, 0) >= :minWhatsappSent)
          AND (:maxWhatsappSent IS NULL OR COALESCE(c.total_whatsapp_sent, 0) <= :maxWhatsappSent)
          AND (
            :hasReceivedMessages IS NULL OR
            (:hasReceivedMessages = TRUE  AND (COALESCE(c.total_sms_sent,0) + COALESCE(c.total_whatsapp_sent,0)) > 0) OR
            (:hasReceivedMessages = FALSE AND (COALESCE(c.total_sms_sent,0) + COALESCE(c.total_whatsapp_sent,0)) = 0)
          )

          AND (:smsStatus IS NULL OR s.status = :smsStatus)
          AND (:deliveryStatus IS NULL OR s.delivery_status = :deliveryStatus)
          AND (:lastErrorContains IS NULL OR s.last_error ILIKE CONCAT('%', :lastErrorContains, '%'))
        ) t
        """,
        nativeQuery = true
    )
    Page<Contact> findContactsByGroupeIdWithAdvancedFiltersAndCampaignOps(
        @Param("groupeId") Long groupeId,
        @Param("nom") String nom,
        @Param("nomOp") String nomOp,
        @Param("prenom") String prenom,
        @Param("prenomOp") String prenomOp,
        @Param("telephone") String telephone,
        @Param("telephoneOp") String telephoneOp,
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
        @Param("lastErrorContains") String lastErrorContains,
        Pageable pageable
    );
    /* ================== Campagnes (SMS) du groupe ================== */

    // Projection simple pour le popup (id + name)

}
