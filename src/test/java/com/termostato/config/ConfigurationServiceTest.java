package com.termostato.config;

import com.termostato.domain.model.Calendario;
import com.termostato.domain.model.SystemConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigurationServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void creaConfigECalendarioDaiDefaultECaricaGliAggiornamentiPersistiti() throws Exception {
        BootstrapProperties properties = properties();
        JsonMapper mapper = JsonMapper.builder().build();

        ConfigurationService first = new ConfigurationService(properties, mapper);
        first.afterPropertiesSet();
        assertTrue(Files.exists(Path.of(properties.getConfigFile())));
        assertTrue(Files.exists(Path.of(properties.getCalendarioFile())));
        assertTrue(first.currentCalendario().giorni().values().stream().allMatch(list -> list.isEmpty()));

        SystemConfiguration updated = new SystemConfiguration(
                new BigDecimal("0.2"), false, null, 30, 4, 7,
                "https://ntfy.example", "topic", true,
                "http://sensor-new", "http://relay-new", properties.getDatabasePath());
        first.update(updated);

        ConfigurationService second = new ConfigurationService(properties, mapper);
        second.afterPropertiesSet();
        assertEquals(updated, second.current());
        assertFalse(second.current().overrideAttivo());
    }

    private BootstrapProperties properties() {
        BootstrapProperties properties = new BootstrapProperties();
        properties.setConfigFile(tempDir.resolve("config.json").toString());
        properties.setCalendarioFile(tempDir.resolve("calendario.json").toString());
        properties.setDatabasePath(tempDir.resolve("termostato.db").toString());
        properties.setSensore(new BootstrapProperties.EndpointProperties("http://sensor"));
        properties.setRelay(new BootstrapProperties.EndpointProperties("http://relay"));
        return properties;
    }
}
