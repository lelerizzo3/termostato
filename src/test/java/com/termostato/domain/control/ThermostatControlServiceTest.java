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
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.EnumMap;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

    // 04:30Z corrisponde alle 06:30 civili di Europe/Rome (CEST, UTC+2) del 3 settembre 2026 (giovedì):
    // rientra nell'intervallo GIOVEDI 06:00-08:00 del calendario di test dopo la conversione di fuso.
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-03T04:30:00Z"), ZoneOffset.UTC);
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
    void forzaturaScadutaVieneDisattivataEPersistitaConNotifica() {
        // overrideFine 06:00 locale (Europe/Rome, CEST) = 04:00Z, precedente a now (04:30Z): scaduta.
        SystemConfiguration conForzaturaScaduta = configConOverride(new BigDecimal("22.0"),
                LocalDateTime.of(2026, 9, 3, 6, 0));
        when(configuration.current()).thenReturn(conForzaturaScaduta);
        when(relayClient.leggiStato()).thenReturn(true);
        when(temperatureClient.leggiLettura()).thenReturn(new TemperatureReading(new BigDecimal("20.3"), new BigDecimal("50.0")));

        service.initializeRelayAtStartup();
        service.executePollingCycle();

        // La forzatura viene disattivata e persistita (config senza override).
        verify(configuration).update(conForzaturaScaduta.senzaForzatura());
        verify(notificationService).notificaInformazione(
                "Forzatura terminata — ripristino della normale operatività basata sul calendario");
        // Ripristinato il calendario: 06:30 locale rientra in 06:00-08:00 (target 20.5); 20.3 è zona neutra.
        verify(relayClient, never()).inviaComando(any(Boolean.class));
        verify(pollingLogs).save(any());
    }

    @Test
    void forzaturaNonScadutaRestaAttivaSenzaPersistenza() {
        // overrideFine 07:00 locale = 05:00Z, successiva a now (04:30Z): ancora valida.
        SystemConfiguration conForzaturaValida = configConOverride(new BigDecimal("22.0"),
                LocalDateTime.of(2026, 9, 3, 7, 0));
        when(configuration.current()).thenReturn(conForzaturaValida);
        when(relayClient.leggiStato()).thenReturn(false);
        when(temperatureClient.leggiLettura()).thenReturn(new TemperatureReading(new BigDecimal("20.3"), new BigDecimal("50.0")));

        service.initializeRelayAtStartup();
        service.executePollingCycle();

        // Nessuna disattivazione: override attivo con target 22.0 e temperatura 20.3 -> accensione.
        verify(configuration, never()).update(any());
        verify(relayClient).inviaComando(eq(true));
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

    private SystemConfiguration configConOverride(BigDecimal temperaturaOverride, LocalDateTime overrideFine) {
        return new SystemConfiguration(
                new BigDecimal("0.3"), true, temperaturaOverride, 60, 2, 30,
                "http://ntfy", "topic", false, "http://sensor", "http://relay", "./data/test.db",
                List.of(), SystemConfiguration.DEFAULT_METEO_ESTERNO_URL,
                SystemConfiguration.DEFAULT_METEO_ESTERNO_LATITUDINE,
                SystemConfiguration.DEFAULT_METEO_ESTERNO_LONGITUDINE,
                true, SystemConfiguration.DEFAULT_FUSO_ORARIO, SystemConfiguration.DEFAULT_ORA_LEGALE,
                overrideFine);
    }
}
