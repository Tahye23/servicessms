package com.example.myproject.domain;

import static com.example.myproject.domain.CompanyTestSamples.*;
import static com.example.myproject.domain.ContactTestSamples.*;
import static com.example.myproject.domain.ParticipantTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.example.myproject.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class ParticipantTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Participant.class);
        Participant participant1 = getParticipantSample1();
        Participant participant2 = new Participant();
        assertThat(participant1).isNotEqualTo(participant2);

        participant2.setId(participant1.getId());
        assertThat(participant1).isEqualTo(participant2);

        participant2 = getParticipantSample2();
        assertThat(participant1).isNotEqualTo(participant2);
    }

    @Test
    void patcontactTest() {
        Participant participant = getParticipantRandomSampleGenerator();
        Contact contactBack = getContactRandomSampleGenerator();

        participant.setPatcontact(contactBack);
        assertThat(participant.getPatcontact()).isEqualTo(contactBack);

        participant.patcontact(null);
        assertThat(participant.getPatcontact()).isNull();
    }

    @Test
    void patenquetteTest() {
        Participant participant = getParticipantRandomSampleGenerator();
        Company companyBack = getCompanyRandomSampleGenerator();

        participant.setPatenquette(companyBack);
        assertThat(participant.getPatenquette()).isEqualTo(companyBack);

        participant.patenquette(null);
        assertThat(participant.getPatenquette()).isNull();
    }
}
