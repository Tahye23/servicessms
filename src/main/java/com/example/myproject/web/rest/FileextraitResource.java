package com.example.myproject.web.rest;

import com.example.myproject.domain.Contact;
import com.example.myproject.domain.Fileextrait;
import com.example.myproject.repository.ContactRepository;
import com.example.myproject.repository.FileextraitRepository;
import com.example.myproject.service.FileextraitService;
import com.example.myproject.web.rest.dto.DuplicateContactsResponse;
import com.example.myproject.web.rest.errors.BadRequestAlertException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.List;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.PaginationUtil;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link com.example.myproject.domain.Fileextrait}.
 */
@RestController
@RequestMapping("/api/fileextraits")
public class FileextraitResource {

    private final ContactRepository contactRepository;
    private final Logger log = LoggerFactory.getLogger(FileextraitResource.class);

    private static final String ENTITY_NAME = "fileextrait";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final FileextraitService fileextraitService;

    private final FileextraitRepository fileextraitRepository;

    public FileextraitResource(
        FileextraitService fileextraitService,
        FileextraitRepository fileextraitRepository,
        ContactRepository contactRepository
    ) {
        this.fileextraitService = fileextraitService;
        this.fileextraitRepository = fileextraitRepository;
        this.contactRepository = contactRepository;
    }

    /**
     * {@code POST  /fileextraits} : Create a new fileextrait.
     *
     * @param fileextrait the fileextrait to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new fileextrait, or with status {@code 400 (Bad Request)} if the fileextrait has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<Fileextrait> createFileextrait(@RequestBody Fileextrait fileextrait) throws URISyntaxException {
        log.debug("REST request to save Fileextrait : {}", fileextrait);
        if (fileextrait.getId() != null) {
            throw new BadRequestAlertException("A new fileextrait cannot already have an ID", ENTITY_NAME, "idexists");
        }
        fileextrait = fileextraitService.save(fileextrait);
        return ResponseEntity.created(new URI("/api/fileextraits/" + fileextrait.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, fileextrait.getId().toString()))
            .body(fileextrait);
    }

    @PostMapping("/pushfile/idop")
    public ResponseEntity<Void> uploadCSV(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            List<Contact> contacts = new ArrayList<>();
            //Set<String> uniquePhoneNumbers = new HashSet<>();
            // Skip the header line
            reader.readLine();
            while ((line = reader.readLine()) != null) {
                String[] fields = line.split(",");
                Contact contact = new Contact();

                //contact.setContelephone(fields[2]);
                //String phoneNumber = fields[2];
                log.info("Numéro de téléphone brut : '{}'", fields[2]);
                //String Name = fields[0].replaceAll("[^\\d]", "").trim();
                //String Prenom =fields[1].replaceAll("[^\\d]", "").trim();
                String phoneNumber = fields[2].replaceAll("[^\\d]", "").trim();
                contact.setConnom(fields[0]);
                contact.setConprenom(fields[1]);
                contact.setContelephone(phoneNumber);
                contacts.add(contact);
                // if (isValidPhoneNumber(phoneNumber)) {
                //   if (uniquePhoneNumbers.add(phoneNumber)) {
                //     contact.setConnom(fields[0]);
                //   contact.setConprenom(fields[1]);
                // contact.setContelephone(phoneNumber);
                //contacts.add(contact);
                //}
                //} else {
                //  log.error("Le numéro de téléphone '{}' est invalide.", phoneNumber);
                //}
                // Set other fields as needed

            }

            contactRepository.saveAll(contacts);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }

    public static boolean isValidPhoneNumber(String phoneNumber) {
        // Vérifie que la longueur est exactement 8 et que tous les caractères sont des chiffres
        return phoneNumber != null && phoneNumber.length() == 8 && phoneNumber.chars().allMatch(Character::isDigit);
    }

    public void processContacts() {
        // Récupérer tous les contacts de la base de données
        List<Contact> allContacts = contactRepository.findAll();

        // Set pour suivre les numéros de téléphone uniques
        Set<String> uniquePhoneNumbers = new HashSet<>();

        // Listes pour stocker les contacts valides, en doublon et avec erreurs
        List<Contact> uniqueContacts = new ArrayList<>();
        List<Contact> duplicateContacts = new ArrayList<>();
        List<Contact> errorContacts = new ArrayList<>();

        // Parcourir tous les contacts pour les vérifier
        for (Contact contact : allContacts) {
            // Initialiser le statut à 0 (pas encore traité)
            contact.setStatuttraitement(0);

            // Vérifier si le numéro de téléphone est valide
            if (!isValidPhoneNumber(contact.getContelephone())) {
                // Numéro non valide, ajouter à la liste des erreurs
                contact.setStatuttraitement(2); // 2 : non valide
                //errorContacts.add(contact);
            }
            // Vérifier si le numéro est dupliqué
            else if (!uniquePhoneNumbers.add(contact.getContelephone())) {
                // Numéro déjà présent, ajouter à la liste des doublons
                contact.setStatuttraitement(3); // 3 : doublon
                //duplicateContacts.add(contact);
            }
            // Si le numéro est valide et unique
            else {
                // Numéro unique, ajouter à la liste des contacts valides
                contact.setStatuttraitement(1); // 1 : valide
                //uniqueContacts.add(contact);
            }
        }

        // Enregistrer les contacts mis à jour dans la base de données
        contactRepository.saveAll(allContacts); // Sauvegarder tous les contacts avec leur statut mis à jour
        // Retourner la réponse avec les trois listes : contacts uniques, doublons et erreurs
        //return new DuplicateContactsResponse(uniqueContacts, duplicateContacts, errorContacts);
    }

    public List<Contact> validatePhoneNumbers() {
        // Récupérer tous les contacts de la base de données
        List<Contact> allContacts = contactRepository.findAll();

        for (Contact contact : allContacts) {
            // Vérifier si le numéro de téléphone est valide
            if (isValidPhoneNumber(contact.getContelephone())) {
                contact.setStatuttraitement(1); // 1 : valide
            } else {
                contact.setStatuttraitement(2); // 2 : non valide
                System.out.println("Invalid phone number for contact: " + contact.getContelephone());
            }
        }

        // Enregistrer les contacts mis à jour dans la base de données
        return contactRepository.saveAll(allContacts);
    }

    @PostMapping("/clean")
    public ResponseEntity<DuplicateContactsResponse> nettoyage() {
        // Suppression du bloc try-catch inutile
        // Traiter le fichier CSV et obtenir la réponse des doublons
        this.processContacts();
        return ResponseEntity.ok().build();
    }

    //public static boolean isValidPhoneNumber(String Contelephone) {
    // Vérifie que la longueur est exactement 8 et que tous les caractères sont des chiffres
    // String phonePattern = "^(\\+?222)?([324])\\d{7}$";
    //return phoneNumber != null && phoneNumber.length() == 8 && phoneNumber.chars().allMatch(Character::isDigit)&& phoneNumber.matches(phonePattern);
    //   return Contelephone != null && Contelephone.matches(phonePattern);
    // }

    @PostMapping("/uploadCSV")
    //public ResponseEntity<Void> uploadCSV(
    //  @RequestParam("file") MultipartFile file,
    //@RequestParam("sender") String sender,
    //@RequestParam("msgdata") String msgdata
    //) {
    //  try {
    //    fileextraitService.readContactsFromCSV(file, sender, msgdata);
    //  return new ResponseEntity<>(HttpStatus.OK);
    //} catch (Exception e) {
    //  e.printStackTrace();
    //return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    //}
    //}

    /**
     * {@code PUT  /fileextraits/:id} : Updates an existing fileextrait.
     *
     * @param id the id of the fileextrait to save.
     * @param fileextrait the fileextrait to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated fileextrait,
     * or with status {@code 400 (Bad Request)} if the fileextrait is not valid,
     * or with status {@code 500 (Internal Server Error)} if the fileextrait couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */

