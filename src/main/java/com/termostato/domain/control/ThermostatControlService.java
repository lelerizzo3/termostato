package com.termostato.domain.control;

import com.termostato.config.ConfigurationService;
import com.termostato.domain.model.Calendario;
import com.termostato.domain.model.SystemConfiguration;
import com.termostato.external.notification.NotificationService;
import com.termostato.external.relay.RelayClient;
import com.termostato.external.temperature.TemperatureClient;
import com.termostato.external.temperature.TemperatureReading;
import com.termostato.external.weather.ExternalWeatherClient;
import com.termostato.external.weather.WeatherReading;
import com.termostato.persistence.ErrorLogRecord;
import com.termostato.persistence.ErrorLogRepository;
import com.termostato.persistence.PollingLogRecord;
import com.termostato.persistence.PollingLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class ThermostatControlService {

    private static final Logger log = LoggerFactory.getLogger(ThermostatControlService.class);
    private static final String READ_TEMP_MESSAGE = "Impossibile leggere la temperatura dal sensore";
    private static final String READ_WEATHER_MESSAGE = "Impossibile leggere temperatura e umidità esterne";
    private static final String READ_RELAY_MESSAGE = "Impossibile leggere lo stato della caldaia";
    private static final String TURN_ON_MESSAGE = "Impossibile inviare il comando di accensione alla caldaia";
    private static final String TURN_OFF_MESSAGE = "Impossibile inviare il comando di spegnimento alla caldaia";

    private final ConfigurationService configuration;
    private final TargetTemperatureResolver targetResolver;
    private final HeatingDecisionCalculator decisionCalculator;
    private final ErrorTrackingService errorTracking;
    private final TemperatureClient temperatureClient;
    private final ExternalWeatherClient externalWeatherClient;
    private final RelayClient relayClient;
    private final NotificationService notificationService;
    private final PollingLogRepository pollingLogRepository;
    private final ErrorLogRepository errorLogRepository;
    private final Clock clock;
    private final AtomicBoolean relayStartupRead = new AtomicBoolean();
    private final AtomicBoolean spegnimentoPendente = new AtomicBoolean();

    public ThermostatControlService(ConfigurationService configuration,
                                    TargetTemperatureResolver targetResolver,
                                    HeatingDecisionCalculator decisionCalculator,
                                    ErrorTrackingService errorTracking,
                                    TemperatureClient temperatureClient,
                                    ExternalWeatherClient externalWeatherClient,
                                    RelayClient relayClient,
                                    NotificationService notificationService,
                                    PollingLogRepository pollingLogRepository,
                                    ErrorLogRepository errorLogRepository,
                                    Clock clock) {
        this.configuration = configuration;
        this.targetResolver = targetResolver;
        this.decisionCalculator = decisionCalculator;
        this.errorTracking = errorTracking;
        this.temperatureClient = temperatureClient;
        this.externalWeatherClient = externalWeatherClient;
        this.relayClient = relayClient;
        this.notificationService = notificationService;
        this.pollingLogRepository = pollingLogRepository;
        this.errorLogRepository = errorLogRepository;
        this.clock = clock;
    }

    public synchronized void initializeRelayAtStartup() {
        if (relayStartupRead.get()) {
            return;
        }
        try {
            boolean physicalState = relayClient.leggiStato();
            relayStartupRead.set(true);
            log.info("Stato relay letto all'avvio: {}", physicalState ? "ACCESO" : "SPENTO");
        } catch (RuntimeException exception) {
            recordError(ErrorCategory.READ_RELAY, READ_RELAY_MESSAGE, null, null,
                    clock.instant(), configuration.current(), false);
        }
    }

    public boolean relayStartupRead() {
        return relayStartupRead.get();
    }

    public synchronized void executePollingCycle() {
        if (!relayStartupRead.get()) {
            initializeRelayAtStartup();
            if (!relayStartupRead.get()) {
                return;
            }
        }

        Instant now = clock.instant();
        SystemConfiguration currentConfiguration = configuration.current();

        if (spegnimentoPendente.get() && !tryPendingTurnOff(now, currentConfiguration)) {
            return;
        }

        TemperatureReading indoorReading;
        try {
            indoorReading = temperatureClient.leggiLettura();
        } catch (RuntimeException exception) {
            recordError(ErrorCategory.READ_TEMP, READ_TEMP_MESSAGE, null, null,
                    now, currentConfiguration, true);
            return;
        }
        BigDecimal roomTemperature = indoorReading.temperatura();

        WeatherReading outdoorReading = null;
        try {
            outdoorReading = externalWeatherClient.leggiLettura();
        } catch (RuntimeException exception) {
            // Il meteo esterno è informativo: non deve interrompere il controllo locale.
            recordError(ErrorCategory.READ_WEATHER, READ_WEATHER_MESSAGE, null, roomTemperature,
                    now, currentConfiguration, false);
        }

        Optional<BigDecimal> target = targetResolver.resolve(
                now, currentConfiguration, configuration.currentCalendario());

        boolean currentRelayState;
        try {
            // La fonte di verità viene letta dal relay fisico in ogni ciclo.
            currentRelayState = relayClient.leggiStato();
        } catch (RuntimeException exception) {
            recordError(ErrorCategory.READ_RELAY, READ_RELAY_MESSAGE, null, roomTemperature,
                    now, currentConfiguration, true);
            return;
        }

        HeatingDecision decision = decisionCalculator.calculate(
                roomTemperature, target, currentConfiguration, currentRelayState);

        if (decision == HeatingDecision.ON && !currentRelayState) {
            try {
                relayClient.inviaComando(true);
                notificationService.notificaInformazione(
                        "Caldaia accesa — temperatura rilevata " + roomTemperature.toPlainString()
                                + "°C, target " + target.map(BigDecimal::toPlainString).orElse("n/d") + "°C");
                currentRelayState = true;
            } catch (RuntimeException exception) {
                recordError(ErrorCategory.TURN_ON, TURN_ON_MESSAGE, currentRelayState,
                        roomTemperature, now, currentConfiguration, true);
                savePollingLog(now, currentRelayState, indoorReading, target, outdoorReading, currentConfiguration);
                return;
            }
        } else if (decision == HeatingDecision.OFF && currentRelayState) {
            try {
                relayClient.inviaComando(false);
                spegnimentoPendente.set(false);
                notificationService.notificaInformazione(
                        "Caldaia spenta — temperatura rilevata " + roomTemperature.toPlainString()
                                + "°C, target " + target.map(BigDecimal::toPlainString).orElse("n/d") + "°C");
                currentRelayState = false;
            } catch (RuntimeException exception) {
                spegnimentoPendente.set(true);
                recordError(ErrorCategory.TURN_OFF, TURN_OFF_MESSAGE, currentRelayState,
                        roomTemperature, now, currentConfiguration, false);
                savePollingLog(now, currentRelayState, indoorReading, target, outdoorReading, currentConfiguration);
                return;
            }
        }

        savePollingLog(now, currentRelayState, indoorReading, target, outdoorReading, currentConfiguration);
        errorTracking.resetAll();
    }

    private boolean tryPendingTurnOff(Instant now, SystemConfiguration currentConfiguration) {
        try {
            relayClient.inviaComando(false);
            spegnimentoPendente.set(false);
            notificationService.notificaInformazione("Caldaia spenta — retry del comando pendente");
            return true;
        } catch (RuntimeException exception) {
            recordError(ErrorCategory.TURN_OFF, TURN_OFF_MESSAGE, null, null,
                    now, currentConfiguration, false);
            return false;
        }
    }

    private void recordError(ErrorCategory category,
                             String message,
                             Boolean boilerState,
                             BigDecimal roomTemperature,
                             Instant occurredAt,
                             SystemConfiguration currentConfiguration,
                             boolean applySafetyThreshold) {
        int consecutiveErrors = errorTracking.increment(category);
        saveErrorLog(new ErrorLogRecord(null, occurredAt, message, boilerState,
                roomTemperature, consecutiveErrors));
        notificationService.notificaErrore(message);

        if (applySafetyThreshold && category != ErrorCategory.TURN_OFF
                && consecutiveErrors >= currentConfiguration.maxErroriConsecutivi()) {
            trySafetyTurnOff(occurredAt, boilerState, roomTemperature, currentConfiguration);
        }
    }

    private void trySafetyTurnOff(Instant occurredAt,
                                  Boolean boilerState,
                                  BigDecimal roomTemperature,
                                  SystemConfiguration currentConfiguration) {
        try {
            relayClient.inviaComando(false);
            spegnimentoPendente.set(false);
            notificationService.notificaInformazione("Caldaia spenta per sicurezza");
        } catch (RuntimeException exception) {
            spegnimentoPendente.set(true);
            recordError(ErrorCategory.TURN_OFF, TURN_OFF_MESSAGE, boilerState, roomTemperature,
                    occurredAt, currentConfiguration, false);
        }
    }

    private void savePollingLog(Instant timestamp,
                                boolean boilerState,
                                TemperatureReading indoorReading,
                                Optional<BigDecimal> target,
                                WeatherReading outdoorReading,
                                SystemConfiguration currentConfiguration) {
        try {
            pollingLogRepository.save(new PollingLogRecord(
                    null,
                    timestamp,
                    boilerState,
                    indoorReading.temperatura(),
                    indoorReading.umidita(),
                    target.orElse(null),
                    currentConfiguration.overrideAttivo(),
                    currentConfiguration.overrideAttivo() ? currentConfiguration.temperaturaOverride() : null,
                    outdoorReading == null ? null : outdoorReading.temperatura(),
                    outdoorReading == null ? null : outdoorReading.umidita()));
        } catch (RuntimeException exception) {
            log.error("Impossibile salvare il log di polling", exception);
        }
    }

    private void saveErrorLog(ErrorLogRecord record) {
        try {
            errorLogRepository.save(record);
        } catch (RuntimeException exception) {
            log.error("Impossibile salvare il log di errore", exception);
        }
    }
}
