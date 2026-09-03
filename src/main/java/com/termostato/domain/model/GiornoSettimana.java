package com.termostato.domain.model;

import java.text.Normalizer;
import java.time.DayOfWeek;
import java.util.Locale;

public enum GiornoSettimana {
    LUNEDI(DayOfWeek.MONDAY, "lunedi"),
    MARTEDI(DayOfWeek.TUESDAY, "martedi"),
    MERCOLEDI(DayOfWeek.WEDNESDAY, "mercoledi"),
    GIOVEDI(DayOfWeek.THURSDAY, "giovedi"),
    VENERDI(DayOfWeek.FRIDAY, "venerdi"),
    SABATO(DayOfWeek.SATURDAY, "sabato"),
    DOMENICA(DayOfWeek.SUNDAY, "domenica");

    private final DayOfWeek javaDay;
    private final String jsonKey;

    GiornoSettimana(DayOfWeek javaDay, String jsonKey) {
        this.javaDay = javaDay;
        this.jsonKey = jsonKey;
    }

    public DayOfWeek javaDay() {
        return javaDay;
    }

    public String jsonKey() {
        return jsonKey;
    }

    public static GiornoSettimana from(DayOfWeek day) {
        for (GiornoSettimana value : values()) {
            if (value.javaDay == day) {
                return value;
            }
        }
        throw new IllegalArgumentException("Giorno non riconosciuto: " + day);
    }

    public static GiornoSettimana fromJsonKey(String key) {
        if (key == null) {
            throw new IllegalArgumentException("Il giorno del calendario non può essere nullo");
        }
        String normalized = Normalizer.normalize(key, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[_\\- ]", "");
        for (GiornoSettimana value : values()) {
            if (value.jsonKey.equals(normalized)
                    || value.name().toLowerCase(Locale.ROOT).equals(normalized)
                    || value.javaDay.name().toLowerCase(Locale.ROOT).equals(normalized)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Giorno del calendario non valido: " + key);
    }
}
