package com.example.myproject.web.rest;

import static com.example.myproject.domain.ParticipantAsserts.*;
import static com.example.myproject.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.myproject.IntegrationTest;
import com.example.myproject.domain.Participant;
import com.example.myproject.repository.ParticipantRepository;
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
 * Integration tests for the {@link ParticipantResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class ParticipantResourceIT {

    private static final String ENTITY_API_URL = "/api/participants";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private ParticipantRepository participantRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restParticipantMockMvc;

    private Participant participant;

    private Participant insertedParticipant;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Participant createEntity(EntityManager em) {
        Participant participant = new Participant();
        return participant;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Participant createUpdatedEntity(EntityManager em) {
        Participant participant = new Participant();
        return participant;
    }

    @BeforeEach
    public void initTest() {
        participant = createEntity(em);
    }

    @AfterEach
    public void cleanup() {
        if (insertedParticipant != null) {
            participantRepository.delete(insertedParticipant);
            insertedParticipant = null;
        }
    }

    @Test
    @Transactional
    void createParticipant() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the Participant
        var returnedParticipant = om.readValue(
            restParticipantMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(participant)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            Participant.class
        );

        // Validate the Participant in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        assertParticipantUpdatableFieldsEquals(returnedParticipant, getPersistedParticipant(returnedParticipant));

        insertedParticipant = returnedParticipant;
    }

    @Test
    @Transactional
    void createParticipantWithExistingId() throws Exception {
        // Create the Participant with an existing ID
        participant.setId(1L);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restParticipantMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(participant)))
            .andExpect(status().isBadRequest());

        // Validate the Participant in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void getAllParticipants() throws Exception {
        // Initialize the database
        insertedParticipant = participantRepository.saveAndFlush(participant);

        // Get all the participantList
        restParticipantMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(participant.getId().intValue())));
    }

    @Test
    @Transactional
    void getParticipant() throws Exception {
        // Initialize the database
        insertedParticipant = participantRepository.saveAndFlush(participant);

        // Get the participant
        restParticipantMockMvc
            .perform(get(ENTITY_API_URL_ID, participant.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(participant.getId().intValue()));
    }

    @Test
    @Transactional
    void getNonExistingParticipant() throws Exception {
        // Get the participant
        restParticipantMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingParticipant() throws Exception {
        // Initialize the database
        insertedParticipant = participantRepository.saveAndFlush(participant);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the participant
        Participant updatedParticipant = participantRepository.findById(participant.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedParticipant are not directly saved in db
        em.detach(updatedParticipant);

        restParticipantMockMvc
            .perform(
                put(ENTITY_API_URL_ID, updatedParticipant.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(updatedParticipant))
            )
            .andExpect(status().isOk());

        // Validate the Participant in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedParticipantToMatchAllProperties(updatedParticipant);
    }

    @Test
    @Transactional
    void putNonExistingParticipant() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        participant.setId(longCount.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restParticipantMockMvc
            .perform(
                put(ENTITY_API_URL_ID, participant.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(participant))
            )
            .andExpect(status().isBadRequest());

        // Validate the Participant in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchParticipant() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        participant.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restParticipantMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(participant))
            )
            .andExpect(status().isBadRequest());

        // Validate the Participant in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamParticipant() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        participant.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restParticipantMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(participant)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Participant in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateParticipantWithPatch() throws Exception {
        // Initialize the database
        insertedParticipant = participantRepository.saveAndFlush(participant);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the participant using partial update
        Participant partialUpdatedParticipant = new Participant();
        partialUpdatedParticipant.setId(participant.getId());

        restParticipantMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedParticipant.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedParticipant))
            )
            .andExpect(status().isOk());

        // Validate the Participant in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertParticipantUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedParticipant, participant),
            getPersistedParticipant(participant)
        );
    }

    @Test
    @Transactional
    void fullUpdateParticipantWithPatch() throws Exception {
        // Initialize the database
        insertedParticipant = participantRepository.saveAndFlush(participant);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the participant using partial update
        Participant partialUpdatedParticipant = new Participant();
        partialUpdatedParticipant.setId(participant.getId());

        restParticipantMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedParticipant.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedParticipant))
            )
            .andExpect(status().isOk());

        // Validate the Participant in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertParticipantUpdatableFieldsEquals(partialUpdatedParticipant, getPersistedParticipant(partialUpdatedParticipant));
    }

    @Test
    @Transactional
    void patchNonExistingParticipant() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        participant.setId(longCount.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restParticipantMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, participant.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(participant))
            )
            .andExpect(status().isBadRequest());

        // Validate the Participant in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchParticipant() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        participant.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restParticipantMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(participant))
            )
            .andExpect(status().isBadRequest());

        // Validate the Participant in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamParticipant() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        participant.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restParticipantMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(participant)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Participant in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteParticipant() throws Exception {
        // Initialize the database
        insertedParticipant = participantRepository.saveAndFlush(participant);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the participant
        restParticipantMockMvc
            .perform(delete(ENTITY_API_URL_ID, participant.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return participantRepository.count();
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

    protected Participant getPersistedParticipant(Participant participant) {
        return participantRepository.findById(participant.getId()).orElseThrow();
    }

    protected void assertPersistedParticipantToMatchAllProperties(Participant expectedParticipant) {
        assertParticipantAllPropertiesEquals(expectedParticipant, getPersistedParticipant(expectedParticipant));
    }

    protected void assertPersistedParticipantToMatchUpdatableProperties(Participant expectedParticipant) {
        assertParticipantAllUpdatablePropertiesEquals(expectedParticipant, getPersistedParticipant(expectedParticipant));
    }
}
