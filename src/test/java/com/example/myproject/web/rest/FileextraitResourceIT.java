package com.example.myproject.web.rest;

import static com.example.myproject.domain.FileextraitAsserts.*;
import static com.example.myproject.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.myproject.IntegrationTest;
import com.example.myproject.domain.Fileextrait;
import com.example.myproject.repository.FileextraitRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.util.Base64;
import java.util.Random;
import java.util.UUID;
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
 * Integration tests for the {@link FileextraitResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class FileextraitResourceIT {

    private static final UUID DEFAULT_FEXIDFILE = UUID.randomUUID();
    private static final UUID UPDATED_FEXIDFILE = UUID.randomUUID();

    private static final String DEFAULT_FEXPARENT = "AAAAAAAAAA";
    private static final String UPDATED_FEXPARENT = "BBBBBBBBBB";

    private static final byte[] DEFAULT_FEXDATA = TestUtil.createByteArray(1, "0");
    private static final byte[] UPDATED_FEXDATA = TestUtil.createByteArray(1, "1");
    private static final String DEFAULT_FEXDATA_CONTENT_TYPE = "image/jpg";
    private static final String UPDATED_FEXDATA_CONTENT_TYPE = "image/png";

    private static final String DEFAULT_FEXTYPE = "AAAAAAAAAA";
    private static final String UPDATED_FEXTYPE = "BBBBBBBBBB";

    private static final String DEFAULT_FEXNAME = "AAAAAAAAAA";
    private static final String UPDATED_FEXNAME = "BBBBBBBBBB";

    private static final String ENTITY_API_URL = "/api/fileextraits";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private FileextraitRepository fileextraitRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restFileextraitMockMvc;

    private Fileextrait fileextrait;

    private Fileextrait insertedFileextrait;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Fileextrait createEntity(EntityManager em) {
        Fileextrait fileextrait = new Fileextrait()
            .fexidfile(DEFAULT_FEXIDFILE)
            .fexparent(DEFAULT_FEXPARENT)
            .fexdata(DEFAULT_FEXDATA)
            .fexdataContentType(DEFAULT_FEXDATA_CONTENT_TYPE)
            .fextype(DEFAULT_FEXTYPE)
            .fexname(DEFAULT_FEXNAME);
        return fileextrait;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Fileextrait createUpdatedEntity(EntityManager em) {
        Fileextrait fileextrait = new Fileextrait()
            .fexidfile(UPDATED_FEXIDFILE)
            .fexparent(UPDATED_FEXPARENT)
            .fexdata(UPDATED_FEXDATA)
            .fexdataContentType(UPDATED_FEXDATA_CONTENT_TYPE)
            .fextype(UPDATED_FEXTYPE)
            .fexname(UPDATED_FEXNAME);
        return fileextrait;
    }

    @BeforeEach
    public void initTest() {
        fileextrait = createEntity(em);
    }

    @AfterEach
    public void cleanup() {
        if (insertedFileextrait != null) {
            fileextraitRepository.delete(insertedFileextrait);
            insertedFileextrait = null;
        }
    }

    @Test
    @Transactional
    void createFileextrait() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the Fileextrait
        var returnedFileextrait = om.readValue(
            restFileextraitMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(fileextrait)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            Fileextrait.class
        );

        // Validate the Fileextrait in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        assertFileextraitUpdatableFieldsEquals(returnedFileextrait, getPersistedFileextrait(returnedFileextrait));

        insertedFileextrait = returnedFileextrait;
    }

    @Test
    @Transactional
    void createFileextraitWithExistingId() throws Exception {
        // Create the Fileextrait with an existing ID
        fileextrait.setId(1L);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restFileextraitMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(fileextrait)))
            .andExpect(status().isBadRequest());

        // Validate the Fileextrait in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void getAllFileextraits() throws Exception {
        // Initialize the database
        insertedFileextrait = fileextraitRepository.saveAndFlush(fileextrait);

        // Get all the fileextraitList
        restFileextraitMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(fileextrait.getId().intValue())))
            .andExpect(jsonPath("$.[*].fexidfile").value(hasItem(DEFAULT_FEXIDFILE.toString())))
            .andExpect(jsonPath("$.[*].fexparent").value(hasItem(DEFAULT_FEXPARENT)))
            .andExpect(jsonPath("$.[*].fexdataContentType").value(hasItem(DEFAULT_FEXDATA_CONTENT_TYPE)))
            .andExpect(jsonPath("$.[*].fexdata").value(hasItem(Base64.getEncoder().encodeToString(DEFAULT_FEXDATA))))
            .andExpect(jsonPath("$.[*].fextype").value(hasItem(DEFAULT_FEXTYPE)))
            .andExpect(jsonPath("$.[*].fexname").value(hasItem(DEFAULT_FEXNAME)));
    }

    @Test
    @Transactional
    void getFileextrait() throws Exception {
        // Initialize the database
        insertedFileextrait = fileextraitRepository.saveAndFlush(fileextrait);

        // Get the fileextrait
        restFileextraitMockMvc
            .perform(get(ENTITY_API_URL_ID, fileextrait.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(fileextrait.getId().intValue()))
            .andExpect(jsonPath("$.fexidfile").value(DEFAULT_FEXIDFILE.toString()))
            .andExpect(jsonPath("$.fexparent").value(DEFAULT_FEXPARENT))
            .andExpect(jsonPath("$.fexdataContentType").value(DEFAULT_FEXDATA_CONTENT_TYPE))
            .andExpect(jsonPath("$.fexdata").value(Base64.getEncoder().encodeToString(DEFAULT_FEXDATA)))
            .andExpect(jsonPath("$.fextype").value(DEFAULT_FEXTYPE))
            .andExpect(jsonPath("$.fexname").value(DEFAULT_FEXNAME));
    }

    @Test
    @Transactional
    void getNonExistingFileextrait() throws Exception {
        // Get the fileextrait
        restFileextraitMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingFileextrait() throws Exception {
        // Initialize the database
        insertedFileextrait = fileextraitRepository.saveAndFlush(fileextrait);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the fileextrait
        Fileextrait updatedFileextrait = fileextraitRepository.findById(fileextrait.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedFileextrait are not directly saved in db
        em.detach(updatedFileextrait);
        updatedFileextrait
            .fexidfile(UPDATED_FEXIDFILE)
            .fexparent(UPDATED_FEXPARENT)
            .fexdata(UPDATED_FEXDATA)
            .fexdataContentType(UPDATED_FEXDATA_CONTENT_TYPE)
            .fextype(UPDATED_FEXTYPE)
            .fexname(UPDATED_FEXNAME);

        restFileextraitMockMvc
            .perform(
                put(ENTITY_API_URL_ID, updatedFileextrait.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(updatedFileextrait))
            )
            .andExpect(status().isOk());

        // Validate the Fileextrait in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedFileextraitToMatchAllProperties(updatedFileextrait);
    }

    @Test
    @Transactional
    void putNonExistingFileextrait() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        fileextrait.setId(longCount.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restFileextraitMockMvc
            .perform(
                put(ENTITY_API_URL_ID, fileextrait.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(fileextrait))
            )
            .andExpect(status().isBadRequest());

        // Validate the Fileextrait in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchFileextrait() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        fileextrait.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restFileextraitMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(fileextrait))
            )
            .andExpect(status().isBadRequest());

        // Validate the Fileextrait in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamFileextrait() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        fileextrait.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restFileextraitMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(fileextrait)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Fileextrait in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateFileextraitWithPatch() throws Exception {
        // Initialize the database
        insertedFileextrait = fileextraitRepository.saveAndFlush(fileextrait);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the fileextrait using partial update
        Fileextrait partialUpdatedFileextrait = new Fileextrait();
        partialUpdatedFileextrait.setId(fileextrait.getId());

        partialUpdatedFileextrait.fexidfile(UPDATED_FEXIDFILE).fexname(UPDATED_FEXNAME);

        restFileextraitMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedFileextrait.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedFileextrait))
            )
            .andExpect(status().isOk());

        // Validate the Fileextrait in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertFileextraitUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedFileextrait, fileextrait),
            getPersistedFileextrait(fileextrait)
        );
    }

    @Test
    @Transactional
    void fullUpdateFileextraitWithPatch() throws Exception {
        // Initialize the database
        insertedFileextrait = fileextraitRepository.saveAndFlush(fileextrait);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the fileextrait using partial update
        Fileextrait partialUpdatedFileextrait = new Fileextrait();
        partialUpdatedFileextrait.setId(fileextrait.getId());

        partialUpdatedFileextrait
            .fexidfile(UPDATED_FEXIDFILE)
            .fexparent(UPDATED_FEXPARENT)
            .fexdata(UPDATED_FEXDATA)
            .fexdataContentType(UPDATED_FEXDATA_CONTENT_TYPE)
            .fextype(UPDATED_FEXTYPE)
            .fexname(UPDATED_FEXNAME);

        restFileextraitMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedFileextrait.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedFileextrait))
            )
            .andExpect(status().isOk());

        // Validate the Fileextrait in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertFileextraitUpdatableFieldsEquals(partialUpdatedFileextrait, getPersistedFileextrait(partialUpdatedFileextrait));
    }

    @Test
    @Transactional
    void patchNonExistingFileextrait() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        fileextrait.setId(longCount.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restFileextraitMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, fileextrait.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(fileextrait))
            )
            .andExpect(status().isBadRequest());

        // Validate the Fileextrait in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchFileextrait() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        fileextrait.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restFileextraitMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(fileextrait))
            )
            .andExpect(status().isBadRequest());

        // Validate the Fileextrait in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamFileextrait() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        fileextrait.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restFileextraitMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(fileextrait)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Fileextrait in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteFileextrait() throws Exception {
        // Initialize the database
        insertedFileextrait = fileextraitRepository.saveAndFlush(fileextrait);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the fileextrait
        restFileextraitMockMvc
            .perform(delete(ENTITY_API_URL_ID, fileextrait.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return fileextraitRepository.count();
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

    protected Fileextrait getPersistedFileextrait(Fileextrait fileextrait) {
        return fileextraitRepository.findById(fileextrait.getId()).orElseThrow();
    }

    protected void assertPersistedFileextraitToMatchAllProperties(Fileextrait expectedFileextrait) {
        assertFileextraitAllPropertiesEquals(expectedFileextrait, getPersistedFileextrait(expectedFileextrait));
    }

    protected void assertPersistedFileextraitToMatchUpdatableProperties(Fileextrait expectedFileextrait) {
        assertFileextraitAllUpdatablePropertiesEquals(expectedFileextrait, getPersistedFileextrait(expectedFileextrait));
    }
}
