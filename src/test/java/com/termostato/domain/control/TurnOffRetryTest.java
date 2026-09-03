package com.termostato.domain.control;

import com.termostato.config.ConfigurationService;
import com.termostato.domain.model.Calendario;
import com.termostato.domain.model.SystemConfiguration;
import com.termostato.external.notification.NotificationService;
import com.termostato.external.relay.RelayClient;
import com.termostato.external.temperature.TemperatureClient;
import com.termostato.persistence.ErrorLogRepository;
import com.termostato.persistence.PollingLogRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TurnOffRetryTest {

    @Test
    void retrySpegnimentoVieneEseguitoAdOgniCiclo() {
        ConfigurationService configuration = mock(ConfigurationService.class);
        TemperatureClient temperature = mock(TemperatureClient.class);
        RelayClient relay = mock(RelayClient.class);
        NotificationService notification = mock(NotificationService.class);
        PollingLogRepository polling = mock(PollingLogRepository.class);
        ErrorLogRepository errors = mock(ErrorLogRepository.class);
        SystemConfiguration config = new SystemConfiguration(
                new BigDecimal("0.3"), false, null, 60, 3, 30,
                "http://ntfy", "topic", false, "http://sensor", "http://relay", "./data/test.db");
        when(configuration.current()).thenReturn(config);
        when(configuration.currentCalendario()).thenReturn(Calendario.vuoto());
        when(relay.leggiStato()).thenReturn(true);
        when(temperature.leggiTemperatura()).thenReturn(new BigDecimal("21.0"));
        doThrow(new RuntimeException("offline")).when(relay).inviaComando(false);

        ThermostatControlService service = new ThermostatControlService(
                configuration, new TargetTemperatureResolver(), new HeatingDecisionCalculator(),
                new ErrorTrackingService(), temperature, relay, notification, polling, errors,
                Clock.fixed(Instant.parse("2026-09-03T06:30:00Z"), ZoneOffset.UTC));

        service.initializeRelayAtStartup();
        service.executePollingCycle();
        service.executePollingCycle();

        verify(relay, times(2)).inviaComando(false);
        verify(notification, times(2)).notificaErrore(any());
    }
}
