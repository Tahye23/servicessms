package com.example.myproject.service;

import com.example.myproject.domain.Contact;
import com.example.myproject.domain.Fileextrait;
import com.example.myproject.domain.SendSms;
import com.example.myproject.repository.ContactRepository;
import com.example.myproject.repository.FileextraitRepository;
import com.example.myproject.repository.SendSmsRepository;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service Implementation for managing {@link com.example.myproject.domain.Fileextrait}.
 */
@Service
@Transactional
public class FileextraitService {

    private final Logger log = LoggerFactory.getLogger(FileextraitService.class);

    private final FileextraitRepository fileextraitRepository;
    private final SendSmsRepository sendSmsRepository;
    private final ContactRepository contactRepository;

    public FileextraitService(
        FileextraitRepository fileextraitRepository,
        ContactRepository contactRepository,
        SendSmsRepository sendSmsRepository
    ) {
        this.sendSmsRepository = sendSmsRepository;
        this.contactRepository = contactRepository;

        this.fileextraitRepository = fileextraitRepository;
    }

    /**
     * Save a fileextrait.
     *
     * @param fileextrait the entity to save.
     * @return the persisted entity.
     */
    public Fileextrait save(Fileextrait fileextrait) {
        log.debug("Request to save Fileextrait : {}", fileextrait);
        return fileextraitRepository.save(fileextrait);
    }

    /**
     * Update a fileextrait.
     *
     * @param fileextrait the entity to save.
     * @return the persisted entity.
     */
    public Fileextrait update(Fileextrait fileextrait) {
        log.debug("Request to update Fileextrait : {}", fileextrait);
        return fileextraitRepository.save(fileextrait);
    }

    /**
     * Partially update a fileextrait.
     *
     * @param fileextrait the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<Fileextrait> partialUpdate(Fileextrait fileextrait) {
        log.debug("Request to partially update Fileextrait : {}", fileextrait);

        return fileextraitRepository
            .findById(fileextrait.getId())
            .map(existingFileextrait -> {
                if (fileextrait.getFexidfile() != null) {
                    existingFileextrait.setFexidfile(fileextrait.getFexidfile());
                }
                if (fileextrait.getFexparent() != null) {
                    existingFileextrait.setFexparent(fileextrait.getFexparent());
                }
                if (fileextrait.getFexdata() != null) {
                    existingFileextrait.setFexdata(fileextrait.getFexdata());
                }
                if (fileextrait.getFexdataContentType() != null) {
                    existingFileextrait.setFexdataContentType(fileextrait.getFexdataContentType());
                }
                if (fileextrait.getFextype() != null) {
                    existingFileextrait.setFextype(fileextrait.getFextype());
                }
                if (fileextrait.getFexname() != null) {
                    existingFileextrait.setFexname(fileextrait.getFexname());
                }

                return existingFileextrait;
            })
            .map(fileextraitRepository::save);
    }

    /**
     * Get all the fileextraits.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public Page<Fileextrait> findAll(Pageable pageable) {
        log.debug("Request to get all Fileextraits");
        return fileextraitRepository.findAll(pageable);
    }

    /**
     * Get one fileextrait by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<Fileextrait> findOne(Long id) {
        log.debug("Request to get Fileextrait : {}", id);
        return fileextraitRepository.findById(id);
    }

    /**
     * Delete the fileextrait by id.
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        log.debug("Request to delete Fileextrait : {}", id);
        fileextraitRepository.deleteById(id);
    }

    public List<Contact> readContactsFromCSV(MultipartFile file) throws IOException, CsvException {
        List<Contact> contacts = new ArrayList<>();
        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
            String[] line;
            while ((line = reader.readNext()) != null) {
                // Supposons que le fichier CSV ait deux colonnes: name et phoneNumber
                String receiver = line[0];
                String phoneNumber = line[1];
                Contact contact = new Contact();
                contact.setContelephone(phoneNumber);
                contacts.add(contact);
            }
        }
        return contacts;
    }
}
