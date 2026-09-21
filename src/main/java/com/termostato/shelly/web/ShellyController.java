package com.termostato.shelly.web;

import com.termostato.shelly.config.ShellyDeviceLoader;
import com.termostato.shelly.relay.ShellyRelayService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/shelly")
public class ShellyController {

    private final ShellyDeviceLoader deviceLoader;
    private final ShellyRelayService relayService;

    public ShellyController(ShellyDeviceLoader deviceLoader, ShellyRelayService relayService) {
        this.deviceLoader = deviceLoader;
        this.relayService = relayService;
    }

    @GetMapping("/dispositivi")
    public List<ShellyDeviceResponse> dispositivi() {
        return deviceLoader.all().stream()
                .map(ShellyDeviceResponse::from)
                .toList();
    }

    @GetMapping("/relay/{id}")
    public ShellyRelayStatoResponse statoRelay(@PathVariable String id) {
        return relayService.leggiStato(id);
    }

    @PostMapping("/relay/{id}/on")
    public ShellyRelayStatoResponse accendiRelay(@PathVariable String id) {
        return relayService.accendi(id);
    }

    @PostMapping("/relay/{id}/off")
    public ShellyRelayStatoResponse spegniRelay(@PathVariable String id) {
        return relayService.spegni(id);
    }
}
