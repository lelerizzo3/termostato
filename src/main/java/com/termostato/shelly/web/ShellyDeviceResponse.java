package com.termostato.shelly.web;

import com.termostato.shelly.config.ShellyDevice;
import com.termostato.shelly.config.ShellyTipo;

public record ShellyDeviceResponse(
        String id,
        String nome,
        String ip,
        ShellyTipo tipo,
        String modello,
        int generazione) {

    public static ShellyDeviceResponse from(ShellyDevice device) {
        return new ShellyDeviceResponse(
                device.id(),
                device.nome(),
                device.ip(),
                device.tipo(),
                device.modello(),
                device.generazione());
    }
}
