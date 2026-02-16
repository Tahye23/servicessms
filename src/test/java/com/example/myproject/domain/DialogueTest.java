package com.example.myproject.domain;

import static com.example.myproject.domain.DialogueTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.example.myproject.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class DialogueTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Dialogue.class);
        Dialogue dialogue1 = getDialogueSample1();
        Dialogue dialogue2 = new Dialogue();
        assertThat(dialogue1).isNotEqualTo(dialogue2);

        dialogue2.setId(dialogue1.getId());
        assertThat(dialogue1).isEqualTo(dialogue2);

        dialogue2 = getDialogueSample2();
        assertThat(dialogue1).isNotEqualTo(dialogue2);
    }
}
