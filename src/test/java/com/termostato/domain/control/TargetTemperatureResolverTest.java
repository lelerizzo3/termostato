package com.termostato.domain.control;

import com.termostato.domain.model.Calendario;
import com.termostato.domain.model.GiornoSettimana;
import com.termostato.domain.model.IntervalloOrario;
import com.termostato.domain.model.SystemConfiguration;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.EnumMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TargetTemperatureResolverTest {

    private final TargetTemperatureResolver resolver = new TargetTemperatureResolver();

    @Test
    void risolveIntervalloUsandoOrarioUtc() {
        Calendario calendario = calendarioConIntervallo(GiornoSettimana.GIOVEDI,
                new IntervalloOrario(LocalTime.of(6, 0), LocalTime.of(8, 0), new BigDecimal("20.5")));

        var result = resolver.resolve(Instant.parse("2026-09-03T06:30:00Z"), config(false, null), calendario);

        assertEquals(new BigDecimal("20.5"), result.orElseThrow());
    }

    @Test
    void fineIntervalloEsclusaEAssenzaTarget() {
        Calendario calendario = calendarioConIntervallo(GiornoSettimana.GIOVEDI,
                new IntervalloOrario(LocalTime.of(6, 0), LocalTime.of(8, 0), new BigDecimal("20.5")));

        var result = resolver.resolve(Instant.parse("2026-09-03T08:00:00Z"), config(false, null), calendario);

        assertTrue(result.isEmpty());
    }

    @Test
    void overrideIgnoraCalendario() {
        Calendario calendario = Calendario.vuoto();

        var result = resolver.resolve(Instant.parse("2026-09-03T06:30:00Z"),
                config(true, new BigDecimal("19.5")), calendario);

        assertEquals(new BigDecimal("19.5"), result.orElseThrow());
    }

    private static Calendario calendarioConIntervallo(GiornoSettimana giorno, IntervalloOrario intervallo) {
        EnumMap<GiornoSettimana, List<IntervalloOrario>> giorni = new EnumMap<>(GiornoSettimana.class);
        for (GiornoSettimana value : GiornoSettimana.values()) {
            giorni.put(value, value == giorno ? List.of(intervallo) : List.of());
        }
        return new Calendario(giorni);
    }

    private static SystemConfiguration config(boolean override, BigDecimal overrideTemperature) {
        return new SystemConfiguration(new BigDecimal("0.3"), override, overrideTemperature,
                60, 3, 30, "https://ntfy.sh", "sliverd", false,
                "http://sensor", "http://relay", "./data/test.db");
    }
}
