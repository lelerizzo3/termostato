package com.termostato.mock;

import com.termostato.external.relay.RelayCommand;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MockDeviceStateTest {

    @Test
    void mantieneTemperaturaEStatoRelaySoloNelMock() {
        MockDeviceProperties properties = new MockDeviceProperties();
        properties.setTemperature(new BigDecimal("19.0"));
        properties.setHumidity(new BigDecimal("50.0"));
        properties.setInitialRelayOn(false);
        MockDeviceState state = new MockDeviceState(properties);

        assertEquals(new BigDecimal("19.0"), state.temperatureReading().temperatura());
        assertEquals(new BigDecimal("50.0"), state.temperatureReading().umidita());
        assertFalse(state.relayOn());

        state.applyRelayCommand(new RelayCommand(true));
        state.setTemperature(new BigDecimal("21.0"));

        assertTrue(state.relayOn());
        assertEquals(new BigDecimal("21.0"), state.temperatureReading().temperatura());
    }
}
