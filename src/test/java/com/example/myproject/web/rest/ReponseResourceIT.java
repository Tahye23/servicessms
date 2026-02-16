package com.example.myproject.web.rest;

import static com.example.myproject.domain.ReponseAsserts.*;
import static com.example.myproject.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.myproject.IntegrationTest;
import com.example.myproject.domain.Reponse;
import com.example.myproject.repository.ReponseRepository;
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
 * Integration tests for the {@link ReponseResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class ReponseResourceIT {

    private static final String DEFAULT_REPVALEUR = "AAAAAAAAAA";
    private static final String UPDATED_REPVALEUR = "BBBBBBBBBB";

    private static final String ENTITY_API_URL = "/api/reponses";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private ReponseRepository reponseRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restReponseMockMvc;

    private Reponse reponse;

    private Reponse insertedReponse;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Reponse createEntity(EntityManager em) {
        Reponse reponse = new Reponse().repvaleur(DEFAULT_REPVALEUR);
        return reponse;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Reponse createUpdatedEntity(EntityManager em) {
        Reponse reponse = new Reponse().repvaleur(UPDATED_REPVALEUR);
        return reponse;
    }

    @BeforeEach
    public void initTest() {
        reponse = createEntity(em);
    }

    @AfterEach
    public void cleanup() {
        if (insertedReponse != null) {
            reponseRepository.delete(insertedReponse);
            insertedReponse = null;
        }
    }

    @Test
    @Transactional
    void createReponse() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the Reponse
        var returnedReponse = om.readValue(
            restReponseMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(reponse)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            Reponse.class
        );

        // Validate the Reponse in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        assertReponseUpdatableFieldsEquals(returnedReponse, getPersistedReponse(returnedReponse));

        insertedReponse = returnedReponse;
    }

    @Test
    @Transactional
    void createReponseWithExistingId() throws Exception {
        // Create the Reponse with an existing ID
        reponse.setId(1L);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restReponseMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(reponse)))
            .andExpect(status().isBadRequest());

        // Validate the Reponse in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void getAllReponses() throws Exception {
        // Initialize the database
        insertedReponse = reponseRepository.saveAndFlush(reponse);

        // Get all the reponseList
        restReponseMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(reponse.getId().intValue())))
            .andExpect(jsonPath("$.[*].repvaleur").value(hasItem(DEFAULT_REPVALEUR)));
    }

    @Test
    @Transactional
    void getReponse() throws Exception {
        // Initialize the database
        insertedReponse = reponseRepository.saveAndFlush(reponse);

        // Get the reponse
        restReponseMockMvc
            .perform(get(ENTITY_API_URL_ID, reponse.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(reponse.getId().intValue()))
            .andExpect(jsonPath("$.repvaleur").value(DEFAULT_REPVALEUR));
    }

    @Test
    @Transactional
    void getNonExistingReponse() throws Exception {
        // Get the reponse
        restReponseMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingReponse() throws Exception {
        // Initialize the database
        insertedReponse = reponseRepository.saveAndFlush(reponse);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the reponse
        Reponse updatedReponse = reponseRepository.findById(reponse.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedReponse are not directly saved in db
        em.detach(updatedReponse);
        updatedReponse.repvaleur(UPDATED_REPVALEUR);

        restReponseMockMvc
            .perform(
                put(ENTITY_API_URL_ID, updatedReponse.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(updatedReponse))
            )
            .andExpect(status().isOk());

        // Validate the Reponse in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedReponseToMatchAllProperties(updatedReponse);
    }

    @Test
    @Transactional
    void putNonExistingReponse() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        reponse.setId(longCount.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restReponseMockMvc
            .perform(put(ENTITY_API_URL_ID, reponse.getId()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(reponse)))
            .andExpect(status().isBadRequest());

        // Validate the Reponse in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchReponse() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        reponse.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restReponseMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(reponse))
            )
            .andExpect(status().isBadRequest());

        // Validate the Reponse in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamReponse() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        reponse.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restReponseMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(reponse)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Reponse in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateReponseWithPatch() throws Exception {
        // Initialize the database
        insertedReponse = reponseRepository.saveAndFlush(reponse);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the reponse using partial update
        Reponse partialUpdatedReponse = new Reponse();
        partialUpdatedReponse.setId(reponse.getId());

        restReponseMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedReponse.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedReponse))
            )
            .andExpect(status().isOk());

        // Validate the Reponse in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertReponseUpdatableFieldsEquals(createUpdateProxyForBean(partialUpdatedReponse, reponse), getPersistedReponse(reponse));
    }

    @Test
    @Transactional
    void fullUpdateReponseWithPatch() throws Exception {
        // Initialize the database
        insertedReponse = reponseRepository.saveAndFlush(reponse);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the reponse using partial update
        Reponse partialUpdatedReponse = new Reponse();
        partialUpdatedReponse.setId(reponse.getId());

        partialUpdatedReponse.repvaleur(UPDATED_REPVALEUR);

        restReponseMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedReponse.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedReponse))
            )
            .andExpect(status().isOk());

        // Validate the Reponse in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertReponseUpdatableFieldsEquals(partialUpdatedReponse, getPersistedReponse(partialUpdatedReponse));
    }

    @Test
    @Transactional
    void patchNonExistingReponse() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        reponse.setId(longCount.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restReponseMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, reponse.getId()).contentType("application/merge-patch+json").content(om.writeValueAsBytes(reponse))
            )
            .andExpect(status().isBadRequest());

        // Validate the Reponse in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchReponse() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        reponse.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restReponseMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(reponse))
            )
            .andExpect(status().isBadRequest());

        // Validate the Reponse in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamReponse() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        reponse.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restReponseMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(reponse)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Reponse in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteReponse() throws Exception {
        // Initialize the database
        insertedReponse = reponseRepository.saveAndFlush(reponse);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the reponse
        restReponseMockMvc
            .perform(delete(ENTITY_API_URL_ID, reponse.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return reponseRepository.count();
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

    protected Reponse getPersistedReponse(Reponse reponse) {
        return reponseRepository.findById(reponse.getId()).orElseThrow();
    }

    protected void assertPersistedReponseToMatchAllProperties(Reponse expectedReponse) {
        assertReponseAllPropertiesEquals(expectedReponse, getPersistedReponse(expectedReponse));
    }

    protected void assertPersistedReponseToMatchUpdatableProperties(Reponse expectedReponse) {
        assertReponseAllUpdatablePropertiesEquals(expectedReponse, getPersistedReponse(expectedReponse));
    }
}
