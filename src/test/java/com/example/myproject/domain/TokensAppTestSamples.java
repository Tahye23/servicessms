package com.example.myproject.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class TokensAppTestSamples {

    private static final Random random = new Random();
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + (2 * Short.MAX_VALUE));

    public static TokensApp getTokensAppSample1() {
        return new TokensApp().id(1).token("token1");
    }

    public static TokensApp getTokensAppSample2() {
        return new TokensApp().id(2).token("token2");
    }

    public static TokensApp getTokensAppRandomSampleGenerator() {
        return new TokensApp().id(intCount.incrementAndGet()).token(UUID.randomUUID().toString());
    }
}
