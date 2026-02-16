package com.example.myproject.repository;

import com.example.myproject.domain.Choix;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Choix entity.
 */
@SuppressWarnings("unused")
@Repository
public interface ChoixRepository extends JpaRepository<Choix, Long> {}
