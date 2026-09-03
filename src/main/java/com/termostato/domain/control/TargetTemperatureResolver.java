package com.termostato.domain.control;

import com.termostato.domain.model.Calendario;
import com.termostato.domain.model.IntervalloOrario;
import com.termostato.domain.model.SystemConfiguration;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

@Component
public class TargetTemperatureResolver {

    public Optional<BigDecimal> resolve(Instant instant, SystemConfiguration configuration, Calendario calendario) {
        if (configuration.overrideAttivo()) {
            return Optional.of(configuration.temperaturaOverride());
        }
        LocalDateTime utcDateTime = LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
        return calendario.per(utcDateTime.getDayOfWeek()).stream()
                .filter(interval -> interval.contiene(utcDateTime.toLocalTime()))
                .map(IntervalloOrario::temperaturaTarget)
                .findFirst();
    }
}
