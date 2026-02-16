package com.example.myproject.web.rest;

import com.example.myproject.domain.Api;
import com.example.myproject.repository.ApiRepository;
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
 * REST controller for managing {@link com.example.myproject.domain.Api}.
 */
@RestController
@RequestMapping("/api/apis")
@Transactional
public class ApiResource {

    private final Logger log = LoggerFactory.getLogger(ApiResource.class);

    private static final String ENTITY_NAME = "api";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final ApiRepository apiRepository;

    public ApiResource(ApiRepository apiRepository) {
        this.apiRepository = apiRepository;
    }

    /**
     * {@code POST  /apis} : Create a new api.
     *
     * @param api the api to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new api, or with status {@code 400 (Bad Request)} if the api has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<Api> createApi(@RequestBody Api api) throws URISyntaxException {
        log.debug("REST request to save Api : {}", api);
        if (api.getId() != null) {
            throw new BadRequestAlertException("A new api cannot already have an ID", ENTITY_NAME, "idexists");
        }
        api = apiRepository.save(api);
        return ResponseEntity.created(new URI("/api/apis/" + api.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, api.getId().toString()))
            .body(api);
    }

    /**
     * {@code PUT  /apis/:id} : Updates an existing api.
     *
     * @param id the id of the api to save.
     * @param api the api to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated api,
     * or with status {@code 400 (Bad Request)} if the api is not valid,
     * or with status {@code 500 (Internal Server Error)} if the api couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Api> updateApi(@PathVariable(value = "id", required = false) final Long id, @RequestBody Api api)
        throws URISyntaxException {
        log.debug("REST request to update Api : {}, {}", id, api);
        if (api.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, api.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!apiRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        api = apiRepository.save(api);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, api.getId().toString()))
            .body(api);
    }

    /**
     * {@code PATCH  /apis/:id} : Partial updates given fields of an existing api, field will ignore if it is null
     *
     * @param id the id of the api to save.
     * @param api the api to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated api,
     * or with status {@code 400 (Bad Request)} if the api is not valid,
     * or with status {@code 404 (Not Found)} if the api is not found,
     * or with status {@code 500 (Internal Server Error)} if the api couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<Api> partialUpdateApi(@PathVariable(value = "id", required = false) final Long id, @RequestBody Api api)
        throws URISyntaxException {
        log.debug("REST request to partial update Api partially : {}, {}", id, api);
        if (api.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, api.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!apiRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<Api> result = apiRepository
            .findById(api.getId())
            .map(existingApi -> {
                if (api.getApiNom() != null) {
                    existingApi.setApiNom(api.getApiNom());
                }
                if (api.getApiUrl() != null) {
                    existingApi.setApiUrl(api.getApiUrl());
                }
                if (api.getApiVersion() != null) {
                    existingApi.setApiVersion(api.getApiVersion());
                }

                return existingApi;
            })
            .map(apiRepository::save);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, api.getId().toString())
        );
    }

    /**
     * {@code GET  /apis} : get all the apis.
     *
     * @param pageable the pagination information.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of apis in body.
     */
    @GetMapping("")
    public ResponseEntity<List<Api>> getAllApis(@org.springdoc.core.annotations.ParameterObject Pageable pageable) {
        log.debug("REST request to get a page of Apis");
        Page<Api> page = apiRepository.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /apis/:id} : get the "id" api.
     *
     * @param id the id of the api to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the api, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Api> getApi(@PathVariable("id") Long id) {
        log.debug("REST request to get Api : {}", id);
        Optional<Api> api = apiRepository.findById(id);
        return ResponseUtil.wrapOrNotFound(api);
    }

    /**
     * {@code DELETE  /apis/:id} : delete the "id" api.
     *
     * @param id the id of the api to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteApi(@PathVariable("id") Long id) {
        log.debug("REST request to delete Api : {}", id);
        apiRepository.deleteById(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
