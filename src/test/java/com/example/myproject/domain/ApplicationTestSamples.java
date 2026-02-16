package com.example.myproject.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class ApplicationTestSamples {

    private static final Random random = new Random();
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + (2 * Short.MAX_VALUE));

    public static Application getApplicationSample1() {
        return new Application().id(1).name("name1").description("description1").userId(1);
    }

    public static Application getApplicationSample2() {
        return new Application().id(2).name("name2").description("description2").userId(2);
    }

    public static Application getApplicationRandomSampleGenerator() {
        return new Application()
            .id(intCount.incrementAndGet())
            .name(UUID.randomUUID().toString())
            .description(UUID.randomUUID().toString())
            .userId(intCount.incrementAndGet());
    }
}
