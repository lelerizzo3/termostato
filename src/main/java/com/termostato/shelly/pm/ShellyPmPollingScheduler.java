package com.termostato.shelly.pm;

import com.termostato.config.BootstrapProperties;
import com.termostato.domain.control.ErrorCategory;
import com.termostato.persistence.ErrorLogRecord;
import com.termostato.persistence.ErrorLogRepository;
import com.termostato.shelly.config.ShellyDevice;
import com.termostato.shelly.config.ShellyDeviceLoader;
import com.termostato.shelly.config.ShellyTipo;
import com.termostato.shelly.external.ShellyClientFactory;
import com.termostato.shelly.external.ShellyPmStatusResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class ShellyPmPollingScheduler {

    private static final Logger log = LoggerFactory.getLogger(ShellyPmPollingScheduler.class);

    private final ShellyDeviceLoader deviceLoader;
    private final ShellyClientFactory clientFactory;
    private final ShellyConsumoLogRepository consumoLogRepository;
    private final ErrorLogRepository errorLogRepository;
    private final BootstrapProperties bootstrap;
    private final Clock clock;
    private final AtomicReference<Instant> nextPoll = new AtomicReference<>(Instant.MIN);

    public ShellyPmPollingScheduler(ShellyDeviceLoader deviceLoader,
                                    ShellyClientFactory clientFactory,
                                    ShellyConsumoLogRepository consumoLogRepository,
                                    ErrorLogRepository errorLogRepository,
                                    BootstrapProperties bootstrap,
                                    Clock clock) {
        this.deviceLoader = deviceLoader;
        this.clientFactory = clientFactory;
        this.consumoLogRepository = consumoLogRepository;
        this.errorLogRepository = errorLogRepository;
        this.bootstrap = bootstrap;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${termostato.scheduler-tick-millis:1000}")
    public void tick() {
        Instant now = clock.instant();
        if (now.isBefore(nextPoll.get())) {
            return;
        }
        try {
            pollAll();
        } finally {
            int seconds = Math.max(1, bootstrap.getIntervalloPollingConsumiSecondi());
            nextPoll.set(clock.instant().plusSeconds(seconds));
        }
    }

    private void pollAll() {
        deviceLoader.all().stream()
                .filter(device -> device.tipo() == ShellyTipo.PM)
                .forEach(this::pollDevice);
    }

    private void pollDevice(ShellyDevice device) {
        try {
            ShellyPmStatusResponse status = clientFactory.leggiConsumo(device);
            Instant now = clock.instant();
            consumoLogRepository.save(new ShellyConsumoLogRecord(
                    null, now, device.id(),
                    status.apower(), status.voltage(), status.current()));
            log.debug("PM polling {}: {}W", device.id(), status.apower());
        } catch (RuntimeException exception) {
            log.warn("Errore polling PM device {}: {}", device.id(), exception.getMessage());
            try {
                errorLogRepository.save(new ErrorLogRecord(
                        null, clock.instant(), ErrorCategory.SHELLY_PM.name(),
                        null, null, 1));
            } catch (RuntimeException logException) {
                log.error("Impossibile salvare errore per device {}", device.id(), logException);
            }
        }
    }
}
