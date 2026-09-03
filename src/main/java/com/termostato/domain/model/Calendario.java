package com.termostato.domain.model;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record Calendario(Map<GiornoSettimana, List<IntervalloOrario>> giorni) {

    public Calendario {
        if (giorni == null || giorni.size() != GiornoSettimana.values().length
                || !giorni.keySet().equals(EnumSet.allOf(GiornoSettimana.class))) {
            throw new IllegalArgumentException("Il calendario deve contenere esattamente 7 giorni");
        }
        EnumMap<GiornoSettimana, List<IntervalloOrario>> copy = new EnumMap<>(GiornoSettimana.class);
        for (GiornoSettimana giorno : GiornoSettimana.values()) {
            List<IntervalloOrario> intervalli = giorni.get(giorno);
            if (intervalli == null) {
                throw new IllegalArgumentException("L'elenco intervalli di " + giorno + " non può essere nullo");
            }
            List<IntervalloOrario> sorted = new ArrayList<>(intervalli);
            sorted.sort((left, right) -> left.oraInizio().compareTo(right.oraInizio()));
            for (int i = 1; i < sorted.size(); i++) {
                if (sorted.get(i - 1).oraFine().isAfter(sorted.get(i).oraInizio())) {
                    throw new IllegalArgumentException("Gli intervalli di " + giorno + " non possono sovrapporsi");
                }
            }
            copy.put(giorno, List.copyOf(sorted));
        }
        giorni = Collections.unmodifiableMap(copy);
    }

    public static Calendario vuoto() {
        EnumMap<GiornoSettimana, List<IntervalloOrario>> giorni = new EnumMap<>(GiornoSettimana.class);
        for (GiornoSettimana giorno : GiornoSettimana.values()) {
            giorni.put(giorno, List.of());
        }
        return new Calendario(giorni);
    }

    public List<IntervalloOrario> per(DayOfWeek day) {
        return giorni.get(GiornoSettimana.from(day));
    }

    public CalendarioDocument toDocument() {
        Map<String, List<IntervalloOrario>> document = new LinkedHashMap<>();
        for (GiornoSettimana giorno : GiornoSettimana.values()) {
            document.put(giorno.jsonKey(), giorni.get(giorno));
        }
        return new CalendarioDocument(document);
    }

    public static Calendario fromDocument(CalendarioDocument document) {
        if (document == null || document.giorni() == null
                || document.giorni().size() != GiornoSettimana.values().length) {
            throw new IllegalArgumentException("Il calendario deve contenere esattamente 7 giorni");
        }
        EnumMap<GiornoSettimana, List<IntervalloOrario>> normalized = new EnumMap<>(GiornoSettimana.class);
        for (Map.Entry<String, List<IntervalloOrario>> entry : document.giorni().entrySet()) {
            GiornoSettimana day = GiornoSettimana.fromJsonKey(entry.getKey());
            if (normalized.put(day, entry.getValue()) != null) {
                throw new IllegalArgumentException("Giorno duplicato nel calendario: " + entry.getKey());
            }
        }
        return new Calendario(normalized);
    }
}
