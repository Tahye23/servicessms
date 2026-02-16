package com.example.myproject.repository;

import com.example.myproject.domain.Service;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Service entity.
 */
@SuppressWarnings("unused")
@Repository
public interface ServiceRepository extends JpaRepository<Service, Long> {}
