package com.termostato.mock;

import java.math.BigDecimal;

public record MockStateResponse(
        BigDecimal temperatura,
        BigDecimal umidita,
        boolean relayAcceso) {
}
