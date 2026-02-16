package com.example.myproject.domain;

import static com.example.myproject.domain.CompanyTestSamples.*;
import static com.example.myproject.domain.ReferentielTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.example.myproject.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class CompanyTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Company.class);
        Company company1 = getCompanySample1();
        Company company2 = new Company();
        assertThat(company1).isNotEqualTo(company2);

        company2.setId(company1.getId());
        assertThat(company1).isEqualTo(company2);

        company2 = getCompanySample2();
        assertThat(company1).isNotEqualTo(company2);
    }

    @Test
    void camstatusTest() {
        Company company = getCompanyRandomSampleGenerator();
        Referentiel referentielBack = getReferentielRandomSampleGenerator();

        company.setCamstatus(referentielBack);
        assertThat(company.getCamstatus()).isEqualTo(referentielBack);

        company.camstatus(null);
        assertThat(company.getCamstatus()).isNull();
    }
}
