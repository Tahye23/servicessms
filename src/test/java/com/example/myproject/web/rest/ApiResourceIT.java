package com.example.myproject.web.rest;

import static com.example.myproject.domain.ApiAsserts.*;
import static com.example.myproject.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.myproject.IntegrationTest;
import com.example.myproject.domain.Api;
import com.example.myproject.repository.ApiRepository;
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
 * Integration tests for the {@link ApiResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class ApiResourceIT {

    private static final String DEFAULT_API_NOM = "AAAAAAAAAA";
    private static final String UPDATED_API_NOM = "BBBBBBBBBB";

    private static final String DEFAULT_API_URL = "AAAAAAAAAA";
    private static final String UPDATED_API_URL = "BBBBBBBBBB";

    private static final Integer DEFAULT_API_VERSION = 1;
    private static final Integer UPDATED_API_VERSION = 2;

    private static final String ENTITY_API_URL = "/api/apis";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private ApiRepository apiRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restApiMockMvc;

    private Api api;

    private Api insertedApi;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Api createEntity(EntityManager em) {
        Api api = new Api().apiNom(DEFAULT_API_NOM).apiUrl(DEFAULT_API_URL).apiVersion(DEFAULT_API_VERSION);
        return api;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Api createUpdatedEntity(EntityManager em) {
        Api api = new Api().apiNom(UPDATED_API_NOM).apiUrl(UPDATED_API_URL).apiVersion(UPDATED_API_VERSION);
        return api;
    }

    @BeforeEach
    public void initTest() {
        api = createEntity(em);
    }

    @AfterEach
    public void cleanup() {
        if (insertedApi != null) {
            apiRepository.delete(insertedApi);
            insertedApi = null;
        }
    }

    @Test
    @Transactional
    void createApi() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the Api
        var returnedApi = om.readValue(
            restApiMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(api)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            Api.class
        );

        // Validate the Api in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        assertApiUpdatableFieldsEquals(returnedApi, getPersistedApi(returnedApi));

        insertedApi = returnedApi;
    }

    @Test
    @Transactional
    void createApiWithExistingId() throws Exception {
        // Create the Api with an existing ID
        api.setId(1L);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restApiMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(api)))
            .andExpect(status().isBadRequest());

        // Validate the Api in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void getAllApis() throws Exception {
        // Initialize the database
        insertedApi = apiRepository.saveAndFlush(api);

        // Get all the apiList
        restApiMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(api.getId().intValue())))
            .andExpect(jsonPath("$.[*].apiNom").value(hasItem(DEFAULT_API_NOM)))
            .andExpect(jsonPath("$.[*].apiUrl").value(hasItem(DEFAULT_API_URL)))
            .andExpect(jsonPath("$.[*].apiVersion").value(hasItem(DEFAULT_API_VERSION)));
    }

    @Test
    @Transactional
    void getApi() throws Exception {
        // Initialize the database
        insertedApi = apiRepository.saveAndFlush(api);

        // Get the api
        restApiMockMvc
            .perform(get(ENTITY_API_URL_ID, api.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(api.getId().intValue()))
            .andExpect(jsonPath("$.apiNom").value(DEFAULT_API_NOM))
            .andExpect(jsonPath("$.apiUrl").value(DEFAULT_API_URL))
            .andExpect(jsonPath("$.apiVersion").value(DEFAULT_API_VERSION));
    }

    @Test
    @Transactional
    void getNonExistingApi() throws Exception {
        // Get the api
        restApiMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingApi() throws Exception {
        // Initialize the database
        insertedApi = apiRepository.saveAndFlush(api);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the api
        Api updatedApi = apiRepository.findById(api.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedApi are not directly saved in db
        em.detach(updatedApi);
        updatedApi.apiNom(UPDATED_API_NOM).apiUrl(UPDATED_API_URL).apiVersion(UPDATED_API_VERSION);

        restApiMockMvc
            .perform(
                put(ENTITY_API_URL_ID, updatedApi.getId()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(updatedApi))
            )
            .andExpect(status().isOk());

        // Validate the Api in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedApiToMatchAllProperties(updatedApi);
    }

    @Test
    @Transactional
    void putNonExistingApi() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        api.setId(longCount.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restApiMockMvc
            .perform(put(ENTITY_API_URL_ID, api.getId()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(api)))
            .andExpect(status().isBadRequest());

        // Validate the Api in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchApi() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        api.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restApiMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(api))
            )
            .andExpect(status().isBadRequest());

        // Validate the Api in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamApi() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        api.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restApiMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(api)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Api in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateApiWithPatch() throws Exception {
        // Initialize the database
        insertedApi = apiRepository.saveAndFlush(api);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the api using partial update
        Api partialUpdatedApi = new Api();
        partialUpdatedApi.setId(api.getId());

        partialUpdatedApi.apiVersion(UPDATED_API_VERSION);

        restApiMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedApi.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedApi))
            )
            .andExpect(status().isOk());

        // Validate the Api in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertApiUpdatableFieldsEquals(createUpdateProxyForBean(partialUpdatedApi, api), getPersistedApi(api));
    }

    @Test
    @Transactional
    void fullUpdateApiWithPatch() throws Exception {
        // Initialize the database
        insertedApi = apiRepository.saveAndFlush(api);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the api using partial update
        Api partialUpdatedApi = new Api();
        partialUpdatedApi.setId(api.getId());

        partialUpdatedApi.apiNom(UPDATED_API_NOM).apiUrl(UPDATED_API_URL).apiVersion(UPDATED_API_VERSION);

        restApiMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedApi.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedApi))
            )
            .andExpect(status().isOk());

        // Validate the Api in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertApiUpdatableFieldsEquals(partialUpdatedApi, getPersistedApi(partialUpdatedApi));
    }

    @Test
    @Transactional
    void patchNonExistingApi() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        api.setId(longCount.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restApiMockMvc
            .perform(patch(ENTITY_API_URL_ID, api.getId()).contentType("application/merge-patch+json").content(om.writeValueAsBytes(api)))
            .andExpect(status().isBadRequest());

        // Validate the Api in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchApi() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        api.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restApiMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(api))
            )
            .andExpect(status().isBadRequest());

        // Validate the Api in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamApi() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        api.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restApiMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(api)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Api in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteApi() throws Exception {
        // Initialize the database
        insertedApi = apiRepository.saveAndFlush(api);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the api
        restApiMockMvc.perform(delete(ENTITY_API_URL_ID, api.getId()).accept(MediaType.APPLICATION_JSON)).andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return apiRepository.count();
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

    protected Api getPersistedApi(Api api) {
        return apiRepository.findById(api.getId()).orElseThrow();
    }

    protected void assertPersistedApiToMatchAllProperties(Api expectedApi) {
        assertApiAllPropertiesEquals(expectedApi, getPersistedApi(expectedApi));
    }

    protected void assertPersistedApiToMatchUpdatableProperties(Api expectedApi) {
        assertApiAllUpdatablePropertiesEquals(expectedApi, getPersistedApi(expectedApi));
    }
}
