package com.termostato.external.notification;

import com.termostato.config.ConfigurationService;
import com.termostato.domain.model.SystemConfiguration;
import com.termostato.external.RestClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final RestClientFactory factory;
    private final ConfigurationService configuration;

    public NotificationService(RestClientFactory factory, ConfigurationService configuration) {
        this.factory = factory;
        this.configuration = configuration;
    }

    public void notificaErrore(String message) {
        publish(message, "high", true);
    }

    public void notificaInformazione(String message) {
        publish(message, "default", false);
    }

    private void publish(String message, String priority, boolean always) {
        SystemConfiguration config = configuration.current();
        if (!always && !config.debugMode()) {
            return;
        }
        try {
            NtfyHttpApi api = factory.createProxy(config.ntfyUrl(), NtfyHttpApi.class);
            api.pubblica(config.ntfyTopic(), message, priority);
        } catch (RuntimeException exception) {
            // La notifica non deve bloccare il controllo né diventare un errore del sensore/relay.
            log.warn("Invio notifica ntfy fallito: {}", exception.getMessage());
        }
    }
}
