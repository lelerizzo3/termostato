package com.termostato.shelly.web;

import com.termostato.shelly.config.ShellyDevice;
import com.termostato.shelly.config.ShellyTipo;
import com.termostato.shelly.pm.ShellyConsumoLogRecord;

import java.util.List;

public record ShellyConsumiResponse(
        String id,
        String nome,
        String ip,
        String modello,
        int generazione,
        ShellyTipo tipo,
        List<ShellyConsumoLogRecord> misure) {

    public static ShellyConsumiResponse from(ShellyDevice device, List<ShellyConsumoLogRecord> misure) {
        return new ShellyConsumiResponse(
                device.id(),
                device.nome(),
                device.ip(),
                device.modello(),
                device.generazione(),
                device.tipo(),
                misure);
    }
}
