package com.example.myproject.service;

import com.example.myproject.domain.Referentiel;
import com.example.myproject.repository.ReferentielRepository;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link com.example.myproject.domain.Referentiel}.
 */
@Service
@Transactional
public class ReferentielService {

    private final Logger log = LoggerFactory.getLogger(ReferentielService.class);

    private final ReferentielRepository referentielRepository;

    public ReferentielService(ReferentielRepository referentielRepository) {
        this.referentielRepository = referentielRepository;
    }

    /**
     * Save a referentiel.
     *
     * @param referentiel the entity to save.
     * @return the persisted entity.
     */
    public Referentiel save(Referentiel referentiel) {
        log.debug("Request to save Referentiel : {}", referentiel);
        return referentielRepository.save(referentiel);
    }

    /**
     * Update a referentiel.
     *
     * @param referentiel the entity to save.
     * @return the persisted entity.
     */
    public Referentiel update(Referentiel referentiel) {
        log.debug("Request to update Referentiel : {}", referentiel);
        return referentielRepository.save(referentiel);
    }

    /**
     * Partially update a referentiel.
     *
     * @param referentiel the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<Referentiel> partialUpdate(Referentiel referentiel) {
        log.debug("Request to partially update Referentiel : {}", referentiel);

        return referentielRepository
            .findById(referentiel.getId())
            .map(existingReferentiel -> {
                if (referentiel.getRefCode() != null) {
                    existingReferentiel.setRefCode(referentiel.getRefCode());
                }
                if (referentiel.getRefRadical() != null) {
                    existingReferentiel.setRefRadical(referentiel.getRefRadical());
                }
                if (referentiel.getRefFrTitle() != null) {
                    existingReferentiel.setRefFrTitle(referentiel.getRefFrTitle());
                }
                if (referentiel.getRefArTitle() != null) {
                    existingReferentiel.setRefArTitle(referentiel.getRefArTitle());
                }
                if (referentiel.getRefEnTitle() != null) {
                    existingReferentiel.setRefEnTitle(referentiel.getRefEnTitle());
                }

                return existingReferentiel;
            })
            .map(referentielRepository::save);
    }

    /**
     * Get one referentiel by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<Referentiel> findOne(Long id) {
        log.debug("Request to get Referentiel : {}", id);
        return referentielRepository.findById(id);
    }

    /**
     * Delete the referentiel by id.
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        log.debug("Request to delete Referentiel : {}", id);
        referentielRepository.deleteById(id);
    }
}
