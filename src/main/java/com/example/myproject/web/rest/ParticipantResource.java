package com.example.myproject.web.rest;

import com.example.myproject.domain.Participant;
import com.example.myproject.repository.ParticipantRepository;
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
 * REST controller for managing {@link com.example.myproject.domain.Participant}.
 */
@RestController
@RequestMapping("/api/participants")
@Transactional
public class ParticipantResource {

    private final Logger log = LoggerFactory.getLogger(ParticipantResource.class);

    private static final String ENTITY_NAME = "participant";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final ParticipantRepository participantRepository;

    public ParticipantResource(ParticipantRepository participantRepository) {
        this.participantRepository = participantRepository;
    }

    /**
     * {@code POST  /participants} : Create a new participant.
     *
     * @param participant the participant to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new participant, or with status {@code 400 (Bad Request)} if the participant has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<Participant> createParticipant(@RequestBody Participant participant) throws URISyntaxException {
        log.debug("REST request to save Participant : {}", participant);
        if (participant.getId() != null) {
            throw new BadRequestAlertException("A new participant cannot already have an ID", ENTITY_NAME, "idexists");
        }
        participant = participantRepository.save(participant);
        return ResponseEntity.created(new URI("/api/participants/" + participant.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, participant.getId().toString()))
            .body(participant);
    }

    /**
     * {@code PUT  /participants/:id} : Updates an existing participant.
     *
     * @param id the id of the participant to save.
     * @param participant the participant to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated participant,
     * or with status {@code 400 (Bad Request)} if the participant is not valid,
     * or with status {@code 500 (Internal Server Error)} if the participant couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Participant> updateParticipant(
        @PathVariable(value = "id", required = false) final Long id,
        @RequestBody Participant participant
    ) throws URISyntaxException {
        log.debug("REST request to update Participant : {}, {}", id, participant);
        if (participant.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, participant.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!participantRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        participant = participantRepository.save(participant);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, participant.getId().toString()))
            .body(participant);
    }

    /**
     * {@code PATCH  /participants/:id} : Partial updates given fields of an existing participant, field will ignore if it is null
     *
     * @param id the id of the participant to save.
     * @param participant the participant to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated participant,
     * or with status {@code 400 (Bad Request)} if the participant is not valid,
     * or with status {@code 404 (Not Found)} if the participant is not found,
     * or with status {@code 500 (Internal Server Error)} if the participant couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<Participant> partialUpdateParticipant(
        @PathVariable(value = "id", required = false) final Long id,
        @RequestBody Participant participant
    ) throws URISyntaxException {
        log.debug("REST request to partial update Participant partially : {}, {}", id, participant);
        if (participant.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, participant.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!participantRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<Participant> result = participantRepository.findById(participant.getId()).map(participantRepository::save);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, participant.getId().toString())
        );
    }

    /**
     * {@code GET  /participants} : get all the participants.
     *
     * @param pageable the pagination information.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of participants in body.
     */
    @GetMapping("")
    public ResponseEntity<List<Participant>> getAllParticipants(@org.springdoc.core.annotations.ParameterObject Pageable pageable) {
        log.debug("REST request to get a page of Participants");
        Page<Participant> page = participantRepository.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /participants/:id} : get the "id" participant.
     *
     * @param id the id of the participant to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the participant, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Participant> getParticipant(@PathVariable("id") Long id) {
        log.debug("REST request to get Participant : {}", id);
        Optional<Participant> participant = participantRepository.findById(id);
        return ResponseUtil.wrapOrNotFound(participant);
    }

    /**
     * {@code DELETE  /participants/:id} : delete the "id" participant.
     *
     * @param id the id of the participant to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteParticipant(@PathVariable("id") Long id) {
        log.debug("REST request to delete Participant : {}", id);
        participantRepository.deleteById(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
