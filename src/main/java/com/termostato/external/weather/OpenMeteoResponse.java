package com.termostato.external.weather;

import java.util.Map;

public record OpenMeteoResponse(Map<String, Object> current) {
}
