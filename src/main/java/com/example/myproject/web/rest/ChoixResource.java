package com.example.myproject.web.rest;

import com.example.myproject.domain.Choix;
import com.example.myproject.repository.ChoixRepository;
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
 * REST controller for managing {@link com.example.myproject.domain.Choix}.
 */
@RestController
@RequestMapping("/api/choixes")
@Transactional
public class ChoixResource {

    private final Logger log = LoggerFactory.getLogger(ChoixResource.class);

    private static final String ENTITY_NAME = "choix";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final ChoixRepository choixRepository;

    public ChoixResource(ChoixRepository choixRepository) {
        this.choixRepository = choixRepository;
    }

    /**
     * {@code POST  /choixes} : Create a new choix.
     *
     * @param choix the choix to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new choix, or with status {@code 400 (Bad Request)} if the choix has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<Choix> createChoix(@RequestBody Choix choix) throws URISyntaxException {
        log.debug("REST request to save Choix : {}", choix);
        if (choix.getId() != null) {
            throw new BadRequestAlertException("A new choix cannot already have an ID", ENTITY_NAME, "idexists");
        }
        choix = choixRepository.save(choix);
        return ResponseEntity.created(new URI("/api/choixes/" + choix.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, choix.getId().toString()))
            .body(choix);
    }

    /**
     * {@code PUT  /choixes/:id} : Updates an existing choix.
     *
     * @param id the id of the choix to save.
     * @param choix the choix to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated choix,
     * or with status {@code 400 (Bad Request)} if the choix is not valid,
     * or with status {@code 500 (Internal Server Error)} if the choix couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Choix> updateChoix(@PathVariable(value = "id", required = false) final Long id, @RequestBody Choix choix)
        throws URISyntaxException {
        log.debug("REST request to update Choix : {}, {}", id, choix);
        if (choix.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, choix.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!choixRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        choix = choixRepository.save(choix);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, choix.getId().toString()))
            .body(choix);
    }

    /**
     * {@code PATCH  /choixes/:id} : Partial updates given fields of an existing choix, field will ignore if it is null
     *
     * @param id the id of the choix to save.
     * @param choix the choix to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated choix,
     * or with status {@code 400 (Bad Request)} if the choix is not valid,
     * or with status {@code 404 (Not Found)} if the choix is not found,
     * or with status {@code 500 (Internal Server Error)} if the choix couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<Choix> partialUpdateChoix(@PathVariable(value = "id", required = false) final Long id, @RequestBody Choix choix)
        throws URISyntaxException {
        log.debug("REST request to partial update Choix partially : {}, {}", id, choix);
        if (choix.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, choix.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!choixRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<Choix> result = choixRepository
            .findById(choix.getId())
            .map(existingChoix -> {
                if (choix.getChovaleur() != null) {
                    existingChoix.setChovaleur(choix.getChovaleur());
                }

                return existingChoix;
            })
            .map(choixRepository::save);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, choix.getId().toString())
        );
    }

    /**
     * {@code GET  /choixes} : get all the choixes.
     *
     * @param pageable the pagination information.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of choixes in body.
     */
    @GetMapping("")
    public ResponseEntity<List<Choix>> getAllChoixes(@org.springdoc.core.annotations.ParameterObject Pageable pageable) {
        log.debug("REST request to get a page of Choixes");
        Page<Choix> page = choixRepository.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /choixes/:id} : get the "id" choix.
     *
     * @param id the id of the choix to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the choix, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Choix> getChoix(@PathVariable("id") Long id) {
        log.debug("REST request to get Choix : {}", id);
        Optional<Choix> choix = choixRepository.findById(id);
        return ResponseUtil.wrapOrNotFound(choix);
    }

    /**
     * {@code DELETE  /choixes/:id} : delete the "id" choix.
     *
     * @param id the id of the choix to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteChoix(@PathVariable("id") Long id) {
        log.debug("REST request to delete Choix : {}", id);
        choixRepository.deleteById(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
