package com.example.myproject.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class OTPStorageTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    public static OTPStorage getOTPStorageSample1() {
        return new OTPStorage().id(1L).otsOTP("otsOTP1").phoneNumber("phoneNumber1");
    }

    public static OTPStorage getOTPStorageSample2() {
        return new OTPStorage().id(2L).otsOTP("otsOTP2").phoneNumber("phoneNumber2");
    }

    public static OTPStorage getOTPStorageRandomSampleGenerator() {
        return new OTPStorage()
            .id(longCount.incrementAndGet())
            .otsOTP(UUID.randomUUID().toString())
            .phoneNumber(UUID.randomUUID().toString());
    }
}
