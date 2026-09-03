package com.termostato.domain.control;

import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.Map;

@Service
public class ErrorTrackingService {

    private final Map<ErrorCategory, Integer> counters = new EnumMap<>(ErrorCategory.class);

    public ErrorTrackingService() {
        resetAll();
    }

    public synchronized int increment(ErrorCategory category) {
        int next = counters.getOrDefault(category, 0) + 1;
        counters.put(category, next);
        return next;
    }

    public synchronized int current(ErrorCategory category) {
        return counters.getOrDefault(category, 0);
    }

    public synchronized void resetAll() {
        for (ErrorCategory category : ErrorCategory.values()) {
            counters.put(category, 0);
        }
    }
}
