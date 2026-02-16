package com.example.myproject.domain;

import static com.example.myproject.domain.ContactTestSamples.*;
import static com.example.myproject.domain.GroupeTestSamples.*;
import static com.example.myproject.domain.ReferentielTestSamples.*;
import static com.example.myproject.domain.SendSmsTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.example.myproject.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class SendSmsTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(SendSms.class);
        SendSms sendSms1 = getSendSmsSample1();
        SendSms sendSms2 = new SendSms();
        assertThat(sendSms1).isNotEqualTo(sendSms2);

        sendSms2.setId(sendSms1.getId());
        assertThat(sendSms1).isEqualTo(sendSms2);

        sendSms2 = getSendSmsSample2();
        assertThat(sendSms1).isNotEqualTo(sendSms2);
    }

    @Test
    void destinateurTest() {
        SendSms sendSms = getSendSmsRandomSampleGenerator();
        Contact contactBack = getContactRandomSampleGenerator();

        sendSms.setDestinateur(contactBack);
        assertThat(sendSms.getDestinateur()).isEqualTo(contactBack);

        sendSms.destinateur(null);
        assertThat(sendSms.getDestinateur()).isNull();
    }

    @Test
    void destinatairesTest() {
        SendSms sendSms = getSendSmsRandomSampleGenerator();
        Groupe groupeBack = getGroupeRandomSampleGenerator();

        sendSms.setDestinataires(groupeBack);
        assertThat(sendSms.getDestinataires()).isEqualTo(groupeBack);

        sendSms.destinataires(null);
        assertThat(sendSms.getDestinataires()).isNull();
    }

    @Test
    void StatutTest() {
        SendSms sendSms = getSendSmsRandomSampleGenerator();
        Referentiel referentielBack = getReferentielRandomSampleGenerator();

        sendSms.setStatut(referentielBack);
        assertThat(sendSms.getStatut()).isEqualTo(referentielBack);

        sendSms.statut(null);
        assertThat(sendSms.getStatut()).isNull();
    }
}
