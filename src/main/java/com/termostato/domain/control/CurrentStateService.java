package com.termostato.domain.control;

import com.termostato.config.ConfigurationService;
import com.termostato.domain.model.CurrentState;
import com.termostato.domain.model.SystemConfiguration;
import com.termostato.external.relay.RelayClient;
import com.termostato.external.temperature.TemperatureClient;
import com.termostato.external.temperature.TemperatureReading;
import com.termostato.external.weather.ExternalWeatherClient;
import com.termostato.external.weather.WeatherReading;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;

@Service
public class CurrentStateService {

    private final ConfigurationService configuration;
    private final TargetTemperatureResolver targetResolver;
    private final TemperatureClient temperatureClient;
    private final ExternalWeatherClient externalWeatherClient;
    private final RelayClient relayClient;
    private final Clock clock;

    public CurrentStateService(ConfigurationService configuration,
                               TargetTemperatureResolver targetResolver,
                               TemperatureClient temperatureClient,
                               ExternalWeatherClient externalWeatherClient,
                               RelayClient relayClient,
                               Clock clock) {
        this.configuration = configuration;
        this.targetResolver = targetResolver;
        this.temperatureClient = temperatureClient;
        this.externalWeatherClient = externalWeatherClient;
        this.relayClient = relayClient;
        this.clock = clock;
    }

    public CurrentState read() {
        Instant now = clock.instant();
        SystemConfiguration currentConfiguration = configuration.current();
        TemperatureReading indoor = temperatureClient.leggiLettura();
        BigDecimal target = targetResolver
                .resolve(now, currentConfiguration, configuration.currentCalendario())
                .orElse(null);
        boolean relayOn = relayClient.leggiStato();
        WeatherReading outdoor = externalWeatherClient.leggiLettura();
        return new CurrentState(
                indoor.temperatura(), indoor.umidita(), target, relayOn,
                outdoor.temperatura(), outdoor.umidita());
    }
}
