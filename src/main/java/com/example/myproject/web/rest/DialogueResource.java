package com.example.myproject.web.rest;

import com.example.myproject.domain.Dialogue;
import com.example.myproject.repository.DialogueRepository;
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
 * REST controller for managing {@link com.example.myproject.domain.Dialogue}.
 */
@RestController
@RequestMapping("/api/dialogues")
@Transactional
public class DialogueResource {

    private final Logger log = LoggerFactory.getLogger(DialogueResource.class);

    private static final String ENTITY_NAME = "dialogue";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final DialogueRepository dialogueRepository;

    public DialogueResource(DialogueRepository dialogueRepository) {
        this.dialogueRepository = dialogueRepository;
    }

    /**
     * {@code POST  /dialogues} : Create a new dialogue.
     *
     * @param dialogue the dialogue to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new dialogue, or with status {@code 400 (Bad Request)} if the dialogue has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<Dialogue> createDialogue(@RequestBody Dialogue dialogue) throws URISyntaxException {
        log.debug("REST request to save Dialogue : {}", dialogue);
        if (dialogue.getId() != null) {
            throw new BadRequestAlertException("A new dialogue cannot already have an ID", ENTITY_NAME, "idexists");
        }
        dialogue = dialogueRepository.save(dialogue);
        return ResponseEntity.created(new URI("/api/dialogues/" + dialogue.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, dialogue.getId().toString()))
            .body(dialogue);
    }

    /**
     * {@code PUT  /dialogues/:id} : Updates an existing dialogue.
     *
     * @param id the id of the dialogue to save.
     * @param dialogue the dialogue to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated dialogue,
     * or with status {@code 400 (Bad Request)} if the dialogue is not valid,
     * or with status {@code 500 (Internal Server Error)} if the dialogue couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Dialogue> updateDialogue(
        @PathVariable(value = "id", required = false) final Long id,
        @RequestBody Dialogue dialogue
    ) throws URISyntaxException {
        log.debug("REST request to update Dialogue : {}, {}", id, dialogue);
        if (dialogue.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, dialogue.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!dialogueRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        dialogue = dialogueRepository.save(dialogue);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, dialogue.getId().toString()))
            .body(dialogue);
    }

    /**
     * {@code PATCH  /dialogues/:id} : Partial updates given fields of an existing dialogue, field will ignore if it is null
     *
     * @param id the id of the dialogue to save.
     * @param dialogue the dialogue to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated dialogue,
     * or with status {@code 400 (Bad Request)} if the dialogue is not valid,
     * or with status {@code 404 (Not Found)} if the dialogue is not found,
     * or with status {@code 500 (Internal Server Error)} if the dialogue couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<Dialogue> partialUpdateDialogue(
        @PathVariable(value = "id", required = false) final Long id,
        @RequestBody Dialogue dialogue
    ) throws URISyntaxException {
        log.debug("REST request to partial update Dialogue partially : {}, {}", id, dialogue);
        if (dialogue.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, dialogue.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!dialogueRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<Dialogue> result = dialogueRepository
            .findById(dialogue.getId())
            .map(existingDialogue -> {
                if (dialogue.getDialogueId() != null) {
                    existingDialogue.setDialogueId(dialogue.getDialogueId());
                }
                if (dialogue.getContenu() != null) {
                    existingDialogue.setContenu(dialogue.getContenu());
                }

                return existingDialogue;
            })
            .map(dialogueRepository::save);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, dialogue.getId().toString())
        );
    }

    /**
     * {@code GET  /dialogues} : get all the dialogues.
     *
     * @param pageable the pagination information.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of dialogues in body.
     */
    @GetMapping("")
    public ResponseEntity<List<Dialogue>> getAllDialogues(@org.springdoc.core.annotations.ParameterObject Pageable pageable) {
        log.debug("REST request to get a page of Dialogues");
        Page<Dialogue> page = dialogueRepository.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /dialogues/:id} : get the "id" dialogue.
     *
     * @param id the id of the dialogue to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the dialogue, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Dialogue> getDialogue(@PathVariable("id") Long id) {
        log.debug("REST request to get Dialogue : {}", id);
        Optional<Dialogue> dialogue = dialogueRepository.findById(id);
        return ResponseUtil.wrapOrNotFound(dialogue);
    }

    /**
     * {@code DELETE  /dialogues/:id} : delete the "id" dialogue.
     *
     * @param id the id of the dialogue to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDialogue(@PathVariable("id") Long id) {
        log.debug("REST request to delete Dialogue : {}", id);
        dialogueRepository.deleteById(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