    @PutMapping("/{id}")
    public ResponseEntity<Fileextrait> updateFileextrait(
        @PathVariable(value = "id", required = false) final Long id,
        @RequestBody Fileextrait fileextrait
    ) throws URISyntaxException {
        log.debug("REST request to update Fileextrait : {}, {}", id, fileextrait);
        if (fileextrait.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, fileextrait.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!fileextraitRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        fileextrait = fileextraitService.update(fileextrait);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, fileextrait.getId().toString()))
            .body(fileextrait);
    }

    /**
     * {@code PATCH  /fileextraits/:id} : Partial updates given fields of an existing fileextrait, field will ignore if it is null
     *
     * @param id the id of the fileextrait to save.
     * @param fileextrait the fileextrait to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated fileextrait,
     * or with status {@code 400 (Bad Request)} if the fileextrait is not valid,
     * or with status {@code 404 (Not Found)} if the fileextrait is not found,
     * or with status {@code 500 (Internal Server Error)} if the fileextrait couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<Fileextrait> partialUpdateFileextrait(
        @PathVariable(value = "id", required = false) final Long id,
        @RequestBody Fileextrait fileextrait
    ) throws URISyntaxException {
        log.debug("REST request to partial update Fileextrait partially : {}, {}", id, fileextrait);
        if (fileextrait.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, fileextrait.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!fileextraitRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<Fileextrait> result = fileextraitService.partialUpdate(fileextrait);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, fileextrait.getId().toString())
        );
    }

    /**
     * {@code GET  /fileextraits} : get all the fileextraits.
     *
     * @param pageable the pagination information.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of fileextraits in body.
     */
    @GetMapping("")
    public ResponseEntity<List<Fileextrait>> getAllFileextraits(@org.springdoc.core.annotations.ParameterObject Pageable pageable) {
        log.debug("REST request to get a page of Fileextraits");
        Page<Fileextrait> page = fileextraitService.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /fileextraits/:id} : get the "id" fileextrait.
     *
     * @param id the id of the fileextrait to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the fileextrait, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Fileextrait> getFileextrait(@PathVariable("id") Long id) {
        log.debug("REST request to get Fileextrait : {}", id);
        Optional<Fileextrait> fileextrait = fileextraitService.findOne(id);
        return ResponseUtil.wrapOrNotFound(fileextrait);
    }

    /**
     * {@code DELETE  /fileextraits/:id} : delete the "id" fileextrait.
     *
     * @param id the id of the fileextrait to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFileextrait(@PathVariable("id") Long id) {
        log.debug("REST request to delete Fileextrait : {}", id);
        fileextraitService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
