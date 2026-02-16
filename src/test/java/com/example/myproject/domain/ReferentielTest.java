package com.example.myproject.domain;

import static com.example.myproject.domain.ReferentielTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.example.myproject.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class ReferentielTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Referentiel.class);
        Referentiel referentiel1 = getReferentielSample1();
        Referentiel referentiel2 = new Referentiel();
        assertThat(referentiel1).isNotEqualTo(referentiel2);

        referentiel2.setId(referentiel1.getId());
        assertThat(referentiel1).isEqualTo(referentiel2);

        referentiel2 = getReferentielSample2();
        assertThat(referentiel1).isNotEqualTo(referentiel2);
    }
}
