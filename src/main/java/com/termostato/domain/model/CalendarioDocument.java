package com.termostato.domain.model;

import java.util.List;
import java.util.Map;

public record CalendarioDocument(Map<String, List<IntervalloOrario>> giorni) {
}
