package com.example.myproject.domain;

import static com.example.myproject.domain.CompanyTestSamples.*;
import static com.example.myproject.domain.QuestionTestSamples.*;
import static com.example.myproject.domain.ReferentielTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.example.myproject.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class QuestionTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Question.class);
        Question question1 = getQuestionSample1();
        Question question2 = new Question();
        assertThat(question1).isNotEqualTo(question2);

        question2.setId(question1.getId());
        assertThat(question1).isEqualTo(question2);

        question2 = getQuestionSample2();
        assertThat(question1).isNotEqualTo(question2);
    }

    @Test
    void qusenquetteTest() {
        Question question = getQuestionRandomSampleGenerator();
        Company companyBack = getCompanyRandomSampleGenerator();

        question.setQusenquette(companyBack);
        assertThat(question.getQusenquette()).isEqualTo(companyBack);

        question.qusenquette(null);
        assertThat(question.getQusenquette()).isNull();
    }

    @Test
    void qustypequestionTest() {
        Question question = getQuestionRandomSampleGenerator();
        Referentiel referentielBack = getReferentielRandomSampleGenerator();

        question.setQustypequestion(referentielBack);
        assertThat(question.getQustypequestion()).isEqualTo(referentielBack);

        question.qustypequestion(null);
        assertThat(question.getQustypequestion()).isNull();
    }
}
