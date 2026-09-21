package com.termostato.scheduling;

import com.termostato.config.BootstrapProperties;
import com.termostato.config.ConfigurationService;
import com.termostato.persistence.ErrorLogRepository;
import com.termostato.persistence.PollingLogRepository;
import com.termostato.shelly.pm.ShellyConsumoLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
public class LogRetentionScheduler {

    private static final Logger log = LoggerFactory.getLogger(LogRetentionScheduler.class);

    private final ConfigurationService configuration;
    private final BootstrapProperties bootstrap;
    private final PollingLogRepository pollingLogs;
    private final ErrorLogRepository errorLogs;
    private final ShellyConsumoLogRepository shellyConsumoLogs;
    private final Clock clock;

    public LogRetentionScheduler(ConfigurationService configuration,
                                  BootstrapProperties bootstrap,
                                  PollingLogRepository pollingLogs,
                                  ErrorLogRepository errorLogs,
                                  ShellyConsumoLogRepository shellyConsumoLogs,
                                  Clock clock) {
        this.configuration = configuration;
        this.bootstrap = bootstrap;
        this.pollingLogs = pollingLogs;
        this.errorLogs = errorLogs;
        this.shellyConsumoLogs = shellyConsumoLogs;
        this.clock = clock;
    }

    @Scheduled(cron = "0 0 * * * *", zone = "UTC")
    @Transactional
    public void cleanup() {
        Instant now = clock.instant();

        Instant logThreshold = now.minus(
                configuration.current().retentionLogGiorni(), ChronoUnit.DAYS);
        int pollingDeleted = pollingLogs.deleteBefore(logThreshold);
        int errorsDeleted = errorLogs.deleteBefore(logThreshold);

        int retentionConsumiGiorni = Math.max(1, bootstrap.getRetentionConsumiGiorni());
        Instant consumiThreshold = now.minus(retentionConsumiGiorni, ChronoUnit.DAYS);
        int consumiDeleted = shellyConsumoLogs.deleteBefore(consumiThreshold);

        log.info("Retention log completata: {} record polling, {} record errore, {} record consumi Shelly eliminati",
                pollingDeleted, errorsDeleted, consumiDeleted);
    }
}
