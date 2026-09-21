package com.termostato.shelly.external;

import com.termostato.external.RestClientFactory;
import org.springframework.stereotype.Component;

@Component
public class ShellyGen3PmClient {

    private final RestClientFactory factory;

    public ShellyGen3PmClient(RestClientFactory factory) {
        this.factory = factory;
    }

    public ShellyPmStatusResponse leggiConsumo(String baseUrl) {
        ShellyPmStatusResponse response = api(baseUrl).leggiConsumo(new ShellyPmStatusRequest());
        if (response == null) {
            throw new IllegalStateException("Risposta PM Gen 3 vuota da " + baseUrl);
        }
        return response;
    }

    private ShellyGen3PmHttpApi api(String baseUrl) {
        return factory.createProxy(baseUrl, ShellyGen3PmHttpApi.class);
    }
}
