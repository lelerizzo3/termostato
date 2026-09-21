package com.termostato.shelly.external;

import com.termostato.shelly.config.ShellyDevice;
import com.termostato.shelly.config.ShellyTipo;
import org.springframework.stereotype.Component;

@Component
public class ShellyClientFactory {

    private final ShellyGen1RelayClient gen1RelayClient;
    private final ShellyGen3RelayClient gen3RelayClient;
    private final ShellyGen3PmClient gen3PmClient;

    public ShellyClientFactory(ShellyGen1RelayClient gen1RelayClient,
                               ShellyGen3RelayClient gen3RelayClient,
                               ShellyGen3PmClient gen3PmClient) {
        this.gen1RelayClient = gen1RelayClient;
        this.gen3RelayClient = gen3RelayClient;
        this.gen3PmClient = gen3PmClient;
    }

    public boolean leggiStatoRelay(ShellyDevice device) {
        requireTipo(device, ShellyTipo.RELAY);
        String url = baseUrl(device);
        return switch (device.generazione()) {
            case 1 -> gen1RelayClient.leggiStato(url);
            case 3 -> gen3RelayClient.leggiStato(url);
            default -> throw new IllegalArgumentException(
                    "Generazione non supportata per relay: " + device.generazione());
        };
    }

    public void setStatoRelay(ShellyDevice device, boolean on) {
        requireTipo(device, ShellyTipo.RELAY);
        String url = baseUrl(device);
        switch (device.generazione()) {
            case 1 -> gen1RelayClient.setStato(url, on);
            case 3 -> gen3RelayClient.setStato(url, on);
            default -> throw new IllegalArgumentException(
                    "Generazione non supportata per relay: " + device.generazione());
        }
    }

    public ShellyPmStatusResponse leggiConsumo(ShellyDevice device) {
        requireTipo(device, ShellyTipo.PM);
        String url = baseUrl(device);
        return switch (device.generazione()) {
            case 3 -> gen3PmClient.leggiConsumo(url);
            default -> throw new IllegalArgumentException(
                    "Generazione non supportata per PM: " + device.generazione());
        };
    }

    private static void requireTipo(ShellyDevice device, ShellyTipo expected) {
        if (device.tipo() != expected) {
            throw new IllegalArgumentException(
                    "Operazione non supportata per device di tipo " + device.tipo()
                            + " (atteso: " + expected + "): " + device.id());
        }
    }

    private static String baseUrl(ShellyDevice device) {
        return "http://" + device.ip();
    }
}
