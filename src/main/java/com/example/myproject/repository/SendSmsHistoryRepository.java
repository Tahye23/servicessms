package com.example.myproject.repository;

import com.example.myproject.domain.SendSmsHistory;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SendSmsHistoryRepository extends JpaRepository<SendSmsHistory, Long> {
    List<SendSmsHistory> findBySendSmsIdOrderByRetryCountAsc(Long sendSmsId);

    @Query("SELECT h FROM SendSmsHistory h WHERE h.sendSmsId = :sendSmsId ORDER BY h.attemptDate DESC")
    List<SendSmsHistory> findBySendSmsIdOrderByAttemptDateDesc(@Param("sendSmsId") Long sendSmsId);

    @Query("SELECT COUNT(h) FROM SendSmsHistory h WHERE h.sendSmsId = :sendSmsId")
    Integer countBySendSmsId(@Param("sendSmsId") Long sendSmsId);

    // ✅ NOUVELLE MÉTHODE pour trouver un historique spécifique
    @Query("SELECT h FROM SendSmsHistory h WHERE h.sendSmsId = :sendSmsId AND h.retryCount = :retryCount")
    Optional<SendSmsHistory> findBySendSmsIdAndRetryCount(@Param("sendSmsId") Long sendSmsId, @Param("retryCount") Integer retryCount);

    // ✅ MÉTHODE pour trouver les tentatives non terminées
    @Query("SELECT h FROM SendSmsHistory h WHERE h.sendSmsId = :sendSmsId AND h.completionStatus = 'IN_PROGRESS'")
    List<SendSmsHistory> findInProgressBySendSmsId(@Param("sendSmsId") Long sendSmsId);
}
