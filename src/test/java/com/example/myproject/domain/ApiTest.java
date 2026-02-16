package com.example.myproject.domain;

import static com.example.myproject.domain.ApiTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.example.myproject.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class ApiTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Api.class);
        Api api1 = getApiSample1();
        Api api2 = new Api();
        assertThat(api1).isNotEqualTo(api2);

        api2.setId(api1.getId());
        assertThat(api1).isEqualTo(api2);

        api2 = getApiSample2();
        assertThat(api1).isNotEqualTo(api2);
    }
}
