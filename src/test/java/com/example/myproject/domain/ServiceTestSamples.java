package com.example.myproject.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class ServiceTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + (2 * Short.MAX_VALUE));

    public static Service getServiceSample1() {
        return new Service().id(1L).servNom("servNom1").servDescription("servDescription1").nbrusage(1);
    }

    public static Service getServiceSample2() {
        return new Service().id(2L).servNom("servNom2").servDescription("servDescription2").nbrusage(2);
    }

    public static Service getServiceRandomSampleGenerator() {
        return new Service()
            .id(longCount.incrementAndGet())
            .servNom(UUID.randomUUID().toString())
            .servDescription(UUID.randomUUID().toString())
            .nbrusage(intCount.incrementAndGet());
    }
}
