package com.termostato.external.relay;

import com.termostato.config.ConfigurationService;
import com.termostato.external.RestClientFactory;
import org.springframework.stereotype.Component;

@Component
public class RelayClient {

    private final RestClientFactory factory;
    private final ConfigurationService configuration;

    public RelayClient(RestClientFactory factory, ConfigurationService configuration) {
        this.factory = factory;
        this.configuration = configuration;
    }

    public boolean leggiStato() {
        RelayHttpApi api = api();
        RelayStateResponse response = api.leggiStato();
        if (response == null) {
            throw new IllegalStateException("Risposta stato relay vuota");
        }
        return response.acceso();
    }

    public void inviaComando(boolean acceso) {
        api().inviaComando(new RelayCommand(acceso));
    }

    private RelayHttpApi api() {
        return factory.createProxy(configuration.current().relayUrl(), RelayHttpApi.class, true);
    }
}
