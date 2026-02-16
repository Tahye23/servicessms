package com.example.myproject.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class ReferentielTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    public static Referentiel getReferentielSample1() {
        return new Referentiel()
            .id(1L)
            .refCode("refCode1")
            .refRadical("refRadical1")
            .refFrTitle("refFrTitle1")
            .refArTitle("refArTitle1")
            .refEnTitle("refEnTitle1");
    }

    public static Referentiel getReferentielSample2() {
        return new Referentiel()
            .id(2L)
            .refCode("refCode2")
            .refRadical("refRadical2")
            .refFrTitle("refFrTitle2")
            .refArTitle("refArTitle2")
            .refEnTitle("refEnTitle2");
    }

    public static Referentiel getReferentielRandomSampleGenerator() {
        return new Referentiel()
            .id(longCount.incrementAndGet())
            .refCode(UUID.randomUUID().toString())
            .refRadical(UUID.randomUUID().toString())
            .refFrTitle(UUID.randomUUID().toString())
            .refArTitle(UUID.randomUUID().toString())
            .refEnTitle(UUID.randomUUID().toString());
    }
}
