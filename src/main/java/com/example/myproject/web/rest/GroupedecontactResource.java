package com.example.myproject.web.rest;

import com.example.myproject.domain.Groupedecontact;
import com.example.myproject.repository.GroupedecontactRepository;
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
 * REST controller for managing {@link com.example.myproject.domain.Groupedecontact}.
 */
@RestController
@RequestMapping("/api/groupedecontacts")
@Transactional
public class GroupedecontactResource {

    private final Logger log = LoggerFactory.getLogger(GroupedecontactResource.class);

    private static final String ENTITY_NAME = "groupedecontact";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final GroupedecontactRepository groupedecontactRepository;

    public GroupedecontactResource(GroupedecontactRepository groupedecontactRepository) {
        this.groupedecontactRepository = groupedecontactRepository;
    }

    /**
     * {@code POST  /groupedecontacts} : Create a new groupedecontact.
     *
     * @param groupedecontact the groupedecontact to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new groupedecontact, or with status {@code 400 (Bad Request)} if the groupedecontact has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<Groupedecontact> createGroupedecontact(@RequestBody Groupedecontact groupedecontact) throws URISyntaxException {
        log.debug("REST request to save Groupedecontact : {}", groupedecontact);
        if (groupedecontact.getId() != null) {
            throw new BadRequestAlertException("A new groupedecontact cannot already have an ID", ENTITY_NAME, "idexists");
        }
        groupedecontact = groupedecontactRepository.save(groupedecontact);
        return ResponseEntity.created(new URI("/api/groupedecontacts/" + groupedecontact.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, groupedecontact.getId().toString()))
            .body(groupedecontact);
    }

    /**
     * {@code PUT  /groupedecontacts/:id} : Updates an existing groupedecontact.
     *
     * @param id the id of the groupedecontact to save.
     * @param groupedecontact the groupedecontact to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated groupedecontact,
     * or with status {@code 400 (Bad Request)} if the groupedecontact is not valid,
     * or with status {@code 500 (Internal Server Error)} if the groupedecontact couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Groupedecontact> updateGroupedecontact(
        @PathVariable(value = "id", required = false) final Long id,
        @RequestBody Groupedecontact groupedecontact
    ) throws URISyntaxException {
        log.debug("REST request to update Groupedecontact : {}, {}", id, groupedecontact);
        if (groupedecontact.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, groupedecontact.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!groupedecontactRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        groupedecontact = groupedecontactRepository.save(groupedecontact);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, groupedecontact.getId().toString()))
            .body(groupedecontact);
    }

    /**
     * {@code PATCH  /groupedecontacts/:id} : Partial updates given fields of an existing groupedecontact, field will ignore if it is null
     *
     * @param id the id of the groupedecontact to save.
     * @param groupedecontact the groupedecontact to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated groupedecontact,
     * or with status {@code 400 (Bad Request)} if the groupedecontact is not valid,
     * or with status {@code 404 (Not Found)} if the groupedecontact is not found,
     * or with status {@code 500 (Internal Server Error)} if the groupedecontact couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<Groupedecontact> partialUpdateGroupedecontact(
        @PathVariable(value = "id", required = false) final Long id,
        @RequestBody Groupedecontact groupedecontact
    ) throws URISyntaxException {
        log.debug("REST request to partial update Groupedecontact partially : {}, {}", id, groupedecontact);
        if (groupedecontact.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, groupedecontact.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!groupedecontactRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<Groupedecontact> result = groupedecontactRepository.findById(groupedecontact.getId()).map(groupedecontactRepository::save);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, groupedecontact.getId().toString())
        );
    }

    /**
     * {@code GET  /groupedecontacts} : get all the groupedecontacts.
     *
     * @param pageable the pagination information.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of groupedecontacts in body.
     */
    @GetMapping("")
    public ResponseEntity<List<Groupedecontact>> getAllGroupedecontacts(@org.springdoc.core.annotations.ParameterObject Pageable pageable) {
        log.debug("REST request to get a page of Groupedecontacts");
        Page<Groupedecontact> page = groupedecontactRepository.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /groupedecontacts/:id} : get the "id" groupedecontact.
     *
     * @param id the id of the groupedecontact to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the groupedecontact, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Groupedecontact> getGroupedecontact(@PathVariable("id") Long id) {
        log.debug("REST request to get Groupedecontact : {}", id);
        Optional<Groupedecontact> groupedecontact = groupedecontactRepository.findById(id);
        return ResponseUtil.wrapOrNotFound(groupedecontact);
    }

    /**
     * {@code DELETE  /groupedecontacts/:id} : delete the "id" groupedecontact.
     *
     * @param id the id of the groupedecontact to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGroupedecontact(@PathVariable("id") Long id) {
        log.debug("REST request to delete Groupedecontact : {}", id);
        groupedecontactRepository.deleteById(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
