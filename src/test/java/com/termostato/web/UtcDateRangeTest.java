package com.termostato.web;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UtcDateRangeTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-09-03T10:29:56Z"), ZoneOffset.UTC);

    @Test
    void senzaParametriUsaIlGiornoUtcCorrente() {
        UtcDateRange range = UtcDateRange.resolve(null, null, clock);

        assertEquals(Instant.parse("2026-09-03T00:00:00Z"), range.fromInclusive());
        assertEquals(Instant.parse("2026-09-04T00:00:00Z"), range.toExclusive());
    }

    @Test
    void soloDaArrivaAllaFineDelGiornoCorrente() {
        UtcDateRange range = UtcDateRange.resolve(LocalDate.of(2026, 9, 1), null, clock);

        assertEquals(Instant.parse("2026-09-01T00:00:00Z"), range.fromInclusive());
        assertEquals(Instant.parse("2026-09-04T00:00:00Z"), range.toExclusive());
    }

    @Test
    void rangeInvertitoEInvalido() {
        assertThrows(IllegalArgumentException.class, () -> UtcDateRange.resolve(
                LocalDate.of(2026, 9, 4), LocalDate.of(2026, 9, 3), clock));
    }
}
