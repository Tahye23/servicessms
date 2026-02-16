package com.example.myproject.repository;

import com.example.myproject.domain.MessageDeliveryStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageDeliveryStatusRepository extends JpaRepository<MessageDeliveryStatus, Long> {
    Optional<MessageDeliveryStatus> findByMessageIdAndStatus(String messageId, String status);
    Optional<MessageDeliveryStatus> findByMessageId(String messageId);
    Optional<MessageDeliveryStatus> findTopByMessageIdOrderByReceivedAtDesc(String messageId);
    List<MessageDeliveryStatus> findByProcessedAtIsNull();
    List<MessageDeliveryStatus> findByProcessedAtBefore(Instant cutoff);
    List<MessageDeliveryStatus> findByProcessedAtIsNull(Pageable pageable);
    List<MessageDeliveryStatus> findAllByMessageId(String messageId);

    @Query("SELECT m FROM MessageDeliveryStatus m WHERE m.processedAt < :cutoff")
    List<MessageDeliveryStatus> findTopNByProcessedAtBefore(@Param("cutoff") Instant cutoff, Pageable pageable);

    // Ou, si tu veux gérer la taille dans la méthode :
    default List<MessageDeliveryStatus> findTopNByProcessedAtBefore(Instant cutoff, int limit) {
        return findTopNByProcessedAtBefore(cutoff, PageRequest.of(0, limit));
    }
}
