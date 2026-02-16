package com.example.myproject.web.rest;

import static com.example.myproject.domain.ApplicationAsserts.*;
import static com.example.myproject.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.myproject.IntegrationTest;
import com.example.myproject.domain.Application;
import com.example.myproject.repository.ApplicationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
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
 * Integration tests for the {@link ApplicationResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class ApplicationResourceIT {

    private static final String DEFAULT_NAME = "AAAAAAAAAA";
    private static final String UPDATED_NAME = "BBBBBBBBBB";

    private static final String DEFAULT_DESCRIPTION = "AAAAAAAAAA";
    private static final String UPDATED_DESCRIPTION = "BBBBBBBBBB";

    private static final Integer DEFAULT_USER_ID = 1;
    private static final Integer UPDATED_USER_ID = 2;

    private static final String ENTITY_API_URL = "/api/applications";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicInteger intCount = new AtomicInteger(random.nextInt() + (2 * Short.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restApplicationMockMvc;

    private Application application;

    private Application insertedApplication;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Application createEntity(EntityManager em) {
        Application application = new Application().name(DEFAULT_NAME).description(DEFAULT_DESCRIPTION).userId(DEFAULT_USER_ID);
        return application;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Application createUpdatedEntity(EntityManager em) {
        Application application = new Application().name(UPDATED_NAME).description(UPDATED_DESCRIPTION).userId(UPDATED_USER_ID);
        return application;
    }

    @BeforeEach
    public void initTest() {
        application = createEntity(em);
    }

    @AfterEach
    public void cleanup() {
        if (insertedApplication != null) {
            applicationRepository.delete(insertedApplication);
            insertedApplication = null;
        }
    }

    @Test
    @Transactional
    void createApplication() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the Application
        var returnedApplication = om.readValue(
            restApplicationMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(application)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            Application.class
        );

        // Validate the Application in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        assertApplicationUpdatableFieldsEquals(returnedApplication, getPersistedApplication(returnedApplication));

        insertedApplication = returnedApplication;
    }

    @Test
    @Transactional
    void createApplicationWithExistingId() throws Exception {
        // Create the Application with an existing ID
        application.setId(1);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restApplicationMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(application)))
            .andExpect(status().isBadRequest());

        // Validate the Application in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void getAllApplications() throws Exception {
        // Initialize the database
        insertedApplication = applicationRepository.saveAndFlush(application);

        // Get all the applicationList
        restApplicationMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(application.getId().intValue())))
            .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME)))
            .andExpect(jsonPath("$.[*].description").value(hasItem(DEFAULT_DESCRIPTION)))
            .andExpect(jsonPath("$.[*].userId").value(hasItem(DEFAULT_USER_ID)));
    }

    @Test
    @Transactional
    void getApplication() throws Exception {
        // Initialize the database
        insertedApplication = applicationRepository.saveAndFlush(application);

        // Get the application
        restApplicationMockMvc
            .perform(get(ENTITY_API_URL_ID, application.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(application.getId().intValue()))
            .andExpect(jsonPath("$.name").value(DEFAULT_NAME))
            .andExpect(jsonPath("$.description").value(DEFAULT_DESCRIPTION))
            .andExpect(jsonPath("$.userId").value(DEFAULT_USER_ID));
    }

    @Test
    @Transactional
    void getNonExistingApplication() throws Exception {
        // Get the application
        restApplicationMockMvc.perform(get(ENTITY_API_URL_ID, Integer.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingApplication() throws Exception {
        // Initialize the database
        insertedApplication = applicationRepository.saveAndFlush(application);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the application
        Application updatedApplication = applicationRepository.findById(application.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedApplication are not directly saved in db
        em.detach(updatedApplication);
        updatedApplication.name(UPDATED_NAME).description(UPDATED_DESCRIPTION).userId(UPDATED_USER_ID);

        restApplicationMockMvc
            .perform(
                put(ENTITY_API_URL_ID, updatedApplication.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(updatedApplication))
            )
            .andExpect(status().isOk());

        // Validate the Application in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedApplicationToMatchAllProperties(updatedApplication);
    }

    @Test
    @Transactional
    void putNonExistingApplication() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        application.setId(intCount.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restApplicationMockMvc
            .perform(
                put(ENTITY_API_URL_ID, application.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(application))
            )
            .andExpect(status().isBadRequest());

        // Validate the Application in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchApplication() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        application.setId(intCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restApplicationMockMvc
            .perform(
                put(ENTITY_API_URL_ID, intCount.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(application))
            )
            .andExpect(status().isBadRequest());

        // Validate the Application in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamApplication() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        application.setId(intCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restApplicationMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(application)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Application in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateApplicationWithPatch() throws Exception {
        // Initialize the database
        insertedApplication = applicationRepository.saveAndFlush(application);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the application using partial update
        Application partialUpdatedApplication = new Application();
        partialUpdatedApplication.setId(application.getId());

        partialUpdatedApplication.description(UPDATED_DESCRIPTION).userId(UPDATED_USER_ID);

        restApplicationMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedApplication.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedApplication))
            )
            .andExpect(status().isOk());

        // Validate the Application in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertApplicationUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedApplication, application),
            getPersistedApplication(application)
        );
    }

    @Test
    @Transactional
    void fullUpdateApplicationWithPatch() throws Exception {
        // Initialize the database
        insertedApplication = applicationRepository.saveAndFlush(application);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the application using partial update
        Application partialUpdatedApplication = new Application();
        partialUpdatedApplication.setId(application.getId());

        partialUpdatedApplication.name(UPDATED_NAME).description(UPDATED_DESCRIPTION).userId(UPDATED_USER_ID);

        restApplicationMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedApplication.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedApplication))
            )
            .andExpect(status().isOk());

        // Validate the Application in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertApplicationUpdatableFieldsEquals(partialUpdatedApplication, getPersistedApplication(partialUpdatedApplication));
    }

    @Test
    @Transactional
    void patchNonExistingApplication() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        application.setId(intCount.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restApplicationMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, application.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(application))
            )
            .andExpect(status().isBadRequest());

        // Validate the Application in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchApplication() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        application.setId(intCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restApplicationMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, intCount.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(application))
            )
            .andExpect(status().isBadRequest());

        // Validate the Application in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamApplication() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        application.setId(intCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restApplicationMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(application)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Application in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteApplication() throws Exception {
        // Initialize the database
        insertedApplication = applicationRepository.saveAndFlush(application);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the application
        restApplicationMockMvc
            .perform(delete(ENTITY_API_URL_ID, application.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return applicationRepository.count();
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

    protected Application getPersistedApplication(Application application) {
        return applicationRepository.findById(application.getId()).orElseThrow();
    }

    protected void assertPersistedApplicationToMatchAllProperties(Application expectedApplication) {
        assertApplicationAllPropertiesEquals(expectedApplication, getPersistedApplication(expectedApplication));
    }

    protected void assertPersistedApplicationToMatchUpdatableProperties(Application expectedApplication) {
        assertApplicationAllUpdatablePropertiesEquals(expectedApplication, getPersistedApplication(expectedApplication));
    }
}
