package com.termostato.shelly.external;

import com.termostato.external.RestClientFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShellyGen3RelayClientTest {

    @Mock
    RestClientFactory factory;

    @InjectMocks
    ShellyGen3RelayClient client;

    @Test
    void leggiStatoRestituisceTrue() {
        String url = "http://192.168.1.3";
        ShellyGen3RelayHttpApi api = mock(ShellyGen3RelayHttpApi.class);
        when(factory.createProxy(url, ShellyGen3RelayHttpApi.class)).thenReturn(api);
        when(api.leggiStato(any())).thenReturn(new ShellyGen3SwitchGetResponse(true));

        assertTrue(client.leggiStato(url));
    }

    @Test
    void leggiStatoRestituisceFalse() {
        String url = "http://192.168.1.3";
        ShellyGen3RelayHttpApi api = mock(ShellyGen3RelayHttpApi.class);
        when(factory.createProxy(url, ShellyGen3RelayHttpApi.class)).thenReturn(api);
        when(api.leggiStato(any())).thenReturn(new ShellyGen3SwitchGetResponse(false));

        assertFalse(client.leggiStato(url));
    }

    @Test
    void setStatoOnInviaRequestConOnTrue() {
        String url = "http://192.168.1.3";
        ShellyGen3RelayHttpApi api = mock(ShellyGen3RelayHttpApi.class);
        when(factory.createProxy(url, ShellyGen3RelayHttpApi.class)).thenReturn(api);

        client.setStato(url, true);

        verify(api).setStato(new ShellyGen3SwitchSetRequest(0, true));
    }

    @Test
    void setStatoOffInviaRequestConOnFalse() {
        String url = "http://192.168.1.3";
        ShellyGen3RelayHttpApi api = mock(ShellyGen3RelayHttpApi.class);
        when(factory.createProxy(url, ShellyGen3RelayHttpApi.class)).thenReturn(api);

        client.setStato(url, false);

        verify(api).setStato(new ShellyGen3SwitchSetRequest(0, false));
    }
}
