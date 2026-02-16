package com.example.myproject.repository;

import com.example.myproject.domain.Fileextrait;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Fileextrait entity.
 */
@SuppressWarnings("unused")
@Repository
public interface FileextraitRepository extends JpaRepository<Fileextrait, Long> {}
