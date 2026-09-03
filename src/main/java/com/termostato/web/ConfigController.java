package com.termostato.web;

import com.termostato.config.ConfigurationService;
import com.termostato.domain.model.Calendario;
import com.termostato.domain.model.CalendarioDocument;
import com.termostato.domain.model.SystemConfiguration;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/config")
public class ConfigController {

    private final ConfigurationService configuration;

    public ConfigController(ConfigurationService configuration) {
        this.configuration = configuration;
    }

    @GetMapping
    public SystemConfiguration getConfiguration() {
        return configuration.current();
    }

    @PutMapping
    public SystemConfiguration updateConfiguration(@Valid @RequestBody SystemConfiguration requested) {
        configuration.update(requested);
        return configuration.current();
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
