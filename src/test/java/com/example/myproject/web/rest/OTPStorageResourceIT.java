package com.example.myproject.web.rest;

import static com.example.myproject.domain.OTPStorageAsserts.*;
import static com.example.myproject.web.rest.TestUtil.createUpdateProxyForBean;
import static com.example.myproject.web.rest.TestUtil.sameInstant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.myproject.IntegrationTest;
import com.example.myproject.domain.OTPStorage;
import com.example.myproject.repository.OTPStorageRepository;
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
 * Integration tests for the {@link OTPStorageResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class OTPStorageResourceIT {

    private static final String DEFAULT_OTS_OTP = "AAAAAAAAAA";
    private static final String UPDATED_OTS_OTP = "BBBBBBBBBB";

    private static final String DEFAULT_PHONE_NUMBER = "AAAAAAAAAA";
    private static final String UPDATED_PHONE_NUMBER = "BBBBBBBBBB";

    private static final ZonedDateTime DEFAULT_OTSDATEEXPIR = ZonedDateTime.ofInstant(Instant.ofEpochMilli(0L), ZoneOffset.UTC);
    private static final ZonedDateTime UPDATED_OTSDATEEXPIR = ZonedDateTime.now(ZoneId.systemDefault()).withNano(0);

    private static final Boolean DEFAULT_IS_OTP_USED = false;
    private static final Boolean UPDATED_IS_OTP_USED = true;

    private static final Boolean DEFAULT_IS_EXPIRED = false;
    private static final Boolean UPDATED_IS_EXPIRED = true;

    private static final String ENTITY_API_URL = "/api/otp-storages";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private OTPStorageRepository oTPStorageRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restOTPStorageMockMvc;

    private OTPStorage oTPStorage;

    private OTPStorage insertedOTPStorage;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static OTPStorage createEntity(EntityManager em) {
        OTPStorage oTPStorage = new OTPStorage()
            .otsOTP(DEFAULT_OTS_OTP)
            .phoneNumber(DEFAULT_PHONE_NUMBER)
            .otsdateexpir(DEFAULT_OTSDATEEXPIR)
            .isOtpUsed(DEFAULT_IS_OTP_USED)
            .isExpired(DEFAULT_IS_EXPIRED);
        return oTPStorage;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static OTPStorage createUpdatedEntity(EntityManager em) {
        OTPStorage oTPStorage = new OTPStorage()
            .otsOTP(UPDATED_OTS_OTP)
            .phoneNumber(UPDATED_PHONE_NUMBER)
            .otsdateexpir(UPDATED_OTSDATEEXPIR)
            .isOtpUsed(UPDATED_IS_OTP_USED)
            .isExpired(UPDATED_IS_EXPIRED);
        return oTPStorage;
    }

    @BeforeEach
    public void initTest() {
        oTPStorage = createEntity(em);
    }

    @AfterEach
    public void cleanup() {
        if (insertedOTPStorage != null) {
            oTPStorageRepository.delete(insertedOTPStorage);
            insertedOTPStorage = null;
        }
    }

    @Test
    @Transactional
    void createOTPStorage() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the OTPStorage
        var returnedOTPStorage = om.readValue(
            restOTPStorageMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(oTPStorage)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            OTPStorage.class
        );

        // Validate the OTPStorage in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        assertOTPStorageUpdatableFieldsEquals(returnedOTPStorage, getPersistedOTPStorage(returnedOTPStorage));

        insertedOTPStorage = returnedOTPStorage;
    }

    @Test
    @Transactional
    void createOTPStorageWithExistingId() throws Exception {
        // Create the OTPStorage with an existing ID
        oTPStorage.setId(1L);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restOTPStorageMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(oTPStorage)))
            .andExpect(status().isBadRequest());

        // Validate the OTPStorage in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void getAllOTPStorages() throws Exception {
        // Initialize the database
        insertedOTPStorage = oTPStorageRepository.saveAndFlush(oTPStorage);

        // Get all the oTPStorageList
        restOTPStorageMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(oTPStorage.getId().intValue())))
            .andExpect(jsonPath("$.[*].otsOTP").value(hasItem(DEFAULT_OTS_OTP)))
            .andExpect(jsonPath("$.[*].phoneNumber").value(hasItem(DEFAULT_PHONE_NUMBER)))
            .andExpect(jsonPath("$.[*].otsdateexpir").value(hasItem(sameInstant(DEFAULT_OTSDATEEXPIR))))
            .andExpect(jsonPath("$.[*].isOtpUsed").value(hasItem(DEFAULT_IS_OTP_USED.booleanValue())))
            .andExpect(jsonPath("$.[*].isExpired").value(hasItem(DEFAULT_IS_EXPIRED.booleanValue())));
    }

    @Test
    @Transactional
    void getOTPStorage() throws Exception {
        // Initialize the database
        insertedOTPStorage = oTPStorageRepository.saveAndFlush(oTPStorage);

        // Get the oTPStorage
        restOTPStorageMockMvc
            .perform(get(ENTITY_API_URL_ID, oTPStorage.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(oTPStorage.getId().intValue()))
            .andExpect(jsonPath("$.otsOTP").value(DEFAULT_OTS_OTP))
            .andExpect(jsonPath("$.phoneNumber").value(DEFAULT_PHONE_NUMBER))
            .andExpect(jsonPath("$.otsdateexpir").value(sameInstant(DEFAULT_OTSDATEEXPIR)))
            .andExpect(jsonPath("$.isOtpUsed").value(DEFAULT_IS_OTP_USED.booleanValue()))
            .andExpect(jsonPath("$.isExpired").value(DEFAULT_IS_EXPIRED.booleanValue()));
    }

    @Test
    @Transactional
    void getNonExistingOTPStorage() throws Exception {
        // Get the oTPStorage
        restOTPStorageMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingOTPStorage() throws Exception {
        // Initialize the database
        insertedOTPStorage = oTPStorageRepository.saveAndFlush(oTPStorage);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the oTPStorage
        OTPStorage updatedOTPStorage = oTPStorageRepository.findById(oTPStorage.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedOTPStorage are not directly saved in db
        em.detach(updatedOTPStorage);
        updatedOTPStorage
            .otsOTP(UPDATED_OTS_OTP)
            .phoneNumber(UPDATED_PHONE_NUMBER)
            .otsdateexpir(UPDATED_OTSDATEEXPIR)
            .isOtpUsed(UPDATED_IS_OTP_USED)
            .isExpired(UPDATED_IS_EXPIRED);

        restOTPStorageMockMvc
            .perform(
                put(ENTITY_API_URL_ID, updatedOTPStorage.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(updatedOTPStorage))
            )
            .andExpect(status().isOk());

        // Validate the OTPStorage in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedOTPStorageToMatchAllProperties(updatedOTPStorage);
    }

    @Test
    @Transactional
    void putNonExistingOTPStorage() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        oTPStorage.setId(longCount.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restOTPStorageMockMvc
            .perform(
                put(ENTITY_API_URL_ID, oTPStorage.getId()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(oTPStorage))
            )
            .andExpect(status().isBadRequest());

        // Validate the OTPStorage in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchOTPStorage() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        oTPStorage.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restOTPStorageMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(oTPStorage))
            )
            .andExpect(status().isBadRequest());

        // Validate the OTPStorage in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamOTPStorage() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        oTPStorage.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restOTPStorageMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(oTPStorage)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the OTPStorage in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateOTPStorageWithPatch() throws Exception {
        // Initialize the database
        insertedOTPStorage = oTPStorageRepository.saveAndFlush(oTPStorage);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the oTPStorage using partial update
        OTPStorage partialUpdatedOTPStorage = new OTPStorage();
        partialUpdatedOTPStorage.setId(oTPStorage.getId());

        partialUpdatedOTPStorage.phoneNumber(UPDATED_PHONE_NUMBER).otsdateexpir(UPDATED_OTSDATEEXPIR);

        restOTPStorageMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedOTPStorage.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedOTPStorage))
            )
            .andExpect(status().isOk());

        // Validate the OTPStorage in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertOTPStorageUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedOTPStorage, oTPStorage),
            getPersistedOTPStorage(oTPStorage)
        );
    }

    @Test
    @Transactional
    void fullUpdateOTPStorageWithPatch() throws Exception {
        // Initialize the database
        insertedOTPStorage = oTPStorageRepository.saveAndFlush(oTPStorage);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the oTPStorage using partial update
        OTPStorage partialUpdatedOTPStorage = new OTPStorage();
        partialUpdatedOTPStorage.setId(oTPStorage.getId());

        partialUpdatedOTPStorage
            .otsOTP(UPDATED_OTS_OTP)
            .phoneNumber(UPDATED_PHONE_NUMBER)
            .otsdateexpir(UPDATED_OTSDATEEXPIR)
            .isOtpUsed(UPDATED_IS_OTP_USED)
            .isExpired(UPDATED_IS_EXPIRED);

        restOTPStorageMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedOTPStorage.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedOTPStorage))
            )
            .andExpect(status().isOk());

        // Validate the OTPStorage in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertOTPStorageUpdatableFieldsEquals(partialUpdatedOTPStorage, getPersistedOTPStorage(partialUpdatedOTPStorage));
    }

    @Test
    @Transactional
    void patchNonExistingOTPStorage() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        oTPStorage.setId(longCount.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restOTPStorageMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, oTPStorage.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(oTPStorage))
            )
            .andExpect(status().isBadRequest());

        // Validate the OTPStorage in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchOTPStorage() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        oTPStorage.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restOTPStorageMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(oTPStorage))
            )
            .andExpect(status().isBadRequest());

        // Validate the OTPStorage in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamOTPStorage() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        oTPStorage.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restOTPStorageMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(oTPStorage)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the OTPStorage in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteOTPStorage() throws Exception {
        // Initialize the database
        insertedOTPStorage = oTPStorageRepository.saveAndFlush(oTPStorage);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the oTPStorage
        restOTPStorageMockMvc
            .perform(delete(ENTITY_API_URL_ID, oTPStorage.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return oTPStorageRepository.count();
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

    protected OTPStorage getPersistedOTPStorage(OTPStorage oTPStorage) {
        return oTPStorageRepository.findById(oTPStorage.getId()).orElseThrow();
    }

    protected void assertPersistedOTPStorageToMatchAllProperties(OTPStorage expectedOTPStorage) {
        assertOTPStorageAllPropertiesEquals(expectedOTPStorage, getPersistedOTPStorage(expectedOTPStorage));
    }

    protected void assertPersistedOTPStorageToMatchUpdatableProperties(OTPStorage expectedOTPStorage) {
        assertOTPStorageAllUpdatablePropertiesEquals(expectedOTPStorage, getPersistedOTPStorage(expectedOTPStorage));
    }
}
