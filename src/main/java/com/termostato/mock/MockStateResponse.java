package com.termostato.mock;

import java.math.BigDecimal;

public record MockStateResponse(BigDecimal temperatura, boolean relayAcceso) {
}
