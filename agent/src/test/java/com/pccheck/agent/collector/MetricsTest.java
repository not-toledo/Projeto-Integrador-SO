package com.pccheck.agent.collector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void cleanOemTextDescartaTextosGenericosDeFabricantes() {
        assertNull(Metrics.cleanOemText("To be filled by O.E.M."));
        assertNull(Metrics.cleanOemText(" Default string "));
        assertNull(Metrics.cleanOemText("None"));
        assertNull(Metrics.cleanOemText("unknown"));
        assertNull(Metrics.cleanOemText(null));
        assertEquals("ROG STRIX B550-F", Metrics.cleanOemText(" ROG STRIX B550-F "));
    }

    @Test
    void isOemPlaceholderDiferenciaTextoGenericoDeAusente() {
        assertTrue(Metrics.isOemPlaceholder("To be filled by O.E.M."));
        assertTrue(Metrics.isOemPlaceholder("x.x"));
        assertTrue(Metrics.isOemPlaceholder("Base Board Product Name"));
        // Ausente ou "unknown" nao e texto do fabricante: e dado nao disponivel.
        assertFalse(Metrics.isOemPlaceholder(null));
        assertFalse(Metrics.isOemPlaceholder("  "));
        assertFalse(Metrics.isOemPlaceholder("unknown"));
        assertFalse(Metrics.isOemPlaceholder("H81M-S1"));
        assertFalse(Metrics.isOemPlaceholder("Rev 1.xx"));
    }

    @Test
    void usedBytesValidaCoerencia() {
        assertEquals(60L, Metrics.usedBytes(100L, 40L));
        assertEquals(0L, Metrics.usedBytes(100L, 100L));
        assertNull(Metrics.usedBytes(null, 40L));
        assertNull(Metrics.usedBytes(100L, null));
        assertNull(Metrics.usedBytes(0L, 0L));
        assertNull(Metrics.usedBytes(100L, -1L));
        assertNull(Metrics.usedBytes(100L, 101L));
    }

    @Test
    void validTemperatureDescartaSentinelasEValoresImplausiveis() {
        assertEquals(55.5, Metrics.validTemperature(55.5), 0.0001);
        assertNull(Metrics.validTemperature(0.0));
        assertNull(Metrics.validTemperature(-5.0));
        assertNull(Metrics.validTemperature(Double.NaN));
        assertNull(Metrics.validTemperature(Double.POSITIVE_INFINITY));
        assertNull(Metrics.validTemperature(200.0));
    }

    @Test
    void ratePerSecondCalculaTaxaEDescartaContadoresInvalidos() {
        assertEquals(1000.0, Metrics.ratePerSecond(0L, 2000L, 2000L), 0.0001);
        assertEquals(0.0, Metrics.ratePerSecond(500L, 500L, 1000L), 0.0001);
        assertNull(Metrics.ratePerSecond(-1L, 100L, 1000L));
        assertNull(Metrics.ratePerSecond(200L, 100L, 1000L)); // contador reiniciou
        assertNull(Metrics.ratePerSecond(0L, 100L, 0L));
        assertNull(Metrics.ratePerSecond(0L, 100L, -5L));
    }

    @Test
    void busyPercentLimitaA100EDescartaValoresInvalidos() {
        assertEquals(25.0, Metrics.busyPercent(0L, 500L, 2000L), 0.0001);
        assertEquals(100.0, Metrics.busyPercent(0L, 5000L, 2000L), 0.0001);
        assertNull(Metrics.busyPercent(100L, 50L, 2000L));
        assertNull(Metrics.busyPercent(0L, 50L, 0L));
        assertNull(Metrics.busyPercent(-1L, 50L, 1000L));
    }
}
