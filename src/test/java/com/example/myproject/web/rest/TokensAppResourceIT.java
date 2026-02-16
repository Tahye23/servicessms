package com.example.myproject.web.rest;

import static com.example.myproject.domain.TokensAppAsserts.*;
import static com.example.myproject.web.rest.TestUtil.createUpdateProxyForBean;
import static com.example.myproject.web.rest.TestUtil.sameInstant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.myproject.IntegrationTest;
import com.example.myproject.domain.TokensApp;
import com.example.myproject.repository.TokensAppRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
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
 * Integration tests for the {@link TokensAppResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class TokensAppResourceIT {

    private static final ZonedDateTime DEFAULT_DATE_EXPIRATION = ZonedDateTime.ofInstant(Instant.ofEpochMilli(0L), ZoneOffset.UTC);
    private static final ZonedDateTime UPDATED_DATE_EXPIRATION = ZonedDateTime.now(ZoneId.systemDefault()).withNano(0);

    private static final String DEFAULT_TOKEN = "AAAAAAAAAA";
    private static final String UPDATED_TOKEN = "BBBBBBBBBB";

    private static final Boolean DEFAULT_IS_EXPIRED = false;
    private static final Boolean UPDATED_IS_EXPIRED = true;

    private static final String ENTITY_API_URL = "/api/tokens-apps";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicInteger intCount = new AtomicInteger(random.nextInt() + (2 * Short.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private TokensAppRepository tokensAppRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restTokensAppMockMvc;

    private TokensApp tokensApp;

    private TokensApp insertedTokensApp;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static TokensApp createEntity(EntityManager em) {
        TokensApp tokensApp = new TokensApp().dateExpiration(DEFAULT_DATE_EXPIRATION).token(DEFAULT_TOKEN).isExpired(DEFAULT_IS_EXPIRED);
        return tokensApp;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static TokensApp createUpdatedEntity(EntityManager em) {
        TokensApp tokensApp = new TokensApp().dateExpiration(UPDATED_DATE_EXPIRATION).token(UPDATED_TOKEN).isExpired(UPDATED_IS_EXPIRED);
        return tokensApp;
    }

    @BeforeEach
    public void initTest() {
        tokensApp = createEntity(em);
    }

    @AfterEach
    public void cleanup() {
        if (insertedTokensApp != null) {
            tokensAppRepository.delete(insertedTokensApp);
            insertedTokensApp = null;
        }
    }

    @Test
    @Transactional
    void createTokensApp() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the TokensApp
        var returnedTokensApp = om.readValue(
            restTokensAppMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(tokensApp)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            TokensApp.class
        );

        // Validate the TokensApp in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        assertTokensAppUpdatableFieldsEquals(returnedTokensApp, getPersistedTokensApp(returnedTokensApp));

        insertedTokensApp = returnedTokensApp;
    }

    @Test
    @Transactional
    void createTokensAppWithExistingId() throws Exception {
        // Create the TokensApp with an existing ID
        tokensApp.setId(1);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restTokensAppMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(tokensApp)))
            .andExpect(status().isBadRequest());

        // Validate the TokensApp in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void getAllTokensApps() throws Exception {
        // Initialize the database
        insertedTokensApp = tokensAppRepository.saveAndFlush(tokensApp);

        // Get all the tokensAppList
        restTokensAppMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(tokensApp.getId().intValue())))
            .andExpect(jsonPath("$.[*].dateExpiration").value(hasItem(sameInstant(DEFAULT_DATE_EXPIRATION))))
            .andExpect(jsonPath("$.[*].token").value(hasItem(DEFAULT_TOKEN)))
            .andExpect(jsonPath("$.[*].isExpired").value(hasItem(DEFAULT_IS_EXPIRED.booleanValue())));
    }

    @Test
    @Transactional
    void getTokensApp() throws Exception {
        // Initialize the database
        insertedTokensApp = tokensAppRepository.saveAndFlush(tokensApp);

        // Get the tokensApp
        restTokensAppMockMvc
            .perform(get(ENTITY_API_URL_ID, tokensApp.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(tokensApp.getId().intValue()))
            .andExpect(jsonPath("$.dateExpiration").value(sameInstant(DEFAULT_DATE_EXPIRATION)))
            .andExpect(jsonPath("$.token").value(DEFAULT_TOKEN))
            .andExpect(jsonPath("$.isExpired").value(DEFAULT_IS_EXPIRED.booleanValue()));
    }

    @Test
    @Transactional
    void getNonExistingTokensApp() throws Exception {
        // Get the tokensApp
        restTokensAppMockMvc.perform(get(ENTITY_API_URL_ID, Integer.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingTokensApp() throws Exception {
        // Initialize the database
        insertedTokensApp = tokensAppRepository.saveAndFlush(tokensApp);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the tokensApp
        TokensApp updatedTokensApp = tokensAppRepository.findById(tokensApp.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedTokensApp are not directly saved in db
        em.detach(updatedTokensApp);
        updatedTokensApp.dateExpiration(UPDATED_DATE_EXPIRATION).token(UPDATED_TOKEN).isExpired(UPDATED_IS_EXPIRED);

        restTokensAppMockMvc
            .perform(
                put(ENTITY_API_URL_ID, updatedTokensApp.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(updatedTokensApp))
            )
            .andExpect(status().isOk());

        // Validate the TokensApp in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedTokensAppToMatchAllProperties(updatedTokensApp);
    }

    @Test
    @Transactional
    void putNonExistingTokensApp() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        tokensApp.setId(intCount.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restTokensAppMockMvc
            .perform(
                put(ENTITY_API_URL_ID, tokensApp.getId()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(tokensApp))
            )
            .andExpect(status().isBadRequest());

        // Validate the TokensApp in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchTokensApp() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        tokensApp.setId(intCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restTokensAppMockMvc
            .perform(
                put(ENTITY_API_URL_ID, intCount.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(tokensApp))
            )
            .andExpect(status().isBadRequest());

        // Validate the TokensApp in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamTokensApp() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        tokensApp.setId(intCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restTokensAppMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(tokensApp)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the TokensApp in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateTokensAppWithPatch() throws Exception {
        // Initialize the database
        insertedTokensApp = tokensAppRepository.saveAndFlush(tokensApp);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the tokensApp using partial update
        TokensApp partialUpdatedTokensApp = new TokensApp();
        partialUpdatedTokensApp.setId(tokensApp.getId());

        restTokensAppMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedTokensApp.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedTokensApp))
            )
            .andExpect(status().isOk());

        // Validate the TokensApp in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertTokensAppUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedTokensApp, tokensApp),
            getPersistedTokensApp(tokensApp)
        );
    }

    @Test
    @Transactional
    void fullUpdateTokensAppWithPatch() throws Exception {
        // Initialize the database
        insertedTokensApp = tokensAppRepository.saveAndFlush(tokensApp);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the tokensApp using partial update
        TokensApp partialUpdatedTokensApp = new TokensApp();
        partialUpdatedTokensApp.setId(tokensApp.getId());

        partialUpdatedTokensApp.dateExpiration(UPDATED_DATE_EXPIRATION).token(UPDATED_TOKEN).isExpired(UPDATED_IS_EXPIRED);

        restTokensAppMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedTokensApp.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedTokensApp))
            )
            .andExpect(status().isOk());

        // Validate the TokensApp in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertTokensAppUpdatableFieldsEquals(partialUpdatedTokensApp, getPersistedTokensApp(partialUpdatedTokensApp));
    }

    @Test
    @Transactional
    void patchNonExistingTokensApp() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        tokensApp.setId(intCount.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restTokensAppMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, tokensApp.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(tokensApp))
            )
            .andExpect(status().isBadRequest());

        // Validate the TokensApp in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchTokensApp() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        tokensApp.setId(intCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restTokensAppMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, intCount.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(tokensApp))
            )
            .andExpect(status().isBadRequest());

        // Validate the TokensApp in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamTokensApp() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        tokensApp.setId(intCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restTokensAppMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(tokensApp)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the TokensApp in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteTokensApp() throws Exception {
        // Initialize the database
        insertedTokensApp = tokensAppRepository.saveAndFlush(tokensApp);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the tokensApp
        restTokensAppMockMvc
            .perform(delete(ENTITY_API_URL_ID, tokensApp.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return tokensAppRepository.count();
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

    protected TokensApp getPersistedTokensApp(TokensApp tokensApp) {
        return tokensAppRepository.findById(tokensApp.getId()).orElseThrow();
    }

    protected void assertPersistedTokensAppToMatchAllProperties(TokensApp expectedTokensApp) {
        assertTokensAppAllPropertiesEquals(expectedTokensApp, getPersistedTokensApp(expectedTokensApp));
    }

    protected void assertPersistedTokensAppToMatchUpdatableProperties(TokensApp expectedTokensApp) {
        assertTokensAppAllUpdatablePropertiesEquals(expectedTokensApp, getPersistedTokensApp(expectedTokensApp));
    }
}
