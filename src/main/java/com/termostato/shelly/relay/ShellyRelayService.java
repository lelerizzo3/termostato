package com.termostato.shelly.relay;

import com.termostato.shelly.config.ShellyDevice;
import com.termostato.shelly.config.ShellyDeviceLoader;
import com.termostato.shelly.config.ShellyTipo;
import com.termostato.shelly.external.ShellyClientFactory;
import com.termostato.shelly.web.ShellyRelayStatoResponse;
import org.springframework.stereotype.Service;

@Service
public class ShellyRelayService {

    private final ShellyDeviceLoader deviceLoader;
    private final ShellyClientFactory clientFactory;

    public ShellyRelayService(ShellyDeviceLoader deviceLoader, ShellyClientFactory clientFactory) {
        this.deviceLoader = deviceLoader;
        this.clientFactory = clientFactory;
    }

    public ShellyRelayStatoResponse leggiStato(String id) {
        ShellyDevice device = requireRelay(id);
        boolean acceso = clientFactory.leggiStatoRelay(device);
        return ShellyRelayStatoResponse.from(device, acceso);
    }

    public ShellyRelayStatoResponse accendi(String id) {
        ShellyDevice device = requireRelay(id);
        clientFactory.setStatoRelay(device, true);
        boolean acceso = clientFactory.leggiStatoRelay(device);
        return ShellyRelayStatoResponse.from(device, acceso);
    }

    public ShellyRelayStatoResponse spegni(String id) {
        ShellyDevice device = requireRelay(id);
        clientFactory.setStatoRelay(device, false);
        boolean acceso = clientFactory.leggiStatoRelay(device);
        return ShellyRelayStatoResponse.from(device, acceso);
    }

    private ShellyDevice requireRelay(String id) {
        ShellyDevice device = deviceLoader.byId(id);
        if (device.tipo() != ShellyTipo.RELAY) {
            throw new IllegalArgumentException(
                    "Il dispositivo '" + id + "' è di tipo " + device.tipo() + ", non un relay");
        }
        return device;
    }
}
