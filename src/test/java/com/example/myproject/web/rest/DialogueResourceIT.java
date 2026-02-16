package com.example.myproject.web.rest;

import static com.example.myproject.domain.DialogueAsserts.*;
import static com.example.myproject.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.myproject.IntegrationTest;
import com.example.myproject.domain.Dialogue;
import com.example.myproject.repository.DialogueRepository;
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
 * Integration tests for the {@link DialogueResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class DialogueResourceIT {

    private static final Integer DEFAULT_DIALOGUE_ID = 1;
    private static final Integer UPDATED_DIALOGUE_ID = 2;

    private static final String DEFAULT_CONTENU = "AAAAAAAAAA";
    private static final String UPDATED_CONTENU = "BBBBBBBBBB";

    private static final String ENTITY_API_URL = "/api/dialogues";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private DialogueRepository dialogueRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restDialogueMockMvc;

    private Dialogue dialogue;

    private Dialogue insertedDialogue;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Dialogue createEntity(EntityManager em) {
        Dialogue dialogue = new Dialogue().dialogueId(DEFAULT_DIALOGUE_ID).contenu(DEFAULT_CONTENU);
        return dialogue;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Dialogue createUpdatedEntity(EntityManager em) {
        Dialogue dialogue = new Dialogue().dialogueId(UPDATED_DIALOGUE_ID).contenu(UPDATED_CONTENU);
        return dialogue;
    }

    @BeforeEach
    public void initTest() {
        dialogue = createEntity(em);
    }

    @AfterEach
    public void cleanup() {
        if (insertedDialogue != null) {
            dialogueRepository.delete(insertedDialogue);
            insertedDialogue = null;
        }
    }

    @Test
    @Transactional
    void createDialogue() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the Dialogue
        var returnedDialogue = om.readValue(
            restDialogueMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(dialogue)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            Dialogue.class
        );

        // Validate the Dialogue in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        assertDialogueUpdatableFieldsEquals(returnedDialogue, getPersistedDialogue(returnedDialogue));

        insertedDialogue = returnedDialogue;
    }

    @Test
    @Transactional
    void createDialogueWithExistingId() throws Exception {
        // Create the Dialogue with an existing ID
        dialogue.setId(1L);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restDialogueMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(dialogue)))
            .andExpect(status().isBadRequest());

        // Validate the Dialogue in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void getAllDialogues() throws Exception {
        // Initialize the database
        insertedDialogue = dialogueRepository.saveAndFlush(dialogue);

        // Get all the dialogueList
        restDialogueMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(dialogue.getId().intValue())))
            .andExpect(jsonPath("$.[*].dialogueId").value(hasItem(DEFAULT_DIALOGUE_ID)))
            .andExpect(jsonPath("$.[*].contenu").value(hasItem(DEFAULT_CONTENU)));
    }

    @Test
    @Transactional
    void getDialogue() throws Exception {
        // Initialize the database
        insertedDialogue = dialogueRepository.saveAndFlush(dialogue);

        // Get the dialogue
        restDialogueMockMvc
            .perform(get(ENTITY_API_URL_ID, dialogue.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(dialogue.getId().intValue()))
            .andExpect(jsonPath("$.dialogueId").value(DEFAULT_DIALOGUE_ID))
            .andExpect(jsonPath("$.contenu").value(DEFAULT_CONTENU));
    }

    @Test
    @Transactional
    void getNonExistingDialogue() throws Exception {
        // Get the dialogue
        restDialogueMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingDialogue() throws Exception {
        // Initialize the database
        insertedDialogue = dialogueRepository.saveAndFlush(dialogue);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the dialogue
        Dialogue updatedDialogue = dialogueRepository.findById(dialogue.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedDialogue are not directly saved in db
        em.detach(updatedDialogue);
        updatedDialogue.dialogueId(UPDATED_DIALOGUE_ID).contenu(UPDATED_CONTENU);

        restDialogueMockMvc
            .perform(
                put(ENTITY_API_URL_ID, updatedDialogue.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(updatedDialogue))
            )
            .andExpect(status().isOk());

        // Validate the Dialogue in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedDialogueToMatchAllProperties(updatedDialogue);
    }

    @Test
    @Transactional
    void putNonExistingDialogue() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        dialogue.setId(longCount.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restDialogueMockMvc
            .perform(
                put(ENTITY_API_URL_ID, dialogue.getId()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(dialogue))
            )
            .andExpect(status().isBadRequest());

        // Validate the Dialogue in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchDialogue() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        dialogue.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restDialogueMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(dialogue))
            )
            .andExpect(status().isBadRequest());

        // Validate the Dialogue in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamDialogue() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        dialogue.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restDialogueMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(dialogue)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Dialogue in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateDialogueWithPatch() throws Exception {
        // Initialize the database
        insertedDialogue = dialogueRepository.saveAndFlush(dialogue);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the dialogue using partial update
        Dialogue partialUpdatedDialogue = new Dialogue();
        partialUpdatedDialogue.setId(dialogue.getId());

        restDialogueMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedDialogue.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedDialogue))
            )
            .andExpect(status().isOk());

        // Validate the Dialogue in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertDialogueUpdatableFieldsEquals(createUpdateProxyForBean(partialUpdatedDialogue, dialogue), getPersistedDialogue(dialogue));
    }

    @Test
    @Transactional
    void fullUpdateDialogueWithPatch() throws Exception {
        // Initialize the database
        insertedDialogue = dialogueRepository.saveAndFlush(dialogue);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the dialogue using partial update
        Dialogue partialUpdatedDialogue = new Dialogue();
        partialUpdatedDialogue.setId(dialogue.getId());

        partialUpdatedDialogue.dialogueId(UPDATED_DIALOGUE_ID).contenu(UPDATED_CONTENU);

        restDialogueMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedDialogue.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedDialogue))
            )
            .andExpect(status().isOk());

        // Validate the Dialogue in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertDialogueUpdatableFieldsEquals(partialUpdatedDialogue, getPersistedDialogue(partialUpdatedDialogue));
    }

    @Test
    @Transactional
    void patchNonExistingDialogue() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        dialogue.setId(longCount.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restDialogueMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, dialogue.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(dialogue))
            )
            .andExpect(status().isBadRequest());

        // Validate the Dialogue in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchDialogue() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        dialogue.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restDialogueMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(dialogue))
            )
            .andExpect(status().isBadRequest());

        // Validate the Dialogue in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamDialogue() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        dialogue.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restDialogueMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(dialogue)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Dialogue in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteDialogue() throws Exception {
        // Initialize the database
        insertedDialogue = dialogueRepository.saveAndFlush(dialogue);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the dialogue
        restDialogueMockMvc
            .perform(delete(ENTITY_API_URL_ID, dialogue.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return dialogueRepository.count();
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

    protected Dialogue getPersistedDialogue(Dialogue dialogue) {
        return dialogueRepository.findById(dialogue.getId()).orElseThrow();
    }

    protected void assertPersistedDialogueToMatchAllProperties(Dialogue expectedDialogue) {
        assertDialogueAllPropertiesEquals(expectedDialogue, getPersistedDialogue(expectedDialogue));
    }

    protected void assertPersistedDialogueToMatchUpdatableProperties(Dialogue expectedDialogue) {
        assertDialogueAllUpdatablePropertiesEquals(expectedDialogue, getPersistedDialogue(expectedDialogue));
    }
}
