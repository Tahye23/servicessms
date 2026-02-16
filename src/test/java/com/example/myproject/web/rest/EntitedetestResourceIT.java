package com.example.myproject.web.rest;

import static com.example.myproject.domain.EntitedetestAsserts.*;
import static com.example.myproject.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.myproject.IntegrationTest;
import com.example.myproject.domain.Entitedetest;
import com.example.myproject.repository.EntitedetestRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
 * Integration tests for the {@link EntitedetestResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class EntitedetestResourceIT {

    private static final Integer DEFAULT_IDENTITE = 1;
    private static final Integer UPDATED_IDENTITE = 2;

    private static final String DEFAULT_NOM = "AAAAAAAAAA";
    private static final String UPDATED_NOM = "BBBBBBBBBB";

    private static final Integer DEFAULT_NOMBREC = 1;
    private static final Integer UPDATED_NOMBREC = 2;

    private static final Boolean DEFAULT_CHAMB = false;
    private static final Boolean UPDATED_CHAMB = true;

    private static final Instant DEFAULT_CHAMPDATE = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_CHAMPDATE = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String ENTITY_API_URL = "/api/entitedetests";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private EntitedetestRepository entitedetestRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restEntitedetestMockMvc;

    private Entitedetest entitedetest;

    private Entitedetest insertedEntitedetest;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Entitedetest createEntity(EntityManager em) {
        Entitedetest entitedetest = new Entitedetest()
            .identite(DEFAULT_IDENTITE)
            .nom(DEFAULT_NOM)
            .nombrec(DEFAULT_NOMBREC)
            .chamb(DEFAULT_CHAMB)
            .champdate(DEFAULT_CHAMPDATE);
        return entitedetest;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Entitedetest createUpdatedEntity(EntityManager em) {
        Entitedetest entitedetest = new Entitedetest()
            .identite(UPDATED_IDENTITE)
            .nom(UPDATED_NOM)
            .nombrec(UPDATED_NOMBREC)
            .chamb(UPDATED_CHAMB)
            .champdate(UPDATED_CHAMPDATE);
        return entitedetest;
    }

    @BeforeEach
    public void initTest() {
        entitedetest = createEntity(em);
    }

    @AfterEach
    public void cleanup() {
        if (insertedEntitedetest != null) {
            entitedetestRepository.delete(insertedEntitedetest);
            insertedEntitedetest = null;
        }
    }

    @Test
    @Transactional
    void createEntitedetest() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the Entitedetest
        var returnedEntitedetest = om.readValue(
            restEntitedetestMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(entitedetest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            Entitedetest.class
        );

        // Validate the Entitedetest in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        assertEntitedetestUpdatableFieldsEquals(returnedEntitedetest, getPersistedEntitedetest(returnedEntitedetest));

        insertedEntitedetest = returnedEntitedetest;
    }

    @Test
    @Transactional
    void createEntitedetestWithExistingId() throws Exception {
        // Create the Entitedetest with an existing ID
        entitedetest.setId(1L);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restEntitedetestMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(entitedetest)))
            .andExpect(status().isBadRequest());

        // Validate the Entitedetest in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void getAllEntitedetests() throws Exception {
        // Initialize the database
        insertedEntitedetest = entitedetestRepository.saveAndFlush(entitedetest);

        // Get all the entitedetestList
        restEntitedetestMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(entitedetest.getId().intValue())))
            .andExpect(jsonPath("$.[*].identite").value(hasItem(DEFAULT_IDENTITE)))
            .andExpect(jsonPath("$.[*].nom").value(hasItem(DEFAULT_NOM)))
            .andExpect(jsonPath("$.[*].nombrec").value(hasItem(DEFAULT_NOMBREC)))
            .andExpect(jsonPath("$.[*].chamb").value(hasItem(DEFAULT_CHAMB.booleanValue())))
            .andExpect(jsonPath("$.[*].champdate").value(hasItem(DEFAULT_CHAMPDATE.toString())));
    }

    @Test
    @Transactional
    void getEntitedetest() throws Exception {
        // Initialize the database
        insertedEntitedetest = entitedetestRepository.saveAndFlush(entitedetest);

        // Get the entitedetest
        restEntitedetestMockMvc
            .perform(get(ENTITY_API_URL_ID, entitedetest.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(entitedetest.getId().intValue()))
            .andExpect(jsonPath("$.identite").value(DEFAULT_IDENTITE))
            .andExpect(jsonPath("$.nom").value(DEFAULT_NOM))
            .andExpect(jsonPath("$.nombrec").value(DEFAULT_NOMBREC))
            .andExpect(jsonPath("$.chamb").value(DEFAULT_CHAMB.booleanValue()))
            .andExpect(jsonPath("$.champdate").value(DEFAULT_CHAMPDATE.toString()));
    }

    @Test
    @Transactional
    void getNonExistingEntitedetest() throws Exception {
        // Get the entitedetest
        restEntitedetestMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingEntitedetest() throws Exception {
        // Initialize the database
        insertedEntitedetest = entitedetestRepository.saveAndFlush(entitedetest);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the entitedetest
        Entitedetest updatedEntitedetest = entitedetestRepository.findById(entitedetest.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedEntitedetest are not directly saved in db
        em.detach(updatedEntitedetest);
        updatedEntitedetest
            .identite(UPDATED_IDENTITE)
            .nom(UPDATED_NOM)
            .nombrec(UPDATED_NOMBREC)
            .chamb(UPDATED_CHAMB)
            .champdate(UPDATED_CHAMPDATE);

        restEntitedetestMockMvc
            .perform(
                put(ENTITY_API_URL_ID, updatedEntitedetest.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(updatedEntitedetest))
            )
            .andExpect(status().isOk());

        // Validate the Entitedetest in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedEntitedetestToMatchAllProperties(updatedEntitedetest);
    }

    @Test
    @Transactional
    void putNonExistingEntitedetest() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        entitedetest.setId(longCount.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restEntitedetestMockMvc
            .perform(
                put(ENTITY_API_URL_ID, entitedetest.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(entitedetest))
            )
            .andExpect(status().isBadRequest());

        // Validate the Entitedetest in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchEntitedetest() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        entitedetest.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restEntitedetestMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(entitedetest))
            )
            .andExpect(status().isBadRequest());

        // Validate the Entitedetest in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamEntitedetest() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        entitedetest.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restEntitedetestMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(entitedetest)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Entitedetest in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateEntitedetestWithPatch() throws Exception {
        // Initialize the database
        insertedEntitedetest = entitedetestRepository.saveAndFlush(entitedetest);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the entitedetest using partial update
        Entitedetest partialUpdatedEntitedetest = new Entitedetest();
        partialUpdatedEntitedetest.setId(entitedetest.getId());

        partialUpdatedEntitedetest.identite(UPDATED_IDENTITE).chamb(UPDATED_CHAMB).champdate(UPDATED_CHAMPDATE);

        restEntitedetestMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedEntitedetest.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedEntitedetest))
            )
            .andExpect(status().isOk());

        // Validate the Entitedetest in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertEntitedetestUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedEntitedetest, entitedetest),
            getPersistedEntitedetest(entitedetest)
        );
    }

    @Test
    @Transactional
    void fullUpdateEntitedetestWithPatch() throws Exception {
        // Initialize the database
        insertedEntitedetest = entitedetestRepository.saveAndFlush(entitedetest);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the entitedetest using partial update
        Entitedetest partialUpdatedEntitedetest = new Entitedetest();
        partialUpdatedEntitedetest.setId(entitedetest.getId());

        partialUpdatedEntitedetest
            .identite(UPDATED_IDENTITE)
            .nom(UPDATED_NOM)
            .nombrec(UPDATED_NOMBREC)
            .chamb(UPDATED_CHAMB)
            .champdate(UPDATED_CHAMPDATE);

        restEntitedetestMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedEntitedetest.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedEntitedetest))
            )
            .andExpect(status().isOk());

        // Validate the Entitedetest in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertEntitedetestUpdatableFieldsEquals(partialUpdatedEntitedetest, getPersistedEntitedetest(partialUpdatedEntitedetest));
    }

    @Test
    @Transactional
    void patchNonExistingEntitedetest() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        entitedetest.setId(longCount.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restEntitedetestMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, entitedetest.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(entitedetest))
            )
            .andExpect(status().isBadRequest());

        // Validate the Entitedetest in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchEntitedetest() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        entitedetest.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restEntitedetestMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(entitedetest))
            )
            .andExpect(status().isBadRequest());

        // Validate the Entitedetest in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamEntitedetest() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        entitedetest.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restEntitedetestMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(entitedetest)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Entitedetest in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteEntitedetest() throws Exception {
        // Initialize the database
        insertedEntitedetest = entitedetestRepository.saveAndFlush(entitedetest);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the entitedetest
        restEntitedetestMockMvc
            .perform(delete(ENTITY_API_URL_ID, entitedetest.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return entitedetestRepository.count();
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

    protected Entitedetest getPersistedEntitedetest(Entitedetest entitedetest) {
        return entitedetestRepository.findById(entitedetest.getId()).orElseThrow();
    }

    protected void assertPersistedEntitedetestToMatchAllProperties(Entitedetest expectedEntitedetest) {
        assertEntitedetestAllPropertiesEquals(expectedEntitedetest, getPersistedEntitedetest(expectedEntitedetest));
    }

    protected void assertPersistedEntitedetestToMatchUpdatableProperties(Entitedetest expectedEntitedetest) {
        assertEntitedetestAllUpdatablePropertiesEquals(expectedEntitedetest, getPersistedEntitedetest(expectedEntitedetest));
    }
}
