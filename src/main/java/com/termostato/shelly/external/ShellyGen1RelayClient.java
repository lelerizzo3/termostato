package com.termostato.shelly.external;

import com.termostato.external.RestClientFactory;
import org.springframework.stereotype.Component;

@Component
public class ShellyGen1RelayClient {

    private final RestClientFactory factory;

    public ShellyGen1RelayClient(RestClientFactory factory) {
        this.factory = factory;
    }

    public boolean leggiStato(String baseUrl) {
        ShellyGen1RelayResponse response = api(baseUrl).leggiStato();
        if (response == null) {
            throw new IllegalStateException("Risposta stato relay Gen 1 vuota da " + baseUrl);
        }
        return response.ison();
    }

    public void setStato(String baseUrl, boolean on) {
        api(baseUrl).inviaComando(on ? "on" : "off");
    }

    private ShellyGen1RelayHttpApi api(String baseUrl) {
        return factory.createProxy(baseUrl, ShellyGen1RelayHttpApi.class);
    }
}
