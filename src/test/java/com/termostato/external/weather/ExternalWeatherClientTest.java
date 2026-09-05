package com.termostato.external.weather;

import com.termostato.config.ConfigurationService;
import com.termostato.domain.model.SystemConfiguration;
import com.termostato.external.RestClientFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExternalWeatherClientTest {

    @Mock RestClientFactory factory;
    @Mock ConfigurationService configuration;
    @Mock OpenMeteoHttpApi api;

    @Test
    void leggeTemperaturaEUmiditaDaOpenMeteo() {
        BigDecimal latitude = new BigDecimal("37.6167");
        BigDecimal longitude = new BigDecimal("15.1667");
        SystemConfiguration config = new SystemConfiguration(
                new BigDecimal("0.3"), false, null, 60, 3, 30,
                "http://ntfy", "topic", false, "http://sensor", "http://relay", "./data/test.db",
                List.of(), "https://api.open-meteo.com", latitude, longitude, true);
        when(configuration.current()).thenReturn(config);
        when(factory.createProxy("https://api.open-meteo.com", OpenMeteoHttpApi.class)).thenReturn(api);
        Map<String, Object> current = Map.of(
                "time", "2026-09-05T07:00",
                "interval", 900,
                "temperature_2m", 18.74,
                "relative_humidity_2m", 68.56);
        when(api.current(latitude, longitude, "temperature_2m,relative_humidity_2m", "UTC"))
                .thenReturn(new OpenMeteoResponse(current));

        WeatherReading result = new ExternalWeatherClient(factory, configuration).leggiLettura();

        assertEquals(new BigDecimal("18.7"), result.temperatura());
        assertEquals(new BigDecimal("68.6"), result.umidita());
        verify(api).current(latitude, longitude, "temperature_2m,relative_humidity_2m", "UTC");
    }
}
