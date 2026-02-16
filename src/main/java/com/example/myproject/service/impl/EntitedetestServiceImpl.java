package com.example.myproject.service.impl;

import com.example.myproject.domain.Entitedetest;
import com.example.myproject.repository.EntitedetestRepository;
import com.example.myproject.service.EntitedetestService;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link com.example.myproject.domain.Entitedetest}.
 */
@Service
@Transactional
public class EntitedetestServiceImpl implements EntitedetestService {

    private final Logger log = LoggerFactory.getLogger(EntitedetestServiceImpl.class);

    private final EntitedetestRepository entitedetestRepository;

    public EntitedetestServiceImpl(EntitedetestRepository entitedetestRepository) {
        this.entitedetestRepository = entitedetestRepository;
    }

    @Override
    public Entitedetest save(Entitedetest entitedetest) {
        log.debug("Request to save Entitedetest : {}", entitedetest);
        return entitedetestRepository.save(entitedetest);
    }

    @Override
    public Entitedetest update(Entitedetest entitedetest) {
        log.debug("Request to update Entitedetest : {}", entitedetest);
        return entitedetestRepository.save(entitedetest);
    }

    @Override
    public Optional<Entitedetest> partialUpdate(Entitedetest entitedetest) {
        log.debug("Request to partially update Entitedetest : {}", entitedetest);

        return entitedetestRepository
            .findById(entitedetest.getId())
            .map(existingEntitedetest -> {
                if (entitedetest.getIdentite() != null) {
                    existingEntitedetest.setIdentite(entitedetest.getIdentite());
                }
                if (entitedetest.getNom() != null) {
                    existingEntitedetest.setNom(entitedetest.getNom());
                }
                if (entitedetest.getNombrec() != null) {
                    existingEntitedetest.setNombrec(entitedetest.getNombrec());
                }
                if (entitedetest.getChamb() != null) {
                    existingEntitedetest.setChamb(entitedetest.getChamb());
                }
                if (entitedetest.getChampdate() != null) {
                    existingEntitedetest.setChampdate(entitedetest.getChampdate());
                }

                return existingEntitedetest;
            })
            .map(entitedetestRepository::save);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Entitedetest> findAll(Pageable pageable) {
        log.debug("Request to get all Entitedetests");
        return entitedetestRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Entitedetest> findOne(Long id) {
        log.debug("Request to get Entitedetest : {}", id);
        return entitedetestRepository.findById(id);
    }

    @Override
    public void delete(Long id) {
        log.debug("Request to delete Entitedetest : {}", id);
        entitedetestRepository.deleteById(id);
    }
}
