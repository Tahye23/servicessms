package com.example.myproject.domain;

import static com.example.myproject.domain.ReferentielTestSamples.*;
import static com.example.myproject.domain.ServiceTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.example.myproject.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class ServiceTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Service.class);
        Service service1 = getServiceSample1();
        Service service2 = new Service();
        assertThat(service1).isNotEqualTo(service2);

        service2.setId(service1.getId());
        assertThat(service1).isEqualTo(service2);

        service2 = getServiceSample2();
        assertThat(service1).isNotEqualTo(service2);
    }

    @Test
    void accessTypeTest() {
        Service service = getServiceRandomSampleGenerator();
        Referentiel referentielBack = getReferentielRandomSampleGenerator();

        service.setAccessType(referentielBack);
        assertThat(service.getAccessType()).isEqualTo(referentielBack);

        service.accessType(null);
        assertThat(service.getAccessType()).isNull();
    }
}
