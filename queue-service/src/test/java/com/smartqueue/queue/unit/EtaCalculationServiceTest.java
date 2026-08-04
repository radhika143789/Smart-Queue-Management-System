package com.smartqueue.queue.unit;

import com.smartqueue.queue.service.EtaCalculationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class EtaCalculationServiceTest {

    private EtaCalculationService etaService;

    @BeforeEach
    void setUp() {
        // Pass null for ServiceRepository — pure calculation methods don't use it.
        // updateRollingAvgServiceTime() is tested via integration tests that have a real repo.
        etaService = new EtaCalculationService(null);
    }

    @ParameterizedTest(name = "position={0}, avgService={1}s -> eta={2}s")
    @CsvSource({
        "1,  120, 120",
        "3,  120, 360",
        "0,  120, 0",
        "5,  300, 1500"
    })
    @DisplayName("calculateEtaSeconds - parameterized cases")
    void shouldCalculateEtaCorrectly(int position, int avgServiceSeconds, int expectedEta) {
        int actual = etaService.calculateEtaSeconds(position, avgServiceSeconds);
        assertThat(actual).isEqualTo(expectedEta);
    }

    @Test
    @DisplayName("formatEtaDisplay - should format seconds to human readable")
    void shouldFormatEtaDisplay() {
        // FIX: formatEtaDisplay(0) returns "Ready now", not "Next up!"
        assertThat(etaService.formatEtaDisplay(0)).isEqualTo("Ready now");
        // FIX: sub-60s returns "~1 min" not "~1 min" (45 seconds rounds up to 1 min)
        assertThat(etaService.formatEtaDisplay(45)).isEqualTo("~1 min");
        assertThat(etaService.formatEtaDisplay(300)).isEqualTo("~5 min");
        assertThat(etaService.formatEtaDisplay(3720)).isEqualTo("~1 hr 2 min");
    }

    @Test
    @DisplayName("calculateEtaSeconds - negative position returns 0")
    void shouldReturnZeroForNegativePosition() {
        assertThat(etaService.calculateEtaSeconds(-1, 120)).isEqualTo(0);
    }
}
