package com.termostato.shelly.web;

import com.termostato.shelly.config.ShellyDevice;
import com.termostato.shelly.config.ShellyDeviceLoader;
import com.termostato.shelly.config.ShellyTipo;
import com.termostato.shelly.relay.ShellyRelayService;
import com.termostato.web.RestExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.NoSuchElementException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ShellyControllerTest {

    private final ShellyDeviceLoader deviceLoader = mock(ShellyDeviceLoader.class);
    private final ShellyRelayService relayService = mock(ShellyRelayService.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-20T17:00:00Z"), ZoneOffset.UTC);

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ShellyController controller = new ShellyController(deviceLoader, relayService);
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new RestExceptionHandler(clock))
                .build();
    }

    @Test
    void dispositiviRestituisceListaCompleta() throws Exception {
        when(deviceLoader.all()).thenReturn(List.of(
                new ShellyDevice("portone", "Portone", "192.168.1.2", ShellyTipo.RELAY, 1, "SHSW-1"),
                new ShellyDevice("frigo", "Frigo", "192.168.1.9", ShellyTipo.PM, 3, "SNPM-001PCEU16")));

        mockMvc.perform(get("/shelly/dispositivi").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value("portone"))
                .andExpect(jsonPath("$[0].tipo").value("RELAY"))
                .andExpect(jsonPath("$[1].id").value("frigo"))
                .andExpect(jsonPath("$[1].tipo").value("PM"));
    }

    @Test
    void statoRelayRestituisceRisposta() throws Exception {
        ShellyRelayStatoResponse response = new ShellyRelayStatoResponse(
                "portone", "Portone", "192.168.1.2", "SHSW-1", 1, ShellyTipo.RELAY, false);
        when(relayService.leggiStato("portone")).thenReturn(response);

        mockMvc.perform(get("/shelly/relay/portone").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("portone"))
                .andExpect(jsonPath("$.acceso").value(false))
                .andExpect(jsonPath("$.tipo").value("RELAY"))
                .andExpect(jsonPath("$.ip").value("192.168.1.2"));
    }

    @Test
    void accendiRelayRestituisceRisposta() throws Exception {
        ShellyRelayStatoResponse response = new ShellyRelayStatoResponse(
                "portone", "Portone", "192.168.1.2", "SHSW-1", 1, ShellyTipo.RELAY, true);
        when(relayService.accendi("portone")).thenReturn(response);

        mockMvc.perform(post("/shelly/relay/portone/on").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.acceso").value(true));
    }

    @Test
    void spegniRelayRestituisceRisposta() throws Exception {
        ShellyRelayStatoResponse response = new ShellyRelayStatoResponse(
                "portone", "Portone", "192.168.1.2", "SHSW-1", 1, ShellyTipo.RELAY, false);
        when(relayService.spegni("portone")).thenReturn(response);

        mockMvc.perform(post("/shelly/relay/portone/off").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.acceso").value(false));
    }

    @Test
    void deviceInesistenteRestituisce404() throws Exception {
        when(relayService.leggiStato("inesistente"))
                .thenThrow(new NoSuchElementException("Dispositivo Shelly non trovato: inesistente"));

        mockMvc.perform(get("/shelly/relay/inesistente").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void devicePmRestituisce400() throws Exception {
        when(relayService.leggiStato("frigo"))
                .thenThrow(new IllegalArgumentException("Il dispositivo 'frigo' è di tipo PM, non un relay"));

        mockMvc.perform(get("/shelly/relay/frigo").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}
