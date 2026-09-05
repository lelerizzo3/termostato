package com.termostato.web.dto;

import java.math.BigDecimal;

public record CurrentStateResponse(
        BigDecimal temperatura,
        BigDecimal umidita,
        BigDecimal temperaturaTarget,
        boolean relayAcceso,
        BigDecimal temperaturaEsterna,
        BigDecimal umiditaEsterna) {
}
