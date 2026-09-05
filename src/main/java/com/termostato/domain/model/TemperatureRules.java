package com.termostato.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class TemperatureRules {

    private TemperatureRules() {
    }

    public static BigDecimal requireOneDecimal(String fieldName, BigDecimal value, boolean nullable) {
        if (value == null) {
            if (nullable) {
                return null;
            }
            throw new IllegalArgumentException(fieldName + " è obbligatoria");
        }
        if (value.scale() > 1) {
            throw new IllegalArgumentException(fieldName + " deve avere una sola cifra decimale");
        }
        return value.setScale(1, RoundingMode.UNNECESSARY);
    }

    public static BigDecimal requireNonNegativeOneDecimal(String fieldName, BigDecimal value) {
        BigDecimal normalized = requireOneDecimal(fieldName, value, false);
        if (normalized.signum() < 0) {
            throw new IllegalArgumentException(fieldName + " non può essere negativa");
        }
        return normalized;
    }

    public static BigDecimal normalizeHumidity(BigDecimal value) {
        if (value == null) {
            throw new IllegalArgumentException("L'umidità rilevata è obbligatoria");
        }
        if (value.signum() < 0 || value.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("L'umidità rilevata deve essere compresa tra 0 e 100");
        }
        return value.setScale(1, RoundingMode.HALF_UP);
    }

    public static BigDecimal normalizeMeasured(BigDecimal value) {
        if (value == null) {
            throw new IllegalArgumentException("La temperatura rilevata è obbligatoria");
        }
        return value.setScale(1, RoundingMode.HALF_UP);
    }
}
