package com.example.myproject.domain;

import static com.example.myproject.domain.ContactTestSamples.*;
import static com.example.myproject.domain.QuestionTestSamples.*;
import static com.example.myproject.domain.ReponseTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.example.myproject.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class ReponseTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Reponse.class);
        Reponse reponse1 = getReponseSample1();
        Reponse reponse2 = new Reponse();
        assertThat(reponse1).isNotEqualTo(reponse2);

        reponse2.setId(reponse1.getId());
        assertThat(reponse1).isEqualTo(reponse2);

        reponse2 = getReponseSample2();
        assertThat(reponse1).isNotEqualTo(reponse2);
    }

    @Test
    void repquestionTest() {
        Reponse reponse = getReponseRandomSampleGenerator();
        Question questionBack = getQuestionRandomSampleGenerator();

        reponse.setRepquestion(questionBack);
        assertThat(reponse.getRepquestion()).isEqualTo(questionBack);

        reponse.repquestion(null);
        assertThat(reponse.getRepquestion()).isNull();
    }

    @Test
    void repcontactTest() {
        Reponse reponse = getReponseRandomSampleGenerator();
        Contact contactBack = getContactRandomSampleGenerator();

        reponse.setRepcontact(contactBack);
        assertThat(reponse.getRepcontact()).isEqualTo(contactBack);

        reponse.repcontact(null);
        assertThat(reponse.getRepcontact()).isNull();
    }
}
