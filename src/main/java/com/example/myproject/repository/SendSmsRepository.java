package com.example.myproject.repository;

import com.example.myproject.domain.ExtendedUser;
import com.example.myproject.domain.SendSms;
import com.example.myproject.domain.enumeration.MessageType;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SendSmsRepository extends JpaRepository<SendSms, Long> {
    // ===== MÉTHODES EXISTANTES (conservées) =====

    List<SendSms> findByUser(ExtendedUser user);
    Optional<SendSms> findById(Long id);
    long count();
    long countByBulkId(String bulkId);
    long countByBulkIdAndBulkStatus(String bulkId, String status);
    boolean existsByBulkId(String bulkId);
    long countBySendateEnvoiAfter(ZonedDateTime date);
    Optional<SendSms> findOneByBulkId(String bulkId);

    @Query("SELECT s FROM SendSms s WHERE s.id = :id")
    Optional<SendSms> findSendSmsById(@Param("id") Long id);

    @Modifying
    @Query("DELETE FROM SendSms s WHERE s.destinataires.id = :groupeId")
    int deleteByGroupeId(@Param("groupeId") Long groupeId);

    @Modifying
    @Query(value = "DELETE FROM send_sms", nativeQuery = true)
    int deleteAllSendSms();

    @Query(
        """
        SELECT s FROM SendSms s
        WHERE (LOWER(s.user.user.login) = LOWER(:userLogin) OR :userLogin IS NULL)
          AND (s.isSent = :isSent OR :isSent IS NULL)
          AND (s.isbulk = :isBulk OR :isBulk IS NULL)
          AND (LOWER(s.connom) LIKE LOWER(CONCAT('%', :receiver, '%')) OR :receiver IS NULL)
          AND (LOWER(s.grotitre) LIKE LOWER(CONCAT('%', :receiver, '%')) OR :receiver IS NULL)
          AND (
              (LOWER(s.receiver) LIKE LOWER(CONCAT('%', :search, '%')) OR
               LOWER(s.sender) LIKE LOWER(CONCAT('%', :search, '%')) OR
               LOWER(s.titre) LIKE LOWER(CONCAT('%', :search, '%'))) OR :search IS NULL
          )
          AND (s.type = :type OR :type IS NULL)
        ORDER BY s.sendateEnvoi DESC
        """
    )
    Page<SendSms> findFiltered(
        @Param("userLogin") String userLogin,
        @Param("search") String search,
        @Param("isSent") Boolean isSent,
        @Param("isBulk") Boolean isBulk,
        @Param("receiver") String receiver,
        @Param("receivers") String receivers,
        @Param("type") MessageType type,
        Pageable pageable
    );

    @Query("SELECT s.deliveryStatus, COUNT(s) FROM SendSms s GROUP BY s.deliveryStatus")
    List<Object[]> countByDeliveryStatusGrouped();

    @Query(
        """
        SELECT
            SUM(CASE WHEN s.isSent = true THEN 1 ELSE 0 END),
            SUM(CASE WHEN s.isSent = false THEN 1 ELSE 0 END),
            SUM(CASE WHEN s.isSent IS NULL THEN 1 ELSE 0 END)
        FROM SendSms s
        """
    )
    List<Object[]> countIsSentStats();

    @Query(
        "SELECT CAST(s.sendateEnvoi AS localdate) AS date, COUNT(s) " +
        "FROM SendSms s " +
        "WHERE s.isSent = true AND s.sendateEnvoi >= :startDate " +
        "GROUP BY CAST(s.sendateEnvoi AS localdate)"
    )
    List<Object[]> countSentByDateLast7Days(@Param("startDate") ZonedDateTime startDate);

    @Query(
        "SELECT CAST(s.sendateEnvoi AS localdate) AS date, COUNT(s) " +
        "FROM SendSms s " +
        "WHERE s.sendateEnvoi BETWEEN :startDate AND :endDate And s.user.user.login = :login " +
        "GROUP BY CAST(s.sendateEnvoi AS localdate)"
    )
    List<Object[]> countSmsBetweenDatesRaw(
        @Param("login") String login,
        @Param("startDate") ZonedDateTime startDate,
        @Param("endDate") ZonedDateTime endDate
    );

    @Query(
        "SELECT s.isSent, COUNT(s) " +
        "FROM SendSms s " +
        "WHERE s.sendateEnvoi BETWEEN :startDate AND :endDate And s.user.user.login = :login " +
        "GROUP BY s.isSent"
    )
    List<Object[]> countSmsStatusBetweenDates(
        @Param("login") String login,
        @Param("startDate") ZonedDateTime startDate,
        @Param("endDate") ZonedDateTime endDate
    );

    long countByIsSent(Boolean isSent);

    @Query("SELECT COUNT(s) FROM SendSms s WHERE s.isSent = :isSent AND LOWER(s.user.user.login) = LOWER(:login)")
    long countByIsSentAndLogin(@Param("isSent") Boolean isSent, @Param("login") String login);

    @Query("SELECT COUNT(s) FROM SendSms s WHERE s.isSent IS NULL AND LOWER(s.user.user.login) = LOWER(:login)")
    long countPendingByLogin(@Param("login") String login);

    @Query(
        """
        SELECT s.type, COALESCE(SUM(s.totalMessage), 0), COALESCE(SUM(s.totalSent + s.totalDelivered + s.totalRead), 0), COALESCE(SUM(s.totalFailed), 0)
        FROM SendSms s
        WHERE LOWER(s.user.user.login) = LOWER(:login)
        GROUP BY s.type
        """
    )
    List<Object[]> sumStatsByLoginAndType(@Param("login") String login);

    @Query(
        """
        SELECT s.type, COALESCE(SUM(s.totalMessage), 0), COALESCE(SUM(s.totalSent + s.totalDelivered + s.totalRead), 0), COALESCE(SUM(s.totalFailed), 0)
        FROM SendSms s
        WHERE LOWER(s.user.user.login) = LOWER(:login)
          AND s.sendateEnvoi >= :startDate
          AND s.sendateEnvoi <= :endDate
        GROUP BY s.type
        """
    )
    List<Object[]> sumStatsByLoginAndTypeWithDate(
        @Param("login") String login,
        @Param("startDate") ZonedDateTime startDate,
        @Param("endDate") ZonedDateTime endDate
    );

    @Query(
        value = """
        SELECT ss.id AS id, ss.titre AS name
        FROM send_sms ss
        WHERE ss.destinataires_id = :groupeId
          AND (
              :q IS NULL
              OR LOWER(ss.titre) LIKE LOWER(CONCAT('%', :q, '%'))
              OR CAST(ss.id AS TEXT) LIKE CONCAT('%', :q, '%')
          )
        ORDER BY ss.id DESC
        """,
        countQuery = """
        SELECT COUNT(*)
        FROM send_sms ss
        WHERE ss.destinataires_id = :groupeId
          AND (
              :q IS NULL
              OR LOWER(ss.titre) LIKE LOWER(CONCAT('%', :q, '%'))
              OR CAST(ss.id AS TEXT) LIKE CONCAT('%', :q, '%')
          )
        """,
        nativeQuery = true
    )
    Page<CampaignSummaryRow> findSmsCampaignsByGroupe(@Param("groupeId") Long groupeId, @Param("q") String search, Pageable pageable);

    // ===== ✅ NOUVELLES MÉTHODES POUR MONITORING =====

    /**
     * ✅ TROUVER SendSms PAR BULK_ID (pour SmsMonitoringService)
     */
    @Query("SELECT s FROM SendSms s WHERE s.bulkId = :bulkId")
    SendSms findByBulkId(@Param("bulkId") String bulkId);

    /**
     * ✅ COMPTER LES CAMPAGNES EN COURS (inprocess=true)
     */
    @Query("SELECT COUNT(s) FROM SendSms s WHERE s.inprocess = true")
    long countByInprocess(@Param("inprocess") Boolean inprocess);

    // ===== INTERFACE POUR PROJECTION =====

    interface CampaignSummaryRow {
        Long getId();
        String getName();
    }
}
