package com.termostato.domain.control;

import com.termostato.config.ConfigurationService;
import com.termostato.domain.model.Calendario;
import com.termostato.domain.model.GiornoSettimana;
import com.termostato.domain.model.IntervalloOrario;
import com.termostato.domain.model.SystemConfiguration;
import com.termostato.external.notification.NotificationService;
import com.termostato.external.relay.RelayClient;
import com.termostato.external.temperature.TemperatureClient;
import com.termostato.external.temperature.TemperatureReading;
import com.termostato.external.weather.ExternalWeatherClient;
import com.termostato.external.weather.WeatherReading;
import com.termostato.persistence.ErrorLogRepository;
import com.termostato.persistence.PollingLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.EnumMap;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThermostatControlServiceTest {

    @Mock ConfigurationService configuration;
    @Mock TemperatureClient temperatureClient;
    @Mock ExternalWeatherClient externalWeatherClient;
    @Mock RelayClient relayClient;
    @Mock NotificationService notificationService;
    @Mock PollingLogRepository pollingLogs;
    @Mock ErrorLogRepository errorLogs;

    private final Clock clock = Clock.fixed(Instant.parse("2026-09-03T06:30:00Z"), ZoneOffset.UTC);
    private final ErrorTrackingService errors = new ErrorTrackingService();
    private final TargetTemperatureResolver resolver = new TargetTemperatureResolver();
    private final HeatingDecisionCalculator calculator = new HeatingDecisionCalculator();
    private final SystemConfiguration config = new SystemConfiguration(
            new BigDecimal("0.3"), false, null, 60, 2, 30,
            "http://ntfy", "topic", false, "http://sensor", "http://relay", "./data/test.db");
    private Calendario calendar;
    private ThermostatControlService service;

    @BeforeEach
    void setUp() {
        EnumMap<GiornoSettimana, List<IntervalloOrario>> days = new EnumMap<>(GiornoSettimana.class);
        for (GiornoSettimana day : GiornoSettimana.values()) {
            days.put(day, day == GiornoSettimana.GIOVEDI
                    ? List.of(new IntervalloOrario(LocalTime.of(6, 0), LocalTime.of(8, 0), new BigDecimal("20.5")))
                    : List.of());
        }
        calendar = new Calendario(days);
        when(configuration.current()).thenReturn(config);
        lenient().when(configuration.currentCalendario()).thenReturn(calendar);
        service = new ThermostatControlService(configuration, resolver, calculator, errors,
                temperatureClient, externalWeatherClient, relayClient, notificationService, pollingLogs, errorLogs, clock);
    }

    @Test
    void leggeRelayAllAvvioEUsaIlRelayPerLaZonaNeutra() {
        when(relayClient.leggiStato()).thenReturn(true);
        when(temperatureClient.leggiLettura()).thenReturn(new TemperatureReading(new BigDecimal("20.3"), new BigDecimal("50.0")));

        service.initializeRelayAtStartup();
        service.executePollingCycle();

        verify(relayClient, org.mockito.Mockito.times(2)).leggiStato();
        verify(relayClient, never()).inviaComando(any(Boolean.class));
        verify(pollingLogs).save(any());
    }

    @Test
    void assenzaTargetInviaSpegnimentoSeRelayAcceso() {
        when(configuration.currentCalendario()).thenReturn(Calendario.vuoto());
        when(relayClient.leggiStato()).thenReturn(true, true);
        when(temperatureClient.leggiLettura()).thenReturn(new TemperatureReading(new BigDecimal("21.0"), new BigDecimal("50.0")));

        service.initializeRelayAtStartup();
        service.executePollingCycle();

        verify(relayClient).inviaComando(false);
        verify(pollingLogs).save(any());
    }

    @Test
    void raggiuntaSogliaErroriTentaSpegnimentoDiSicurezza() {
        when(relayClient.leggiStato()).thenReturn(false);
        when(temperatureClient.leggiLettura()).thenThrow(new RuntimeException("timeout"));

        service.initializeRelayAtStartup();
        service.executePollingCycle();
        service.executePollingCycle();

        verify(relayClient).inviaComando(false);
        verify(errorLogs, org.mockito.Mockito.times(2)).save(any());
    }
}
