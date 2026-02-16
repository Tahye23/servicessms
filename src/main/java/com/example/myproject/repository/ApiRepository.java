package com.example.myproject.repository;

import com.example.myproject.domain.Api;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Api entity.
 */
@SuppressWarnings("unused")
@Repository
public interface ApiRepository extends JpaRepository<Api, Long> {}
