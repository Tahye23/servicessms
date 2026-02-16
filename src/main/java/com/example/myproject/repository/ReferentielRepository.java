package com.example.myproject.repository;

import com.example.myproject.domain.Referentiel;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Referentiel entity.
 */
@SuppressWarnings("unused")
@Repository
public interface ReferentielRepository extends JpaRepository<Referentiel, Long>, JpaSpecificationExecutor<Referentiel> {}
