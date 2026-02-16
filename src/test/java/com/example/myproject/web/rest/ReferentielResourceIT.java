package com.example.myproject.web.rest;

import static com.example.myproject.domain.ReferentielAsserts.*;
import static com.example.myproject.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.myproject.IntegrationTest;
import com.example.myproject.domain.Referentiel;
import com.example.myproject.repository.ReferentielRepository;
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
 * Integration tests for the {@link ReferentielResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class ReferentielResourceIT {

    private static final String DEFAULT_REF_CODE = "AAAAAAAAAA";
    private static final String UPDATED_REF_CODE = "BBBBBBBBBB";

    private static final String DEFAULT_REF_RADICAL = "AAAAAAAAAA";
    private static final String UPDATED_REF_RADICAL = "BBBBBBBBBB";

    private static final String DEFAULT_REF_FR_TITLE = "AAAAAAAAAA";
    private static final String UPDATED_REF_FR_TITLE = "BBBBBBBBBB";

    private static final String DEFAULT_REF_AR_TITLE = "AAAAAAAAAA";
    private static final String UPDATED_REF_AR_TITLE = "BBBBBBBBBB";

    private static final String DEFAULT_REF_EN_TITLE = "AAAAAAAAAA";
    private static final String UPDATED_REF_EN_TITLE = "BBBBBBBBBB";

    private static final String ENTITY_API_URL = "/api/referentiels";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private ReferentielRepository referentielRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restReferentielMockMvc;

    private Referentiel referentiel;

    private Referentiel insertedReferentiel;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Referentiel createEntity(EntityManager em) {
        Referentiel referentiel = new Referentiel()
            .refCode(DEFAULT_REF_CODE)
            .refRadical(DEFAULT_REF_RADICAL)
            .refFrTitle(DEFAULT_REF_FR_TITLE)
            .refArTitle(DEFAULT_REF_AR_TITLE)
            .refEnTitle(DEFAULT_REF_EN_TITLE);
        return referentiel;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Referentiel createUpdatedEntity(EntityManager em) {
        Referentiel referentiel = new Referentiel()
            .refCode(UPDATED_REF_CODE)
            .refRadical(UPDATED_REF_RADICAL)
            .refFrTitle(UPDATED_REF_FR_TITLE)
            .refArTitle(UPDATED_REF_AR_TITLE)
            .refEnTitle(UPDATED_REF_EN_TITLE);
        return referentiel;
    }

    @BeforeEach
    public void initTest() {
        referentiel = createEntity(em);
    }

    @AfterEach
    public void cleanup() {
        if (insertedReferentiel != null) {
            referentielRepository.delete(insertedReferentiel);
            insertedReferentiel = null;
        }
    }

    @Test
    @Transactional
    void createReferentiel() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the Referentiel
        var returnedReferentiel = om.readValue(
            restReferentielMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(referentiel)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            Referentiel.class
        );

        // Validate the Referentiel in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        assertReferentielUpdatableFieldsEquals(returnedReferentiel, getPersistedReferentiel(returnedReferentiel));

        insertedReferentiel = returnedReferentiel;
    }

    @Test
    @Transactional
    void createReferentielWithExistingId() throws Exception {
        // Create the Referentiel with an existing ID
        referentiel.setId(1L);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restReferentielMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(referentiel)))
            .andExpect(status().isBadRequest());

        // Validate the Referentiel in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkRefCodeIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        referentiel.setRefCode(null);

        // Create the Referentiel, which fails.

        restReferentielMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(referentiel)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkRefRadicalIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        referentiel.setRefRadical(null);

        // Create the Referentiel, which fails.

        restReferentielMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(referentiel)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllReferentiels() throws Exception {
        // Initialize the database
        insertedReferentiel = referentielRepository.saveAndFlush(referentiel);

        // Get all the referentielList
        restReferentielMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(referentiel.getId().intValue())))
            .andExpect(jsonPath("$.[*].refCode").value(hasItem(DEFAULT_REF_CODE)))
            .andExpect(jsonPath("$.[*].refRadical").value(hasItem(DEFAULT_REF_RADICAL)))
            .andExpect(jsonPath("$.[*].refFrTitle").value(hasItem(DEFAULT_REF_FR_TITLE)))
            .andExpect(jsonPath("$.[*].refArTitle").value(hasItem(DEFAULT_REF_AR_TITLE)))
            .andExpect(jsonPath("$.[*].refEnTitle").value(hasItem(DEFAULT_REF_EN_TITLE)));
    }

    @Test
    @Transactional
    void getReferentiel() throws Exception {
        // Initialize the database
        insertedReferentiel = referentielRepository.saveAndFlush(referentiel);

        // Get the referentiel
        restReferentielMockMvc
            .perform(get(ENTITY_API_URL_ID, referentiel.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(referentiel.getId().intValue()))
            .andExpect(jsonPath("$.refCode").value(DEFAULT_REF_CODE))
            .andExpect(jsonPath("$.refRadical").value(DEFAULT_REF_RADICAL))
            .andExpect(jsonPath("$.refFrTitle").value(DEFAULT_REF_FR_TITLE))
            .andExpect(jsonPath("$.refArTitle").value(DEFAULT_REF_AR_TITLE))
            .andExpect(jsonPath("$.refEnTitle").value(DEFAULT_REF_EN_TITLE));
    }

    @Test
    @Transactional
    void getReferentielsByIdFiltering() throws Exception {
        // Initialize the database
        insertedReferentiel = referentielRepository.saveAndFlush(referentiel);

        Long id = referentiel.getId();

        defaultReferentielFiltering("id.equals=" + id, "id.notEquals=" + id);

        defaultReferentielFiltering("id.greaterThanOrEqual=" + id, "id.greaterThan=" + id);

        defaultReferentielFiltering("id.lessThanOrEqual=" + id, "id.lessThan=" + id);
    }

    @Test
    @Transactional
    void getAllReferentielsByRefCodeIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedReferentiel = referentielRepository.saveAndFlush(referentiel);

        // Get all the referentielList where refCode equals to
        defaultReferentielFiltering("refCode.equals=" + DEFAULT_REF_CODE, "refCode.equals=" + UPDATED_REF_CODE);
    }

    @Test
    @Transactional
    void getAllReferentielsByRefCodeIsInShouldWork() throws Exception {
        // Initialize the database
        insertedReferentiel = referentielRepository.saveAndFlush(referentiel);

        // Get all the referentielList where refCode in
        defaultReferentielFiltering("refCode.in=" + DEFAULT_REF_CODE + "," + UPDATED_REF_CODE, "refCode.in=" + UPDATED_REF_CODE);
    }

    @Test
    @Transactional
    void getAllReferentielsByRefCodeIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedReferentiel = referentielRepository.saveAndFlush(referentiel);

        // Get all the referentielList where refCode is not null
        defaultReferentielFiltering("refCode.specified=true", "refCode.specified=false");
    }

    @Test
    @Transactional
    void getAllReferentielsByRefCodeContainsSomething() throws Exception {
        // Initialize the database
        insertedReferentiel = referentielRepository.saveAndFlush(referentiel);

        // Get all the referentielList where refCode contains
        defaultReferentielFiltering("refCode.contains=" + DEFAULT_REF_CODE, "refCode.contains=" + UPDATED_REF_CODE);
    }

    @Test
    @Transactional
    void getAllReferentielsByRefCodeNotContainsSomething() throws Exception {
        // Initialize the database
        insertedReferentiel = referentielRepository.saveAndFlush(referentiel);

        // Get all the referentielList where refCode does not contain
        defaultReferentielFiltering("refCode.doesNotContain=" + UPDATED_REF_CODE, "refCode.doesNotContain=" + DEFAULT_REF_CODE);
    }

    @Test
    @Transactional
    void getAllReferentielsByRefRadicalIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedReferentiel = referentielRepository.saveAndFlush(referentiel);

        // Get all the referentielList where refRadical equals to
        defaultReferentielFiltering("refRadical.equals=" + DEFAULT_REF_RADICAL, "refRadical.equals=" + UPDATED_REF_RADICAL);
    }

    @Test
    @Transactional
    void getAllReferentielsByRefRadicalIsInShouldWork() throws Exception {
        // Initialize the database
        insertedReferentiel = referentielRepository.saveAndFlush(referentiel);

        // Get all the referentielList where refRadical in
        defaultReferentielFiltering(
            "refRadical.in=" + DEFAULT_REF_RADICAL + "," + UPDATED_REF_RADICAL,
            "refRadical.in=" + UPDATED_REF_RADICAL
        );
    }

    @Test
    @Transactional
    void getAllReferentielsByRefRadicalIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedReferentiel = referentielRepository.saveAndFlush(referentiel);

        // Get all the referentielList where refRadical is not null
        defaultReferentielFiltering("refRadical.specified=true", "refRadical.specified=false");
    }

    @Test
    @Transactional
    void getAllReferentielsByRefRadicalContainsSomething() throws Exception {
        // Initialize the database
        insertedReferentiel = referentielRepository.saveAndFlush(referentiel);

        // Get all the referentielList where refRadical contains
        defaultReferentielFiltering("refRadical.contains=" + DEFAULT_REF_RADICAL, "refRadical.contains=" + UPDATED_REF_RADICAL);
    }

    @Test
    @Transactional
    void getAllReferentielsByRefRadicalNotContainsSomething() throws Exception {
        // Initialize the database
        insertedReferentiel = referentielRepository.saveAndFlush(referentiel);

        // Get all the referentielList where refRadical does not contain
        defaultReferentielFiltering("refRadical.doesNotContain=" + UPDATED_REF_RADICAL, "refRadical.doesNotContain=" + DEFAULT_REF_RADICAL);
    }

    @Test
    @Transactional
    void getAllReferentielsByRefFrTitleIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedReferentiel = referentielRepository.saveAndFlush(referentiel);

        // Get all the referentielList where refFrTitle equals to
        defaultReferentielFiltering("refFrTitle.equals=" + DEFAULT_REF_FR_TITLE, "refFrTitle.equals=" + UPDATED_REF_FR_TITLE);
    }

    @Test
    @Transactional
    void getAllReferentielsByRefFrTitleIsInShouldWork() throws Exception {
        // Initialize the database
        insertedReferentiel = referentielRepository.saveAndFlush(referentiel);

        // Get all the referentielList where refFrTitle in
        defaultReferentielFiltering(
            "refFrTitle.in=" + DEFAULT_REF_FR_TITLE + "," + UPDATED_REF_FR_TITLE,
            "refFrTitle.in=" + UPDATED_REF_FR_TITLE
        );
    }

    @Test
    @Transactional
    void getAllReferentielsByRefFrTitleIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedReferentiel = referentielRepository.saveAndFlush(referentiel);

        // Get all the referentielList where refFrTitle is not null
        defaultReferentielFiltering("refFrTitle.specified=true", "refFrTitle.specified=false");
    }

    @Test
    @Transactional
    void getAllReferentielsByRefFrTitleContainsSomething() throws Exception {
        // Initialize the database
        insertedReferentiel = referentielRepository.saveAndFlush(referentiel);

        // Get all the referentielList where refFrTitle contains
        defaultReferentielFiltering("refFrTitle.contains=" + DEFAULT_REF_FR_TITLE, "refFrTitle.contains=" + UPDATED_REF_FR_TITLE);
    }

    @Test
    @Transactional
    void getAllReferentielsByRefFrTitleNotContainsSomething() throws Exception {
        // Initialize the database
        insertedReferentiel = referentielRepository.saveAndFlush(referentiel);

        // Get all the referentielList where refFrTitle does not contain
        defaultReferentielFiltering(
            "refFrTitle.doesNotContain=" + UPDATED_REF_FR_TITLE,
            "refFrTitle.doesNotContain=" + DEFAULT_REF_FR_TITLE
        );
    }

    @Test
    @Transactional
    void getAllReferentielsByRefArTitleIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedReferentiel = referentielRepository.saveAndFlush(referentiel);

        // Get all the referentielList where refArTitle equals to
        defaultReferentielFiltering("refArTitle.equals=" + DEFAULT_REF_AR_TITLE, "refArTitle.equals=" + UPDATED_REF_AR_TITLE);
    }

    @Test
    @Transactional
    void getAllReferentielsByRefArTitleIsInShouldWork() throws Exception {
        // Initialize the database
        insertedReferentiel = referentielRepository.saveAndFlush(referentiel);

        // Get all the referentielList where refArTitle in
        defaultReferentielFiltering(
            "refArTitle.in=" + DEFAULT_REF_AR_TITLE + "," + UPDATED_REF_AR_TITLE,
            "refArTitle.in=" + UPDATED_REF_AR_TITLE
        );
    }

    @Test
    @Transactional
    void getAllReferentielsByRefArTitleIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedReferentiel = referentielRepository.saveAndFlush(referentiel);

        // Get all the referentielList where refArTitle is not null
        defaultReferentielFiltering("refArTitle.specified=true", "refArTitle.specified=false");
    }

    @Test
    @Transactional
    void getAllReferentielsByRefArTitleContainsSomething() throws Exception {
        // Initialize the database
        insertedReferentiel = referentielRepository.saveAndFlush(referentiel);

        // Get all the referentielList where refArTitle contains
        defaultReferentielFiltering("refArTitle.contains=" + DEFAULT_REF_AR_TITLE, "refArTitle.contains=" + UPDATED_REF_AR_TITLE);
    }

    @Test
    @Transactional
    void getAllReferentielsByRefArTitleNotContainsSomething() throws Exception {
        // Initialize the database
        insertedReferentiel = referentielRepository.saveAndFlush(referentiel);

        // Get all the referentielList where refArTitle does not contain
        defaultReferentielFiltering(
            "refArTitle.doesNotContain=" + UPDATED_REF_AR_TITLE,
            "refArTitle.doesNotContain=" + DEFAULT_REF_AR_TITLE
        );
    }

    @Test
    @Transactional
    void getAllReferentielsByRefEnTitleIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedReferentiel = referentielRepository.saveAndFlush(referentiel);

        // Get all the referentielList where refEnTitle equals to
        defaultReferentielFiltering("refEnTitle.equals=" + DEFAULT_REF_EN_TITLE, "refEnTitle.equals=" + UPDATED_REF_EN_TITLE);
    }

    @Test
    @Transactional
    void getAllReferentielsByRefEnTitleIsInShouldWork() throws Exception {
        // Initialize the database
        insertedReferentiel = referentielRepository.saveAndFlush(referentiel);

        // Get all the referentielList where refEnTitle in
        defaultReferentielFiltering(
            "refEnTitle.in=" + DEFAULT_REF_EN_TITLE + "," + UPDATED_REF_EN_TITLE,
            "refEnTitle.in=" + UPDATED_REF_EN_TITLE
        );
    }

    @Test
    @Transactional
    void getAllReferentielsByRefEnTitleIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedReferentiel = referentielRepository.saveAndFlush(referentiel);

        // Get all the referentielList where refEnTitle is not null
        defaultReferentielFiltering("refEnTitle.specified=true", "refEnTitle.specified=false");
    }

    @Test
    @Transactional
    void getAllReferentielsByRefEnTitleContainsSomething() throws Exception {
        // Initialize the database
        insertedReferentiel = referentielRepository.saveAndFlush(referentiel);

        // Get all the referentielList where refEnTitle contains
        defaultReferentielFiltering("refEnTitle.contains=" + DEFAULT_REF_EN_TITLE, "refEnTitle.contains=" + UPDATED_REF_EN_TITLE);
    }

    @Test
    @Transactional
    void getAllReferentielsByRefEnTitleNotContainsSomething() throws Exception {
        // Initialize the database
        insertedReferentiel = referentielRepository.saveAndFlush(referentiel);

        // Get all the referentielList where refEnTitle does not contain
        defaultReferentielFiltering(
            "refEnTitle.doesNotContain=" + UPDATED_REF_EN_TITLE,
            "refEnTitle.doesNotContain=" + DEFAULT_REF_EN_TITLE
        );
    }

    private void defaultReferentielFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultReferentielShouldBeFound(shouldBeFound);
        defaultReferentielShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultReferentielShouldBeFound(String filter) throws Exception {
        restReferentielMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(referentiel.getId().intValue())))
            .andExpect(jsonPath("$.[*].refCode").value(hasItem(DEFAULT_REF_CODE)))
            .andExpect(jsonPath("$.[*].refRadical").value(hasItem(DEFAULT_REF_RADICAL)))
            .andExpect(jsonPath("$.[*].refFrTitle").value(hasItem(DEFAULT_REF_FR_TITLE)))
            .andExpect(jsonPath("$.[*].refArTitle").value(hasItem(DEFAULT_REF_AR_TITLE)))
            .andExpect(jsonPath("$.[*].refEnTitle").value(hasItem(DEFAULT_REF_EN_TITLE)));

        // Check, that the count call also returns 1
        restReferentielMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultReferentielShouldNotBeFound(String filter) throws Exception {
        restReferentielMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restReferentielMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingReferentiel() throws Exception {
        // Get the referentiel
        restReferentielMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingReferentiel() throws Exception {
        // Initialize the database
        insertedReferentiel = referentielRepository.saveAndFlush(referentiel);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the referentiel
        Referentiel updatedReferentiel = referentielRepository.findById(referentiel.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedReferentiel are not directly saved in db
        em.detach(updatedReferentiel);
        updatedReferentiel
            .refCode(UPDATED_REF_CODE)
            .refRadical(UPDATED_REF_RADICAL)
            .refFrTitle(UPDATED_REF_FR_TITLE)
            .refArTitle(UPDATED_REF_AR_TITLE)
            .refEnTitle(UPDATED_REF_EN_TITLE);

        restReferentielMockMvc
            .perform(
                put(ENTITY_API_URL_ID, updatedReferentiel.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(updatedReferentiel))
            )
            .andExpect(status().isOk());

        // Validate the Referentiel in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedReferentielToMatchAllProperties(updatedReferentiel);
    }

    @Test
    @Transactional
    void putNonExistingReferentiel() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        referentiel.setId(longCount.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restReferentielMockMvc
            .perform(
                put(ENTITY_API_URL_ID, referentiel.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(referentiel))
            )
            .andExpect(status().isBadRequest());

        // Validate the Referentiel in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchReferentiel() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        referentiel.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restReferentielMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(referentiel))
            )
            .andExpect(status().isBadRequest());

        // Validate the Referentiel in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamReferentiel() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        referentiel.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restReferentielMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(referentiel)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Referentiel in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateReferentielWithPatch() throws Exception {
        // Initialize the database
        insertedReferentiel = referentielRepository.saveAndFlush(referentiel);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the referentiel using partial update
        Referentiel partialUpdatedReferentiel = new Referentiel();
        partialUpdatedReferentiel.setId(referentiel.getId());

        partialUpdatedReferentiel.refRadical(UPDATED_REF_RADICAL).refFrTitle(UPDATED_REF_FR_TITLE);

        restReferentielMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedReferentiel.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedReferentiel))
            )
            .andExpect(status().isOk());

        // Validate the Referentiel in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertReferentielUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedReferentiel, referentiel),
            getPersistedReferentiel(referentiel)
        );
    }

    @Test
    @Transactional
    void fullUpdateReferentielWithPatch() throws Exception {
        // Initialize the database
        insertedReferentiel = referentielRepository.saveAndFlush(referentiel);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the referentiel using partial update
        Referentiel partialUpdatedReferentiel = new Referentiel();
        partialUpdatedReferentiel.setId(referentiel.getId());

        partialUpdatedReferentiel
            .refCode(UPDATED_REF_CODE)
            .refRadical(UPDATED_REF_RADICAL)
            .refFrTitle(UPDATED_REF_FR_TITLE)
            .refArTitle(UPDATED_REF_AR_TITLE)
            .refEnTitle(UPDATED_REF_EN_TITLE);

        restReferentielMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedReferentiel.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedReferentiel))
            )
            .andExpect(status().isOk());

        // Validate the Referentiel in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertReferentielUpdatableFieldsEquals(partialUpdatedReferentiel, getPersistedReferentiel(partialUpdatedReferentiel));
    }

    @Test
    @Transactional
    void patchNonExistingReferentiel() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        referentiel.setId(longCount.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restReferentielMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, referentiel.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(referentiel))
            )
            .andExpect(status().isBadRequest());

        // Validate the Referentiel in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchReferentiel() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        referentiel.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restReferentielMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(referentiel))
            )
            .andExpect(status().isBadRequest());

        // Validate the Referentiel in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamReferentiel() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        referentiel.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restReferentielMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(referentiel)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Referentiel in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteReferentiel() throws Exception {
        // Initialize the database
        insertedReferentiel = referentielRepository.saveAndFlush(referentiel);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the referentiel
        restReferentielMockMvc
            .perform(delete(ENTITY_API_URL_ID, referentiel.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return referentielRepository.count();
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

    protected Referentiel getPersistedReferentiel(Referentiel referentiel) {
        return referentielRepository.findById(referentiel.getId()).orElseThrow();
    }

    protected void assertPersistedReferentielToMatchAllProperties(Referentiel expectedReferentiel) {
        assertReferentielAllPropertiesEquals(expectedReferentiel, getPersistedReferentiel(expectedReferentiel));
    }

    protected void assertPersistedReferentielToMatchUpdatableProperties(Referentiel expectedReferentiel) {
        assertReferentielAllUpdatablePropertiesEquals(expectedReferentiel, getPersistedReferentiel(expectedReferentiel));
    }
}
