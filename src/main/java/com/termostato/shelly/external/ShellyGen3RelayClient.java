package com.termostato.shelly.external;

import com.termostato.external.RestClientFactory;
import org.springframework.stereotype.Component;

@Component
public class ShellyGen3RelayClient {

    private final RestClientFactory factory;

    public ShellyGen3RelayClient(RestClientFactory factory) {
        this.factory = factory;
    }

    public boolean leggiStato(String baseUrl) {
        ShellyGen3SwitchGetResponse response = api(baseUrl).leggiStato(new ShellyGen3SwitchGetRequest());
        if (response == null) {
            throw new IllegalStateException("Risposta stato relay Gen 3 vuota da " + baseUrl);
        }
        return response.output();
    }

    public void setStato(String baseUrl, boolean on) {
        api(baseUrl).setStato(new ShellyGen3SwitchSetRequest(on));
    }

    private ShellyGen3RelayHttpApi api(String baseUrl) {
        return factory.createProxy(baseUrl, ShellyGen3RelayHttpApi.class);
    }
}
