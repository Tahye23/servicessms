package com.example.myproject.web.rest;

import com.example.myproject.domain.PartnershipRequest;
import com.example.myproject.service.PartnershipRequestService;
import com.example.myproject.service.dto.PartnershipRequestDTO;
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

@RestController
@RequestMapping("/api")
public class PartnershipRequestController {

    private final Logger log = LoggerFactory.getLogger(PartnershipRequestController.class);

    private static final String ENTITY_NAME = "partnershipRequest";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final PartnershipRequestService partnershipRequestService;

    public PartnershipRequestController(PartnershipRequestService partnershipRequestService) {
        this.partnershipRequestService = partnershipRequestService;
    }

    /**
     * {@code POST /partnership-requests} : Crée une nouvelle demande de partenariat.
     *
     * @param partnershipRequestDTO la demande de partenariat à créer.
     * @return le {@link ResponseEntity} avec le statut {@code 201 (Created)} et avec le corps de la nouvelle demande,
     * ou avec le statut {@code 400 (Bad Request)} si la demande a déjà un ID.
     * @throws URISyntaxException si la syntaxe de l'URI de localisation n'est pas correcte.
     */
    @PostMapping("/partnership-requests")
    public ResponseEntity<PartnershipRequestDTO> createPartnershipRequest(@Valid @RequestBody PartnershipRequestDTO partnershipRequestDTO)
        throws URISyntaxException {
        log.debug("REST request to save PartnershipRequest : {}", partnershipRequestDTO);

        if (partnershipRequestDTO.getId() != null) {
            throw new BadRequestAlertException("Une nouvelle demande ne peut pas avoir d'ID", ENTITY_NAME, "idexists");
        }

        try {
            PartnershipRequestDTO result = partnershipRequestService.save(partnershipRequestDTO);
            return ResponseEntity.created(new URI("/api/partnership-requests/" + result.getId()))
                .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
                .body(result);
        } catch (RuntimeException e) {
            log.error("Erreur lors de la création de la demande de partenariat", e);
            throw new BadRequestAlertException(e.getMessage(), ENTITY_NAME, "creationerror");
        }
    }

