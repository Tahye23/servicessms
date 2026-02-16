package com.example.myproject.service;

import com.example.myproject.domain.Customer;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service Interface for managing {@link com.example.myproject.domain.Customer}.
 */
public interface CustomerService {
    /**
     * Save a customer.
     *
     * @param customer the entity to save.
     * @return the persisted entity.
     */
    Customer save(Customer customer);

    /**
     * Updates a customer.
     *
     * @param customer the entity to update.
     * @return the persisted entity.
     */
    Customer update(Customer customer);

    /**
     * Partially updates a customer.
     *
     * @param customer the entity to update partially.
     * @return the persisted entity.
     */
    Optional<Customer> partialUpdate(Customer customer);

    /**
     * Get all the customers.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    Page<Customer> findAll(Pageable pageable);

    /**
     * Get the "id" customer.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Optional<Customer> findOne(Long id);

    /**
     * Delete the "id" customer.
     *
     * @param id the id of the entity.
     */
    void delete(Long id);

    void saveCustomersToDatabase(MultipartFile file);
}
