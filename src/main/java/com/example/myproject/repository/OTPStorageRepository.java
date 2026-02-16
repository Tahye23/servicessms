package com.example.myproject.repository;

import com.example.myproject.domain.OTPStorage;
import java.util.List; // Ajouter cette ligne pour importer List
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the OTPStorage entity.
 */
@SuppressWarnings("unused")
@Repository
public interface OTPStorageRepository extends JpaRepository<OTPStorage, Long> {
    // Optional<OTPStorage> findById(Long id); // Recherche de l'objet par ID
    Optional<OTPStorage> findByOtsOTP(String otsOTP);
    List<OTPStorage> findAllByIsExpiredFalse();
    //Optional<OTPStorage> findByOtsOTP(String otsOTP);
}
