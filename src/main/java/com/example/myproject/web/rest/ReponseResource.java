package com.example.myproject.web.rest;

import com.example.myproject.domain.Reponse;
import com.example.myproject.repository.ReponseRepository;
import com.example.myproject.web.rest.errors.BadRequestAlertException;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.PaginationUtil;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link com.example.myproject.domain.Reponse}.
 */
@RestController
@RequestMapping("/api/reponses")
@Transactional
public class ReponseResource {

    private final Logger log = LoggerFactory.getLogger(ReponseResource.class);

    private static final String ENTITY_NAME = "reponse";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final ReponseRepository reponseRepository;

    public ReponseResource(ReponseRepository reponseRepository) {
        this.reponseRepository = reponseRepository;
    }

    /**
     * {@code POST  /reponses} : Create a new reponse.
     *
     * @param reponse the reponse to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new reponse, or with status {@code 400 (Bad Request)} if the reponse has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<Reponse> createReponse(@RequestBody Reponse reponse) throws URISyntaxException {
        log.debug("REST request to save Reponse : {}", reponse);
        if (reponse.getId() != null) {
            throw new BadRequestAlertException("A new reponse cannot already have an ID", ENTITY_NAME, "idexists");
        }
        reponse = reponseRepository.save(reponse);
        return ResponseEntity.created(new URI("/api/reponses/" + reponse.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, reponse.getId().toString()))
            .body(reponse);
    }

    /**
     * {@code PUT  /reponses/:id} : Updates an existing reponse.
     *
     * @param id the id of the reponse to save.
     * @param reponse the reponse to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated reponse,
     * or with status {@code 400 (Bad Request)} if the reponse is not valid,
     * or with status {@code 500 (Internal Server Error)} if the reponse couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Reponse> updateReponse(@PathVariable(value = "id", required = false) final Long id, @RequestBody Reponse reponse)
        throws URISyntaxException {
        log.debug("REST request to update Reponse : {}, {}", id, reponse);
        if (reponse.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, reponse.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!reponseRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        reponse = reponseRepository.save(reponse);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, reponse.getId().toString()))
            .body(reponse);
    }

    /**
     * {@code PATCH  /reponses/:id} : Partial updates given fields of an existing reponse, field will ignore if it is null
     *
     * @param id the id of the reponse to save.
     * @param reponse the reponse to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated reponse,
     * or with status {@code 400 (Bad Request)} if the reponse is not valid,
     * or with status {@code 404 (Not Found)} if the reponse is not found,
     * or with status {@code 500 (Internal Server Error)} if the reponse couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<Reponse> partialUpdateReponse(
        @PathVariable(value = "id", required = false) final Long id,
        @RequestBody Reponse reponse
    ) throws URISyntaxException {
        log.debug("REST request to partial update Reponse partially : {}, {}", id, reponse);
        if (reponse.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, reponse.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!reponseRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<Reponse> result = reponseRepository
            .findById(reponse.getId())
            .map(existingReponse -> {
                if (reponse.getRepvaleur() != null) {
                    existingReponse.setRepvaleur(reponse.getRepvaleur());
                }

                return existingReponse;
            })
            .map(reponseRepository::save);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, reponse.getId().toString())
        );
    }

    /**
     * {@code GET  /reponses} : get all the reponses.
     *
     * @param pageable the pagination information.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of reponses in body.
     */
    @GetMapping("")
    public ResponseEntity<List<Reponse>> getAllReponses(@org.springdoc.core.annotations.ParameterObject Pageable pageable) {
        log.debug("REST request to get a page of Reponses");
        Page<Reponse> page = reponseRepository.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /reponses/:id} : get the "id" reponse.
     *
     * @param id the id of the reponse to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the reponse, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Reponse> getReponse(@PathVariable("id") Long id) {
        log.debug("REST request to get Reponse : {}", id);
        Optional<Reponse> reponse = reponseRepository.findById(id);
        return ResponseUtil.wrapOrNotFound(reponse);
    }

    /**
     * {@code DELETE  /reponses/:id} : delete the "id" reponse.
     *
     * @param id the id of the reponse to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReponse(@PathVariable("id") Long id) {
        log.debug("REST request to delete Reponse : {}", id);
        reponseRepository.deleteById(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
