package com.pccheck.agent.collector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class MetricsTest {

    @Test
    void percentCalculaValorValido() {
        assertEquals(25.0, Metrics.percent(4L, 16L), 0.0001);
        assertEquals(0.0, Metrics.percent(0L, 16L), 0.0001);
        assertEquals(100.0, Metrics.percent(16L, 16L), 0.0001);
    }

    @Test
    void percentRetornaNullParaValoresInvalidos() {
        assertNull(Metrics.percent(null, 16L));
        assertNull(Metrics.percent(4L, null));
        assertNull(Metrics.percent(4L, 0L));
        assertNull(Metrics.percent(-1L, 16L));
        assertNull(Metrics.percent(17L, 16L));
    }

    @Test
    void positiveOrNullTrataSentinelas() {
        assertNull(Metrics.positiveOrNull(-1L));
        assertNull(Metrics.positiveOrNull(0L));
        assertEquals(3_600_000_000L, Metrics.positiveOrNull(3_600_000_000L));
        assertNull(Metrics.positiveOrNull(-1));
        assertEquals(8, Metrics.positiveOrNull(8));
    }

    @Test
    void cleanTextIgnoraVazioEUnknown() {
        assertNull(Metrics.cleanText(null));
        assertNull(Metrics.cleanText("   "));
        assertNull(Metrics.cleanText("unknown"));
        assertNull(Metrics.cleanText("Unknown"));
        assertEquals("Intel", Metrics.cleanText("  Intel "));
    }

    @Test
    void cpuLoadConverteEDescartaValoresInvalidos() {
        assertEquals(50.0, Metrics.cpuLoadToPercent(0.5), 0.0001);
        assertEquals(0.0, Metrics.cpuLoadToPercent(0.0), 0.0001);
        assertNull(Metrics.cpuLoadToPercent(Double.NaN));
        assertNull(Metrics.cpuLoadToPercent(-1.0));
        assertNull(Metrics.cpuLoadToPercent(1.5));
        assertNull(Metrics.cpuLoadToPercent(Double.POSITIVE_INFINITY));
    }
}
