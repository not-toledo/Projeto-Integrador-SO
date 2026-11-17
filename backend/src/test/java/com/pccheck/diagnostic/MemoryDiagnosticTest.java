package com.pccheck.diagnostic;

import static com.pccheck.diagnostic.TestData.NOW;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pccheck.agent.model.CollectorResult;
import com.pccheck.agent.model.MemoryInfo;
import org.junit.jupiter.api.Test;

class MemoryDiagnosticTest {

    private final MemoryDiagnostic diagnostic = new MemoryDiagnostic(HealthThresholds.defaults());

    private Alert run(CollectorResult<MemoryInfo> memory) {
        return diagnostic.evaluate(TestData.input(memory, TestData.storage(), TestData.temperature(40)), NOW).get(0);
    }

    @Test
    void usoNormalGeraInformacao() {
        Alert a = run(TestData.memory(50));
        assertEquals(Severity.INFO, a.severity());
        assertEquals(AlertKind.NORMAL, a.kind());
        assertTrue(a.observedValue().startsWith("50,0%"));
    }

    @Test
    void usoElevadoGeraApenasAvisoPreventivo() {
        Alert a = run(TestData.memory(90));
        assertEquals(Severity.WARNING, a.severity());
        assertEquals(AlertKind.PREVENTIVE, a.kind());
        assertTrue(a.description().contains("não indica defeito"));
    }

    @Test
    void usoDe100PorCentoNuncaViraCritico() {
        assertEquals(Severity.WARNING, run(TestData.memory(100)).severity());
    }

    @Test
    void limiteDeAvisoEInclusivo() {
        assertEquals(Severity.INFO, run(TestData.memory(84.9)).severity());
        assertEquals(Severity.WARNING, run(TestData.memory(85.0)).severity());
    }

    @Test
    void semDadosEIndisponivel() {
        assertEquals(AlertKind.UNAVAILABLE, run(CollectorResult.unavailable("RamCollector", "x")).kind());
        assertEquals(AlertKind.UNAVAILABLE, run(CollectorResult.error("RamCollector", "x")).kind());
        assertEquals(AlertKind.UNAVAILABLE,
                run(CollectorResult.ok("RamCollector", new MemoryInfo(null, null, null, null))).kind());
    }
}
