package com.termostato.shelly.external;

import com.termostato.external.RestClientFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShellyGen1RelayClientTest {

    @Mock
    RestClientFactory factory;

    @InjectMocks
    ShellyGen1RelayClient client;

    @Test
    void leggiStatoRestituisceTrue() {
        String url = "http://192.168.1.2";
        ShellyGen1RelayHttpApi api = mock(ShellyGen1RelayHttpApi.class);
        when(factory.createProxy(url, ShellyGen1RelayHttpApi.class)).thenReturn(api);
        when(api.leggiStato()).thenReturn(new ShellyGen1RelayResponse(true));

        assertTrue(client.leggiStato(url));
    }

    @Test
    void leggiStatoRestituisceFalse() {
        String url = "http://192.168.1.2";
        ShellyGen1RelayHttpApi api = mock(ShellyGen1RelayHttpApi.class);
        when(factory.createProxy(url, ShellyGen1RelayHttpApi.class)).thenReturn(api);
        when(api.leggiStato()).thenReturn(new ShellyGen1RelayResponse(false));

        assertFalse(client.leggiStato(url));
    }

    @Test
    void setStatoOnInviaOn() {
        String url = "http://192.168.1.2";
        ShellyGen1RelayHttpApi api = mock(ShellyGen1RelayHttpApi.class);
        when(factory.createProxy(url, ShellyGen1RelayHttpApi.class)).thenReturn(api);

        client.setStato(url, true);

        verify(api).inviaComando("on");
    }

    @Test
    void setStatoOffInviaOff() {
        String url = "http://192.168.1.2";
        ShellyGen1RelayHttpApi api = mock(ShellyGen1RelayHttpApi.class);
        when(factory.createProxy(url, ShellyGen1RelayHttpApi.class)).thenReturn(api);

        client.setStato(url, false);

        verify(api).inviaComando("off");
    }
}
