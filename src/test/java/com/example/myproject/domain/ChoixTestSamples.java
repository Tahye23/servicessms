package com.example.myproject.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class ChoixTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    public static Choix getChoixSample1() {
        return new Choix().id(1L).chovaleur("chovaleur1");
    }

    public static Choix getChoixSample2() {
        return new Choix().id(2L).chovaleur("chovaleur2");
    }

    public static Choix getChoixRandomSampleGenerator() {
        return new Choix().id(longCount.incrementAndGet()).chovaleur(UUID.randomUUID().toString());
    }
}
