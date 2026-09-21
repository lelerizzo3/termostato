package com.termostato.shelly.external;

import com.termostato.shelly.config.ShellyDevice;
import com.termostato.shelly.config.ShellyTipo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShellyClientFactoryTest {

    @Mock ShellyGen1RelayClient gen1RelayClient;
    @Mock ShellyGen3RelayClient gen3RelayClient;
    @Mock ShellyGen3PmClient gen3PmClient;

    @InjectMocks
    ShellyClientFactory factory;

    private static final ShellyDevice RELAY_GEN1 =
            new ShellyDevice("portone", "Portone", "192.168.1.2", ShellyTipo.RELAY, 1, "SHSW-1");
    private static final ShellyDevice RELAY_GEN3 =
            new ShellyDevice("cancello", "Cancello", "192.168.1.3", ShellyTipo.RELAY, 3, "S3SW-001X8EU");
    private static final ShellyDevice PM_GEN3 =
            new ShellyDevice("frigo", "Frigo", "192.168.1.9", ShellyTipo.PM, 3, "SNPM-001PCEU16");

    @Test
    void leggiStatoRelayGen1UsaGen1Client() {
        when(gen1RelayClient.leggiStato("http://192.168.1.2")).thenReturn(true);

        assertTrue(factory.leggiStatoRelay(RELAY_GEN1));
        verify(gen1RelayClient).leggiStato("http://192.168.1.2");
    }

    @Test
    void leggiStatoRelayGen3UsaGen3Client() {
        when(gen3RelayClient.leggiStato("http://192.168.1.3")).thenReturn(false);

        factory.leggiStatoRelay(RELAY_GEN3);
        verify(gen3RelayClient).leggiStato("http://192.168.1.3");
    }

    @Test
    void setStatoRelayGen1UsaGen1Client() {
        factory.setStatoRelay(RELAY_GEN1, true);
        verify(gen1RelayClient).setStato("http://192.168.1.2", true);
    }

    @Test
    void setStatoRelayGen3UsaGen3Client() {
        factory.setStatoRelay(RELAY_GEN3, false);
        verify(gen3RelayClient).setStato("http://192.168.1.3", false);
    }

    @Test
    void leggiConsumoGen3UsaGen3PmClient() {
        ShellyPmStatusResponse expected = new ShellyPmStatusResponse(
                new BigDecimal("12.5"), new BigDecimal("230.1"), new BigDecimal("0.054"));
        when(gen3PmClient.leggiConsumo("http://192.168.1.9")).thenReturn(expected);

        ShellyPmStatusResponse result = factory.leggiConsumo(PM_GEN3);
        assertEquals(new BigDecimal("12.5"), result.apower());
        verify(gen3PmClient).leggiConsumo("http://192.168.1.9");
    }

    @Test
    void leggiStatoRelaySuPmLanciaEccezione() {
        assertThrows(IllegalArgumentException.class, () -> factory.leggiStatoRelay(PM_GEN3));
    }

    @Test
    void setStatoRelaySuPmLanciaEccezione() {
        assertThrows(IllegalArgumentException.class, () -> factory.setStatoRelay(PM_GEN3, true));
    }

    @Test
    void leggiConsumoSuRelayLanciaEccezione() {
        assertThrows(IllegalArgumentException.class, () -> factory.leggiConsumo(RELAY_GEN1));
    }

    @Test
    void generazioneNonSupportataPerRelayLanciaEccezione() {
        ShellyDevice relayGen2 = new ShellyDevice("x", "X", "192.168.1.50", ShellyTipo.RELAY, 2, "UNKNOWN");
        assertThrows(IllegalArgumentException.class, () -> factory.leggiStatoRelay(relayGen2));
    }

    @Test
    void generazioneNonSupportataPerPmLanciaEccezione() {
        ShellyDevice pmGen1 = new ShellyDevice("x", "X", "192.168.1.50", ShellyTipo.PM, 1, "UNKNOWN");
        assertThrows(IllegalArgumentException.class, () -> factory.leggiConsumo(pmGen1));
    }
}
