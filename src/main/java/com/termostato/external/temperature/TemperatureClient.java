package com.termostato.external.temperature;

import com.termostato.config.ConfigurationService;
import com.termostato.external.RestClientFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class TemperatureClient {

    private final RestClientFactory factory;
    private final ConfigurationService configuration;

    public TemperatureClient(RestClientFactory factory, ConfigurationService configuration) {
        this.factory = factory;
        this.configuration = configuration;
    }

    public BigDecimal leggiTemperatura() {
        TemperatureHttpApi api = factory.createProxy(configuration.current().sensoreUrl(), TemperatureHttpApi.class);
        TemperatureReading reading = api.leggiTemperatura();
        if (reading == null || reading.temperatura() == null) {
            throw new IllegalStateException("Risposta temperatura vuota");
        }
        return reading.temperatura();
    }
}
