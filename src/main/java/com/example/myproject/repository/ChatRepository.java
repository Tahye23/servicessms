package com.example.myproject.repository;

import com.example.myproject.domain.Chat;
import com.example.myproject.domain.enumeration.Channel;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface ChatRepository extends JpaRepository<Chat, Long> {
    Optional<Chat> findByContactIdAndChannel(Long contactId, Channel channel);

    @Modifying
    @Query(value = "DELETE FROM chat", nativeQuery = true)
    int deleteAllChat();

    List<Chat> findByContactId(Long contactId);
}
