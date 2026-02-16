package com.example.myproject.domain;

import static com.example.myproject.domain.ApiTestSamples.*;
import static com.example.myproject.domain.ApplicationTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.example.myproject.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class ApplicationTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Application.class);
        Application application1 = getApplicationSample1();
        Application application2 = new Application();
        assertThat(application1).isNotEqualTo(application2);

        application2.setId(application1.getId());
        assertThat(application1).isEqualTo(application2);

        application2 = getApplicationSample2();
        assertThat(application1).isNotEqualTo(application2);
    }

    @Test
    void apiTest() {
        Application application = getApplicationRandomSampleGenerator();
        Api apiBack = getApiRandomSampleGenerator();

        application.setApi(apiBack);
        assertThat(application.getApi()).isEqualTo(apiBack);

        application.api(null);
        assertThat(application.getApi()).isNull();
    }
}
