package com.example.myproject.service;

import com.example.myproject.domain.SendSms;
import com.example.myproject.domain.enumeration.MessageType;
import jakarta.transaction.Transactional;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service Interface for managing {@link com.example.myproject.domain.SendSms}.
 */
public interface SendSmsService {
    /**
     * Save a sendSms.
     *
     * @param sendSms the entity to save.
     * @return the persisted entity.
     */
    SendSms save(SendSms sendSms);

    /**
     * Updates a sendSms.
     *
     * @param sendSms the entity to update.
     * @return the persisted entity.
     */
    SendSms update(SendSms sendSms);

    /**
     * Partially updates a sendSms.
     *
     * @param sendSms the entity to update partially.
     * @return the persisted entity.
     */
    Optional<SendSms> partialUpdate(SendSms sendSms);

    /**
     * Get all the sendSms.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    Page<SendSms> findAll(Pageable pageable);

    /**
     * Get the "id" sendSms.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Optional<SendSms> findOne(Long id);

    /**
     * Delete the "id" sendSms.
     *
     * @param id the id of the entity.
     */
    void delete(Long id);

    Page<SendSms> findFiltered(
        String search,
        Boolean isSent,
        Boolean isBulk,
        String receiver,
        String receivers,
        Pageable pageable,
        boolean isAdmin,
        String userLogin,
        MessageType type
    );
}
