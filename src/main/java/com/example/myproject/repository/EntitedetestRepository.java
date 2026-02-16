package com.example.myproject.repository;

import com.example.myproject.domain.Entitedetest;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Entitedetest entity.
 */
@SuppressWarnings("unused")
@Repository
public interface EntitedetestRepository extends JpaRepository<Entitedetest, Long> {}
