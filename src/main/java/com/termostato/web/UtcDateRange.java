package com.termostato.web;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

public record UtcDateRange(Instant fromInclusive, Instant toExclusive) {

    public static UtcDateRange resolve(LocalDate from, LocalDate to, Clock clock) {
        LocalDate today = LocalDate.now(clock.withZone(ZoneOffset.UTC));
        LocalDate effectiveFrom = from != null ? from : (to != null ? to : today);
        LocalDate effectiveTo = to != null ? to : today;
        if (effectiveFrom.isAfter(effectiveTo)) {
            throw new IllegalArgumentException("Il parametro da non può essere successivo al parametro a");
        }
        return new UtcDateRange(
                effectiveFrom.atStartOfDay(ZoneOffset.UTC).toInstant(),
                effectiveTo.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant());
    }
}
