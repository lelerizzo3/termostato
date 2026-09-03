package com.termostato.scheduling;

import com.termostato.config.ConfigurationService;
import com.termostato.domain.control.ThermostatControlService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class PollingScheduler {

    private static final Logger log = LoggerFactory.getLogger(PollingScheduler.class);

    private final ThermostatControlService controlService;
    private final ConfigurationService configuration;
    private final Clock clock;
    private final AtomicReference<Instant> nextPoll = new AtomicReference<>(Instant.MIN);

    public PollingScheduler(ThermostatControlService controlService,
                            ConfigurationService configuration,
                            Clock clock) {
        this.controlService = controlService;
        this.configuration = configuration;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${termostato.scheduler-tick-millis:1000}")
    public void tick() {
        Instant now = clock.instant();
        Instant scheduledAt = nextPoll.get();
        if (now.isBefore(scheduledAt)) {
            return;
        }
        try {
            controlService.executePollingCycle();
        } catch (RuntimeException exception) {
            log.error("Errore non gestito nel ciclo di polling", exception);
        } finally {
            int seconds = Math.max(1, configuration.current().intervalloPollingSecondi());
            nextPoll.set(clock.instant().plusSeconds(seconds));
        }
    }
}
