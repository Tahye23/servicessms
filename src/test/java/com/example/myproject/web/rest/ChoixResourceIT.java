package com.example.myproject.web.rest;

import static com.example.myproject.domain.ChoixAsserts.*;
import static com.example.myproject.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.myproject.IntegrationTest;
import com.example.myproject.domain.Choix;
import com.example.myproject.repository.ChoixRepository;
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
 * Integration tests for the {@link ChoixResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class ChoixResourceIT {

    private static final String DEFAULT_CHOVALEUR = "AAAAAAAAAA";
    private static final String UPDATED_CHOVALEUR = "BBBBBBBBBB";

    private static final String ENTITY_API_URL = "/api/choixes";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private ChoixRepository choixRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restChoixMockMvc;

    private Choix choix;

    private Choix insertedChoix;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Choix createEntity(EntityManager em) {
        Choix choix = new Choix().chovaleur(DEFAULT_CHOVALEUR);
        return choix;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Choix createUpdatedEntity(EntityManager em) {
        Choix choix = new Choix().chovaleur(UPDATED_CHOVALEUR);
        return choix;
    }

    @BeforeEach
    public void initTest() {
        choix = createEntity(em);
    }

    @AfterEach
    public void cleanup() {
        if (insertedChoix != null) {
            choixRepository.delete(insertedChoix);
            insertedChoix = null;
        }
    }

    @Test
    @Transactional
    void createChoix() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the Choix
        var returnedChoix = om.readValue(
            restChoixMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(choix)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            Choix.class
        );

        // Validate the Choix in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        assertChoixUpdatableFieldsEquals(returnedChoix, getPersistedChoix(returnedChoix));

        insertedChoix = returnedChoix;
    }

    @Test
    @Transactional
    void createChoixWithExistingId() throws Exception {
        // Create the Choix with an existing ID
        choix.setId(1L);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restChoixMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(choix)))
            .andExpect(status().isBadRequest());

        // Validate the Choix in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void getAllChoixes() throws Exception {
        // Initialize the database
        insertedChoix = choixRepository.saveAndFlush(choix);

        // Get all the choixList
        restChoixMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(choix.getId().intValue())))
            .andExpect(jsonPath("$.[*].chovaleur").value(hasItem(DEFAULT_CHOVALEUR)));
    }

    @Test
    @Transactional
    void getChoix() throws Exception {
        // Initialize the database
        insertedChoix = choixRepository.saveAndFlush(choix);

        // Get the choix
        restChoixMockMvc
            .perform(get(ENTITY_API_URL_ID, choix.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(choix.getId().intValue()))
            .andExpect(jsonPath("$.chovaleur").value(DEFAULT_CHOVALEUR));
    }

    @Test
    @Transactional
    void getNonExistingChoix() throws Exception {
        // Get the choix
        restChoixMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingChoix() throws Exception {
        // Initialize the database
        insertedChoix = choixRepository.saveAndFlush(choix);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the choix
        Choix updatedChoix = choixRepository.findById(choix.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedChoix are not directly saved in db
        em.detach(updatedChoix);
        updatedChoix.chovaleur(UPDATED_CHOVALEUR);

        restChoixMockMvc
            .perform(
                put(ENTITY_API_URL_ID, updatedChoix.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(updatedChoix))
            )
            .andExpect(status().isOk());

        // Validate the Choix in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedChoixToMatchAllProperties(updatedChoix);
    }

    @Test
    @Transactional
    void putNonExistingChoix() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        choix.setId(longCount.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restChoixMockMvc
            .perform(put(ENTITY_API_URL_ID, choix.getId()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(choix)))
            .andExpect(status().isBadRequest());

        // Validate the Choix in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchChoix() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        choix.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restChoixMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(choix))
            )
            .andExpect(status().isBadRequest());

        // Validate the Choix in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamChoix() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        choix.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restChoixMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(choix)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Choix in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateChoixWithPatch() throws Exception {
        // Initialize the database
        insertedChoix = choixRepository.saveAndFlush(choix);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the choix using partial update
        Choix partialUpdatedChoix = new Choix();
        partialUpdatedChoix.setId(choix.getId());

        partialUpdatedChoix.chovaleur(UPDATED_CHOVALEUR);

        restChoixMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedChoix.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedChoix))
            )
            .andExpect(status().isOk());

        // Validate the Choix in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertChoixUpdatableFieldsEquals(createUpdateProxyForBean(partialUpdatedChoix, choix), getPersistedChoix(choix));
    }

    @Test
    @Transactional
    void fullUpdateChoixWithPatch() throws Exception {
        // Initialize the database
        insertedChoix = choixRepository.saveAndFlush(choix);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the choix using partial update
        Choix partialUpdatedChoix = new Choix();
        partialUpdatedChoix.setId(choix.getId());

        partialUpdatedChoix.chovaleur(UPDATED_CHOVALEUR);

        restChoixMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedChoix.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedChoix))
            )
            .andExpect(status().isOk());

        // Validate the Choix in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertChoixUpdatableFieldsEquals(partialUpdatedChoix, getPersistedChoix(partialUpdatedChoix));
    }

    @Test
    @Transactional
    void patchNonExistingChoix() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        choix.setId(longCount.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restChoixMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, choix.getId()).contentType("application/merge-patch+json").content(om.writeValueAsBytes(choix))
            )
            .andExpect(status().isBadRequest());

        // Validate the Choix in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchChoix() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        choix.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restChoixMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(choix))
            )
            .andExpect(status().isBadRequest());

        // Validate the Choix in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamChoix() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        choix.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restChoixMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(choix)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Choix in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteChoix() throws Exception {
        // Initialize the database
        insertedChoix = choixRepository.saveAndFlush(choix);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the choix
        restChoixMockMvc
            .perform(delete(ENTITY_API_URL_ID, choix.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return choixRepository.count();
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

    protected Choix getPersistedChoix(Choix choix) {
        return choixRepository.findById(choix.getId()).orElseThrow();
    }

    protected void assertPersistedChoixToMatchAllProperties(Choix expectedChoix) {
        assertChoixAllPropertiesEquals(expectedChoix, getPersistedChoix(expectedChoix));
    }

    protected void assertPersistedChoixToMatchUpdatableProperties(Choix expectedChoix) {
        assertChoixAllUpdatablePropertiesEquals(expectedChoix, getPersistedChoix(expectedChoix));
    }
}
