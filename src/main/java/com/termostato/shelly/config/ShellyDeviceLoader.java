package com.termostato.shelly.config;

import com.termostato.config.BootstrapProperties;
import com.termostato.config.ConfigurationPersistenceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class ShellyDeviceLoader implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(ShellyDeviceLoader.class);

    private static final List<ShellyDevice> DEFAULT_DEVICES = List.of(
            new ShellyDevice("portone", "Portone", "192.168.1.2", ShellyTipo.RELAY, 1, "SHSW-1"),
            new ShellyDevice("cancello", "Cancello", "192.168.1.3", ShellyTipo.RELAY, 3, "S3SW-001X8EU"),
            new ShellyDevice("terrazzo", "Terrazzo", "192.168.1.10", ShellyTipo.RELAY, 3, "S3SW-001X8EU"),
            new ShellyDevice("frigo-e-forno", "Frigo e Forno", "192.168.1.9", ShellyTipo.PM, 3, "SNPM-001PCEU16"),
            new ShellyDevice("top-cucina", "Top Cucina", "192.168.1.7", ShellyTipo.PM, 3, "SNPM-001PCEU16"),
            new ShellyDevice("lavanderia", "Lavanderia", "192.168.1.12", ShellyTipo.PM, 3, "SNPM-001PCEU16"),
            new ShellyDevice("modem", "Modem", "192.168.1.5", ShellyTipo.PM, 3, "SNPM-001PCEU16"),
            new ShellyDevice("tv-cucina", "Tv Cucina", "192.168.1.8", ShellyTipo.PM, 3, "SNPM-001PCEU16"),
            new ShellyDevice("lavastoviglie-e-cappa", "Lavastoviglie e Cappa", "192.168.1.14", ShellyTipo.PM, 3, "SNPM-001PCEU16")
    );

    private final BootstrapProperties bootstrap;
    private final JsonMapper jsonMapper;
    private final AtomicReference<List<ShellyDevice>> devices = new AtomicReference<>(List.of());

    public ShellyDeviceLoader(BootstrapProperties bootstrap, JsonMapper jsonMapper) {
        this.bootstrap = bootstrap;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void afterPropertiesSet() {
        Path path = shellyPath();
        if (!Files.exists(path)) {
            ShellyDeviceList defaultList = new ShellyDeviceList(DEFAULT_DEVICES);
            devices.set(DEFAULT_DEVICES);
            try {
                writeJson(path, defaultList);
                log.info("File dispositivi Shelly creato con i default in {}", path);
            } catch (RuntimeException exception) {
                log.warn("Impossibile creare il file dispositivi Shelly {}: uso i default in memoria", path, exception);
            }
            return;
        }
        try {
            String rawJson = Files.readString(path, StandardCharsets.UTF_8);
            ShellyDeviceList loaded = jsonMapper.readValue(rawJson, ShellyDeviceList.class);
            List<ShellyDevice> list = loaded.dispositivi() != null ? List.copyOf(loaded.dispositivi()) : List.of();
            devices.set(list);
            log.info("Caricati {} dispositivi Shelly da {}", list.size(), path);
        } catch (Exception exception) {
            log.warn("File dispositivi Shelly malformato in {}: uso i default in memoria", path, exception);
            devices.set(DEFAULT_DEVICES);
        }
    }

    public List<ShellyDevice> all() {
        return devices.get();
    }

    public ShellyDevice byId(String id) {
        return devices.get().stream()
                .filter(d -> d.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Dispositivo Shelly non trovato: " + id));
    }

    public Path shellyPath() {
        return Path.of(bootstrap.getShellyFile());
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
