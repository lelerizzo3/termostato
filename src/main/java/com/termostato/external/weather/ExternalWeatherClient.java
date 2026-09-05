package com.termostato.external.weather;

import com.termostato.config.ConfigurationService;
import com.termostato.domain.model.SystemConfiguration;
import com.termostato.external.RestClientFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

@Component
public class ExternalWeatherClient {

    private static final String CURRENT_VARIABLES = "temperature_2m,relative_humidity_2m";

    private final RestClientFactory factory;
    private final ConfigurationService configuration;

    public ExternalWeatherClient(RestClientFactory factory, ConfigurationService configuration) {
        this.factory = factory;
        this.configuration = configuration;
    }

    public WeatherReading leggiLettura() {
        SystemConfiguration config = configuration.current();
        OpenMeteoHttpApi api = factory.createProxy(config.meteoEsternoUrl(), OpenMeteoHttpApi.class);
        OpenMeteoResponse response = api.current(
                config.meteoEsternoLatitudine(),
                config.meteoEsternoLongitudine(),
                CURRENT_VARIABLES,
                "UTC");
        if (response == null || response.current() == null) {
            throw new IllegalStateException("Risposta meteo esterno vuota");
        }
        Map<String, Object> current = response.current();
        BigDecimal temperature = decimal(current.get("temperature_2m"));
        BigDecimal humidity = decimal(current.get("relative_humidity_2m"));
        if (temperature == null || humidity == null) {
            throw new IllegalStateException("Risposta meteo esterno priva di temperatura o umidità");
        }
        return new WeatherReading(temperature, humidity);
    }

    private static BigDecimal decimal(Object value) {
        if (value instanceof Number number) {
            return new BigDecimal(number.toString());
        }
        if (value instanceof String text && !text.isBlank()) {
            return new BigDecimal(text);
        }
        return null;
    }
}