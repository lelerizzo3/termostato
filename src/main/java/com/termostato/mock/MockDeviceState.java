package com.termostato.mock;

import com.termostato.domain.model.TemperatureRules;
import com.termostato.external.relay.RelayCommand;
import com.termostato.external.temperature.TemperatureReading;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Component
@ConditionalOnProperty(prefix = "termostato.mock-devices", name = "enabled", havingValue = "true")
public class MockDeviceState {

    private final AtomicReference<BigDecimal> temperature;
    private final AtomicBoolean relayOn;

    public MockDeviceState(MockDeviceProperties properties) {
        this.temperature = new AtomicReference<>(
                TemperatureRules.normalizeMeasured(properties.getTemperature()));
        this.relayOn = new AtomicBoolean(properties.isInitialRelayOn());
    }

    public TemperatureReading temperatureReading() {
        return new TemperatureReading(temperature.get());
    }

    public boolean relayOn() {
        return relayOn.get();
    }

    public void setTemperature(BigDecimal value) {
        temperature.set(TemperatureRules.normalizeMeasured(value));
    }

    public void applyRelayCommand(RelayCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Il comando relay è obbligatorio");
        }
        relayOn.set(command.acceso());
    }

    public void reset(MockDeviceProperties properties) {
        setTemperature(properties.getTemperature());
        relayOn.set(properties.isInitialRelayOn());
    }
}
