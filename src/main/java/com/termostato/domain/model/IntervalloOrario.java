package com.termostato.domain.model;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Objects;

public record IntervalloOrario(
        LocalTime oraInizio,
        LocalTime oraFine,
        BigDecimal temperaturaTarget) {

    public IntervalloOrario {
        Objects.requireNonNull(oraInizio, "oraInizio è obbligatoria");
        Objects.requireNonNull(oraFine, "oraFine è obbligatoria");
        if (!oraInizio.isBefore(oraFine)) {
            throw new IllegalArgumentException("oraInizio deve essere precedente a oraFine");
        }
        temperaturaTarget = TemperatureRules.requireOneDecimal("temperaturaTarget", temperaturaTarget, false);
    }

    public boolean contiene(LocalTime time) {
        return !time.isBefore(oraInizio) && time.isBefore(oraFine);
    }
}
