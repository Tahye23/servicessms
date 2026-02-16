package com.example.myproject.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class GroupeTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    public static Groupe getGroupeSample1() {
        return new Groupe().id(1L).grotitre("grotitre1").user("user1");
    }

    public static Groupe getGroupeSample2() {
        return new Groupe().id(2L).grotitre("grotitre2").user("user2");
    }

    public static Groupe getGroupeRandomSampleGenerator() {
        return new Groupe().id(longCount.incrementAndGet()).grotitre(UUID.randomUUID().toString()).user(UUID.randomUUID().toString());
    }
}
