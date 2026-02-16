package com.example.myproject.web.rest;

import static com.example.myproject.domain.ConversationAsserts.*;
import static com.example.myproject.web.rest.TestUtil.createUpdateProxyForBean;
import static com.example.myproject.web.rest.TestUtil.sameInstant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.myproject.IntegrationTest;
import com.example.myproject.domain.Conversation;
import com.example.myproject.repository.ConversationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
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
 * Integration tests for the {@link ConversationResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class ConversationResourceIT {

    private static final ZonedDateTime DEFAULT_COVDATEDEBUT = ZonedDateTime.ofInstant(Instant.ofEpochMilli(0L), ZoneOffset.UTC);
    private static final ZonedDateTime UPDATED_COVDATEDEBUT = ZonedDateTime.now(ZoneId.systemDefault()).withNano(0);

    private static final ZonedDateTime DEFAULT_COVDATEFIN = ZonedDateTime.ofInstant(Instant.ofEpochMilli(0L), ZoneOffset.UTC);
    private static final ZonedDateTime UPDATED_COVDATEFIN = ZonedDateTime.now(ZoneId.systemDefault()).withNano(0);

    private static final String ENTITY_API_URL = "/api/conversations";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restConversationMockMvc;

    private Conversation conversation;

    private Conversation insertedConversation;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Conversation createEntity(EntityManager em) {
        Conversation conversation = new Conversation().covdatedebut(DEFAULT_COVDATEDEBUT).covdatefin(DEFAULT_COVDATEFIN);
        return conversation;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Conversation createUpdatedEntity(EntityManager em) {
        Conversation conversation = new Conversation().covdatedebut(UPDATED_COVDATEDEBUT).covdatefin(UPDATED_COVDATEFIN);
        return conversation;
    }

    @BeforeEach
    public void initTest() {
        conversation = createEntity(em);
    }

    @AfterEach
    public void cleanup() {
        if (insertedConversation != null) {
            conversationRepository.delete(insertedConversation);
            insertedConversation = null;
        }
    }

    @Test
    @Transactional
    void createConversation() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the Conversation
        var returnedConversation = om.readValue(
            restConversationMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(conversation)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            Conversation.class
        );

        // Validate the Conversation in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        assertConversationUpdatableFieldsEquals(returnedConversation, getPersistedConversation(returnedConversation));

        insertedConversation = returnedConversation;
    }

    @Test
    @Transactional
    void createConversationWithExistingId() throws Exception {
        // Create the Conversation with an existing ID
        conversation.setId(1L);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restConversationMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(conversation)))
            .andExpect(status().isBadRequest());

        // Validate the Conversation in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void getAllConversations() throws Exception {
        // Initialize the database
        insertedConversation = conversationRepository.saveAndFlush(conversation);

        // Get all the conversationList
        restConversationMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(conversation.getId().intValue())))
            .andExpect(jsonPath("$.[*].covdatedebut").value(hasItem(sameInstant(DEFAULT_COVDATEDEBUT))))
            .andExpect(jsonPath("$.[*].covdatefin").value(hasItem(sameInstant(DEFAULT_COVDATEFIN))));
    }

    @Test
    @Transactional
    void getConversation() throws Exception {
        // Initialize the database
        insertedConversation = conversationRepository.saveAndFlush(conversation);

        // Get the conversation
        restConversationMockMvc
            .perform(get(ENTITY_API_URL_ID, conversation.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(conversation.getId().intValue()))
            .andExpect(jsonPath("$.covdatedebut").value(sameInstant(DEFAULT_COVDATEDEBUT)))
            .andExpect(jsonPath("$.covdatefin").value(sameInstant(DEFAULT_COVDATEFIN)));
    }

    @Test
    @Transactional
    void getNonExistingConversation() throws Exception {
        // Get the conversation
        restConversationMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingConversation() throws Exception {
        // Initialize the database
        insertedConversation = conversationRepository.saveAndFlush(conversation);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the conversation
        Conversation updatedConversation = conversationRepository.findById(conversation.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedConversation are not directly saved in db
        em.detach(updatedConversation);
        updatedConversation.covdatedebut(UPDATED_COVDATEDEBUT).covdatefin(UPDATED_COVDATEFIN);

        restConversationMockMvc
            .perform(
                put(ENTITY_API_URL_ID, updatedConversation.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(updatedConversation))
            )
            .andExpect(status().isOk());

        // Validate the Conversation in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedConversationToMatchAllProperties(updatedConversation);
    }

    @Test
    @Transactional
    void putNonExistingConversation() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        conversation.setId(longCount.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restConversationMockMvc
            .perform(
                put(ENTITY_API_URL_ID, conversation.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(conversation))
            )
            .andExpect(status().isBadRequest());

        // Validate the Conversation in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchConversation() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        conversation.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restConversationMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(conversation))
            )
            .andExpect(status().isBadRequest());

        // Validate the Conversation in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamConversation() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        conversation.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restConversationMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(conversation)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Conversation in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateConversationWithPatch() throws Exception {
        // Initialize the database
        insertedConversation = conversationRepository.saveAndFlush(conversation);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the conversation using partial update
        Conversation partialUpdatedConversation = new Conversation();
        partialUpdatedConversation.setId(conversation.getId());

        partialUpdatedConversation.covdatedebut(UPDATED_COVDATEDEBUT);

        restConversationMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedConversation.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedConversation))
            )
            .andExpect(status().isOk());

        // Validate the Conversation in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertConversationUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedConversation, conversation),
            getPersistedConversation(conversation)
        );
    }

    @Test
    @Transactional
    void fullUpdateConversationWithPatch() throws Exception {
        // Initialize the database
        insertedConversation = conversationRepository.saveAndFlush(conversation);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the conversation using partial update
        Conversation partialUpdatedConversation = new Conversation();
        partialUpdatedConversation.setId(conversation.getId());

        partialUpdatedConversation.covdatedebut(UPDATED_COVDATEDEBUT).covdatefin(UPDATED_COVDATEFIN);

        restConversationMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedConversation.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedConversation))
            )
            .andExpect(status().isOk());

        // Validate the Conversation in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertConversationUpdatableFieldsEquals(partialUpdatedConversation, getPersistedConversation(partialUpdatedConversation));
    }

    @Test
    @Transactional
    void patchNonExistingConversation() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        conversation.setId(longCount.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restConversationMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, conversation.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(conversation))
            )
            .andExpect(status().isBadRequest());

        // Validate the Conversation in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchConversation() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        conversation.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restConversationMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(conversation))
            )
            .andExpect(status().isBadRequest());

        // Validate the Conversation in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamConversation() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        conversation.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restConversationMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(conversation)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Conversation in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteConversation() throws Exception {
        // Initialize the database
        insertedConversation = conversationRepository.saveAndFlush(conversation);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the conversation
        restConversationMockMvc
            .perform(delete(ENTITY_API_URL_ID, conversation.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return conversationRepository.count();
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

    protected Conversation getPersistedConversation(Conversation conversation) {
        return conversationRepository.findById(conversation.getId()).orElseThrow();
    }

    protected void assertPersistedConversationToMatchAllProperties(Conversation expectedConversation) {
        assertConversationAllPropertiesEquals(expectedConversation, getPersistedConversation(expectedConversation));
    }

    protected void assertPersistedConversationToMatchUpdatableProperties(Conversation expectedConversation) {
        assertConversationAllUpdatablePropertiesEquals(expectedConversation, getPersistedConversation(expectedConversation));
    }
}
