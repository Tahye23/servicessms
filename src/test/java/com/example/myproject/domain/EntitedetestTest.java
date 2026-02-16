package com.example.myproject.domain;

import static com.example.myproject.domain.EntitedetestTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.example.myproject.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class EntitedetestTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Entitedetest.class);
        Entitedetest entitedetest1 = getEntitedetestSample1();
        Entitedetest entitedetest2 = new Entitedetest();
        assertThat(entitedetest1).isNotEqualTo(entitedetest2);

        entitedetest2.setId(entitedetest1.getId());
        assertThat(entitedetest1).isEqualTo(entitedetest2);

        entitedetest2 = getEntitedetestSample2();
        assertThat(entitedetest1).isNotEqualTo(entitedetest2);
    }
}
