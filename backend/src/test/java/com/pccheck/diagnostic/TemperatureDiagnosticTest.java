package com.pccheck.diagnostic;

import static com.pccheck.diagnostic.TestData.NOW;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pccheck.agent.model.CollectorResult;
import com.pccheck.agent.model.SensorInfo;
import java.util.List;
import org.junit.jupiter.api.Test;

class TemperatureDiagnosticTest {

    private final TemperatureDiagnostic diagnostic = new TemperatureDiagnostic(HealthThresholds.defaults());

    private Alert run(CollectorResult<SensorInfo> sensors) {
        List<Alert> alerts = diagnostic.evaluate(TestData.input(TestData.memory(10), TestData.storage(), sensors), NOW);
        assertEquals(1, alerts.size());
        return alerts.get(0);
    }

    @Test
    void temperaturaNormalGeraInformacao() {
        Alert a = run(TestData.temperature(45.0));
        assertEquals(Severity.INFO, a.severity());
        assertEquals(AlertKind.NORMAL, a.kind());
        assertEquals("45,0 °C", a.observedValue());
    }

    @Test
    void limiteDeAtencaoEInclusivo() {
        assertEquals(Severity.INFO, run(TestData.temperature(79.9)).severity());
        assertEquals(Severity.WARNING, run(TestData.temperature(80.0)).severity());
        assertEquals(Severity.WARNING, run(TestData.temperature(94.9)).severity());
    }

    @Test
    void temperaturaCriticaEPreventivaEMostraLimite() {
        Alert a = run(TestData.temperature(95.0));
        assertEquals(Severity.CRITICAL, a.severity());
        assertEquals(AlertKind.PREVENTIVE, a.kind());
        assertTrue(a.threshold().contains("95,0 °C"));
        assertTrue(a.description().contains("não comprova defeito"));
    }

    @Test
    void limitesConfiguraveisSaoRespeitados() {
        TemperatureDiagnostic custom = new TemperatureDiagnostic(new HealthThresholds(60, 70, 15, 5, 85));
        Alert a = custom.evaluate(TestData.input(TestData.memory(10), TestData.storage(),
                TestData.temperature(65.0)), NOW).get(0);
        assertEquals(Severity.WARNING, a.severity());
    }

    @Test
    void semSensorInformaIndisponibilidadeSemInventarValor() {
        Alert a = run(CollectorResult.unavailable("SensorCollector", "sem dado"));
        assertEquals(Severity.INFO, a.severity());
        assertEquals(AlertKind.UNAVAILABLE, a.kind());
        assertNull(a.observedValue());
        assertTrue(a.description().contains("não indica defeito"));
    }

    @Test
    void falhaNaLeituraDoSensorNaoViraAlertaDeTemperatura() {
        Alert a = run(CollectorResult.error("SensorCollector", "IOException"));
        assertEquals(Severity.INFO, a.severity());
        assertEquals(AlertKind.UNAVAILABLE, a.kind());
    }

    @Test
    void resultadoOkSemValorTratadoComoIndisponivel() {
        Alert a = run(CollectorResult.ok("SensorCollector", new SensorInfo(null)));
        assertEquals(AlertKind.UNAVAILABLE, a.kind());
    }
}
