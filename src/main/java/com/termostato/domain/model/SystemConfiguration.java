package com.termostato.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

public record SystemConfiguration(
        BigDecimal sogliaAttivazione,
        boolean overrideAttivo,
        BigDecimal temperaturaOverride,
        int intervalloPollingSecondi,
        int maxErroriConsecutivi,
        int retentionLogGiorni,
        String ntfyUrl,
        String ntfyTopic,
        boolean debugMode,
        String sensoreUrl,
        String relayUrl,
        String databasePath) {

    public SystemConfiguration {
        sogliaAttivazione = TemperatureRules.requireNonNegativeOneDecimal("sogliaAttivazione", sogliaAttivazione);
        if (overrideAttivo) {
            temperaturaOverride = TemperatureRules.requireOneDecimal("temperaturaOverride", temperaturaOverride, false);
        } else if (temperaturaOverride != null) {
            temperaturaOverride = TemperatureRules.requireOneDecimal("temperaturaOverride", temperaturaOverride, true);
        }
        if (intervalloPollingSecondi <= 0) {
            throw new IllegalArgumentException("intervalloPollingSecondi deve essere positivo");
        }
        if (maxErroriConsecutivi <= 0) {
            throw new IllegalArgumentException("maxErroriConsecutivi deve essere positivo");
        }
        if (retentionLogGiorni <= 0) {
            throw new IllegalArgumentException("retentionLogGiorni deve essere positivo");
        }
        ntfyUrl = requireText("ntfyUrl", ntfyUrl);
        ntfyTopic = requireText("ntfyTopic", ntfyTopic);
        sensoreUrl = requireText("sensoreUrl", sensoreUrl);
        relayUrl = requireText("relayUrl", relayUrl);
        databasePath = requireText("databasePath", databasePath);
    }

    private static String requireText(String field, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " è obbligatorio");
        }
        return value.trim();
    }
}
