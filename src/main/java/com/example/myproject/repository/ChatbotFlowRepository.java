package com.example.myproject.repository;

import com.example.myproject.domain.ChatbotFlow;
import java.util.List;
import java.util.Optional;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface ChatbotFlowRepository extends JpaRepository<ChatbotFlow, Long> {
    // Un utilisateur = un seul flow
    Optional<ChatbotFlow> findByUserId(Long userId);

    @Modifying
    @Transactional
    @Query("UPDATE ChatbotFlow cf SET cf.active = false WHERE cf.userId = :userId")
    void deactivateAllUserFlows(@Param("userId") Long userId);
}
