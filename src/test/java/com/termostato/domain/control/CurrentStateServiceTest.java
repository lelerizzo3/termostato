package com.termostato.domain.control;

import com.termostato.config.ConfigurationService;
import com.termostato.domain.model.Calendario;
import com.termostato.domain.model.CurrentState;
import com.termostato.domain.model.GiornoSettimana;
import com.termostato.domain.model.IntervalloOrario;
import com.termostato.domain.model.SystemConfiguration;
import com.termostato.external.relay.RelayClient;
import com.termostato.external.temperature.TemperatureClient;
import com.termostato.external.temperature.TemperatureReading;
import com.termostato.external.weather.ExternalWeatherClient;
import com.termostato.external.weather.WeatherReading;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrentStateServiceTest {

    @Mock ConfigurationService configuration;
    @Mock TemperatureClient temperatureClient;
    @Mock ExternalWeatherClient externalWeatherClient;
    @Mock RelayClient relayClient;

    private final Clock clock = Clock.fixed(Instant.parse("2026-09-03T06:30:00Z"), ZoneOffset.UTC);
    private final TargetTemperatureResolver targetResolver = new TargetTemperatureResolver();
    private CurrentStateService service;

    @BeforeEach
    void setUp() {
        service = new CurrentStateService(
                configuration, targetResolver, temperatureClient, externalWeatherClient, relayClient, clock);
        lenient().when(externalWeatherClient.leggiLettura())
                .thenReturn(new WeatherReading(new BigDecimal("15.0"), new BigDecimal("70.0")));
    }

    @Test
    void leggeTemperaturaTargetOverrideEStatoRelay() {
        SystemConfiguration config = config(true, new BigDecimal("20.5"));
        when(configuration.current()).thenReturn(config);
        when(configuration.currentCalendario()).thenReturn(Calendario.vuoto());
        when(temperatureClient.leggiLettura()).thenReturn(new TemperatureReading(new BigDecimal("19.0"), new BigDecimal("50.0")));
        when(relayClient.leggiStato()).thenReturn(true);

        CurrentState result = service.read();

        assertEquals(new BigDecimal("19.0"), result.temperatura());
        assertEquals(new BigDecimal("50.0"), result.umidita());
        assertEquals(new BigDecimal("20.5"), result.temperaturaTarget());
        assertEquals(true, result.relayAcceso());
        assertEquals(new BigDecimal("15.0"), result.temperaturaEsterna());
        assertEquals(new BigDecimal("70.0"), result.umiditaEsterna());
        verify(temperatureClient).leggiLettura();
        verify(externalWeatherClient).leggiLettura();
        verify(relayClient).leggiStato();
    }

    @Test
    void risolveTargetDalCalendarioUtcQuandoOverrideDisattivo() {
        when(configuration.current()).thenReturn(config(false, null));
        when(configuration.currentCalendario()).thenReturn(calendarWithTarget());
        when(temperatureClient.leggiLettura()).thenReturn(new TemperatureReading(new BigDecimal("19.0"), new BigDecimal("50.0")));
        when(relayClient.leggiStato()).thenReturn(false);

        CurrentState result = service.read();

        assertEquals(new BigDecimal("20.5"), result.temperaturaTarget());
        assertEquals(false, result.relayAcceso());
    }

    @Test
    void restituisceTargetNulloSenzaOverrideEIntervalloAttivo() {
        when(configuration.current()).thenReturn(config(false, null));
        when(configuration.currentCalendario()).thenReturn(Calendario.vuoto());
        when(temperatureClient.leggiLettura()).thenReturn(new TemperatureReading(new BigDecimal("19.0"), new BigDecimal("50.0")));
        when(relayClient.leggiStato()).thenReturn(false);

        CurrentState result = service.read();

        assertNull(result.temperaturaTarget());
    }

    @Test
    void propagaErroreDiLetturaTemperatura() {
        when(configuration.current()).thenReturn(config(false, null));
        when(temperatureClient.leggiLettura()).thenThrow(new IllegalStateException("timeout"));

        assertThrows(IllegalStateException.class, service::read);
    }

    private static SystemConfiguration config(boolean override, BigDecimal overrideTemperature) {
        return new SystemConfiguration(new BigDecimal("0.3"), override, overrideTemperature,
                60, 3, 30, "https://ntfy.sh", "sliverd", false,
                "http://sensor", "http://relay", "./data/test.db");
    }

    private static Calendario calendarWithTarget() {
        EnumMap<GiornoSettimana, List<IntervalloOrario>> days = new EnumMap<>(GiornoSettimana.class);
        for (GiornoSettimana day : GiornoSettimana.values()) {
            days.put(day, day == GiornoSettimana.GIOVEDI
                    ? List.of(new IntervalloOrario(LocalTime.of(6, 0), LocalTime.of(8, 0), new BigDecimal("20.5")))
                    : List.of());
        }
        return new Calendario(days);
    }
}
