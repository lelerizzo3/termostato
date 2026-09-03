package com.termostato.domain.control;

import com.termostato.domain.model.SystemConfiguration;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HeatingDecisionCalculatorTest {

    private final HeatingDecisionCalculator calculator = new HeatingDecisionCalculator();
    private final SystemConfiguration configuration = new SystemConfiguration(
            new BigDecimal("0.3"), false, null, 60, 3, 30,
            "https://ntfy.sh", "sliverd", false, "http://sensor", "http://relay", "./data/test.db");

    @Test
    void accendeSottoLaSogliaDiAttivazione() {
        assertEquals(HeatingDecision.ON, calculator.calculate(
                new BigDecimal("19.9"), Optional.of(new BigDecimal("20.5")), configuration, false));
    }

    @Test
    void spegneAlRaggiungimentoDelTarget() {
        assertEquals(HeatingDecision.OFF, calculator.calculate(
                new BigDecimal("20.5"), Optional.of(new BigDecimal("20.5")), configuration, true));
    }

    @Test
    void mantieneLoStatoNellaZonaNeutra() {
        assertEquals(HeatingDecision.UNCHANGED, calculator.calculate(
                new BigDecimal("20.3"), Optional.of(new BigDecimal("20.5")), configuration, true));
        assertEquals(HeatingDecision.UNCHANGED, calculator.calculate(
                new BigDecimal("20.3"), Optional.of(new BigDecimal("20.5")), configuration, false));
    }

    @Test
    void senzaTargetSpegneSoloSeIlRelayRisultaAcceso() {
        assertEquals(HeatingDecision.OFF, calculator.calculate(
                new BigDecimal("20.0"), Optional.empty(), configuration, true));
        assertEquals(HeatingDecision.UNCHANGED, calculator.calculate(
                new BigDecimal("20.0"), Optional.empty(), configuration, false));
    }
}
