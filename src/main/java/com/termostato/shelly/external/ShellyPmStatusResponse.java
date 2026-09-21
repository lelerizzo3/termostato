package com.termostato.shelly.external;

import java.math.BigDecimal;

public record ShellyPmStatusResponse(
        BigDecimal apower,
        BigDecimal voltage,
        BigDecimal current) {
}
