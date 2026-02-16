package com.example.myproject.repository;

import com.example.myproject.domain.Sms;
import com.example.myproject.domain.enumeration.MessageType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SmsRepository extends JpaRepository<Sms, Long> {
    // ===== MÉTHODES EXISTANTES (conservées) =====

    long countByBatch_BulkId(String bulkId);
    long countByBatch_BulkIdAndStatus(String bulkId, String status);
    long countByBulkId(String bulkId);
    Page<Sms> findBySenderOrReceiverOrderBySendDateAsc(String sender, String receiver, Pageable pageable);
    long countByBatch_Id(Long sendSmsId);
    List<Sms> findAllByMessageIdIn(List<String> messageIds);
    List<Sms> findByBatchId(Long batchId);

    @Query("SELECT COALESCE(SUM(s.totalMessage), 0) FROM Sms s WHERE s.user_login = :userLogin AND s.status = 'SENT' AND s.type = 'SMS'")
    Integer countSuccessfulSmsByUserLogin(@Param("userLogin") String userLogin);

    @Query(
        "SELECT COALESCE(SUM(s.totalMessage), 0) FROM Sms s WHERE s.user_login = :userLogin AND s.status = 'SENT' AND s.type = 'WHATSAPP'"
    )
    Integer countSuccessfulWhatsappByUserLogin(@Param("userLogin") String userLogin);

    @Modifying
    @Query("DELETE FROM Sms s WHERE s.receiver IN (SELECT c.contelephone FROM Contact c WHERE c.id IN :contactIds)")
    int deleteByContactIds(@Param("contactIds") List<Long> contactIds);

    @Modifying
    @Query("DELETE FROM Sms s WHERE s.batch.id = :sendSmsId")
    int deleteBySendSmsId(@Param("sendSmsId") Long sendSmsId);

    @Query("SELECT COUNT(s) FROM Sms s WHERE s.sender = :userLogin")
    Integer countByUserLogin(@Param("userLogin") String userLogin);

    @Query("SELECT COUNT(s) FROM Sms s WHERE s.sender = :userLogin AND s.type = 'WHATSAPP'")
    Integer countWhatsappByUserLogin(@Param("userLogin") String userLogin);

    @Modifying
    @Query(value = "DELETE FROM sms", nativeQuery = true)
    int deleteAllSms();

    Page<Sms> findByBulkId(String bulkId, Pageable pageable);
    Optional<Sms> findByMessageId(String messageId);

    @Query("SELECT s FROM Sms s WHERE s.batch.id = :sendSmsId")
    Page<Sms> findBysendSmsId(Long sendSmsId, Pageable pageable);

    @Query("SELECT s FROM Sms s WHERE s.batch.id = :sendSmsId")
    List<Sms> findALLBYsendSmsId(Long sendSmsId);

    @Query(
        """
        SELECT s FROM Sms s
         WHERE s.batch.id  = :bulkId
           AND (
             LOWER(s.receiver)  LIKE CONCAT('%', LOWER(:term), '%')
             OR LOWER(s.status) LIKE CONCAT('%', LOWER(:term), '%')
             OR LOWER(s.msgdata) LIKE CONCAT('%', LOWER(:term), '%')
           )
        """
    )
    Page<Sms> searchByBulkIdAndTerm(@Param("bulkId") Long bulkId, @Param("term") String term, Pageable pageable);

    List<Sms> findByBulkIdAndDeliveryStatusIn(String bulkId, List<String> statuses);
    List<Sms> findByBatch_Id(Long sendSmsId);
    long countByBulkIdAndStatus(String bulkId, String status);

    @Query(
        """
        SELECT s FROM Sms s
        WHERE s.bulkId = :bulkId
          AND s.deliveryStatus IN :statuses
          AND (s.last_error IS NULL OR s.last_error NOT LIKE %:errorToExclude%)
        """
    )
    List<Sms> findByBulkIdAndDeliveryStatusInExcludingError(
        @Param("bulkId") String bulkId,
        @Param("statuses") List<String> statuses,
        @Param("errorToExclude") String errorToExclude
    );

    Page<Sms> findByChatIdOrderBySendDateAsc(Long chatId, Pageable pageable);

    @Query(
        value = """
        SELECT s.*
        FROM sms s
        WHERE s.send_sms_id = :bulkId
          AND (
            :search IS NULL
            OR btrim(:search) = ''
            OR lower(s.receiver) LIKE lower('%%' || btrim(:search) || '%%')
            OR lower(coalesce(s.namereceiver, '')) LIKE lower('%%' || btrim(:search) || '%%')
            OR (s.msgdata IS NOT NULL AND lower(s.msgdata::text) LIKE lower('%%' || btrim(:search) || '%%'))
            OR lower(coalesce(s.last_error, '')) LIKE lower('%%' || btrim(:search) || '%%')
          )
          AND (
            :deliveryStatus IS NULL
            OR btrim(:deliveryStatus) = ''
            OR s.delivery_status = :deliveryStatus
          )
          AND (CAST(:dateFrom AS timestamp) IS NULL OR s.send_date >= CAST(:dateFrom AS timestamp))
          AND (CAST(:dateTo   AS timestamp) IS NULL OR s.send_date <= CAST(:dateTo   AS timestamp))
        ORDER BY coalesce(s.send_date, s.bulk_created_at, TIMESTAMP '1970-01-01 00:00:00') DESC, s.id DESC
        """,
        countQuery = """
        SELECT count(*)
        FROM sms s
        WHERE s.send_sms_id = :bulkId
          AND (
            :search IS NULL
            OR btrim(:search) = ''
            OR lower(s.receiver) LIKE lower('%%' || btrim(:search) || '%%')
            OR lower(coalesce(s.namereceiver, '')) LIKE lower('%%' || btrim(:search) || '%%')
            OR (s.msgdata IS NOT NULL AND lower(s.msgdata::text) LIKE lower('%%' || btrim(:search) || '%%'))
            OR lower(coalesce(s.last_error, '')) LIKE lower('%%' || btrim(:search) || '%%')
          )
          AND (
            :deliveryStatus IS NULL
            OR btrim(:deliveryStatus) = ''
            OR s.delivery_status = :deliveryStatus
          )
          AND (CAST(:dateFrom AS timestamp) IS NULL OR s.send_date >= CAST(:dateFrom AS timestamp))
          AND (CAST(:dateTo   AS timestamp) IS NULL OR s.send_date <= CAST(:dateTo   AS timestamp))
        """,
        nativeQuery = true
    )
    Page<Sms> findByBulkIdWithFiltersNative(
        @Param("bulkId") Long bulkId,
        @Param("search") String search,
        @Param("deliveryStatus") String deliveryStatus,
        @Param("dateFrom") Instant dateFrom,
        @Param("dateTo") Instant dateTo,
        Pageable pageable
    );

    @Query(
        """
        SELECT s.status, COUNT(s)
        FROM Sms s
        WHERE s.bulkId = :bulkId
        GROUP BY s.status
        """
    )
    List<Object[]> countByBulkIdGroupByStatus(@Param("bulkId") String bulkId);

    @Query(
        """
        SELECT s.deliveryStatus, COUNT(s)
        FROM Sms s
        WHERE s.bulkId = :bulkId
        GROUP BY s.deliveryStatus
        """
    )
    List<Object[]> countByBulkIdGroupByDeliveryStatus(@Param("bulkId") String bulkId);

    @Query("SELECT s FROM Sms s WHERE s.messageId IS NULL AND s.status = 'PENDING'")
    List<Sms> findPendingSmsWithoutMessageId();

    @Query(
        value = """
        SELECT s.* FROM sms s
        WHERE s.send_sms_id = :bulkId
        AND (CAST(:search AS TEXT) IS NULL OR LOWER(s.receiver) LIKE LOWER(CONCAT('%', CAST(:search AS TEXT), '%'))
             OR LOWER(s.namereceiver) LIKE LOWER(CONCAT('%', CAST(:search AS TEXT), '%')))
        AND (CAST(:deliveryStatus AS TEXT) IS NULL OR s.delivery_status = CAST(:deliveryStatus AS TEXT))
        AND (CAST(:dateFrom AS TIMESTAMP) IS NULL OR s.send_date >= CAST(:dateFrom AS TIMESTAMP))
        AND (CAST(:dateTo AS TIMESTAMP) IS NULL OR s.send_date <= CAST(:dateTo AS TIMESTAMP))
        ORDER BY s.id DESC
        """,
        nativeQuery = true
    )
    List<Sms> findAllByBulkIdWithFilters(
        @Param("bulkId") Long bulkId,
        @Param("search") String search,
        @Param("deliveryStatus") String deliveryStatus,
        @Param("dateFrom") Instant dateFrom,
        @Param("dateTo") Instant dateTo
    );

    @Query(
        value = "SELECT s.* FROM sms s " +
        "WHERE s.bulk_id LIKE 'API_TOKEN_%' " +
        "AND (:status IS NULL OR :status = '' OR s.status = :status) " +
        "AND (:type IS NULL OR :type = '' OR s.type::text = :type) " +
        "AND (:search IS NULL OR :search = '' OR " +
        "    s.receiver ILIKE CONCAT('%', :search, '%') OR " +
        "    s.sender ILIKE CONCAT('%', :search, '%')) " +
        "ORDER BY s.send_date DESC",
        countQuery = "SELECT COUNT(*) FROM sms s " +
        "WHERE s.bulk_id LIKE 'API_TOKEN_%' " +
        "AND (:status IS NULL OR :status = '' OR s.status = :status) " +
        "AND (:type IS NULL OR :type = '' OR s.type::text = :type) " +
        "AND (:search IS NULL OR :search = '' OR " +
        "    s.receiver ILIKE CONCAT('%', :search, '%') OR " +
        "    s.sender ILIKE CONCAT('%', :search, '%'))",
        nativeQuery = true
    )
    Page<Sms> findExternalApiMessages(
        @Param("status") String status,
        @Param("type") String type,
        @Param("search") String search,
        Pageable pageable
    );

    @Query("SELECT COUNT(s) FROM Sms s WHERE s.bulkId LIKE 'API_TOKEN_%' AND s.type = :type")
    long countExternalApiByType(@Param("type") MessageType type);

    @Query("SELECT COUNT(s) FROM Sms s WHERE s.bulkId LIKE 'API_TOKEN_%' AND s.type = :type AND s.status = :status")
    long countExternalApiByTypeAndStatus(@Param("type") MessageType type, @Param("status") String status);

    @Query("SELECT COALESCE(SUM(s.totalMessage), 0) FROM Sms s WHERE s.bulkId LIKE 'API_TOKEN_%'")
    long sumExternalApiSegments();

    @Query("SELECT COUNT(s) FROM Sms s WHERE s.bulkId = :bulkId AND s.deliveryStatus = :status")
    Long countByBulkIdAndDeliveryStatus(@Param("bulkId") String bulkId, @Param("status") String status);

    @Query("SELECT s FROM Sms s WHERE s.bulkId = :bulkId AND s.deliveryStatus = :status ORDER BY s.id ASC")
    Page<Sms> findByBulkIdAndDeliveryStatusPaged(@Param("bulkId") String bulkId, @Param("status") String status, Pageable pageable);

    // ===== ✅ NOUVELLES MÉTHODES POUR MONITORING =====

    /**
     * ✅ COMPTER TOUS LES SMS PAR STATUT (pour monitoring global)
     */
    @Query("SELECT COUNT(s) FROM Sms s WHERE s.deliveryStatus = :status")
    long countByDeliveryStatus(@Param("status") String status);

    /**
     * ✅ STATS PAR TYPE (SMS vs WhatsApp)
     */
    @Query(
        """
        SELECT s.type,
               COUNT(s),
               COALESCE(SUM(s.totalMessage), 0)
        FROM Sms s
        GROUP BY s.type
        """
    )
    List<Object[]> getStatsByType();

    /**
     * ✅ DERNIERS SMS ENVOYÉS (pour debug)
     */
    @Query(
        """
        SELECT s FROM Sms s
        WHERE s.bulkId = :bulkId
        ORDER BY s.id DESC
        """
    )
    List<Sms> findRecentByBulkId(@Param("bulkId") String bulkId, Pageable pageable);

    /**
     * ✅ SMS ÉCHOUÉS AVEC ERREUR (pour debug)
     */
    @Query(
        """
        SELECT s FROM Sms s
        WHERE s.bulkId = :bulkId
          AND s.deliveryStatus = 'failed'
          AND s.last_error IS NOT NULL
        ORDER BY s.id DESC
        """
    )
    List<Sms> findFailedWithError(@Param("bulkId") String bulkId, Pageable pageable);

    /**
     * ✅ SUPPRIMER PAR BULK_ID (pour nettoyage)
     */
    @Modifying
    @Query("DELETE FROM Sms s WHERE s.bulkId = :bulkId")
    int deleteByBulkId(@Param("bulkId") String bulkId);

    /**
     * ✅ COMPTER SMS RÉCENTS (dernières 24h)
     */
    @Query(
        """
        SELECT COUNT(s) FROM Sms s
        WHERE s.bulkCreatedAt >= :since
        """
    )
    long countRecentSms(@Param("since") Instant since);

    @Query(
        """
            SELECT s FROM Sms s
            WHERE s.user_login = :login
            ORDER BY s.sendDate DESC
        """
    )
    Page<Sms> findAllByUser(@Param("login") String login, Pageable pageable);

    /**
     * 🔐 Sécuriser GET /sms/{id} pour empêcher accès à l’ID d’un autre user
     */
    @Query(
        """
            SELECT s FROM Sms s
            WHERE s.id = :id
              AND s.user_login = :login
        """
    )
    Optional<Sms> findByIdAndUser(@Param("id") Long id, @Param("login") String login);
}
