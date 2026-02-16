package com.example.myproject.domain;

import static com.example.myproject.domain.ContactTestSamples.*;
import static com.example.myproject.domain.GroupeTestSamples.*;
import static com.example.myproject.domain.GroupedecontactTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.example.myproject.web.rest.TestUtil;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ContactTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Contact.class);
        Contact contact1 = getContactSample1();
        Contact contact2 = new Contact();
        assertThat(contact1).isNotEqualTo(contact2);

        contact2.setId(contact1.getId());
        assertThat(contact1).isEqualTo(contact2);

        contact2 = getContactSample2();
        assertThat(contact1).isNotEqualTo(contact2);
    }

    @Test
    void GroupeTest() {
        Contact contact = getContactRandomSampleGenerator();
        Groupe groupeBack = getGroupeRandomSampleGenerator();

        contact.setGroupe(groupeBack);
        assertThat(contact.getGroupe()).isEqualTo(groupeBack);

        contact.groupe(null);
        assertThat(contact.getGroupe()).isNull();
    }

    @Test
    void groupedecontactTest() {
        Contact contact = getContactRandomSampleGenerator();
        Groupedecontact groupedecontactBack = getGroupedecontactRandomSampleGenerator();

        contact.addGroupedecontact(groupedecontactBack);
        assertThat(contact.getGroupedecontacts()).containsOnly(groupedecontactBack);
        assertThat(groupedecontactBack.getContact()).isEqualTo(contact);

        contact.removeGroupedecontact(groupedecontactBack);
        assertThat(contact.getGroupedecontacts()).doesNotContain(groupedecontactBack);
        assertThat(groupedecontactBack.getContact()).isNull();

        contact.groupedecontacts(new HashSet<>(Set.of(groupedecontactBack)));
        assertThat(contact.getGroupedecontacts()).containsOnly(groupedecontactBack);
        assertThat(groupedecontactBack.getContact()).isEqualTo(contact);

        contact.setGroupedecontacts(new HashSet<>());
        assertThat(contact.getGroupedecontacts()).doesNotContain(groupedecontactBack);
        assertThat(groupedecontactBack.getContact()).isNull();
    }
}
