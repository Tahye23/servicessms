package com.example.myproject.domain;

import static com.example.myproject.domain.ApiTestSamples.*;
import static com.example.myproject.domain.TokensAppTestSamples.*;
import static com.example.myproject.domain.UserTokenApiTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.example.myproject.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class UserTokenApiTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(UserTokenApi.class);
        UserTokenApi userTokenApi1 = getUserTokenApiSample1();
        UserTokenApi userTokenApi2 = new UserTokenApi();
        assertThat(userTokenApi1).isNotEqualTo(userTokenApi2);

        userTokenApi2.setId(userTokenApi1.getId());
        assertThat(userTokenApi1).isEqualTo(userTokenApi2);

        userTokenApi2 = getUserTokenApiSample2();
        assertThat(userTokenApi1).isNotEqualTo(userTokenApi2);
    }

    @Test
    void apiTest() {
        UserTokenApi userTokenApi = getUserTokenApiRandomSampleGenerator();
        Api apiBack = getApiRandomSampleGenerator();

        userTokenApi.setApi(apiBack);
        assertThat(userTokenApi.getApi()).isEqualTo(apiBack);

        userTokenApi.api(null);
        assertThat(userTokenApi.getApi()).isNull();
    }

    @Test
    void tokenTest() {
        UserTokenApi userTokenApi = getUserTokenApiRandomSampleGenerator();
        TokensApp tokensAppBack = getTokensAppRandomSampleGenerator();

        userTokenApi.setToken(tokensAppBack);
        assertThat(userTokenApi.getToken()).isEqualTo(tokensAppBack);

        userTokenApi.token(null);
        assertThat(userTokenApi.getToken()).isNull();
    }
}
