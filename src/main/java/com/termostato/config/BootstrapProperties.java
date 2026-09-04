package com.termostato.config;

import com.termostato.domain.model.SystemConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;
import java.math.RoundingMode;

@ConfigurationProperties(prefix = "termostato")
public class BootstrapProperties {

    private BigDecimal sogliaAttivazione = new BigDecimal("0.3");
    private boolean overrideAttivo;
    private BigDecimal temperaturaOverride;
    private int intervalloPollingSecondi = 60;
    private int maxErroriConsecutivi = 3;
    private int retentionLogGiorni = 30;
    private String ntfyUrl = "https://ntfy.sh";
    private String ntfyTopic = "sliverd";
    private boolean debugMode;
    private EndpointProperties sensore = new EndpointProperties("http://sensore.local");
    private EndpointProperties relay = new EndpointProperties("http://relay.local");
    private String configFile = "./data/config.json";
    private String calendarioFile = "./data/calendario.json";
    private String databasePath = "./data/termostato.db";
    private List<String> apiKeys = new ArrayList<>();
    private int httpTimeoutMillis = 3000;
    private int schedulerTickMillis = 1000;

    public SystemConfiguration defaults() {
        BigDecimal threshold = validTemperature(sogliaAttivazione, new BigDecimal("0.3"));
        boolean effectiveOverride = overrideAttivo && temperaturaOverride != null;
        BigDecimal effectiveOverrideTemperature = effectiveOverride
                ? validTemperature(temperaturaOverride, new BigDecimal("20.0"))
                : null;
        return new SystemConfiguration(
                threshold,
                effectiveOverride,
                effectiveOverrideTemperature,
                positiveOrDefault(intervalloPollingSecondi, 60),
                positiveOrDefault(maxErroriConsecutivi, 3),
                positiveOrDefault(retentionLogGiorni, 30),
                textOrDefault(ntfyUrl, "https://ntfy.sh"),
                textOrDefault(ntfyTopic, "sliverd"),
                debugMode,
                textOrDefault(sensore == null ? null : sensore.getUrl(), "http://sensore.local"),
                textOrDefault(relay == null ? null : relay.getUrl(), "http://relay.local"),
                textOrDefault(databasePath, "./data/termostato.db"),
                apiKeys);
    }

    private static BigDecimal validTemperature(BigDecimal value, BigDecimal fallback) {
        if (value == null || value.signum() < 0 || value.scale() > 1) {
            return fallback;
        }
        return value.setScale(1, RoundingMode.UNNECESSARY);
    }

    private static int positiveOrDefault(int value, int fallback) {
        return value > 0 ? value : fallback;
    }

    private static String textOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    public BigDecimal getSogliaAttivazione() { return sogliaAttivazione; }
    public void setSogliaAttivazione(BigDecimal value) { this.sogliaAttivazione = value; }
    public boolean isOverrideAttivo() { return overrideAttivo; }
    public void setOverrideAttivo(boolean value) { this.overrideAttivo = value; }
    public BigDecimal getTemperaturaOverride() { return temperaturaOverride; }
    public void setTemperaturaOverride(BigDecimal value) { this.temperaturaOverride = value; }
    public int getIntervalloPollingSecondi() { return intervalloPollingSecondi; }
    public void setIntervalloPollingSecondi(int value) { this.intervalloPollingSecondi = value; }
    public int getMaxErroriConsecutivi() { return maxErroriConsecutivi; }
    public void setMaxErroriConsecutivi(int value) { this.maxErroriConsecutivi = value; }
    public int getRetentionLogGiorni() { return retentionLogGiorni; }
    public void setRetentionLogGiorni(int value) { this.retentionLogGiorni = value; }
    public String getNtfyUrl() { return ntfyUrl; }
    public void setNtfyUrl(String value) { this.ntfyUrl = value; }
    public String getNtfyTopic() { return ntfyTopic; }
    public void setNtfyTopic(String value) { this.ntfyTopic = value; }
    public boolean isDebugMode() { return debugMode; }
    public void setDebugMode(boolean value) { this.debugMode = value; }
    public EndpointProperties getSensore() { return sensore; }
    public void setSensore(EndpointProperties value) { this.sensore = value; }
    public EndpointProperties getRelay() { return relay; }
    public void setRelay(EndpointProperties value) { this.relay = value; }
    public String getConfigFile() { return configFile; }
    public void setConfigFile(String value) { this.configFile = value; }
    public String getCalendarioFile() { return calendarioFile; }
    public void setCalendarioFile(String value) { this.calendarioFile = value; }
    public String getDatabasePath() { return databasePath; }
    public void setDatabasePath(String value) { this.databasePath = value; }
    public List<String> getApiKeys() { return apiKeys; }
    public void setApiKeys(List<String> value) { this.apiKeys = value == null ? new ArrayList<>() : new ArrayList<>(value); }
    public int getHttpTimeoutMillis() { return httpTimeoutMillis; }
    public void setHttpTimeoutMillis(int value) { this.httpTimeoutMillis = value; }
    public int getSchedulerTickMillis() { return schedulerTickMillis; }
    public void setSchedulerTickMillis(int value) { this.schedulerTickMillis = value; }

    public static class EndpointProperties {
        private String url;

        public EndpointProperties() {
        }

        public EndpointProperties(String url) {
            this.url = url;
        }

        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
    }
}
