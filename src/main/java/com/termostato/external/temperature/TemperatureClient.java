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

    public TemperatureReading leggiLettura() {
        TemperatureHttpApi api = factory.createProxy(configuration.current().sensoreUrl(), TemperatureHttpApi.class, true);
        TemperatureReading reading = api.leggiTemperatura();
        if (reading == null || reading.temperatura() == null || reading.umidita() == null) {
            throw new IllegalStateException("Risposta sensore priva di temperatura o umidità");
        }
        return reading;
    }

    /** Compatibilità per i client che richiedono solo la temperatura. */
    public BigDecimal leggiTemperatura() {
        return leggiLettura().temperatura();
    }
}
