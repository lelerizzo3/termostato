package com.termostato.shelly.pm;

import java.math.BigDecimal;
import java.time.Instant;

public record ShellyConsumoLogRecord(
        Long id,
        Instant dataOra,
        String deviceId,
        BigDecimal potenzaW,
        BigDecimal tensioneV,
        BigDecimal correnteA) {
}
