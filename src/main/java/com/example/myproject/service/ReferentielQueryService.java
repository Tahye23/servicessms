package com.example.myproject.service;

import com.example.myproject.domain.*; // for static metamodels
import com.example.myproject.domain.Referentiel;
import com.example.myproject.repository.ReferentielRepository;
import com.example.myproject.service.criteria.ReferentielCriteria;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.jhipster.service.QueryService;

/**
 * Service for executing complex queries for {@link Referentiel} entities in the database.
 * The main input is a {@link ReferentielCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link Referentiel} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class ReferentielQueryService extends QueryService<Referentiel> {

    private final Logger log = LoggerFactory.getLogger(ReferentielQueryService.class);

    private final ReferentielRepository referentielRepository;

    public ReferentielQueryService(ReferentielRepository referentielRepository) {
        this.referentielRepository = referentielRepository;
    }

    /**
     * Return a {@link Page} of {@link Referentiel} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<Referentiel> findByCriteria(ReferentielCriteria criteria, Pageable page) {
        log.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<Referentiel> specification = createSpecification(criteria);
        return referentielRepository.findAll(specification, page);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(ReferentielCriteria criteria) {
        log.debug("count by criteria : {}", criteria);
        final Specification<Referentiel> specification = createSpecification(criteria);
        return referentielRepository.count(specification);
    }

    /**
     * Function to convert {@link ReferentielCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<Referentiel> createSpecification(ReferentielCriteria criteria) {
        Specification<Referentiel> specification = Specification.where(null);
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            if (criteria.getDistinct() != null) {
                specification = specification.and(distinct(criteria.getDistinct()));
            }
            if (criteria.getId() != null) {
                specification = specification.and(buildRangeSpecification(criteria.getId(), Referentiel_.id));
            }
            if (criteria.getRefCode() != null) {
                specification = specification.and(buildStringSpecification(criteria.getRefCode(), Referentiel_.refCode));
            }
            if (criteria.getRefRadical() != null) {
                specification = specification.and(buildStringSpecification(criteria.getRefRadical(), Referentiel_.refRadical));
            }
            if (criteria.getRefFrTitle() != null) {
                specification = specification.and(buildStringSpecification(criteria.getRefFrTitle(), Referentiel_.refFrTitle));
            }
            if (criteria.getRefArTitle() != null) {
                specification = specification.and(buildStringSpecification(criteria.getRefArTitle(), Referentiel_.refArTitle));
            }
            if (criteria.getRefEnTitle() != null) {
                specification = specification.and(buildStringSpecification(criteria.getRefEnTitle(), Referentiel_.refEnTitle));
            }
        }
        return specification;
    }
}
