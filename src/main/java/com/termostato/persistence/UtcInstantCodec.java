package com.termostato.persistence;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

public final class UtcInstantCodec {

    private static final DateTimeFormatter FORMATTER = new DateTimeFormatterBuilder()
            .appendInstant(9)
            .toFormatter();

    private UtcInstantCodec() {
    }

    public static String format(Instant value) {
        return FORMATTER.format(value);
    }

    public static Instant parse(String value) {
        return Instant.parse(value);
    }
}
