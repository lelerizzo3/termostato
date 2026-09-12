package com.termostato.domain.control;

import com.termostato.domain.model.SystemConfiguration;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * Centralizza la conversione tra {@link Instant} assoluto e orario civile locale in base al
 * {@code fusoOrario}/{@code oraLegale} della configurazione. Riusata sia per interpretare gli
 * intervalli del calendario sia per valutare la scadenza della forzatura ({@code overrideFine}).
 */
public final class ZoneResolver {

    private ZoneResolver() {
    }

    /** Converte un istante assoluto nell'orario civile del fuso configurato. */
    public static LocalDateTime toLocal(Instant instant, SystemConfiguration configuration) {
        ZoneId zone = ZoneId.of(configuration.fusoOrario());
        if (configuration.oraLegale()) {
            // ZoneId usa il database TZDB: il passaggio CET/CEST è automatico.
            return instant.atZone(zone).toLocalDateTime();
        }
        // Modalità esplicita senza ora legale: usa l'offset standard del fuso.
        ZoneOffset standardOffset = zone.getRules().getStandardOffset(instant);
        return LocalDateTime.ofInstant(instant, standardOffset);
    }

    /** Converte un orario civile locale nell'istante assoluto corrispondente nel fuso configurato. */
    public static Instant toInstant(LocalDateTime localDateTime, SystemConfiguration configuration) {
        ZoneId zone = ZoneId.of(configuration.fusoOrario());
        if (configuration.oraLegale()) {
            return localDateTime.atZone(zone).toInstant();
        }
        ZoneOffset standardOffset = zone.getRules().getStandardOffset(localDateTime.atZone(zone).toInstant());
        return localDateTime.toInstant(standardOffset);
    }
}
