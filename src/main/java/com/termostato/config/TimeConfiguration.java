package com.termostato.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneOffset;

@Configuration
public class TimeConfiguration {

    @Bean
    public Clock applicationClock() {
        return Clock.system(ZoneOffset.UTC);
    }
}
