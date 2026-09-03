package com.termostato.mock;

import com.termostato.external.relay.RelayCommand;
import com.termostato.external.relay.RelayStateResponse;
import com.termostato.external.temperature.TemperatureReading;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ConditionalOnProperty(prefix = "termostato.mock-devices", name = "enabled", havingValue = "true")
public class MockDeviceController {

    private final MockDeviceState state;
    private final MockDeviceProperties properties;

    public MockDeviceController(MockDeviceState state, MockDeviceProperties properties) {
        this.state = state;
        this.properties = properties;
    }

    /** Contratto usato dal TemperatureHttpApi del controllo. */
    @GetMapping("/temperature")
    public TemperatureReading temperature() {
        return state.temperatureReading();
    }

    /** Contratto usato dal RelayHttpApi del controllo. */
    @GetMapping("/relay")
    public RelayStateResponse relayState() {
        return new RelayStateResponse(state.relayOn());
    }

    /** Il mock rappresenta il dispositivo fisico e aggiorna il proprio stato locale. */
    @PostMapping("/relay")
    public ResponseEntity<Void> relayCommand(@RequestBody RelayCommand command) {
        state.applyRelayCommand(command);
        return ResponseEntity.noContent().build();
    }

    /** Comando di test per cambiare la temperatura simulata. */
    @PutMapping("/mock/temperature")
    public TemperatureReading setTemperature(@RequestBody TemperatureReading reading) {
        state.setTemperature(reading.temperatura());
        return state.temperatureReading();
    }

    @GetMapping("/mock/state")
    public MockStateResponse state() {
        return new MockStateResponse(state.temperatureReading().temperatura(), state.relayOn());
    }

    @PostMapping("/mock/reset")
    public MockStateResponse reset() {
        state.reset(properties);
        return state();
    }
}
