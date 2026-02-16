package com.example.myproject.web.rest;

import com.example.myproject.domain.UserService;
import com.example.myproject.repository.UserServiceRepository;
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
 * REST controller for managing {@link com.example.myproject.domain.UserService}.
 */
@RestController
@RequestMapping("/api/user-services")
@Transactional
public class UserServiceResource {

    private final Logger log = LoggerFactory.getLogger(UserServiceResource.class);

    private static final String ENTITY_NAME = "userService";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final UserServiceRepository userServiceRepository;

    public UserServiceResource(UserServiceRepository userServiceRepository) {
        this.userServiceRepository = userServiceRepository;
    }

    /**
     * {@code POST  /user-services} : Create a new userService.
     *
     * @param userService the userService to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new userService, or with status {@code 400 (Bad Request)} if the userService has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<UserService> createUserService(@RequestBody UserService userService) throws URISyntaxException {
        log.debug("REST request to save UserService : {}", userService);
        if (userService.getId() != null) {
            throw new BadRequestAlertException("A new userService cannot already have an ID", ENTITY_NAME, "idexists");
        }
        userService = userServiceRepository.save(userService);
        return ResponseEntity.created(new URI("/api/user-services/" + userService.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, userService.getId().toString()))
            .body(userService);
    }

    /**
     * {@code PUT  /user-services/:id} : Updates an existing userService.
     *
     * @param id the id of the userService to save.
     * @param userService the userService to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated userService,
     * or with status {@code 400 (Bad Request)} if the userService is not valid,
     * or with status {@code 500 (Internal Server Error)} if the userService couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<UserService> updateUserService(
        @PathVariable(value = "id", required = false) final Long id,
        @RequestBody UserService userService
    ) throws URISyntaxException {
        log.debug("REST request to update UserService : {}, {}", id, userService);
        if (userService.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, userService.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!userServiceRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        userService = userServiceRepository.save(userService);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, userService.getId().toString()))
            .body(userService);
    }

    /**
     * {@code PATCH  /user-services/:id} : Partial updates given fields of an existing userService, field will ignore if it is null
     *
     * @param id the id of the userService to save.
     * @param userService the userService to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated userService,
     * or with status {@code 400 (Bad Request)} if the userService is not valid,
     * or with status {@code 404 (Not Found)} if the userService is not found,
     * or with status {@code 500 (Internal Server Error)} if the userService couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<UserService> partialUpdateUserService(
        @PathVariable(value = "id", required = false) final Long id,
        @RequestBody UserService userService
    ) throws URISyntaxException {
        log.debug("REST request to partial update UserService partially : {}, {}", id, userService);
        if (userService.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, userService.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!userServiceRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<UserService> result = userServiceRepository
            .findById(userService.getId())
            .map(existingUserService -> {
                if (userService.getUrSService() != null) {
                    existingUserService.setUrSService(userService.getUrSService());
                }
                if (userService.getUrSUser() != null) {
                    existingUserService.setUrSUser(userService.getUrSUser());
                }

                return existingUserService;
            })
            .map(userServiceRepository::save);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, userService.getId().toString())
        );
    }

    /**
     * {@code GET  /user-services} : get all the userServices.
     *
     * @param pageable the pagination information.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of userServices in body.
     */
    @GetMapping("")
    public ResponseEntity<List<UserService>> getAllUserServices(@org.springdoc.core.annotations.ParameterObject Pageable pageable) {
        log.debug("REST request to get a page of UserServices");
        Page<UserService> page = userServiceRepository.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /user-services/:id} : get the "id" userService.
     *
     * @param id the id of the userService to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the userService, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserService> getUserService(@PathVariable("id") Long id) {
        log.debug("REST request to get UserService : {}", id);
        Optional<UserService> userService = userServiceRepository.findById(id);
        return ResponseUtil.wrapOrNotFound(userService);
    }

    /**
     * {@code DELETE  /user-services/:id} : delete the "id" userService.
     *
     * @param id the id of the userService to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUserService(@PathVariable("id") Long id) {
        log.debug("REST request to delete UserService : {}", id);
        userServiceRepository.deleteById(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
