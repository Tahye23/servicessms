package com.example.myproject.web.rest;

import com.example.myproject.domain.ExtendedUser;
import com.example.myproject.repository.ExtendedUserRepository;
import com.example.myproject.security.SecurityUtils;
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

@RestController
@RequestMapping("/api/extended-users")
@Transactional
public class ExtendedUserResource {

    private final Logger log = LoggerFactory.getLogger(ExtendedUserResource.class);
    private static final String ENTITY_NAME = "extendedUser";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final ExtendedUserRepository extendedUserRepository;

    public ExtendedUserResource(ExtendedUserRepository extendedUserRepository) {
        this.extendedUserRepository = extendedUserRepository;
    }

    // ===== CRÉATION =====

    @PostMapping("")
    public ResponseEntity<ExtendedUser> createExtendedUser(@RequestBody ExtendedUser extendedUser) throws URISyntaxException {
        log.debug("REST request to save ExtendedUser : {}", extendedUser);
        if (extendedUser.getId() != null) {
            throw new BadRequestAlertException("A new extendedUser cannot already have an ID", ENTITY_NAME, "idexists");
        }
        if (extendedUser.getUser() == null || extendedUser.getUser().getId() == null) {
            throw new BadRequestAlertException("A user must be attached", ENTITY_NAME, "usernull");
        }
        // Optionally: check user exists, or uniqueness

        extendedUser = extendedUserRepository.save(extendedUser);
        return ResponseEntity.created(new URI("/api/extended-users/" + extendedUser.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, extendedUser.getId().toString()))
            .body(extendedUser);
    }

    // ===== MISE À JOUR =====

    @PutMapping("/{id}")
    public ResponseEntity<ExtendedUser> updateExtendedUser(
        @PathVariable(value = "id", required = false) final Long id,
        @RequestBody ExtendedUser extendedUser
    ) throws URISyntaxException {
        log.debug("REST request to update ExtendedUser : {}, {}", id, extendedUser);
        if (extendedUser.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, extendedUser.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }
        if (!extendedUserRepository.existsById(Math.toIntExact(id))) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }
        extendedUser = extendedUserRepository.save(extendedUser);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, extendedUser.getId().toString()))
            .body(extendedUser);
    }

    // ===== PATCH (mise à jour partielle) =====

    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<ExtendedUser> partialUpdateExtendedUser(
        @PathVariable(value = "id", required = false) final Long id,
        @RequestBody ExtendedUser extendedUser
    ) {
        log.debug("REST request to partial update ExtendedUser partially : {}, {}", id, extendedUser);
        if (extendedUser.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, extendedUser.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }
        if (!extendedUserRepository.existsById(Math.toIntExact(id))) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<ExtendedUser> result = extendedUserRepository
            .findById(id)
            .map(existingExtendedUser -> {
                // On ne met à jour QUE les champs non-nuls du payload reçu :
                if (extendedUser.getAddress() != null) existingExtendedUser.setAddress(extendedUser.getAddress());
                if (extendedUser.getPhoneNumber() != null) existingExtendedUser.setPhoneNumber(extendedUser.getPhoneNumber());
                if (extendedUser.getCity() != null) existingExtendedUser.setCity(extendedUser.getCity());
                if (extendedUser.getCountry() != null) existingExtendedUser.setCountry(extendedUser.getCountry());
                if (extendedUser.getPostalCode() != null) existingExtendedUser.setPostalCode(extendedUser.getPostalCode());
                if (extendedUser.getCompanyName() != null) existingExtendedUser.setCompanyName(extendedUser.getCompanyName());
                if (extendedUser.getWebsite() != null) existingExtendedUser.setWebsite(extendedUser.getWebsite());
                if (extendedUser.getTimezone() != null) existingExtendedUser.setTimezone(extendedUser.getTimezone());
                if (extendedUser.getLanguage() != null) existingExtendedUser.setLanguage(extendedUser.getLanguage());
                if (extendedUser.getNotificationsEmail() != null) existingExtendedUser.setNotificationsEmail(
                    extendedUser.getNotificationsEmail()
                );
                if (extendedUser.getNotificationsSms() != null) existingExtendedUser.setNotificationsSms(
                    extendedUser.getNotificationsSms()
                );
                if (extendedUser.getMarketingEmails() != null) existingExtendedUser.setMarketingEmails(extendedUser.getMarketingEmails());
                if (extendedUser.getBillingAddress() != null) existingExtendedUser.setBillingAddress(extendedUser.getBillingAddress());
                if (extendedUser.getBillingCity() != null) existingExtendedUser.setBillingCity(extendedUser.getBillingCity());
                if (extendedUser.getBillingCountry() != null) existingExtendedUser.setBillingCountry(extendedUser.getBillingCountry());
                if (extendedUser.getBillingPostalCode() != null) existingExtendedUser.setBillingPostalCode(
                    extendedUser.getBillingPostalCode()
                );
                if (extendedUser.getTaxId() != null) existingExtendedUser.setTaxId(extendedUser.getTaxId());
                if (extendedUser.getPaymentMethodId() != null) existingExtendedUser.setPaymentMethodId(extendedUser.getPaymentMethodId());
                // Ajoute d'autres champs selon tes besoins métier
                // ...
                return existingExtendedUser;
            })
            .map(extendedUserRepository::save);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, extendedUser.getId().toString())
        );
    }

    // ===== LECTURE (LISTE & DÉTAIL) =====

    @GetMapping("")
    public ResponseEntity<List<ExtendedUser>> getAllExtendedUsers(
        @org.springdoc.core.annotations.ParameterObject Pageable pageable,
        @RequestParam(name = "eagerload", required = false, defaultValue = "true") boolean eagerload
    ) {
        log.debug("REST request to get a page of ExtendedUsers");
        Page<ExtendedUser> page;
        if (SecurityUtils.hasCurrentUserAnyOfAuthorities("ROLE_ADMIN")) {
            // ✅ Si Admin, retourner tous les utilisateurs
            page = eagerload ? extendedUserRepository.findAllWithEagerRelationships(pageable) : extendedUserRepository.findAll(pageable);
        } else {
            String currentUserLogin = SecurityUtils.getCurrentUserLogin()
                .orElseThrow(() -> new IllegalStateException("User not authenticated"));
            page = extendedUserRepository.findByUserLogin(currentUserLogin, pageable);
        }
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExtendedUser> getExtendedUser(@PathVariable("id") Long id) {
        log.debug("REST request to get ExtendedUser : {}", id);
        Optional<ExtendedUser> extendedUser = extendedUserRepository.findById(id);
        return ResponseUtil.wrapOrNotFound(extendedUser);
    }

    // ===== SUPPRESSION =====

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExtendedUser(@PathVariable("id") Long id) {
        log.debug("REST request to delete ExtendedUser : {}", id);
        extendedUserRepository.deleteById(Math.toIntExact(id));
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
