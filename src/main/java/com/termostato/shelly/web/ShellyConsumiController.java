package com.termostato.shelly.web;

import com.termostato.shelly.config.ShellyDevice;
import com.termostato.shelly.config.ShellyDeviceLoader;
import com.termostato.shelly.config.ShellyTipo;
import com.termostato.shelly.pm.ShellyConsumoLogRecord;
import com.termostato.shelly.pm.ShellyConsumoLogRepository;
import com.termostato.web.UtcDateRange;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/shelly")
public class ShellyConsumiController {

    private final ShellyDeviceLoader deviceLoader;
    private final ShellyConsumoLogRepository consumoLogRepository;
    private final Clock clock;

    public ShellyConsumiController(ShellyDeviceLoader deviceLoader,
                                   ShellyConsumoLogRepository consumoLogRepository,
                                   Clock clock) {
        this.deviceLoader = deviceLoader;
        this.consumoLogRepository = consumoLogRepository;
        this.clock = clock;
    }

    @GetMapping("/consumi/{id}")
    public ShellyConsumiResponse consumi(
            @PathVariable String id,
            @RequestParam(name = "da", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(name = "a", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        ShellyDevice device = deviceLoader.byId(id);
        if (device.tipo() != ShellyTipo.PM) {
            throw new IllegalArgumentException(
                    "Il dispositivo '" + id + "' è di tipo " + device.tipo()
                            + ": i consumi sono disponibili solo per dispositivi PM");
        }
        UtcDateRange range = UtcDateRange.resolve(from, to, clock);
        List<ShellyConsumoLogRecord> misure =
                consumoLogRepository.findBetween(id, range.fromInclusive(), range.toExclusive());
        return ShellyConsumiResponse.from(device, misure);
    }
}
