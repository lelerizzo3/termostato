package com.termostato.external.notification;

import com.termostato.config.ConfigurationService;
import com.termostato.domain.model.SystemConfiguration;
import com.termostato.external.RestClientFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock RestClientFactory factory;
    @Mock ConfigurationService configuration;
    @Mock NtfyHttpApi api;

    @Test
    void nonInviaErroriQuandoLaNotificaEDisabilitata() {
        when(configuration.current()).thenReturn(config(false));
        NotificationService service = new NotificationService(factory, configuration);

        service.notificaErrore("errore");

        verify(factory, never()).createProxy("http://ntfy", NtfyHttpApi.class);
    }

    @Test
    void inviaErroriQuandoLaNotificaEAbilitata() {
        when(configuration.current()).thenReturn(config(true));
        when(factory.createProxy("http://ntfy", NtfyHttpApi.class)).thenReturn(api);
        NotificationService service = new NotificationService(factory, configuration);

        service.notificaErrore("errore");

        verify(api).pubblica("topic", "errore", "high");
    }

    private static SystemConfiguration config(boolean errorNotificationsEnabled) {
        return new SystemConfiguration(
                new BigDecimal("0.3"), false, null, 60, 3, 30,
                "http://ntfy", "topic", false, "http://sensor", "http://relay", "./data/test.db",
                List.of(), "http://weather", new BigDecimal("37.6167"),
                new BigDecimal("15.1667"), errorNotificationsEnabled);
    }
}
