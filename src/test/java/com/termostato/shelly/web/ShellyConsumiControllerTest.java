package com.termostato.shelly.web;

import com.termostato.shelly.config.ShellyDevice;
import com.termostato.shelly.config.ShellyDeviceLoader;
import com.termostato.shelly.config.ShellyTipo;
import com.termostato.shelly.pm.ShellyConsumoLogRecord;
import com.termostato.shelly.pm.ShellyConsumoLogRepository;
import com.termostato.web.RestExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ShellyConsumiControllerTest {

    private final ShellyDeviceLoader deviceLoader = mock(ShellyDeviceLoader.class);
    private final ShellyConsumoLogRepository repository = mock(ShellyConsumoLogRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-20T17:00:00Z"), ZoneOffset.UTC);

    private MockMvc mockMvc;

    private static final ShellyDevice PM =
            new ShellyDevice("frigo", "Frigo e Forno", "192.168.1.9", ShellyTipo.PM, 3, "SNPM-001PCEU16");
    private static final ShellyDevice RELAY =
            new ShellyDevice("portone", "Portone", "192.168.1.2", ShellyTipo.RELAY, 1, "SHSW-1");

    @BeforeEach
    void setUp() {
        ShellyConsumiController controller = new ShellyConsumiController(deviceLoader, repository, clock);
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new RestExceptionHandler(clock))
                .build();
    }

    @Test
    void rangeValidoRestituisceMetadatiEMisure() throws Exception {
        when(deviceLoader.byId("frigo")).thenReturn(PM);
        ShellyConsumoLogRecord record = new ShellyConsumoLogRecord(
                1L, Instant.parse("2026-09-20T10:00:00Z"), "frigo",
                new BigDecimal("45.2"), new BigDecimal("229.8"), new BigDecimal("0.197"));
        when(repository.findBetween(eq("frigo"), any(), any())).thenReturn(List.of(record));

        mockMvc.perform(get("/shelly/consumi/frigo")
                        .param("da", "2026-09-20")
                        .param("a", "2026-09-20")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("frigo"))
                .andExpect(jsonPath("$.nome").value("Frigo e Forno"))
                .andExpect(jsonPath("$.tipo").value("PM"))
                .andExpect(jsonPath("$.ip").value("192.168.1.9"))
                .andExpect(jsonPath("$.misure.length()").value(1))
                .andExpect(jsonPath("$.misure[0].deviceId").value("frigo"));
    }

    @Test
    void deviceNonTrovatoRestituisce404() throws Exception {
        when(deviceLoader.byId("inesistente"))
                .thenThrow(new NoSuchElementException("Dispositivo Shelly non trovato: inesistente"));

        mockMvc.perform(get("/shelly/consumi/inesistente")
                        .param("da", "2026-09-20")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void deviceRelayRestituisce400() throws Exception {
        when(deviceLoader.byId("portone")).thenReturn(RELAY);

        mockMvc.perform(get("/shelly/consumi/portone")
                        .param("da", "2026-09-20")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void rangeInvertitoRestituisce400() throws Exception {
        when(deviceLoader.byId("frigo")).thenReturn(PM);

        mockMvc.perform(get("/shelly/consumi/frigo")
                        .param("da", "2026-09-21")
                        .param("a", "2026-09-20")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void senzaParametriUsaOggiComeDefault() throws Exception {
        when(deviceLoader.byId("frigo")).thenReturn(PM);
        when(repository.findBetween(eq("frigo"), any(), any())).thenReturn(List.of());

        mockMvc.perform(get("/shelly/consumi/frigo")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.misure.length()").value(0));
    }
}
