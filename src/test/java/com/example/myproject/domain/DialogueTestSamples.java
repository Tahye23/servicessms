package com.example.myproject.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class DialogueTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + (2 * Short.MAX_VALUE));

    public static Dialogue getDialogueSample1() {
        return new Dialogue().id(1L).dialogueId(1).contenu("contenu1");
    }

    public static Dialogue getDialogueSample2() {
        return new Dialogue().id(2L).dialogueId(2).contenu("contenu2");
    }

    public static Dialogue getDialogueRandomSampleGenerator() {
        return new Dialogue().id(longCount.incrementAndGet()).dialogueId(intCount.incrementAndGet()).contenu(UUID.randomUUID().toString());
    }
}
