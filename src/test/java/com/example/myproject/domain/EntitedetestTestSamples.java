package com.example.myproject.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class EntitedetestTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + (2 * Short.MAX_VALUE));

    public static Entitedetest getEntitedetestSample1() {
        return new Entitedetest().id(1L).identite(1).nom("nom1").nombrec(1);
    }

    public static Entitedetest getEntitedetestSample2() {
        return new Entitedetest().id(2L).identite(2).nom("nom2").nombrec(2);
    }

    public static Entitedetest getEntitedetestRandomSampleGenerator() {
        return new Entitedetest()
            .id(longCount.incrementAndGet())
            .identite(intCount.incrementAndGet())
            .nom(UUID.randomUUID().toString())
            .nombrec(intCount.incrementAndGet());
    }
}
