package com.termostato.external.weather;

import com.termostato.domain.model.TemperatureRules;

import java.math.BigDecimal;

public record WeatherReading(
        BigDecimal temperatura,
        BigDecimal umidita) {

    public WeatherReading {
        temperatura = TemperatureRules.normalizeMeasured(temperatura);
        umidita = TemperatureRules.normalizeHumidity(umidita);
    }
}
