package com.termostato.shelly.relay;

import com.termostato.shelly.config.ShellyDevice;
import com.termostato.shelly.config.ShellyDeviceLoader;
import com.termostato.shelly.config.ShellyTipo;
import com.termostato.shelly.external.ShellyClientFactory;
import com.termostato.shelly.web.ShellyRelayStatoResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShellyRelayServiceTest {

    @Mock ShellyDeviceLoader deviceLoader;
    @Mock ShellyClientFactory clientFactory;

    @InjectMocks
    ShellyRelayService service;

    private static final ShellyDevice RELAY =
            new ShellyDevice("portone", "Portone", "192.168.1.2", ShellyTipo.RELAY, 1, "SHSW-1");
    private static final ShellyDevice PM =
            new ShellyDevice("frigo", "Frigo", "192.168.1.9", ShellyTipo.PM, 3, "SNPM-001PCEU16");

    @Test
    void leggiStatoRestituisceStatoCorretto() {
        when(deviceLoader.byId("portone")).thenReturn(RELAY);
        when(clientFactory.leggiStatoRelay(RELAY)).thenReturn(true);

        ShellyRelayStatoResponse response = service.leggiStato("portone");

        assertTrue(response.acceso());
        assertEquals("portone", response.id());
        assertEquals("Portone", response.nome());
        assertEquals(ShellyTipo.RELAY, response.tipo());
    }

    @Test
    void accendiInviaComandoErileggeStato() {
        when(deviceLoader.byId("portone")).thenReturn(RELAY);
        when(clientFactory.leggiStatoRelay(RELAY)).thenReturn(true);

        ShellyRelayStatoResponse response = service.accendi("portone");

        verify(clientFactory).setStatoRelay(RELAY, true);
        verify(clientFactory).leggiStatoRelay(RELAY);
        assertTrue(response.acceso());
    }

    @Test
    void spegniInviaComandoErileggeStato() {
        when(deviceLoader.byId("portone")).thenReturn(RELAY);
        when(clientFactory.leggiStatoRelay(RELAY)).thenReturn(false);

        ShellyRelayStatoResponse response = service.spegni("portone");

        verify(clientFactory).setStatoRelay(RELAY, false);
        assertEquals(false, response.acceso());
    }

    @Test
    void deviceInesistentePropagatoComeBubble() {
        when(deviceLoader.byId("inesistente")).thenThrow(new NoSuchElementException("non trovato"));

        assertThrows(NoSuchElementException.class, () -> service.leggiStato("inesistente"));
    }

    @Test
    void devicePmLanciaIllegalArgument() {
        when(deviceLoader.byId("frigo")).thenReturn(PM);

        assertThrows(IllegalArgumentException.class, () -> service.leggiStato("frigo"));
    }
}
