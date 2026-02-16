package com.example.myproject.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class CustomerTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + (2 * Short.MAX_VALUE));

    public static Customer getCustomerSample1() {
        return new Customer()
            .id(1L)
            .customerId(1)
            .firstName("firstName1")
            .lastName("lastName1")
            .country("country1")
            .telephone("telephone1");
    }

    public static Customer getCustomerSample2() {
        return new Customer()
            .id(2L)
            .customerId(2)
            .firstName("firstName2")
            .lastName("lastName2")
            .country("country2")
            .telephone("telephone2");
    }

    public static Customer getCustomerRandomSampleGenerator() {
        return new Customer()
            .id(longCount.incrementAndGet())
            .customerId(intCount.incrementAndGet())
            .firstName(UUID.randomUUID().toString())
            .lastName(UUID.randomUUID().toString())
            .country(UUID.randomUUID().toString())
            .telephone(UUID.randomUUID().toString());
    }
}
