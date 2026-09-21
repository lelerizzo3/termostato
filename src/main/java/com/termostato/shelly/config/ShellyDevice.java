package com.termostato.shelly.config;

public record ShellyDevice(
        String id,
        String nome,
        String ip,
        ShellyTipo tipo,
        int generazione,
        String modello) {
}
