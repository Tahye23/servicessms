package com.example.myproject.domain;

import static com.example.myproject.domain.OTPStorageTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.example.myproject.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class OTPStorageTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(OTPStorage.class);
        OTPStorage oTPStorage1 = getOTPStorageSample1();
        OTPStorage oTPStorage2 = new OTPStorage();
        assertThat(oTPStorage1).isNotEqualTo(oTPStorage2);

        oTPStorage2.setId(oTPStorage1.getId());
        assertThat(oTPStorage1).isEqualTo(oTPStorage2);

        oTPStorage2 = getOTPStorageSample2();
        assertThat(oTPStorage1).isNotEqualTo(oTPStorage2);
    }
}
