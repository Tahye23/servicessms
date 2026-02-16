package com.example.myproject.repository;

import com.example.myproject.domain.SmsDlr;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SmsDlrRepository extends JpaRepository<SmsDlr, Long> {
    /**
     * Trouver un DLR par message_id et status code
     * (pour éviter doublons)
     */
    Optional<SmsDlr> findByMessageIdAndDlrStatusCode(String messageId, Integer dlrStatusCode);

    /**
     * Trouver tous les DLR d'un SMS
     */
    List<SmsDlr> findBySmsIdOrderByReceivedAtDesc(Long smsId);

    /**
     * Trouver le dernier DLR d'un message_id
     */
    Optional<SmsDlr> findFirstByMessageIdOrderByReceivedAtDesc(String messageId);

    /**
     * Trouver les DLR non traités
     */
    List<SmsDlr> findByProcessedFalseOrderByReceivedAtAsc();

    /**
     * Compter les DLR non traités
     */
    long countByProcessedFalse();

    /**
     * Trouver les DLR reçus après une certaine date
     */
    List<SmsDlr> findByReceivedAtAfterAndProcessedFalse(Instant after);
}
