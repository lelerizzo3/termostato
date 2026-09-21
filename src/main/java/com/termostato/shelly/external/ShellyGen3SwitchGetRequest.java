package com.termostato.shelly.external;

public record ShellyGen3SwitchGetRequest(int id) {
    public ShellyGen3SwitchGetRequest() {
        this(0);
    }
}
