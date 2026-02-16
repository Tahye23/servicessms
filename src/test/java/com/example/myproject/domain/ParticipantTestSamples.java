package com.example.myproject.domain;

import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

public class ParticipantTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    public static Participant getParticipantSample1() {
        return new Participant().id(1L);
    }

    public static Participant getParticipantSample2() {
        return new Participant().id(2L);
    }

    public static Participant getParticipantRandomSampleGenerator() {
        return new Participant().id(longCount.incrementAndGet());
    }
}
