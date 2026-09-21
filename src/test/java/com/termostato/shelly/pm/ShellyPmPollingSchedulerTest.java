package com.termostato.shelly.pm;

import com.termostato.config.BootstrapProperties;
import com.termostato.persistence.ErrorLogRepository;
import com.termostato.shelly.config.ShellyDevice;
import com.termostato.shelly.config.ShellyDeviceLoader;
import com.termostato.shelly.config.ShellyTipo;
import com.termostato.shelly.external.ShellyClientFactory;
import com.termostato.shelly.external.ShellyPmStatusResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShellyPmPollingSchedulerTest {

    @Mock ShellyDeviceLoader deviceLoader;
    @Mock ShellyClientFactory clientFactory;
    @Mock ShellyConsumoLogRepository consumoLogRepository;
    @Mock ErrorLogRepository errorLogRepository;

    private static final ShellyDevice PM1 =
            new ShellyDevice("frigo", "Frigo", "192.168.1.9", ShellyTipo.PM, 3, "SNPM-001PCEU16");
    private static final ShellyDevice PM2 =
            new ShellyDevice("modem", "Modem", "192.168.1.5", ShellyTipo.PM, 3, "SNPM-001PCEU16");
    private static final ShellyDevice RELAY =
            new ShellyDevice("portone", "Portone", "192.168.1.2", ShellyTipo.RELAY, 1, "SHSW-1");

    private static final ShellyPmStatusResponse STATUS = new ShellyPmStatusResponse(
            new BigDecimal("45.2"), new BigDecimal("229.8"), new BigDecimal("0.197"));

    private ShellyPmPollingScheduler scheduler(Clock clock, int intervalloSecondi) {
        BootstrapProperties props = new BootstrapProperties();
        props.setIntervalloPollingConsumiSecondi(intervalloSecondi);
        return new ShellyPmPollingScheduler(deviceLoader, clientFactory,
                consumoLogRepository, errorLogRepository, props, clock);
    }

    @Test
    void tickSaltatoPrimaDellaScadenza() {
        Instant now = Instant.parse("2026-09-20T10:00:00Z");
        Clock clock = Clock.fixed(now, ZoneOffset.UTC);
        ShellyPmPollingScheduler sched = scheduler(clock, 30);

        // primo tick: nextPoll è Instant.MIN, viene eseguito
        when(deviceLoader.all()).thenReturn(List.of(PM1));
        when(clientFactory.leggiConsumo(PM1)).thenReturn(STATUS);
        sched.tick();
        verify(consumoLogRepository).save(any());

        // secondo tick con clock fermo: nextPoll è now+30s, deve essere saltato
        clearInvocations(consumoLogRepository);
        sched.tick();
        verify(consumoLogRepository, never()).save(any());
    }

    @Test
    void tickPollaTuttiIPmDevice() {
        Clock clock = Clock.fixed(Instant.parse("2026-09-20T10:00:00Z"), ZoneOffset.UTC);
        ShellyPmPollingScheduler sched = scheduler(clock, 30);

        when(deviceLoader.all()).thenReturn(List.of(PM1, RELAY, PM2));
        when(clientFactory.leggiConsumo(PM1)).thenReturn(STATUS);
        when(clientFactory.leggiConsumo(PM2)).thenReturn(STATUS);

        sched.tick();

        verify(consumoLogRepository).save(argThat(r -> "frigo".equals(r.deviceId())));
        verify(consumoLogRepository).save(argThat(r -> "modem".equals(r.deviceId())));
        // il relay non deve essere pollato
        verify(clientFactory, never()).leggiConsumo(RELAY);
    }

    @Test
    void erroreUnDeviceNonBloccaGliAltri() {
        Clock clock = Clock.fixed(Instant.parse("2026-09-20T10:00:00Z"), ZoneOffset.UTC);
        ShellyPmPollingScheduler sched = scheduler(clock, 30);

        when(deviceLoader.all()).thenReturn(List.of(PM1, PM2));
        when(clientFactory.leggiConsumo(PM1)).thenThrow(new RuntimeException("timeout"));
        when(clientFactory.leggiConsumo(PM2)).thenReturn(STATUS);

        sched.tick();

        // PM1 fallisce → errore loggato in error_log
        verify(errorLogRepository).save(any());
        // PM2 prosegue normalmente
        verify(consumoLogRepository).save(argThat(r -> "modem".equals(r.deviceId())));
    }
}
