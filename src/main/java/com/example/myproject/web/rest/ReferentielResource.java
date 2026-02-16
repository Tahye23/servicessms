package com.example.myproject.web.rest;

import com.example.myproject.domain.Referentiel;
import com.example.myproject.repository.ReferentielRepository;
import com.example.myproject.service.ReferentielQueryService;
import com.example.myproject.service.ReferentielService;
import com.example.myproject.service.criteria.ReferentielCriteria;
import com.example.myproject.web.rest.errors.BadRequestAlertException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.PaginationUtil;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link com.example.myproject.domain.Referentiel}.
 */
@RestController
@RequestMapping("/api/referentiels")
public class ReferentielResource {

    private final Logger log = LoggerFactory.getLogger(ReferentielResource.class);

    private static final String ENTITY_NAME = "referentiel";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final ReferentielService referentielService;

    private final ReferentielRepository referentielRepository;

    private final ReferentielQueryService referentielQueryService;

    public ReferentielResource(
        ReferentielService referentielService,
        ReferentielRepository referentielRepository,
        ReferentielQueryService referentielQueryService
    ) {
        this.referentielService = referentielService;
        this.referentielRepository = referentielRepository;
        this.referentielQueryService = referentielQueryService;
    }

    /**
     * {@code POST  /referentiels} : Create a new referentiel.
     *
     * @param referentiel the referentiel to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new referentiel, or with status {@code 400 (Bad Request)} if the referentiel has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<Referentiel> createReferentiel(@Valid @RequestBody Referentiel referentiel) throws URISyntaxException {
        log.debug("REST request to save Referentiel : {}", referentiel);
        if (referentiel.getId() != null) {
            throw new BadRequestAlertException("A new referentiel cannot already have an ID", ENTITY_NAME, "idexists");
        }
        referentiel = referentielService.save(referentiel);
        return ResponseEntity.created(new URI("/api/referentiels/" + referentiel.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, referentiel.getId().toString()))
            .body(referentiel);
    }

    /**
     * {@code PUT  /referentiels/:id} : Updates an existing referentiel.
     *
     * @param id the id of the referentiel to save.
     * @param referentiel the referentiel to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated referentiel,
     * or with status {@code 400 (Bad Request)} if the referentiel is not valid,
     * or with status {@code 500 (Internal Server Error)} if the referentiel couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Referentiel> updateReferentiel(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody Referentiel referentiel
    ) throws URISyntaxException {
        log.debug("REST request to update Referentiel : {}, {}", id, referentiel);
        if (referentiel.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, referentiel.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!referentielRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        referentiel = referentielService.update(referentiel);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, referentiel.getId().toString()))
            .body(referentiel);
    }

    /**
     * {@code PATCH  /referentiels/:id} : Partial updates given fields of an existing referentiel, field will ignore if it is null
     *
     * @param id the id of the referentiel to save.
     * @param referentiel the referentiel to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated referentiel,
     * or with status {@code 400 (Bad Request)} if the referentiel is not valid,
     * or with status {@code 404 (Not Found)} if the referentiel is not found,
     * or with status {@code 500 (Internal Server Error)} if the referentiel couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<Referentiel> partialUpdateReferentiel(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody Referentiel referentiel
    ) throws URISyntaxException {
        log.debug("REST request to partial update Referentiel partially : {}, {}", id, referentiel);
        if (referentiel.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, referentiel.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!referentielRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<Referentiel> result = referentielService.partialUpdate(referentiel);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, referentiel.getId().toString())
        );
    }

    /**
     * {@code GET  /referentiels} : get all the referentiels.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of referentiels in body.
     */
    @GetMapping("")
    public ResponseEntity<List<Referentiel>> getAllReferentiels(
        ReferentielCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        log.debug("REST request to get Referentiels by criteria: {}", criteria);

        Page<Referentiel> page = referentielQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /referentiels/count} : count all the referentiels.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countReferentiels(ReferentielCriteria criteria) {
        log.debug("REST request to count Referentiels by criteria: {}", criteria);
        return ResponseEntity.ok().body(referentielQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /referentiels/:id} : get the "id" referentiel.
     *
     * @param id the id of the referentiel to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the referentiel, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Referentiel> getReferentiel(@PathVariable("id") Long id) {
        log.debug("REST request to get Referentiel : {}", id);
        Optional<Referentiel> referentiel = referentielService.findOne(id);
        return ResponseUtil.wrapOrNotFound(referentiel);
    }

    /**
     * {@code DELETE  /referentiels/:id} : delete the "id" referentiel.
     *
     * @param id the id of the referentiel to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReferentiel(@PathVariable("id") Long id) {
        log.debug("REST request to delete Referentiel : {}", id);
        referentielService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
