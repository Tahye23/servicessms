package com.example.myproject.web.rest;

import static com.example.myproject.domain.GroupedecontactAsserts.*;
import static com.example.myproject.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.myproject.IntegrationTest;
import com.example.myproject.domain.Groupedecontact;
import com.example.myproject.repository.GroupedecontactRepository;
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
 * Integration tests for the {@link GroupedecontactResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class GroupedecontactResourceIT {

    private static final String ENTITY_API_URL = "/api/groupedecontacts";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private GroupedecontactRepository groupedecontactRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restGroupedecontactMockMvc;

    private Groupedecontact groupedecontact;

    private Groupedecontact insertedGroupedecontact;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Groupedecontact createEntity(EntityManager em) {
        Groupedecontact groupedecontact = new Groupedecontact();
        return groupedecontact;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Groupedecontact createUpdatedEntity(EntityManager em) {
        Groupedecontact groupedecontact = new Groupedecontact();
        return groupedecontact;
    }

    @BeforeEach
    public void initTest() {
        groupedecontact = createEntity(em);
    }

    @AfterEach
    public void cleanup() {
        if (insertedGroupedecontact != null) {
            groupedecontactRepository.delete(insertedGroupedecontact);
            insertedGroupedecontact = null;
        }
    }

    @Test
    @Transactional
    void createGroupedecontact() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the Groupedecontact
        var returnedGroupedecontact = om.readValue(
            restGroupedecontactMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(groupedecontact)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            Groupedecontact.class
        );

        // Validate the Groupedecontact in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        assertGroupedecontactUpdatableFieldsEquals(returnedGroupedecontact, getPersistedGroupedecontact(returnedGroupedecontact));

        insertedGroupedecontact = returnedGroupedecontact;
    }

    @Test
    @Transactional
    void createGroupedecontactWithExistingId() throws Exception {
        // Create the Groupedecontact with an existing ID
        groupedecontact.setId(1L);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restGroupedecontactMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(groupedecontact)))
            .andExpect(status().isBadRequest());

        // Validate the Groupedecontact in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void getAllGroupedecontacts() throws Exception {
        // Initialize the database
        insertedGroupedecontact = groupedecontactRepository.saveAndFlush(groupedecontact);

        // Get all the groupedecontactList
        restGroupedecontactMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(groupedecontact.getId().intValue())));
    }

    @Test
    @Transactional
    void getGroupedecontact() throws Exception {
        // Initialize the database
        insertedGroupedecontact = groupedecontactRepository.saveAndFlush(groupedecontact);

        // Get the groupedecontact
        restGroupedecontactMockMvc
            .perform(get(ENTITY_API_URL_ID, groupedecontact.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(groupedecontact.getId().intValue()));
    }

    @Test
    @Transactional
    void getNonExistingGroupedecontact() throws Exception {
        // Get the groupedecontact
        restGroupedecontactMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingGroupedecontact() throws Exception {
        // Initialize the database
        insertedGroupedecontact = groupedecontactRepository.saveAndFlush(groupedecontact);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the groupedecontact
        Groupedecontact updatedGroupedecontact = groupedecontactRepository.findById(groupedecontact.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedGroupedecontact are not directly saved in db
        em.detach(updatedGroupedecontact);

        restGroupedecontactMockMvc
            .perform(
                put(ENTITY_API_URL_ID, updatedGroupedecontact.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(updatedGroupedecontact))
            )
            .andExpect(status().isOk());

        // Validate the Groupedecontact in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedGroupedecontactToMatchAllProperties(updatedGroupedecontact);
    }

    @Test
    @Transactional
    void putNonExistingGroupedecontact() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        groupedecontact.setId(longCount.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restGroupedecontactMockMvc
            .perform(
                put(ENTITY_API_URL_ID, groupedecontact.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(groupedecontact))
            )
            .andExpect(status().isBadRequest());

        // Validate the Groupedecontact in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchGroupedecontact() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        groupedecontact.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restGroupedecontactMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(groupedecontact))
            )
            .andExpect(status().isBadRequest());

        // Validate the Groupedecontact in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamGroupedecontact() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        groupedecontact.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restGroupedecontactMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(groupedecontact)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Groupedecontact in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateGroupedecontactWithPatch() throws Exception {
        // Initialize the database
        insertedGroupedecontact = groupedecontactRepository.saveAndFlush(groupedecontact);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the groupedecontact using partial update
        Groupedecontact partialUpdatedGroupedecontact = new Groupedecontact();
        partialUpdatedGroupedecontact.setId(groupedecontact.getId());

        restGroupedecontactMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedGroupedecontact.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedGroupedecontact))
            )
            .andExpect(status().isOk());

        // Validate the Groupedecontact in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertGroupedecontactUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedGroupedecontact, groupedecontact),
            getPersistedGroupedecontact(groupedecontact)
        );
    }

    @Test
    @Transactional
    void fullUpdateGroupedecontactWithPatch() throws Exception {
        // Initialize the database
        insertedGroupedecontact = groupedecontactRepository.saveAndFlush(groupedecontact);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the groupedecontact using partial update
        Groupedecontact partialUpdatedGroupedecontact = new Groupedecontact();
        partialUpdatedGroupedecontact.setId(groupedecontact.getId());

        restGroupedecontactMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedGroupedecontact.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedGroupedecontact))
            )
            .andExpect(status().isOk());

        // Validate the Groupedecontact in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertGroupedecontactUpdatableFieldsEquals(
            partialUpdatedGroupedecontact,
            getPersistedGroupedecontact(partialUpdatedGroupedecontact)
        );
    }

    @Test
    @Transactional
    void patchNonExistingGroupedecontact() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        groupedecontact.setId(longCount.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restGroupedecontactMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, groupedecontact.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(groupedecontact))
            )
            .andExpect(status().isBadRequest());

        // Validate the Groupedecontact in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchGroupedecontact() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        groupedecontact.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restGroupedecontactMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(groupedecontact))
            )
            .andExpect(status().isBadRequest());

        // Validate the Groupedecontact in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamGroupedecontact() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        groupedecontact.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restGroupedecontactMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(groupedecontact)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Groupedecontact in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteGroupedecontact() throws Exception {
        // Initialize the database
        insertedGroupedecontact = groupedecontactRepository.saveAndFlush(groupedecontact);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the groupedecontact
        restGroupedecontactMockMvc
            .perform(delete(ENTITY_API_URL_ID, groupedecontact.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return groupedecontactRepository.count();
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

    protected Groupedecontact getPersistedGroupedecontact(Groupedecontact groupedecontact) {
        return groupedecontactRepository.findById(groupedecontact.getId()).orElseThrow();
    }

    protected void assertPersistedGroupedecontactToMatchAllProperties(Groupedecontact expectedGroupedecontact) {
        assertGroupedecontactAllPropertiesEquals(expectedGroupedecontact, getPersistedGroupedecontact(expectedGroupedecontact));
    }

    protected void assertPersistedGroupedecontactToMatchUpdatableProperties(Groupedecontact expectedGroupedecontact) {
        assertGroupedecontactAllUpdatablePropertiesEquals(expectedGroupedecontact, getPersistedGroupedecontact(expectedGroupedecontact));
    }
}
