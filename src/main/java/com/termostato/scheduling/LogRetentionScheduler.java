package com.termostato.scheduling;

import com.termostato.config.ConfigurationService;
import com.termostato.persistence.ErrorLogRepository;
import com.termostato.persistence.PollingLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Component
public class LogRetentionScheduler {

    private static final Logger log = LoggerFactory.getLogger(LogRetentionScheduler.class);

    private final ConfigurationService configuration;
    private final PollingLogRepository pollingLogs;
    private final ErrorLogRepository errorLogs;
    private final Clock clock;

    public LogRetentionScheduler(ConfigurationService configuration,
                                  PollingLogRepository pollingLogs,
                                  ErrorLogRepository errorLogs,
                                  Clock clock) {
        this.configuration = configuration;
        this.pollingLogs = pollingLogs;
        this.errorLogs = errorLogs;
        this.clock = clock;
    }

    @Scheduled(cron = "0 0 * * * *", zone = "UTC")
    @Transactional
    public void cleanup() {
        Instant threshold = clock.instant().minus(
                configuration.current().retentionLogGiorni(), java.time.temporal.ChronoUnit.DAYS);
        int pollingDeleted = pollingLogs.deleteBefore(threshold);
        int errorsDeleted = errorLogs.deleteBefore(threshold);
        log.info("Retention log completata: {} record polling e {} record errore eliminati",
                pollingDeleted, errorsDeleted);
    }
}
