package com.example.myproject.web.rest;

import static com.example.myproject.domain.UserTokenApiAsserts.*;
import static com.example.myproject.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.myproject.IntegrationTest;
import com.example.myproject.domain.UserTokenApi;
import com.example.myproject.repository.UserTokenApiRepository;
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
 * Integration tests for the {@link UserTokenApiResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class UserTokenApiResourceIT {

    private static final String ENTITY_API_URL = "/api/user-token-apis";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private UserTokenApiRepository userTokenApiRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restUserTokenApiMockMvc;

    private UserTokenApi userTokenApi;

    private UserTokenApi insertedUserTokenApi;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static UserTokenApi createEntity(EntityManager em) {
        UserTokenApi userTokenApi = new UserTokenApi();
        return userTokenApi;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static UserTokenApi createUpdatedEntity(EntityManager em) {
        UserTokenApi userTokenApi = new UserTokenApi();
        return userTokenApi;
    }

    @BeforeEach
    public void initTest() {
        userTokenApi = createEntity(em);
    }

    @AfterEach
    public void cleanup() {
        if (insertedUserTokenApi != null) {
            userTokenApiRepository.delete(insertedUserTokenApi);
            insertedUserTokenApi = null;
        }
    }

    @Test
    @Transactional
    void createUserTokenApi() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the UserTokenApi
        var returnedUserTokenApi = om.readValue(
            restUserTokenApiMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(userTokenApi)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            UserTokenApi.class
        );

        // Validate the UserTokenApi in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        assertUserTokenApiUpdatableFieldsEquals(returnedUserTokenApi, getPersistedUserTokenApi(returnedUserTokenApi));

        insertedUserTokenApi = returnedUserTokenApi;
    }

    @Test
    @Transactional
    void createUserTokenApiWithExistingId() throws Exception {
        // Create the UserTokenApi with an existing ID
        userTokenApi.setId(1L);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restUserTokenApiMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(userTokenApi)))
            .andExpect(status().isBadRequest());

        // Validate the UserTokenApi in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void getAllUserTokenApis() throws Exception {
        // Initialize the database
        insertedUserTokenApi = userTokenApiRepository.saveAndFlush(userTokenApi);

        // Get all the userTokenApiList
        restUserTokenApiMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(userTokenApi.getId().intValue())));
    }

    @Test
    @Transactional
    void getUserTokenApi() throws Exception {
        // Initialize the database
        insertedUserTokenApi = userTokenApiRepository.saveAndFlush(userTokenApi);

        // Get the userTokenApi
        restUserTokenApiMockMvc
            .perform(get(ENTITY_API_URL_ID, userTokenApi.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(userTokenApi.getId().intValue()));
    }

    @Test
    @Transactional
    void getNonExistingUserTokenApi() throws Exception {
        // Get the userTokenApi
        restUserTokenApiMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingUserTokenApi() throws Exception {
        // Initialize the database
        insertedUserTokenApi = userTokenApiRepository.saveAndFlush(userTokenApi);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the userTokenApi
        UserTokenApi updatedUserTokenApi = userTokenApiRepository.findById(userTokenApi.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedUserTokenApi are not directly saved in db
        em.detach(updatedUserTokenApi);

        restUserTokenApiMockMvc
            .perform(
                put(ENTITY_API_URL_ID, updatedUserTokenApi.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(updatedUserTokenApi))
            )
            .andExpect(status().isOk());

        // Validate the UserTokenApi in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedUserTokenApiToMatchAllProperties(updatedUserTokenApi);
    }

    @Test
    @Transactional
    void putNonExistingUserTokenApi() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        userTokenApi.setId(longCount.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restUserTokenApiMockMvc
            .perform(
                put(ENTITY_API_URL_ID, userTokenApi.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(userTokenApi))
            )
            .andExpect(status().isBadRequest());

        // Validate the UserTokenApi in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchUserTokenApi() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        userTokenApi.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restUserTokenApiMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(userTokenApi))
            )
            .andExpect(status().isBadRequest());

        // Validate the UserTokenApi in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamUserTokenApi() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        userTokenApi.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restUserTokenApiMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(userTokenApi)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the UserTokenApi in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateUserTokenApiWithPatch() throws Exception {
        // Initialize the database
        insertedUserTokenApi = userTokenApiRepository.saveAndFlush(userTokenApi);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the userTokenApi using partial update
        UserTokenApi partialUpdatedUserTokenApi = new UserTokenApi();
        partialUpdatedUserTokenApi.setId(userTokenApi.getId());

        restUserTokenApiMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedUserTokenApi.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedUserTokenApi))
            )
            .andExpect(status().isOk());

        // Validate the UserTokenApi in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertUserTokenApiUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedUserTokenApi, userTokenApi),
            getPersistedUserTokenApi(userTokenApi)
        );
    }

    @Test
    @Transactional
    void fullUpdateUserTokenApiWithPatch() throws Exception {
        // Initialize the database
        insertedUserTokenApi = userTokenApiRepository.saveAndFlush(userTokenApi);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the userTokenApi using partial update
        UserTokenApi partialUpdatedUserTokenApi = new UserTokenApi();
        partialUpdatedUserTokenApi.setId(userTokenApi.getId());

        restUserTokenApiMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedUserTokenApi.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedUserTokenApi))
            )
            .andExpect(status().isOk());

        // Validate the UserTokenApi in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertUserTokenApiUpdatableFieldsEquals(partialUpdatedUserTokenApi, getPersistedUserTokenApi(partialUpdatedUserTokenApi));
    }

    @Test
    @Transactional
    void patchNonExistingUserTokenApi() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        userTokenApi.setId(longCount.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restUserTokenApiMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, userTokenApi.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(userTokenApi))
            )
            .andExpect(status().isBadRequest());

        // Validate the UserTokenApi in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchUserTokenApi() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        userTokenApi.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restUserTokenApiMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(userTokenApi))
            )
            .andExpect(status().isBadRequest());

        // Validate the UserTokenApi in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamUserTokenApi() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        userTokenApi.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restUserTokenApiMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(userTokenApi)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the UserTokenApi in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteUserTokenApi() throws Exception {
        // Initialize the database
        insertedUserTokenApi = userTokenApiRepository.saveAndFlush(userTokenApi);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the userTokenApi
        restUserTokenApiMockMvc
            .perform(delete(ENTITY_API_URL_ID, userTokenApi.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return userTokenApiRepository.count();
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

    protected UserTokenApi getPersistedUserTokenApi(UserTokenApi userTokenApi) {
        return userTokenApiRepository.findById(userTokenApi.getId()).orElseThrow();
    }

    protected void assertPersistedUserTokenApiToMatchAllProperties(UserTokenApi expectedUserTokenApi) {
        assertUserTokenApiAllPropertiesEquals(expectedUserTokenApi, getPersistedUserTokenApi(expectedUserTokenApi));
    }

    protected void assertPersistedUserTokenApiToMatchUpdatableProperties(UserTokenApi expectedUserTokenApi) {
        assertUserTokenApiAllUpdatablePropertiesEquals(expectedUserTokenApi, getPersistedUserTokenApi(expectedUserTokenApi));
    }
}
