package com.termostato.config;

import com.termostato.domain.model.Calendario;
import com.termostato.domain.model.CalendarioDocument;
import com.termostato.domain.model.SystemConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class ConfigurationService implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(ConfigurationService.class);

    private final BootstrapProperties bootstrap;
    private final JsonMapper jsonMapper;
    private final AtomicReference<SystemConfiguration> configuration = new AtomicReference<>();
    private final AtomicReference<Calendario> calendario = new AtomicReference<>(Calendario.vuoto());

    public ConfigurationService(BootstrapProperties bootstrap, JsonMapper jsonMapper) {
        this.bootstrap = bootstrap;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void afterPropertiesSet() {
        SystemConfiguration defaults = bootstrap.defaults();
        configuration.set(defaults);
        loadConfiguration(defaults);
        loadCalendar();
    }

    public SystemConfiguration current() {
        SystemConfiguration value = configuration.get();
        if (value == null) {
            throw new IllegalStateException("Configurazione non inizializzata");
        }
        return value;
    }

    public Calendario currentCalendario() {
        return calendario.get();
    }

    public synchronized void update(SystemConfiguration requested) {
        Objects.requireNonNull(requested, "La configurazione è obbligatoria");
        SystemConfiguration old = current();
        if (!old.databasePath().equals(requested.databasePath())) {
            throw new IllegalArgumentException("database_path è un parametro di bootstrap e non può essere cambiato a runtime");
        }
        configuration.set(requested);
        try {
            writeJson(configPath(), requested);
        } catch (RuntimeException exception) {
            configuration.set(old);
            throw exception;
        }
    }

    public synchronized void updateCalendario(Calendario requested) {
        Objects.requireNonNull(requested, "Il calendario è obbligatorio");
        Calendario old = calendario.get();
        calendario.set(requested);
        try {
            writeJson(calendarPath(), requested.toDocument());
        } catch (RuntimeException exception) {
            calendario.set(old);
            throw exception;
        }
    }

    public Path configPath() {
        return Path.of(bootstrap.getConfigFile());
    }

    public Path calendarPath() {
        return Path.of(bootstrap.getCalendarioFile());
    }

    private void loadConfiguration(SystemConfiguration defaults) {
        Path path = configPath();
        if (!Files.exists(path)) {
            try {
                writeJson(path, defaults);
            } catch (RuntimeException exception) {
                log.warn("Impossibile creare il file di configurazione {}: uso i default in memoria", path, exception);
            }
            return;
        }
        try {
            String rawJson = Files.readString(path, StandardCharsets.UTF_8);
            SystemConfiguration loaded = jsonMapper.readValue(rawJson, SystemConfiguration.class);
            boolean apiKeysMissing = !rawJson.contains("\"api_keys\"") && !rawJson.contains("\"apiKeys\"");
            var effectiveApiKeys = apiKeysMissing ? defaults.apiKeys() : loaded.apiKeys();
            // Il path del database determina il datasource prima del caricamento JSON: resta bootstrap-only.
            if (!defaults.databasePath().equals(loaded.databasePath()) || apiKeysMissing) {
                loaded = new SystemConfiguration(
                        loaded.sogliaAttivazione(), loaded.overrideAttivo(), loaded.temperaturaOverride(),
                        loaded.intervalloPollingSecondi(), loaded.maxErroriConsecutivi(), loaded.retentionLogGiorni(),
                        loaded.ntfyUrl(), loaded.ntfyTopic(), loaded.debugMode(), loaded.sensoreUrl(),
                        loaded.relayUrl(), defaults.databasePath(), effectiveApiKeys);
                if (apiKeysMissing) {
                    try {
                        writeJson(path, loaded);
                    } catch (RuntimeException migrationFailure) {
                        log.warn("Impossibile migrare api_keys nel file {}: uso il valore in memoria", path,
                                migrationFailure);
                    }
                }
            }
            configuration.set(loaded);
        } catch (Exception exception) {
            log.warn("Configurazione JSON malformata in {}: uso i default e non sovrascrivo il file", path, exception);
        }
    }

    private void loadCalendar() {
        Path path = calendarPath();
        if (!Files.exists(path)) {
            Calendario empty = Calendario.vuoto();
            calendario.set(empty);
            try {
                writeJson(path, empty.toDocument());
            } catch (RuntimeException exception) {
                log.warn("Impossibile creare il file calendario {}: uso il calendario vuoto in memoria", path, exception);
            }
            return;
        }
        try {
            CalendarioDocument document = jsonMapper.readValue(Files.readString(path, StandardCharsets.UTF_8),
                    CalendarioDocument.class);
            calendario.set(Calendario.fromDocument(document));
        } catch (Exception exception) {
            log.warn("Calendario JSON malformato in {}: uso il calendario vuoto", path, exception);
            calendario.set(Calendario.vuoto());
        }
    }

    private void writeJson(Path target, Object value) {
        try {
            Path absolute = target.toAbsolutePath().normalize();
            Path parent = absolute.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            String json = jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value) + System.lineSeparator();
            Path temporary = Files.createTempFile(parent, absolute.getFileName() + ".", ".tmp-" + UUID.randomUUID());
            try {
                Files.writeString(temporary, json, StandardCharsets.UTF_8,
                        StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
                try {
                    Files.move(temporary, absolute, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } catch (AtomicMoveNotSupportedException unsupported) {
                    Files.move(temporary, absolute, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(temporary);
            }
        } catch (IOException | RuntimeException exception) {
            throw new ConfigurationPersistenceException("Impossibile persistere " + target, exception);
        }
    }
}
