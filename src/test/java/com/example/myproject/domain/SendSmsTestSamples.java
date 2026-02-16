package com.example.myproject.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class SendSmsTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    public static SendSms getSendSmsSample1() {
        return new SendSms().id(1L).sender("sender1").receiver("receiver1").msgdata("msgdata1").dialogue("dialogue1").Titre("Titre1");
    }

    public static SendSms getSendSmsSample2() {
        return new SendSms().id(2L).sender("sender2").receiver("receiver2").msgdata("msgdata2").dialogue("dialogue2").Titre("Titre2");
    }

    public static SendSms getSendSmsRandomSampleGenerator() {
        return new SendSms()
            .id(longCount.incrementAndGet())
            .sender(UUID.randomUUID().toString())
            .receiver(UUID.randomUUID().toString())
            .msgdata(UUID.randomUUID().toString())
            .dialogue(UUID.randomUUID().toString())
            .Titre(UUID.randomUUID().toString());
    }
}