    /**
     * {@code PUT /partnership-requests/:id} : Met à jour une demande de partenariat existante.
     */
    @PutMapping("/partnership-requests/{id}")
    public ResponseEntity<PartnershipRequestDTO> updatePartnershipRequest(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody PartnershipRequestDTO partnershipRequestDTO
    ) throws URISyntaxException {
        log.debug("REST request to update PartnershipRequest : {}, {}", id, partnershipRequestDTO);

        if (partnershipRequestDTO.getId() == null) {
            throw new BadRequestAlertException("ID invalide", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, partnershipRequestDTO.getId())) {
            throw new BadRequestAlertException("ID invalide", ENTITY_NAME, "idinvalid");
        }

        if (!partnershipRequestService.findOne(id).isPresent()) {
            throw new BadRequestAlertException("Entité non trouvée", ENTITY_NAME, "idnotfound");
        }

        PartnershipRequestDTO result = partnershipRequestService.update(partnershipRequestDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, partnershipRequestDTO.getId().toString()))
            .body(result);
    }

    /**
     * {@code PATCH /partnership-requests/:id} : Mise à jour partielle d'une demande de partenariat existante.
     */
    @PatchMapping(value = "/partnership-requests/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<PartnershipRequestDTO> partialUpdatePartnershipRequest(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody PartnershipRequestDTO partnershipRequestDTO
    ) throws URISyntaxException {
        log.debug("REST request to partial update PartnershipRequest partially : {}, {}", id, partnershipRequestDTO);

        if (partnershipRequestDTO.getId() == null) {
            throw new BadRequestAlertException("ID invalide", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, partnershipRequestDTO.getId())) {
            throw new BadRequestAlertException("ID invalide", ENTITY_NAME, "idinvalid");
        }

        if (!partnershipRequestService.findOne(id).isPresent()) {
            throw new BadRequestAlertException("Entité non trouvée", ENTITY_NAME, "idnotfound");
        }

        Optional<PartnershipRequestDTO> result = partnershipRequestService
            .findOne(id)
            .map(existingRequest -> {
                // Mise à jour des champs modifiables uniquement
                if (partnershipRequestDTO.getStatus() != null) {
                    existingRequest.setStatus(partnershipRequestDTO.getStatus());
                }
                if (partnershipRequestDTO.getAdminNotes() != null) {
                    existingRequest.setAdminNotes(partnershipRequestDTO.getAdminNotes());
                }
                return existingRequest;
            })
            .map(partnershipRequestService::update);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, partnershipRequestDTO.getId().toString())
        );
    }

    /**
     * {@code GET /partnership-requests} : Récupère toutes les demandes de partenariat.
     */
    @GetMapping("/partnership-requests")
    public ResponseEntity<List<PartnershipRequestDTO>> getAllPartnershipRequests(
        @RequestParam(required = false) PartnershipRequest.RequestStatus status,
        @RequestParam(required = false) String industry,
        @RequestParam(required = false) String email,
        @RequestParam(required = false) String companyName,
        Pageable pageable
    ) {
        log.debug("REST request to get a page of PartnershipRequests");

        Page<PartnershipRequestDTO> page;

        if (status != null || industry != null || email != null || companyName != null) {
            page = partnershipRequestService.findByMultipleCriteria(status, industry, email, companyName, pageable);
        } else {
            page = partnershipRequestService.findAll(pageable);
        }

        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET /partnership-requests/:id} : Récupère la demande de partenariat par ID.
     */
    @GetMapping("/partnership-requests/{id}")
    public ResponseEntity<PartnershipRequestDTO> getPartnershipRequest(@PathVariable Long id) {
        log.debug("REST request to get PartnershipRequest : {}", id);
        Optional<PartnershipRequestDTO> partnershipRequestDTO = partnershipRequestService.findOne(id);
        return ResponseUtil.wrapOrNotFound(partnershipRequestDTO);
    }

    /**
     * {@code DELETE /partnership-requests/:id} : Supprime la demande de partenariat par ID.
     */
    @DeleteMapping("/partnership-requests/{id}")
    public ResponseEntity<Void> deletePartnershipRequest(@PathVariable Long id) {
        log.debug("REST request to delete PartnershipRequest : {}", id);
        partnershipRequestService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }

    /**
     * {@code POST /partnership-requests/:id/approve} : Approuve une demande de partenariat.
     */
    @PostMapping("/partnership-requests/{id}/approve")
    public ResponseEntity<PartnershipRequestDTO> approvePartnershipRequest(
        @PathVariable Long id,
        @RequestBody(required = false) String adminNotes
    ) {
        log.debug("REST request to approve PartnershipRequest : {}", id);

        try {
            PartnershipRequestDTO result = partnershipRequestService.approveRequest(id, adminNotes);
            return ResponseEntity.ok()
                .headers(HeaderUtil.createAlert(applicationName, "Demande approuvée avec succès", id.toString()))
                .body(result);
        } catch (RuntimeException e) {
            throw new BadRequestAlertException(e.getMessage(), ENTITY_NAME, "approvalerror");
        }
    }

    /**
     * {@code POST /partnership-requests/:id/reject} : Rejette une demande de partenariat.
     */
    @PostMapping("/partnership-requests/{id}/reject")
    public ResponseEntity<PartnershipRequestDTO> rejectPartnershipRequest(
        @PathVariable Long id,
        @RequestBody(required = false) String adminNotes
    ) {
        log.debug("REST request to reject PartnershipRequest : {}", id);

        try {
            PartnershipRequestDTO result = partnershipRequestService.rejectRequest(id, adminNotes);
            return ResponseEntity.ok().headers(HeaderUtil.createAlert(applicationName, "Demande rejetée", id.toString())).body(result);
        } catch (RuntimeException e) {
            throw new BadRequestAlertException(e.getMessage(), ENTITY_NAME, "rejectionerror");
        }
    }

    /**
     * {@code GET /partnership-requests/by-status/:status} : Récupère les demandes par statut.
     */
    @GetMapping("/partnership-requests/by-status/{status}")
    public ResponseEntity<List<PartnershipRequestDTO>> getPartnershipRequestsByStatus(
        @PathVariable PartnershipRequest.RequestStatus status,
        Pageable pageable
    ) {
        log.debug("REST request to get PartnershipRequests by status : {}", status);
        Page<PartnershipRequestDTO> page = partnershipRequestService.findByStatus(status, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET /partnership-requests/statistics} : Récupère les statistiques des demandes.
     */
    @GetMapping("/partnership-requests/statistics")
    public ResponseEntity<PartnershipRequestService.PartnershipRequestStats> getPartnershipRequestStatistics() {
        log.debug("REST request to get PartnershipRequest statistics");
        PartnershipRequestService.PartnershipRequestStats stats = partnershipRequestService.getStatistics();
        return ResponseEntity.ok().body(stats);
    }

    /**
     * {@code GET /partnership-requests/old-pending} : Récupère les demandes en attente anciennes.
     */
    @GetMapping("/partnership-requests/old-pending")
    public ResponseEntity<List<PartnershipRequestDTO>> getOldPendingRequests(@RequestParam(defaultValue = "7") int daysOld) {
        log.debug("REST request to get old pending PartnershipRequests older than {} days", daysOld);
        List<PartnershipRequestDTO> oldRequests = partnershipRequestService.findOldPendingRequests(daysOld);
        return ResponseEntity.ok().body(oldRequests);
    }
}
