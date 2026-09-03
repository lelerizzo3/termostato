package com.termostato.persistence;

import java.math.BigDecimal;
import java.time.Instant;

public record ErrorLogRecord(
        Long id,
        Instant dataOra,
        String tipoErrore,
        Boolean caldaiaAccesa,
        BigDecimal temperaturaRilevata,
        int numErroriConsecutivi) {
}
