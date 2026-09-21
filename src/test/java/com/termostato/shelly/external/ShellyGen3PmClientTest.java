package com.termostato.shelly.external;

import com.termostato.external.RestClientFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShellyGen3PmClientTest {

    @Mock
    RestClientFactory factory;

    @InjectMocks
    ShellyGen3PmClient client;

    @Test
    void leggiConsumoRestituisceRispostaCorretta() {
        String url = "http://192.168.1.9";
        ShellyGen3PmHttpApi api = mock(ShellyGen3PmHttpApi.class);
        when(factory.createProxy(url, ShellyGen3PmHttpApi.class)).thenReturn(api);
        ShellyPmStatusResponse expected = new ShellyPmStatusResponse(
                new BigDecimal("45.2"),
                new BigDecimal("229.8"),
                new BigDecimal("0.197"));
        when(api.leggiConsumo(any())).thenReturn(expected);

        ShellyPmStatusResponse result = client.leggiConsumo(url);

        assertEquals(new BigDecimal("45.2"), result.apower());
        assertEquals(new BigDecimal("229.8"), result.voltage());
        assertEquals(new BigDecimal("0.197"), result.current());
    }

    @Test
    void leggiConsumoInviaRequestConIdZero() {
        String url = "http://192.168.1.9";
        ShellyGen3PmHttpApi api = mock(ShellyGen3PmHttpApi.class);
        when(factory.createProxy(url, ShellyGen3PmHttpApi.class)).thenReturn(api);
        when(api.leggiConsumo(any())).thenReturn(new ShellyPmStatusResponse(
                new BigDecimal("10.0"), new BigDecimal("230.0"), new BigDecimal("0.04")));

        client.leggiConsumo(url);

        // verifica che la request inviata abbia id=0
        org.mockito.Mockito.verify(api).leggiConsumo(new ShellyPmStatusRequest(0));
    }
}
