package com.example.myproject.domain;

import static com.example.myproject.domain.FileextraitTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.example.myproject.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class FileextraitTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Fileextrait.class);
        Fileextrait fileextrait1 = getFileextraitSample1();
        Fileextrait fileextrait2 = new Fileextrait();
        assertThat(fileextrait1).isNotEqualTo(fileextrait2);

        fileextrait2.setId(fileextrait1.getId());
        assertThat(fileextrait1).isEqualTo(fileextrait2);

        fileextrait2 = getFileextraitSample2();
        assertThat(fileextrait1).isNotEqualTo(fileextrait2);
    }
}
