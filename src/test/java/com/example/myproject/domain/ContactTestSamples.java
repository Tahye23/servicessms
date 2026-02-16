package com.example.myproject.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class ContactTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + (2 * Short.MAX_VALUE));

    public static Contact getContactSample1() {
        return new Contact().id(1L).conid(1).connom("connom1").conprenom("conprenom1").contelephone("contelephone1").statuttraitement(1);
    }

    public static Contact getContactSample2() {
        return new Contact().id(2L).conid(2).connom("connom2").conprenom("conprenom2").contelephone("contelephone2").statuttraitement(2);
    }

    public static Contact getContactRandomSampleGenerator() {
        return new Contact()
            .id(longCount.incrementAndGet())
            .conid(intCount.incrementAndGet())
            .connom(UUID.randomUUID().toString())
            .conprenom(UUID.randomUUID().toString())
            .contelephone(UUID.randomUUID().toString())
            .statuttraitement(intCount.incrementAndGet());
    }
}
