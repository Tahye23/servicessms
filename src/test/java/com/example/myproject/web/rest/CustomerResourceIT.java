package com.example.myproject.web.rest;

import static com.example.myproject.domain.CustomerAsserts.*;
import static com.example.myproject.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.myproject.IntegrationTest;
import com.example.myproject.domain.Customer;
import com.example.myproject.repository.CustomerRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for the {@link CustomerResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class CustomerResourceIT {

    private static final Integer DEFAULT_CUSTOMER_ID = 1;
    private static final Integer UPDATED_CUSTOMER_ID = 2;

    private static final String DEFAULT_FIRST_NAME = "AAAAAAAAAA";
    private static final String UPDATED_FIRST_NAME = "BBBBBBBBBB";

    private static final String DEFAULT_LAST_NAME = "AAAAAAAAAA";
    private static final String UPDATED_LAST_NAME = "BBBBBBBBBB";

    private static final String DEFAULT_COUNTRY = "AAAAAAAAAA";
    private static final String UPDATED_COUNTRY = "BBBBBBBBBB";

    private static final String DEFAULT_TELEPHONE = "AAAAAAAAAA";
    private static final String UPDATED_TELEPHONE = "BBBBBBBBBB";

    private static final String ENTITY_API_URL = "/api/customers";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restCustomerMockMvc;

    private Customer customer;

    private Customer insertedCustomer;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Customer createEntity(EntityManager em) {
        Customer customer = new Customer()
            .customerId(DEFAULT_CUSTOMER_ID)
            .firstName(DEFAULT_FIRST_NAME)
            .lastName(DEFAULT_LAST_NAME)
            .country(DEFAULT_COUNTRY)
            .telephone(DEFAULT_TELEPHONE);
        return customer;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Customer createUpdatedEntity(EntityManager em) {
        Customer customer = new Customer()
            .customerId(UPDATED_CUSTOMER_ID)
            .firstName(UPDATED_FIRST_NAME)
            .lastName(UPDATED_LAST_NAME)
            .country(UPDATED_COUNTRY)
            .telephone(UPDATED_TELEPHONE);
        return customer;
    }

    @BeforeEach
    public void initTest() {
        customer = createEntity(em);
    }

    @AfterEach
    public void cleanup() {
        if (insertedCustomer != null) {
            customerRepository.delete(insertedCustomer);
            insertedCustomer = null;
        }
    }

    @Test
    @Transactional
    void createCustomer() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the Customer
        var returnedCustomer = om.readValue(
            restCustomerMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(customer)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            Customer.class
        );

        // Validate the Customer in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        assertCustomerUpdatableFieldsEquals(returnedCustomer, getPersistedCustomer(returnedCustomer));

        insertedCustomer = returnedCustomer;
    }

    @Test
    @Transactional
    void createCustomerWithExistingId() throws Exception {
        // Create the Customer with an existing ID
        customer.setId(1L);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restCustomerMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(customer)))
            .andExpect(status().isBadRequest());

        // Validate the Customer in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void getAllCustomers() throws Exception {
        // Initialize the database
        insertedCustomer = customerRepository.saveAndFlush(customer);

        // Get all the customerList
        restCustomerMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(customer.getId().intValue())))
            .andExpect(jsonPath("$.[*].customerId").value(hasItem(DEFAULT_CUSTOMER_ID)))
            .andExpect(jsonPath("$.[*].firstName").value(hasItem(DEFAULT_FIRST_NAME)))
            .andExpect(jsonPath("$.[*].lastName").value(hasItem(DEFAULT_LAST_NAME)))
            .andExpect(jsonPath("$.[*].country").value(hasItem(DEFAULT_COUNTRY)))
            .andExpect(jsonPath("$.[*].telephone").value(hasItem(DEFAULT_TELEPHONE)));
    }

    @Test
    @Transactional
    void getCustomer() throws Exception {
        // Initialize the database
        insertedCustomer = customerRepository.saveAndFlush(customer);

        // Get the customer
        restCustomerMockMvc
            .perform(get(ENTITY_API_URL_ID, customer.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(customer.getId().intValue()))
            .andExpect(jsonPath("$.customerId").value(DEFAULT_CUSTOMER_ID))
            .andExpect(jsonPath("$.firstName").value(DEFAULT_FIRST_NAME))
            .andExpect(jsonPath("$.lastName").value(DEFAULT_LAST_NAME))
            .andExpect(jsonPath("$.country").value(DEFAULT_COUNTRY))
            .andExpect(jsonPath("$.telephone").value(DEFAULT_TELEPHONE));
    }

    @Test
    @Transactional
    void getNonExistingCustomer() throws Exception {
        // Get the customer
        restCustomerMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingCustomer() throws Exception {
        // Initialize the database
        insertedCustomer = customerRepository.saveAndFlush(customer);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the customer
        Customer updatedCustomer = customerRepository.findById(customer.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedCustomer are not directly saved in db
        em.detach(updatedCustomer);
        updatedCustomer
            .customerId(UPDATED_CUSTOMER_ID)
            .firstName(UPDATED_FIRST_NAME)
            .lastName(UPDATED_LAST_NAME)
            .country(UPDATED_COUNTRY)
            .telephone(UPDATED_TELEPHONE);

        restCustomerMockMvc
            .perform(
                put(ENTITY_API_URL_ID, updatedCustomer.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(updatedCustomer))
            )
            .andExpect(status().isOk());

        // Validate the Customer in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedCustomerToMatchAllProperties(updatedCustomer);
    }

    @Test
    @Transactional
    void putNonExistingCustomer() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        customer.setId(longCount.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restCustomerMockMvc
            .perform(
                put(ENTITY_API_URL_ID, customer.getId()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(customer))
            )
            .andExpect(status().isBadRequest());

        // Validate the Customer in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchCustomer() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        customer.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restCustomerMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(customer))
            )
            .andExpect(status().isBadRequest());

        // Validate the Customer in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamCustomer() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        customer.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restCustomerMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(customer)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Customer in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateCustomerWithPatch() throws Exception {
        // Initialize the database
        insertedCustomer = customerRepository.saveAndFlush(customer);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the customer using partial update
        Customer partialUpdatedCustomer = new Customer();
        partialUpdatedCustomer.setId(customer.getId());

        partialUpdatedCustomer.customerId(UPDATED_CUSTOMER_ID).telephone(UPDATED_TELEPHONE);

        restCustomerMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedCustomer.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedCustomer))
            )
            .andExpect(status().isOk());

        // Validate the Customer in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertCustomerUpdatableFieldsEquals(createUpdateProxyForBean(partialUpdatedCustomer, customer), getPersistedCustomer(customer));
    }

    @Test
    @Transactional
    void fullUpdateCustomerWithPatch() throws Exception {
        // Initialize the database
        insertedCustomer = customerRepository.saveAndFlush(customer);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the customer using partial update
        Customer partialUpdatedCustomer = new Customer();
        partialUpdatedCustomer.setId(customer.getId());

        partialUpdatedCustomer
            .customerId(UPDATED_CUSTOMER_ID)
            .firstName(UPDATED_FIRST_NAME)
            .lastName(UPDATED_LAST_NAME)
            .country(UPDATED_COUNTRY)
            .telephone(UPDATED_TELEPHONE);

        restCustomerMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedCustomer.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedCustomer))
            )
            .andExpect(status().isOk());

        // Validate the Customer in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertCustomerUpdatableFieldsEquals(partialUpdatedCustomer, getPersistedCustomer(partialUpdatedCustomer));
    }

    @Test
    @Transactional
    void patchNonExistingCustomer() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        customer.setId(longCount.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restCustomerMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, customer.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(customer))
            )
            .andExpect(status().isBadRequest());

        // Validate the Customer in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchCustomer() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        customer.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restCustomerMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(customer))
            )
            .andExpect(status().isBadRequest());

        // Validate the Customer in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamCustomer() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        customer.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restCustomerMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(customer)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Customer in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteCustomer() throws Exception {
        // Initialize the database
        insertedCustomer = customerRepository.saveAndFlush(customer);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the customer
        restCustomerMockMvc
            .perform(delete(ENTITY_API_URL_ID, customer.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return customerRepository.count();
    }

    protected void assertIncrementedRepositoryCount(long countBefore) {
        assertThat(countBefore + 1).isEqualTo(getRepositoryCount());
    }

    protected void assertDecrementedRepositoryCount(long countBefore) {
        assertThat(countBefore - 1).isEqualTo(getRepositoryCount());
    }

    protected void assertSameRepositoryCount(long countBefore) {
        assertThat(countBefore).isEqualTo(getRepositoryCount());
    }

    protected Customer getPersistedCustomer(Customer customer) {
        return customerRepository.findById(customer.getId()).orElseThrow();
    }

    protected void assertPersistedCustomerToMatchAllProperties(Customer expectedCustomer) {
        assertCustomerAllPropertiesEquals(expectedCustomer, getPersistedCustomer(expectedCustomer));
    }

    protected void assertPersistedCustomerToMatchUpdatableProperties(Customer expectedCustomer) {
        assertCustomerAllUpdatablePropertiesEquals(expectedCustomer, getPersistedCustomer(expectedCustomer));
    }
}
