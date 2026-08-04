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
        etaService = new EtaCalculationService();
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
        assertThat(etaService.formatEtaDisplay(0)).isEqualTo("Ready now");
        assertThat(etaService.formatEtaDisplay(45)).isEqualTo("~1 min");
        assertThat(etaService.formatEtaDisplay(300)).isEqualTo("~5 min");
        assertThat(etaService.formatEtaDisplay(3700)).isEqualTo("~1 hr 2 min");
    }
}
