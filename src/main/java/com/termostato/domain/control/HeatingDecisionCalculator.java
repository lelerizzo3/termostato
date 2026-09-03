package com.termostato.domain.control;

import com.termostato.domain.model.SystemConfiguration;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

@Component
public class HeatingDecisionCalculator {

    public HeatingDecision calculate(BigDecimal roomTemperature,
                                     Optional<BigDecimal> target,
                                     SystemConfiguration configuration,
                                     boolean currentRelayOn) {
        if (target.isEmpty()) {
            return currentRelayOn ? HeatingDecision.OFF : HeatingDecision.UNCHANGED;
        }
        BigDecimal desired = target.get();
        if (roomTemperature.compareTo(desired.subtract(configuration.sogliaAttivazione())) < 0) {
            return HeatingDecision.ON;
        }
        if (roomTemperature.compareTo(desired) >= 0) {
            return HeatingDecision.OFF;
        }
        return HeatingDecision.UNCHANGED;
    }
}
