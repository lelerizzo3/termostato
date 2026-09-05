package com.termostato.persistence;

import java.math.BigDecimal;
import java.time.Instant;

public record PollingLogRecord(
        Long id,
        Instant dataOra,
        boolean caldaiaAccesa,
        BigDecimal temperaturaRilevata,
        BigDecimal umiditaRilevata,
        BigDecimal temperaturaTarget,
        boolean overrideAttivo,
        BigDecimal temperaturaOverride,
        BigDecimal temperaturaEsterna,
        BigDecimal umiditaEsterna) {

    /** Compatibilità con i record creati prima dell’aggiunta delle misure climatiche. */
    public PollingLogRecord(Long id,
                            Instant dataOra,
                            boolean caldaiaAccesa,
                            BigDecimal temperaturaRilevata,
                            BigDecimal temperaturaTarget,
                            boolean overrideAttivo,
                            BigDecimal temperaturaOverride) {
        this(id, dataOra, caldaiaAccesa, temperaturaRilevata, null, temperaturaTarget,
                overrideAttivo, temperaturaOverride, null, null);
    }
}
