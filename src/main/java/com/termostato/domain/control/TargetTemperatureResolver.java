package com.termostato.domain.control;

import com.termostato.domain.model.Calendario;
import com.termostato.domain.model.IntervalloOrario;
import com.termostato.domain.model.SystemConfiguration;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;

@Component
public class TargetTemperatureResolver {

    public Optional<BigDecimal> resolve(Instant instant, SystemConfiguration configuration, Calendario calendario) {
        if (configuration.overrideAttivo()) {
            return Optional.of(configuration.temperaturaOverride());
        }
        LocalDateTime localDateTime = localDateTime(instant, configuration);
        return calendario.per(localDateTime.getDayOfWeek()).stream()
                .filter(interval -> interval.contiene(localDateTime.toLocalTime()))
                .map(IntervalloOrario::temperaturaTarget)
                .findFirst();
    }

    private static LocalDateTime localDateTime(Instant instant, SystemConfiguration configuration) {
        ZoneId zone = ZoneId.of(configuration.fusoOrario());
        if (configuration.oraLegale()) {
            // ZoneId usa il database TZDB: il passaggio CET/CEST è automatico.
            return instant.atZone(zone).toLocalDateTime();
        }
        // Modalità esplicita senza ora legale: usa l'offset standard del fuso.
        ZoneOffset standardOffset = zone.getRules().getStandardOffset(instant);
        return LocalDateTime.ofInstant(instant, standardOffset);
    }
}
