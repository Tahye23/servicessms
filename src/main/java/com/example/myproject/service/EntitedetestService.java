package com.example.myproject.service;

import com.example.myproject.domain.Entitedetest;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service Interface for managing {@link com.example.myproject.domain.Entitedetest}.
 */
public interface EntitedetestService {
    /**
     * Save a entitedetest.
     *
     * @param entitedetest the entity to save.
     * @return the persisted entity.
     */
    Entitedetest save(Entitedetest entitedetest);

    /**
     * Updates a entitedetest.
     *
     * @param entitedetest the entity to update.
     * @return the persisted entity.
     */
    Entitedetest update(Entitedetest entitedetest);

    /**
     * Partially updates a entitedetest.
     *
     * @param entitedetest the entity to update partially.
     * @return the persisted entity.
     */
    Optional<Entitedetest> partialUpdate(Entitedetest entitedetest);

    /**
     * Get all the entitedetests.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    Page<Entitedetest> findAll(Pageable pageable);

    /**
     * Get the "id" entitedetest.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Optional<Entitedetest> findOne(Long id);

    /**
     * Delete the "id" entitedetest.
     *
     * @param id the id of the entity.
     */
    void delete(Long id);
}
