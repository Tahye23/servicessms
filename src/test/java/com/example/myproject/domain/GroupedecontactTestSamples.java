package com.example.myproject.domain;

import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

public class GroupedecontactTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    public static Groupedecontact getGroupedecontactSample1() {
        return new Groupedecontact().id(1L);
    }

    public static Groupedecontact getGroupedecontactSample2() {
        return new Groupedecontact().id(2L);
    }

    public static Groupedecontact getGroupedecontactRandomSampleGenerator() {
        return new Groupedecontact().id(longCount.incrementAndGet());
    }
}
