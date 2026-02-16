package com.example.myproject.repository;

import com.example.myproject.domain.Dialogue;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Dialogue entity.
 */
@SuppressWarnings("unused")
@Repository
public interface DialogueRepository extends JpaRepository<Dialogue, Long> {}
