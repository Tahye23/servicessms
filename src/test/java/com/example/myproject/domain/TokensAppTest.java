package com.example.myproject.domain;

import static com.example.myproject.domain.ApplicationTestSamples.*;
import static com.example.myproject.domain.TokensAppTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.example.myproject.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class TokensAppTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(TokensApp.class);
        TokensApp tokensApp1 = getTokensAppSample1();
        TokensApp tokensApp2 = new TokensApp();
        assertThat(tokensApp1).isNotEqualTo(tokensApp2);

        tokensApp2.setId(tokensApp1.getId());
        assertThat(tokensApp1).isEqualTo(tokensApp2);

        tokensApp2 = getTokensAppSample2();
        assertThat(tokensApp1).isNotEqualTo(tokensApp2);
    }

    @Test
    void applicationTest() {
        TokensApp tokensApp = getTokensAppRandomSampleGenerator();
        Application applicationBack = getApplicationRandomSampleGenerator();

        tokensApp.setApplication(applicationBack);
        assertThat(tokensApp.getApplication()).isEqualTo(applicationBack);

        tokensApp.application(null);
        assertThat(tokensApp.getApplication()).isNull();
    }
}
