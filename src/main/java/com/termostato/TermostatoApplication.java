package com.termostato;

import com.termostato.config.BootstrapProperties;
import com.termostato.mock.MockDeviceProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({BootstrapProperties.class, MockDeviceProperties.class})
public class TermostatoApplication {

    public static void main(String[] args) {
        SpringApplication.run(TermostatoApplication.class, args);
    }
}
