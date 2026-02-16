package com.example.myproject.repository;

import com.example.myproject.domain.SendWhatsapp;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface SendWhatsappRepository extends JpaRepository<SendWhatsapp, Long> {
    @Query("SELECT s FROM SendWhatsapp s ")
    Page<SendWhatsapp> findAllSms(Pageable pageable);
}
