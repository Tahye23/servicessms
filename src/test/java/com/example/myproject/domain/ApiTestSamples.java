package com.example.myproject.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class ApiTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + (2 * Short.MAX_VALUE));

    public static Api getApiSample1() {
        return new Api().id(1L).apiNom("apiNom1").apiUrl("apiUrl1").apiVersion(1);
    }

    public static Api getApiSample2() {
        return new Api().id(2L).apiNom("apiNom2").apiUrl("apiUrl2").apiVersion(2);
    }

    public static Api getApiRandomSampleGenerator() {
        return new Api()
            .id(longCount.incrementAndGet())
            .apiNom(UUID.randomUUID().toString())
            .apiUrl(UUID.randomUUID().toString())
            .apiVersion(intCount.incrementAndGet());
    }
}
