package com.example.myproject.web.rest;

import static com.example.myproject.domain.SendSmsAsserts.*;
import static com.example.myproject.web.rest.TestUtil.createUpdateProxyForBean;
import static com.example.myproject.web.rest.TestUtil.sameInstant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.myproject.IntegrationTest;
import com.example.myproject.domain.SendSms;
import com.example.myproject.repository.SendSmsRepository;
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
 * Integration tests for the {@link SendSmsResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class SendSmsResourceIT {

    private static final String DEFAULT_SENDER = "AAAAAAAAAA";
    private static final String UPDATED_SENDER = "BBBBBBBBBB";

    private static final String DEFAULT_RECEIVER = "AAAAAAAAAA";
    private static final String UPDATED_RECEIVER = "BBBBBBBBBB";

    private static final String DEFAULT_MSGDATA = "AAAAAAAAAA";
    private static final String UPDATED_MSGDATA = "BBBBBBBBBB";

    private static final ZonedDateTime DEFAULT_SENDATE_ENVOI = ZonedDateTime.ofInstant(Instant.ofEpochMilli(0L), ZoneOffset.UTC);
    private static final ZonedDateTime UPDATED_SENDATE_ENVOI = ZonedDateTime.now(ZoneId.systemDefault()).withNano(0);

    private static final String DEFAULT_DIALOGUE = "AAAAAAAAAA";
    private static final String UPDATED_DIALOGUE = "BBBBBBBBBB";

    private static final Boolean DEFAULT_IS_SENT = false;
    private static final Boolean UPDATED_IS_SENT = true;

    private static final Boolean DEFAULT_ISBULK = false;
    private static final Boolean UPDATED_ISBULK = true;

    private static final String DEFAULT_TITRE = "AAAAAAAAAA";
    private static final String UPDATED_TITRE = "BBBBBBBBBB";

    private static final String ENTITY_API_URL = "/api/send-sms";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private SendSmsRepository sendSmsRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restSendSmsMockMvc;

    private SendSms sendSms;

    private SendSms insertedSendSms;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static SendSms createEntity(EntityManager em) {
        SendSms sendSms = new SendSms()
            .sender(DEFAULT_SENDER)
            .receiver(DEFAULT_RECEIVER)
            .msgdata(DEFAULT_MSGDATA)
            .sendateEnvoi(DEFAULT_SENDATE_ENVOI)
            .dialogue(DEFAULT_DIALOGUE)
            .isSent(DEFAULT_IS_SENT)
            .isbulk(DEFAULT_ISBULK)
            .Titre(DEFAULT_TITRE);
        return sendSms;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static SendSms createUpdatedEntity(EntityManager em) {
        SendSms sendSms = new SendSms()
            .sender(UPDATED_SENDER)
            .receiver(UPDATED_RECEIVER)
            .msgdata(UPDATED_MSGDATA)
            .sendateEnvoi(UPDATED_SENDATE_ENVOI)
            .dialogue(UPDATED_DIALOGUE)
            .isSent(UPDATED_IS_SENT)
            .isbulk(UPDATED_ISBULK)
            .Titre(UPDATED_TITRE);
        return sendSms;
    }

    @BeforeEach
    public void initTest() {
        sendSms = createEntity(em);
    }

    @AfterEach
    public void cleanup() {
        if (insertedSendSms != null) {
            sendSmsRepository.delete(insertedSendSms);
            insertedSendSms = null;
        }
    }

    @Test
    @Transactional
    void createSendSms() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the SendSms
        var returnedSendSms = om.readValue(
            restSendSmsMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(sendSms)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            SendSms.class
        );

        // Validate the SendSms in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        assertSendSmsUpdatableFieldsEquals(returnedSendSms, getPersistedSendSms(returnedSendSms));

        insertedSendSms = returnedSendSms;
    }

    @Test
    @Transactional
    void createSendSmsWithExistingId() throws Exception {
        // Create the SendSms with an existing ID
        sendSms.setId(1L);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restSendSmsMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(sendSms)))
            .andExpect(status().isBadRequest());

        // Validate the SendSms in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void getAllSendSms() throws Exception {
        // Initialize the database
        insertedSendSms = sendSmsRepository.saveAndFlush(sendSms);

        // Get all the sendSmsList
        restSendSmsMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(sendSms.getId().intValue())))
            .andExpect(jsonPath("$.[*].sender").value(hasItem(DEFAULT_SENDER)))
            .andExpect(jsonPath("$.[*].receiver").value(hasItem(DEFAULT_RECEIVER)))
            .andExpect(jsonPath("$.[*].msgdata").value(hasItem(DEFAULT_MSGDATA)))
            .andExpect(jsonPath("$.[*].sendateEnvoi").value(hasItem(sameInstant(DEFAULT_SENDATE_ENVOI))))
            .andExpect(jsonPath("$.[*].dialogue").value(hasItem(DEFAULT_DIALOGUE)))
            .andExpect(jsonPath("$.[*].isSent").value(hasItem(DEFAULT_IS_SENT.booleanValue())))
            .andExpect(jsonPath("$.[*].isbulk").value(hasItem(DEFAULT_ISBULK.booleanValue())))
            .andExpect(jsonPath("$.[*].Titre").value(hasItem(DEFAULT_TITRE)));
    }

    @Test
    @Transactional
    void getSendSms() throws Exception {
        // Initialize the database
        insertedSendSms = sendSmsRepository.saveAndFlush(sendSms);

        // Get the sendSms
        restSendSmsMockMvc
            .perform(get(ENTITY_API_URL_ID, sendSms.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(sendSms.getId().intValue()))
            .andExpect(jsonPath("$.sender").value(DEFAULT_SENDER))
            .andExpect(jsonPath("$.receiver").value(DEFAULT_RECEIVER))
            .andExpect(jsonPath("$.msgdata").value(DEFAULT_MSGDATA))
            .andExpect(jsonPath("$.sendateEnvoi").value(sameInstant(DEFAULT_SENDATE_ENVOI)))
            .andExpect(jsonPath("$.dialogue").value(DEFAULT_DIALOGUE))
            .andExpect(jsonPath("$.isSent").value(DEFAULT_IS_SENT.booleanValue()))
            .andExpect(jsonPath("$.isbulk").value(DEFAULT_ISBULK.booleanValue()))
            .andExpect(jsonPath("$.Titre").value(DEFAULT_TITRE));
    }

    @Test
    @Transactional
    void getNonExistingSendSms() throws Exception {
        // Get the sendSms
        restSendSmsMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingSendSms() throws Exception {
        // Initialize the database
        insertedSendSms = sendSmsRepository.saveAndFlush(sendSms);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the sendSms
        SendSms updatedSendSms = sendSmsRepository.findById(sendSms.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedSendSms are not directly saved in db
        em.detach(updatedSendSms);
        updatedSendSms
            .sender(UPDATED_SENDER)
            .receiver(UPDATED_RECEIVER)
            .msgdata(UPDATED_MSGDATA)
            .sendateEnvoi(UPDATED_SENDATE_ENVOI)
            .dialogue(UPDATED_DIALOGUE)
            .isSent(UPDATED_IS_SENT)
            .isbulk(UPDATED_ISBULK)
            .Titre(UPDATED_TITRE);

        restSendSmsMockMvc
            .perform(
                put(ENTITY_API_URL_ID, updatedSendSms.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(updatedSendSms))
            )
            .andExpect(status().isOk());

        // Validate the SendSms in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedSendSmsToMatchAllProperties(updatedSendSms);
    }

    @Test
    @Transactional
    void putNonExistingSendSms() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        sendSms.setId(longCount.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restSendSmsMockMvc
            .perform(put(ENTITY_API_URL_ID, sendSms.getId()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(sendSms)))
            .andExpect(status().isBadRequest());

        // Validate the SendSms in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchSendSms() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        sendSms.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restSendSmsMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(sendSms))
            )
            .andExpect(status().isBadRequest());

        // Validate the SendSms in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamSendSms() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        sendSms.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restSendSmsMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(sendSms)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the SendSms in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateSendSmsWithPatch() throws Exception {
        // Initialize the database
        insertedSendSms = sendSmsRepository.saveAndFlush(sendSms);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the sendSms using partial update
        SendSms partialUpdatedSendSms = new SendSms();
        partialUpdatedSendSms.setId(sendSms.getId());

        partialUpdatedSendSms
            .sender(UPDATED_SENDER)
            .receiver(UPDATED_RECEIVER)
            .sendateEnvoi(UPDATED_SENDATE_ENVOI)
            .isSent(UPDATED_IS_SENT)
            .isbulk(UPDATED_ISBULK);

        restSendSmsMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedSendSms.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedSendSms))
            )
            .andExpect(status().isOk());

        // Validate the SendSms in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertSendSmsUpdatableFieldsEquals(createUpdateProxyForBean(partialUpdatedSendSms, sendSms), getPersistedSendSms(sendSms));
    }

    @Test
    @Transactional
    void fullUpdateSendSmsWithPatch() throws Exception {
        // Initialize the database
        insertedSendSms = sendSmsRepository.saveAndFlush(sendSms);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the sendSms using partial update
        SendSms partialUpdatedSendSms = new SendSms();
        partialUpdatedSendSms.setId(sendSms.getId());

        partialUpdatedSendSms
            .sender(UPDATED_SENDER)
            .receiver(UPDATED_RECEIVER)
            .msgdata(UPDATED_MSGDATA)
            .sendateEnvoi(UPDATED_SENDATE_ENVOI)
            .dialogue(UPDATED_DIALOGUE)
            .isSent(UPDATED_IS_SENT)
            .isbulk(UPDATED_ISBULK)
            .Titre(UPDATED_TITRE);

        restSendSmsMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedSendSms.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedSendSms))
            )
            .andExpect(status().isOk());

        // Validate the SendSms in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertSendSmsUpdatableFieldsEquals(partialUpdatedSendSms, getPersistedSendSms(partialUpdatedSendSms));
    }

    @Test
    @Transactional
    void patchNonExistingSendSms() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        sendSms.setId(longCount.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restSendSmsMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, sendSms.getId()).contentType("application/merge-patch+json").content(om.writeValueAsBytes(sendSms))
            )
            .andExpect(status().isBadRequest());

        // Validate the SendSms in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchSendSms() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        sendSms.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restSendSmsMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(sendSms))
            )
            .andExpect(status().isBadRequest());

        // Validate the SendSms in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamSendSms() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        sendSms.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restSendSmsMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(sendSms)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the SendSms in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteSendSms() throws Exception {
        // Initialize the database
        insertedSendSms = sendSmsRepository.saveAndFlush(sendSms);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the sendSms
        restSendSmsMockMvc
            .perform(delete(ENTITY_API_URL_ID, sendSms.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return sendSmsRepository.count();
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

    protected SendSms getPersistedSendSms(SendSms sendSms) {
        return sendSmsRepository.findById(sendSms.getId()).orElseThrow();
    }

    protected void assertPersistedSendSmsToMatchAllProperties(SendSms expectedSendSms) {
        assertSendSmsAllPropertiesEquals(expectedSendSms, getPersistedSendSms(expectedSendSms));
    }

    protected void assertPersistedSendSmsToMatchUpdatableProperties(SendSms expectedSendSms) {
        assertSendSmsAllUpdatablePropertiesEquals(expectedSendSms, getPersistedSendSms(expectedSendSms));
    }
}
