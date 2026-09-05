package com.termostato.domain.model;

import java.math.BigDecimal;

public record CurrentState(
        BigDecimal temperatura,
        BigDecimal umidita,
        BigDecimal temperaturaTarget,
        boolean relayAcceso,
        BigDecimal temperaturaEsterna,
        BigDecimal umiditaEsterna) {
}
