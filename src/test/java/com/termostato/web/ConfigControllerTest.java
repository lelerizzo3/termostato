package com.termostato.web;

import com.termostato.config.ConfigurationService;
import com.termostato.domain.model.SystemConfiguration;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConfigControllerTest {

    // now = 06:30 civili di Europe/Rome (CEST) del 12 settembre 2026.
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-12T04:30:00Z"), ZoneOffset.UTC);
    private final ConfigurationService configuration = mock(ConfigurationService.class);
    private final ConfigController controller = new ConfigController(configuration, clock);

    @Test
    void overrideFinePassatoRestituisce400() {
        // 06:00 locale = 04:00Z, precedente a now (04:30Z): scaduto -> deve essere rifiutato.
        SystemConfiguration requested = configConOverride(LocalDateTime.of(2026, 9, 12, 6, 0));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> controller.updateConfiguration(requested));

        assertEquals("override_fine deve essere un istante futuro", error.getMessage());
        verify(configuration, never()).update(any());
    }

    @Test
    void overrideFineFuturoVieneAccettato() {
        // 08:00 locale = 06:00Z, successivo a now (04:30Z): valido.
        SystemConfiguration requested = configConOverride(LocalDateTime.of(2026, 9, 12, 8, 0));
        when(configuration.current()).thenReturn(requested);

        controller.updateConfiguration(requested);

        verify(configuration).update(requested);
    }

    @Test
    void overrideAttivoSenzaFineVieneAccettato() {
        SystemConfiguration requested = configConOverride(null);
        when(configuration.current()).thenReturn(requested);

        controller.updateConfiguration(requested);

        verify(configuration).update(requested);
    }

    private SystemConfiguration configConOverride(LocalDateTime overrideFine) {
        return new SystemConfiguration(
                new BigDecimal("0.3"), true, new BigDecimal("21.0"), 60, 3, 30,
                "https://ntfy.sh", "topic", false, "http://sensor", "http://relay", "./data/test.db",
                List.of(), SystemConfiguration.DEFAULT_METEO_ESTERNO_URL,
                SystemConfiguration.DEFAULT_METEO_ESTERNO_LATITUDINE,
                SystemConfiguration.DEFAULT_METEO_ESTERNO_LONGITUDINE,
                true, SystemConfiguration.DEFAULT_FUSO_ORARIO, SystemConfiguration.DEFAULT_ORA_LEGALE,
                overrideFine);
    }
}
