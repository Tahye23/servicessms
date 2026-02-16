package com.example.myproject.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class CompanyTestSamples {

    private static final Random random = new Random();
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + (2 * Short.MAX_VALUE));

    public static Company getCompanySample1() {
        return new Company().id(1).name("name1").activity("activity1").camtitre("camtitre1");
    }

    public static Company getCompanySample2() {
        return new Company().id(2).name("name2").activity("activity2").camtitre("camtitre2");
    }

    public static Company getCompanyRandomSampleGenerator() {
        return new Company()
            .id(intCount.incrementAndGet())
            .name(UUID.randomUUID().toString())
            .activity(UUID.randomUUID().toString())
            .camtitre(UUID.randomUUID().toString());
    }
}
