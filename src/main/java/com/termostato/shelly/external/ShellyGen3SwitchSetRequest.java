package com.termostato.shelly.external;

public record ShellyGen3SwitchSetRequest(int id, boolean on) {
    public ShellyGen3SwitchSetRequest(boolean on) {
        this(0, on);
    }
}
