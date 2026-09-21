package com.termostato.shelly.config;

import com.termostato.config.BootstrapProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShellyDeviceLoaderTest {

    @TempDir
    Path tempDir;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    void creaFileConDefaultSeAssente() throws Exception {
        Path shellyFile = tempDir.resolve("shelly.json");
        ShellyDeviceLoader loader = loader(shellyFile);
        loader.afterPropertiesSet();

        assertTrue(Files.exists(shellyFile), "Il file shelly.json deve essere creato");
        List<ShellyDevice> devices = loader.all();
        assertEquals(9, devices.size());
    }

    @Test
    void caricaCorrettamenteDalJson() throws Exception {
        Path shellyFile = tempDir.resolve("shelly.json");
        String json = """
                {
                  "dispositivi": [
                    {"id":"test1","nome":"Test 1","ip":"192.168.1.99","tipo":"RELAY","generazione":1,"modello":"SHSW-1"},
                    {"id":"test2","nome":"Test 2","ip":"192.168.1.98","tipo":"PM","generazione":3,"modello":"SNPM-001PCEU16"}
                  ]
                }
                """;
        Files.writeString(shellyFile, json, StandardCharsets.UTF_8);
        ShellyDeviceLoader loader = loader(shellyFile);
        loader.afterPropertiesSet();

        List<ShellyDevice> devices = loader.all();
        assertEquals(2, devices.size());
        assertEquals("test1", devices.get(0).id());
        assertEquals("Test 1", devices.get(0).nome());
        assertEquals(ShellyTipo.RELAY, devices.get(0).tipo());
        assertEquals(1, devices.get(0).generazione());
    }

    @Test
    void byIdRestituisceDeviceCorretto() throws Exception {
        Path shellyFile = tempDir.resolve("shelly.json");
        ShellyDeviceLoader loader = loader(shellyFile);
        loader.afterPropertiesSet();

        ShellyDevice portone = loader.byId("portone");

        assertNotNull(portone);
        assertEquals("portone", portone.id());
        assertEquals("Portone", portone.nome());
        assertEquals("192.168.1.2", portone.ip());
        assertEquals(ShellyTipo.RELAY, portone.tipo());
        assertEquals(1, portone.generazione());
    }

    @Test
    void byIdLanciaEccezionePerIdSconosciuto() throws Exception {
        Path shellyFile = tempDir.resolve("shelly.json");
        ShellyDeviceLoader loader = loader(shellyFile);
        loader.afterPropertiesSet();

        assertThrows(NoSuchElementException.class, () -> loader.byId("inesistente"));
    }

    @Test
    void defaultContieneNoveDevice() throws Exception {
        Path shellyFile = tempDir.resolve("shelly.json");
        ShellyDeviceLoader loader = loader(shellyFile);
        loader.afterPropertiesSet();

        List<ShellyDevice> devices = loader.all();
        long relayCount = devices.stream().filter(d -> d.tipo() == ShellyTipo.RELAY).count();
        long pmCount = devices.stream().filter(d -> d.tipo() == ShellyTipo.PM).count();
        assertEquals(3, relayCount);
        assertEquals(6, pmCount);
    }

    private ShellyDeviceLoader loader(Path shellyFile) {
        BootstrapProperties props = new BootstrapProperties();
        props.setShellyFile(shellyFile.toString());
        return new ShellyDeviceLoader(props, jsonMapper);
    }
}
