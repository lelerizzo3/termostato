package com.termostato.mock;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

@ConfigurationProperties(prefix = "termostato.mock-devices")
public class MockDeviceProperties {

    private boolean enabled;
    private BigDecimal temperature = new BigDecimal("19.0");
    private boolean initialRelayOn;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public BigDecimal getTemperature() {
        return temperature;
    }

    public void setTemperature(BigDecimal temperature) {
        this.temperature = temperature;
    }

    public boolean isInitialRelayOn() {
        return initialRelayOn;
    }

    public void setInitialRelayOn(boolean initialRelayOn) {
        this.initialRelayOn = initialRelayOn;
    }
}
