package com.example.myproject.domain;

import static com.example.myproject.domain.ContactTestSamples.*;
import static com.example.myproject.domain.GroupeTestSamples.*;
import static com.example.myproject.domain.GroupedecontactTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.example.myproject.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class GroupedecontactTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Groupedecontact.class);
        Groupedecontact groupedecontact1 = getGroupedecontactSample1();
        Groupedecontact groupedecontact2 = new Groupedecontact();
        assertThat(groupedecontact1).isNotEqualTo(groupedecontact2);

        groupedecontact2.setId(groupedecontact1.getId());
        assertThat(groupedecontact1).isEqualTo(groupedecontact2);

        groupedecontact2 = getGroupedecontactSample2();
        assertThat(groupedecontact1).isNotEqualTo(groupedecontact2);
    }

    @Test
    void cgrgroupeTest() {
        Groupedecontact groupedecontact = getGroupedecontactRandomSampleGenerator();
        Groupe groupeBack = getGroupeRandomSampleGenerator();

        groupedecontact.setCgrgroupe(groupeBack);
        assertThat(groupedecontact.getCgrgroupe()).isEqualTo(groupeBack);

        groupedecontact.cgrgroupe(null);
        assertThat(groupedecontact.getCgrgroupe()).isNull();
    }

    @Test
    void contactTest() {
        Groupedecontact groupedecontact = getGroupedecontactRandomSampleGenerator();
        Contact contactBack = getContactRandomSampleGenerator();

        groupedecontact.setContact(contactBack);
        assertThat(groupedecontact.getContact()).isEqualTo(contactBack);

        groupedecontact.contact(null);
        assertThat(groupedecontact.getContact()).isNull();
    }
}
