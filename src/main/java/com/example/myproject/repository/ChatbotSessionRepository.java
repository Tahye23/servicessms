package com.example.myproject.repository;

import com.example.myproject.domain.ChatbotSession;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatbotSessionRepository extends JpaRepository<ChatbotSession, Long> {
    // AJOUTER ces méthodes utilisées dans le service :
    Optional<ChatbotSession> findByPhoneNumberAndUserLogin(String phoneNumber, String userLogin);

    @Query("SELECT s FROM ChatbotSession s WHERE s.phoneNumber = :phoneNumber AND s.userLogin = :userLogin AND s.isActive = true")
    Optional<ChatbotSession> findByPhoneNumberAndUserLoginAndIsActiveTrue(
        @Param("phoneNumber") String phoneNumber,
        @Param("userLogin") String userLogin
    );

    @Modifying
    @Query("UPDATE ChatbotSession s SET s.isActive = false WHERE s.phoneNumber = :phoneNumber AND s.userLogin = :userLogin")
    void deactivateSessionsForPhoneAndUser(@Param("phoneNumber") String phoneNumber, @Param("userLogin") String userLogin);
}
