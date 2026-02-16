package com.example.myproject.domain;

import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

public class UserTokenApiTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    public static UserTokenApi getUserTokenApiSample1() {
        return new UserTokenApi().id(1L);
    }

    public static UserTokenApi getUserTokenApiSample2() {
        return new UserTokenApi().id(2L);
    }

    public static UserTokenApi getUserTokenApiRandomSampleGenerator() {
        return new UserTokenApi().id(longCount.incrementAndGet());
    }
}
