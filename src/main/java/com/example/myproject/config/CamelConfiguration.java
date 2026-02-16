package com.example.myproject.config;

import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CamelConfiguration {

    @Bean
    public CamelContext camelContext() throws Exception {
        CamelContext context = new DefaultCamelContext();
        context.start();
        return context;
    }
}
