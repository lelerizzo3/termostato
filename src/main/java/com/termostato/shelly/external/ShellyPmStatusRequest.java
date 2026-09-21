package com.termostato.shelly.external;

public record ShellyPmStatusRequest(int id) {
    public ShellyPmStatusRequest() {
        this(0);
    }
}
