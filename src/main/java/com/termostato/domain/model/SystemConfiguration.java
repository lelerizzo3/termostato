package com.termostato.domain.model;

import java.math.BigDecimal;
import java.util.List;
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
        String databasePath,
        List<String> apiKeys,
        String meteoEsternoUrl,
        BigDecimal meteoEsternoLatitudine,
        BigDecimal meteoEsternoLongitudine,
        boolean notificheErroriAbilitate) {

    public static final String DEFAULT_METEO_ESTERNO_URL = "https://api.open-meteo.com";
    public static final BigDecimal DEFAULT_METEO_ESTERNO_LATITUDINE = new BigDecimal("37.6167");
    public static final BigDecimal DEFAULT_METEO_ESTERNO_LONGITUDINE = new BigDecimal("15.1667");
    public static final boolean DEFAULT_NOTIFICHE_ERRORI_ABILITATE = true;

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
        apiKeys = normalizeApiKeys(apiKeys);
        meteoEsternoUrl = textOrDefault(meteoEsternoUrl, DEFAULT_METEO_ESTERNO_URL);
        meteoEsternoLatitudine = coordinate("meteoEsternoLatitudine", meteoEsternoLatitudine,
                new BigDecimal("-90"), new BigDecimal("90"), DEFAULT_METEO_ESTERNO_LATITUDINE);
        meteoEsternoLongitudine = coordinate("meteoEsternoLongitudine", meteoEsternoLongitudine,
                new BigDecimal("-180"), new BigDecimal("180"), DEFAULT_METEO_ESTERNO_LONGITUDINE);
    }

    /** Compatibilità con i costruttori usati dal dominio/test con API-key configurate. */
    public SystemConfiguration(BigDecimal sogliaAttivazione,
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
                               String databasePath,
                               List<String> apiKeys) {
        this(sogliaAttivazione, overrideAttivo, temperaturaOverride, intervalloPollingSecondi,
                maxErroriConsecutivi, retentionLogGiorni, ntfyUrl, ntfyTopic, debugMode,
                sensoreUrl, relayUrl, databasePath, apiKeys,
                DEFAULT_METEO_ESTERNO_URL, DEFAULT_METEO_ESTERNO_LATITUDINE,
                DEFAULT_METEO_ESTERNO_LONGITUDINE, DEFAULT_NOTIFICHE_ERRORI_ABILITATE);
    }

    /** Compatibilità con i costruttori usati dal dominio/test senza autenticazione configurata. */
    public SystemConfiguration(BigDecimal sogliaAttivazione,
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
        this(sogliaAttivazione, overrideAttivo, temperaturaOverride, intervalloPollingSecondi,
                maxErroriConsecutivi, retentionLogGiorni, ntfyUrl, ntfyTopic, debugMode,
                sensoreUrl, relayUrl, databasePath, List.of(),
                DEFAULT_METEO_ESTERNO_URL, DEFAULT_METEO_ESTERNO_LATITUDINE,
                DEFAULT_METEO_ESTERNO_LONGITUDINE, DEFAULT_NOTIFICHE_ERRORI_ABILITATE);
    }

    private static List<String> normalizeApiKeys(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .map(value -> {
                    if (value == null || value.isBlank()) {
                        throw new IllegalArgumentException("apiKeys non può contenere valori vuoti");
                    }
                    return value;
                })
                .distinct()
                .toList();
    }

    private static BigDecimal coordinate(String fieldName,
                                         BigDecimal value,
                                         BigDecimal minimum,
                                         BigDecimal maximum,
                                         BigDecimal fallback) {
        if (value == null) {
            return fallback;
        }
        if (value.compareTo(minimum) < 0 || value.compareTo(maximum) > 0) {
            throw new IllegalArgumentException(fieldName + " fuori intervallo");
        }
        return value;
    }

    private static String textOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static String requireText(String field, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " è obbligatorio");
        }
        return value.trim();
    }
}
