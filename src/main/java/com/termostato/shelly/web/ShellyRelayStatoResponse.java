package com.termostato.shelly.web;

import com.termostato.shelly.config.ShellyDevice;
import com.termostato.shelly.config.ShellyTipo;

public record ShellyRelayStatoResponse(
        String id,
        String nome,
        String ip,
        String modello,
        int generazione,
        ShellyTipo tipo,
        boolean acceso) {

    public static ShellyRelayStatoResponse from(ShellyDevice device, boolean acceso) {
        return new ShellyRelayStatoResponse(
                device.id(),
                device.nome(),
                device.ip(),
                device.modello(),
                device.generazione(),
                device.tipo(),
                acceso);
    }
}
