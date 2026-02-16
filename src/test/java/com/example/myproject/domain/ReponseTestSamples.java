package com.example.myproject.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class ReponseTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    public static Reponse getReponseSample1() {
        return new Reponse().id(1L).repvaleur("repvaleur1");
    }

    public static Reponse getReponseSample2() {
        return new Reponse().id(2L).repvaleur("repvaleur2");
    }

    public static Reponse getReponseRandomSampleGenerator() {
        return new Reponse().id(longCount.incrementAndGet()).repvaleur(UUID.randomUUID().toString());
    }
}
