package com.example.myproject.web.rest;

import com.example.myproject.domain.UserTokenApi;
import com.example.myproject.repository.UserTokenApiRepository;
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
 * REST controller for managing {@link com.example.myproject.domain.UserTokenApi}.
 */
@RestController
@RequestMapping("/api/user-token-apis")
@Transactional
public class UserTokenApiResource {

    private final Logger log = LoggerFactory.getLogger(UserTokenApiResource.class);

    private static final String ENTITY_NAME = "userTokenApi";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final UserTokenApiRepository userTokenApiRepository;

    public UserTokenApiResource(UserTokenApiRepository userTokenApiRepository) {
        this.userTokenApiRepository = userTokenApiRepository;
    }

    /**
     * {@code POST  /user-token-apis} : Create a new userTokenApi.
     *
     * @param userTokenApi the userTokenApi to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new userTokenApi, or with status {@code 400 (Bad Request)} if the userTokenApi has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<UserTokenApi> createUserTokenApi(@RequestBody UserTokenApi userTokenApi) throws URISyntaxException {
        log.debug("REST request to save UserTokenApi : {}", userTokenApi);
        if (userTokenApi.getId() != null) {
            throw new BadRequestAlertException("A new userTokenApi cannot already have an ID", ENTITY_NAME, "idexists");
        }
        userTokenApi = userTokenApiRepository.save(userTokenApi);
        return ResponseEntity.created(new URI("/api/user-token-apis/" + userTokenApi.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, userTokenApi.getId().toString()))
            .body(userTokenApi);
    }

    /**
     * {@code PUT  /user-token-apis/:id} : Updates an existing userTokenApi.
     *
     * @param id the id of the userTokenApi to save.
     * @param userTokenApi the userTokenApi to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated userTokenApi,
     * or with status {@code 400 (Bad Request)} if the userTokenApi is not valid,
     * or with status {@code 500 (Internal Server Error)} if the userTokenApi couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<UserTokenApi> updateUserTokenApi(
        @PathVariable(value = "id", required = false) final Long id,
        @RequestBody UserTokenApi userTokenApi
    ) throws URISyntaxException {
        log.debug("REST request to update UserTokenApi : {}, {}", id, userTokenApi);
        if (userTokenApi.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, userTokenApi.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!userTokenApiRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        userTokenApi = userTokenApiRepository.save(userTokenApi);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, userTokenApi.getId().toString()))
            .body(userTokenApi);
    }

    /**
     * {@code PATCH  /user-token-apis/:id} : Partial updates given fields of an existing userTokenApi, field will ignore if it is null
     *
     * @param id the id of the userTokenApi to save.
     * @param userTokenApi the userTokenApi to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated userTokenApi,
     * or with status {@code 400 (Bad Request)} if the userTokenApi is not valid,
     * or with status {@code 404 (Not Found)} if the userTokenApi is not found,
     * or with status {@code 500 (Internal Server Error)} if the userTokenApi couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<UserTokenApi> partialUpdateUserTokenApi(
        @PathVariable(value = "id", required = false) final Long id,
        @RequestBody UserTokenApi userTokenApi
    ) throws URISyntaxException {
        log.debug("REST request to partial update UserTokenApi partially : {}, {}", id, userTokenApi);
        if (userTokenApi.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, userTokenApi.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!userTokenApiRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<UserTokenApi> result = userTokenApiRepository.findById(userTokenApi.getId()).map(userTokenApiRepository::save);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, userTokenApi.getId().toString())
        );
    }

    /**
     * {@code GET  /user-token-apis} : get all the userTokenApis.
     *
     * @param pageable the pagination information.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of userTokenApis in body.
     */
    @GetMapping("")
    public ResponseEntity<List<UserTokenApi>> getAllUserTokenApis(@org.springdoc.core.annotations.ParameterObject Pageable pageable) {
        log.debug("REST request to get a page of UserTokenApis");
        Page<UserTokenApi> page = userTokenApiRepository.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /user-token-apis/:id} : get the "id" userTokenApi.
     *
     * @param id the id of the userTokenApi to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the userTokenApi, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserTokenApi> getUserTokenApi(@PathVariable("id") Long id) {
        log.debug("REST request to get UserTokenApi : {}", id);
        Optional<UserTokenApi> userTokenApi = userTokenApiRepository.findById(id);
        return ResponseUtil.wrapOrNotFound(userTokenApi);
    }

    /**
     * {@code DELETE  /user-token-apis/:id} : delete the "id" userTokenApi.
     *
     * @param id the id of the userTokenApi to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUserTokenApi(@PathVariable("id") Long id) {
        log.debug("REST request to delete UserTokenApi : {}", id);
        userTokenApiRepository.deleteById(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
