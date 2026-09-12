package com.termostato.web;

import com.termostato.config.ConfigurationService;
import com.termostato.domain.control.ZoneResolver;
import com.termostato.domain.model.Calendario;
import com.termostato.domain.model.CalendarioDocument;
import com.termostato.domain.model.SystemConfiguration;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.Instant;

@RestController
@RequestMapping("/config")
public class ConfigController {

    private final ConfigurationService configuration;
    private final Clock clock;

    public ConfigController(ConfigurationService configuration, Clock clock) {
        this.configuration = configuration;
        this.clock = clock;
    }

    @GetMapping
    public SystemConfiguration getConfiguration() {
        return configuration.current();
    }

    @PutMapping
    public SystemConfiguration updateConfiguration(@Valid @RequestBody SystemConfiguration requested) {
        validaOverrideFine(requested);
        configuration.update(requested);
        return configuration.current();
    }

    /**
     * La forzatura può avere una fine ({@code overrideFine}, orario civile locale). Se presente con
     * forzatura attiva deve essere futura rispetto all'ora corrente: un valore già trascorso viene
     * rifiutato con HTTP 400 (via {@link IllegalArgumentException} gestita dal RestExceptionHandler).
     */
    private void validaOverrideFine(SystemConfiguration requested) {
        if (!requested.overrideAttivo() || requested.overrideFine() == null) {
            return;
        }
        Instant fine = ZoneResolver.toInstant(requested.overrideFine(), requested);
        if (!fine.isAfter(clock.instant())) {
            throw new IllegalArgumentException("override_fine deve essere un istante futuro");
        }
    }

    @GetMapping("/calendario")
    public CalendarioDocument getCalendario() {
        return configuration.currentCalendario().toDocument();
    }

    @PutMapping("/calendario")
    public CalendarioDocument updateCalendario(@Valid @RequestBody CalendarioDocument requested) {
        Calendario validated = Calendario.fromDocument(requested);
        configuration.updateCalendario(validated);
        return configuration.currentCalendario().toDocument();
    }
}
