package com.example.myproject.domain;

import static com.example.myproject.domain.CompanyTestSamples.*;
import static com.example.myproject.domain.ContactTestSamples.*;
import static com.example.myproject.domain.ConversationTestSamples.*;
import static com.example.myproject.domain.QuestionTestSamples.*;
import static com.example.myproject.domain.ReferentielTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.example.myproject.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class ConversationTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Conversation.class);
        Conversation conversation1 = getConversationSample1();
        Conversation conversation2 = new Conversation();
        assertThat(conversation1).isNotEqualTo(conversation2);

        conversation2.setId(conversation1.getId());
        assertThat(conversation1).isEqualTo(conversation2);

        conversation2 = getConversationSample2();
        assertThat(conversation1).isNotEqualTo(conversation2);
    }

    @Test
    void contactTest() {
        Conversation conversation = getConversationRandomSampleGenerator();
        Contact contactBack = getContactRandomSampleGenerator();

        conversation.setContact(contactBack);
        assertThat(conversation.getContact()).isEqualTo(contactBack);

        conversation.contact(null);
        assertThat(conversation.getContact()).isNull();
    }

    @Test
    void questionTest() {
        Conversation conversation = getConversationRandomSampleGenerator();
        Question questionBack = getQuestionRandomSampleGenerator();

        conversation.setQuestion(questionBack);
        assertThat(conversation.getQuestion()).isEqualTo(questionBack);

        conversation.question(null);
        assertThat(conversation.getQuestion()).isNull();
    }

    @Test
    void covenquetteTest() {
        Conversation conversation = getConversationRandomSampleGenerator();
        Company companyBack = getCompanyRandomSampleGenerator();

        conversation.setCovenquette(companyBack);
        assertThat(conversation.getCovenquette()).isEqualTo(companyBack);

        conversation.covenquette(null);
        assertThat(conversation.getCovenquette()).isNull();
    }

    @Test
    void covstateTest() {
        Conversation conversation = getConversationRandomSampleGenerator();
        Referentiel referentielBack = getReferentielRandomSampleGenerator();

        conversation.setCovstate(referentielBack);
        assertThat(conversation.getCovstate()).isEqualTo(referentielBack);

        conversation.covstate(null);
        assertThat(conversation.getCovstate()).isNull();
    }
}
