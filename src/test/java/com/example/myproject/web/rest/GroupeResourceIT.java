package com.example.myproject.web.rest;

import static com.example.myproject.domain.GroupeAsserts.*;
import static com.example.myproject.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.myproject.IntegrationTest;
import com.example.myproject.domain.Groupe;
import com.example.myproject.repository.GroupeRepository;
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
 * Integration tests for the {@link GroupeResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class GroupeResourceIT {

    private static final String DEFAULT_GROTITRE = "AAAAAAAAAA";
    private static final String UPDATED_GROTITRE = "BBBBBBBBBB";

    private static final String DEFAULT_USER = "AAAAAAAAAA";
    private static final String UPDATED_USER = "BBBBBBBBBB";

    private static final String ENTITY_API_URL = "/api/groupes";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private GroupeRepository groupeRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restGroupeMockMvc;

    private Groupe groupe;

    private Groupe insertedGroupe;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Groupe createEntity(EntityManager em) {
        Groupe groupe = new Groupe().grotitre(DEFAULT_GROTITRE).user(DEFAULT_USER);
        return groupe;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Groupe createUpdatedEntity(EntityManager em) {
        Groupe groupe = new Groupe().grotitre(UPDATED_GROTITRE).user(UPDATED_USER);
        return groupe;
    }

    @BeforeEach
    public void initTest() {
        groupe = createEntity(em);
    }

    @AfterEach
    public void cleanup() {
        if (insertedGroupe != null) {
            groupeRepository.delete(insertedGroupe);
            insertedGroupe = null;
        }
    }

    @Test
    @Transactional
    void createGroupe() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the Groupe
        var returnedGroupe = om.readValue(
            restGroupeMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(groupe)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            Groupe.class
        );

        // Validate the Groupe in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        assertGroupeUpdatableFieldsEquals(returnedGroupe, getPersistedGroupe(returnedGroupe));

        insertedGroupe = returnedGroupe;
    }

    @Test
    @Transactional
    void createGroupeWithExistingId() throws Exception {
        // Create the Groupe with an existing ID
        groupe.setId(1L);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restGroupeMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(groupe)))
            .andExpect(status().isBadRequest());

        // Validate the Groupe in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void getAllGroupes() throws Exception {
        // Initialize the database
        insertedGroupe = groupeRepository.saveAndFlush(groupe);

        // Get all the groupeList
        restGroupeMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(groupe.getId().intValue())))
            .andExpect(jsonPath("$.[*].grotitre").value(hasItem(DEFAULT_GROTITRE)))
            .andExpect(jsonPath("$.[*].user").value(hasItem(DEFAULT_USER)));
    }

    @Test
    @Transactional
    void getGroupe() throws Exception {
        // Initialize the database
        insertedGroupe = groupeRepository.saveAndFlush(groupe);

        // Get the groupe
        restGroupeMockMvc
            .perform(get(ENTITY_API_URL_ID, groupe.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(groupe.getId().intValue()))
            .andExpect(jsonPath("$.grotitre").value(DEFAULT_GROTITRE))
            .andExpect(jsonPath("$.user").value(DEFAULT_USER));
    }

    @Test
    @Transactional
    void getNonExistingGroupe() throws Exception {
        // Get the groupe
        restGroupeMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingGroupe() throws Exception {
        // Initialize the database
        insertedGroupe = groupeRepository.saveAndFlush(groupe);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the groupe
        Groupe updatedGroupe = groupeRepository.findById(groupe.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedGroupe are not directly saved in db
        em.detach(updatedGroupe);
        updatedGroupe.grotitre(UPDATED_GROTITRE).user(UPDATED_USER);

        restGroupeMockMvc
            .perform(
                put(ENTITY_API_URL_ID, updatedGroupe.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(updatedGroupe))
            )
            .andExpect(status().isOk());

        // Validate the Groupe in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedGroupeToMatchAllProperties(updatedGroupe);
    }

    @Test
    @Transactional
    void putNonExistingGroupe() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        groupe.setId(longCount.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restGroupeMockMvc
            .perform(put(ENTITY_API_URL_ID, groupe.getId()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(groupe)))
            .andExpect(status().isBadRequest());

        // Validate the Groupe in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchGroupe() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        groupe.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restGroupeMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(groupe))
            )
            .andExpect(status().isBadRequest());

        // Validate the Groupe in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamGroupe() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        groupe.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restGroupeMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(groupe)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Groupe in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateGroupeWithPatch() throws Exception {
        // Initialize the database
        insertedGroupe = groupeRepository.saveAndFlush(groupe);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the groupe using partial update
        Groupe partialUpdatedGroupe = new Groupe();
        partialUpdatedGroupe.setId(groupe.getId());

        restGroupeMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedGroupe.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedGroupe))
            )
            .andExpect(status().isOk());

        // Validate the Groupe in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertGroupeUpdatableFieldsEquals(createUpdateProxyForBean(partialUpdatedGroupe, groupe), getPersistedGroupe(groupe));
    }

    @Test
    @Transactional
    void fullUpdateGroupeWithPatch() throws Exception {
        // Initialize the database
        insertedGroupe = groupeRepository.saveAndFlush(groupe);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the groupe using partial update
        Groupe partialUpdatedGroupe = new Groupe();
        partialUpdatedGroupe.setId(groupe.getId());

        partialUpdatedGroupe.grotitre(UPDATED_GROTITRE).user(UPDATED_USER);

        restGroupeMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedGroupe.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedGroupe))
            )
            .andExpect(status().isOk());

        // Validate the Groupe in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertGroupeUpdatableFieldsEquals(partialUpdatedGroupe, getPersistedGroupe(partialUpdatedGroupe));
    }

    @Test
    @Transactional
    void patchNonExistingGroupe() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        groupe.setId(longCount.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restGroupeMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, groupe.getId()).contentType("application/merge-patch+json").content(om.writeValueAsBytes(groupe))
            )
            .andExpect(status().isBadRequest());

        // Validate the Groupe in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchGroupe() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        groupe.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restGroupeMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(groupe))
            )
            .andExpect(status().isBadRequest());

        // Validate the Groupe in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamGroupe() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        groupe.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restGroupeMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(groupe)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Groupe in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteGroupe() throws Exception {
        // Initialize the database
        insertedGroupe = groupeRepository.saveAndFlush(groupe);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the groupe
        restGroupeMockMvc
            .perform(delete(ENTITY_API_URL_ID, groupe.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return groupeRepository.count();
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

    protected Groupe getPersistedGroupe(Groupe groupe) {
        return groupeRepository.findById(groupe.getId()).orElseThrow();
    }

    protected void assertPersistedGroupeToMatchAllProperties(Groupe expectedGroupe) {
        assertGroupeAllPropertiesEquals(expectedGroupe, getPersistedGroupe(expectedGroupe));
    }

    protected void assertPersistedGroupeToMatchUpdatableProperties(Groupe expectedGroupe) {
        assertGroupeAllUpdatablePropertiesEquals(expectedGroupe, getPersistedGroupe(expectedGroupe));
    }
}
