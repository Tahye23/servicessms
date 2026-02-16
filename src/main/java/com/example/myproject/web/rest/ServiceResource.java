package com.example.myproject.web.rest;

import com.example.myproject.domain.Service;
import com.example.myproject.repository.ServiceRepository;
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
 * REST controller for managing {@link com.example.myproject.domain.Service}.
 */
@RestController
@RequestMapping("/api/services")
@Transactional
public class ServiceResource {

    private final Logger log = LoggerFactory.getLogger(ServiceResource.class);

    private static final String ENTITY_NAME = "service";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final ServiceRepository serviceRepository;

    public ServiceResource(ServiceRepository serviceRepository) {
        this.serviceRepository = serviceRepository;
    }

    /**
     * {@code POST  /services} : Create a new service.
     *
     * @param service the service to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new service, or with status {@code 400 (Bad Request)} if the service has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<Service> createService(@RequestBody Service service) throws URISyntaxException {
        log.debug("REST request to save Service : {}", service);
        if (service.getId() != null) {
            throw new BadRequestAlertException("A new service cannot already have an ID", ENTITY_NAME, "idexists");
        }
        String sname = service.getServNom();
        if (sname != null) {
            service = serviceRepository.save(service);
            return ResponseEntity.created(new URI("/api/services/" + service.getId()))
                .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, service.getId().toString()))
                .body(service);
        }
        //return null;
        service = serviceRepository.save(service);
        return ResponseEntity.created(new URI("/api/services/" + service.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, service.getId().toString()))
            .body(service);
    }

    /**
     * {@code PUT  /services/:id} : Updates an existing service.
     *
     * @param id the id of the service to save.
     * @param service the service to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated service,
     * or with status {@code 400 (Bad Request)} if the service is not valid,
     * or with status {@code 500 (Internal Server Error)} if the service couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */

    @PutMapping("/{id}/activate")
    public ResponseEntity<Void> activateService(@PathVariable Long id) {
        // Récupérer le service par son ID
        Service service = serviceRepository.findById(id).orElseThrow(() -> new RuntimeException("Service not found"));

        // Changer le statut à 'true' (activer le service)
        service.setStatut(true);

        // Sauvegarder les modifications
        serviceRepository.save(service);

        // Retourner une réponse 200 OK
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateService(@PathVariable Long id) {
        // Récupérer le service par son ID
        Service service = serviceRepository.findById(id).orElseThrow(() -> new RuntimeException("Service not found"));

        // Changer le statut à 'false' (désactiver le service)
        service.setStatut(false);

        // Sauvegarder les modifications
        serviceRepository.save(service);

        // Retourner une réponse 200 OK
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Service> updateService(@PathVariable(value = "id", required = false) final Long id, @RequestBody Service service)
        throws URISyntaxException {
        log.debug("REST request to update Service : {}, {}", id, service);
        if (service.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, service.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!serviceRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        service = serviceRepository.save(service);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, service.getId().toString()))
            .body(service);
    }

    /**
     * {@code PATCH  /services/:id} : Partial updates given fields of an existing service, field will ignore if it is null
     *
     * @param id the id of the service to save.
     * @param service the service to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated service,
     * or with status {@code 400 (Bad Request)} if the service is not valid,
     * or with status {@code 404 (Not Found)} if the service is not found,
     * or with status {@code 500 (Internal Server Error)} if the service couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<Service> partialUpdateService(
        @PathVariable(value = "id", required = false) final Long id,
        @RequestBody Service service
    ) throws URISyntaxException {
        log.debug("REST request to partial update Service partially : {}, {}", id, service);
        if (service.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, service.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!serviceRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<Service> result = serviceRepository
            .findById(service.getId())
            .map(existingService -> {
                if (service.getServNom() != null) {
                    existingService.setServNom(service.getServNom());
                }
                if (service.getServDescription() != null) {
                    existingService.setServDescription(service.getServDescription());
                }

                if (service.getNbrusage() != null) {
                    existingService.setNbrusage(service.getNbrusage());
                }
                if (service.getMontant() != null) {
                    existingService.setMontant(service.getMontant());
                }
                if (service.getStatut() != null) {
                    existingService.setStatut(service.getStatut());
                }

                return existingService;
            })
            .map(serviceRepository::save);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, service.getId().toString())
        );
    }

    /**
     * {@code GET  /services} : get all the services.
     *
     * @param pageable the pagination information.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of services in body.
     */
    @GetMapping("")
    public ResponseEntity<List<Service>> getAllServices(@org.springdoc.core.annotations.ParameterObject Pageable pageable) {
        log.debug("REST request to get a page of Services");
        Page<Service> page = serviceRepository.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /services/:id} : get the "id" service.
     *
     * @param id the id of the service to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the service, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Service> getService(@PathVariable("id") Long id) {
        log.debug("REST request to get Service : {}", id);
        Optional<Service> service = serviceRepository.findById(id);
        return ResponseUtil.wrapOrNotFound(service);
    }

    /**
     * {@code DELETE  /services/:id} : delete the "id" service.
     *
     * @param id the id of the service to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteService(@PathVariable("id") Long id) {
        log.debug("REST request to delete Service : {}", id);
        serviceRepository.deleteById(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
