package com.termostato.external.temperature;

import com.termostato.domain.model.TemperatureRules;

import java.math.BigDecimal;

public record TemperatureReading(BigDecimal temperatura) {
    public TemperatureReading {
        temperatura = TemperatureRules.normalizeMeasured(temperatura);
    }
}
