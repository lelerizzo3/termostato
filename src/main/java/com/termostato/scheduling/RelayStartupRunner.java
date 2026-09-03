package com.termostato.scheduling;

import com.termostato.domain.control.ThermostatControlService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(Integer.MIN_VALUE)
public class RelayStartupRunner implements ApplicationRunner {

    private final ThermostatControlService controlService;

    public RelayStartupRunner(ThermostatControlService controlService) {
        this.controlService = controlService;
    }

    @Override
    public void run(ApplicationArguments args) {
        controlService.initializeRelayAtStartup();
    }
}
