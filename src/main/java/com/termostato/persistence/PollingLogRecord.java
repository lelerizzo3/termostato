package com.termostato.persistence;

import java.math.BigDecimal;
import java.time.Instant;

public record PollingLogRecord(
        Long id,
        Instant dataOra,
        boolean caldaiaAccesa,
        BigDecimal temperaturaRilevata,
        BigDecimal temperaturaTarget,
        boolean overrideAttivo,
        BigDecimal temperaturaOverride) {
}
