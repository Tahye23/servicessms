package com.example.myproject.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class FileextraitTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    public static Fileextrait getFileextraitSample1() {
        return new Fileextrait()
            .id(1L)
            .fexidfile(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .fexparent("fexparent1")
            .fextype("fextype1")
            .fexname("fexname1");
    }

    public static Fileextrait getFileextraitSample2() {
        return new Fileextrait()
            .id(2L)
            .fexidfile(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .fexparent("fexparent2")
            .fextype("fextype2")
            .fexname("fexname2");
    }

    public static Fileextrait getFileextraitRandomSampleGenerator() {
        return new Fileextrait()
            .id(longCount.incrementAndGet())
            .fexidfile(UUID.randomUUID())
            .fexparent(UUID.randomUUID().toString())
            .fextype(UUID.randomUUID().toString())
            .fexname(UUID.randomUUID().toString());
    }
}
