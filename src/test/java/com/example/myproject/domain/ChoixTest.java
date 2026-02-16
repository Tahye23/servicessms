package com.example.myproject.domain;

import static com.example.myproject.domain.ChoixTestSamples.*;
import static com.example.myproject.domain.QuestionTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.example.myproject.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class ChoixTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Choix.class);
        Choix choix1 = getChoixSample1();
        Choix choix2 = new Choix();
        assertThat(choix1).isNotEqualTo(choix2);

        choix2.setId(choix1.getId());
        assertThat(choix1).isEqualTo(choix2);

        choix2 = getChoixSample2();
        assertThat(choix1).isNotEqualTo(choix2);
    }

    @Test
    void choquestionTest() {
        Choix choix = getChoixRandomSampleGenerator();
        Question questionBack = getQuestionRandomSampleGenerator();

        choix.setChoquestion(questionBack);
        assertThat(choix.getChoquestion()).isEqualTo(questionBack);

        choix.choquestion(null);
        assertThat(choix.getChoquestion()).isNull();
    }

    @Test
    void choquestionSuivanteTest() {
        Choix choix = getChoixRandomSampleGenerator();
        Question questionBack = getQuestionRandomSampleGenerator();

        choix.setChoquestionSuivante(questionBack);
        assertThat(choix.getChoquestionSuivante()).isEqualTo(questionBack);

        choix.choquestionSuivante(null);
        assertThat(choix.getChoquestionSuivante()).isNull();
    }
}
