package com.pccheck.diagnostic;

import static com.pccheck.diagnostic.TestData.GIB;
import static com.pccheck.diagnostic.TestData.NOW;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pccheck.agent.model.CollectorResult;
import com.pccheck.agent.model.SensorInfo;
import java.util.List;
import org.junit.jupiter.api.Test;

class HealthEvaluatorTest {

    private final HealthEvaluator evaluator = new HealthEvaluator(HealthThresholds.defaults());

    @Test
    void cenarioNormalNaoTemAtencaoNemCritico() {
        HealthReport r = evaluator.evaluate(TestData.input(TestData.memory(40),
                TestData.storage(TestData.volume("C:", "NTFS", 100 * GIB, 50 * GIB)), TestData.temperature(45)), NOW);
        assertEquals(Severity.INFO, r.overall());
        assertEquals(0, r.warningCount());
        assertEquals(0, r.criticalCount());
        assertTrue(r.infoCount() >= 4); // temperatura, disco, RAM e saúde dos discos (sem leitura)
        assertEquals(NOW, r.collectedAt());
    }

    @Test
    void severidadeGeralEaMaisGraveEAlertasVemOrdenados() {
        HealthReport r = evaluator.evaluate(TestData.input(TestData.memory(90),
                TestData.storage(TestData.volume("C:", "NTFS", 100 * GIB, 2 * GIB)), TestData.temperature(45)), NOW);
        assertEquals(Severity.CRITICAL, r.overall());
        assertEquals(1, r.criticalCount());
        assertEquals(1, r.warningCount());
        assertEquals(Severity.CRITICAL, r.alerts().get(0).severity());
        assertEquals(Severity.WARNING, r.alerts().get(1).severity());
    }

    @Test
    void semSensorNaoGeraAtencaoNemCritico() {
        HealthReport r = evaluator.evaluate(TestData.input(TestData.memory(40),
                TestData.storage(TestData.volume("C:", "NTFS", 100 * GIB, 50 * GIB)),
                CollectorResult.unavailable("SensorCollector", "sem dado")), NOW);
        assertEquals(Severity.INFO, r.overall());
        assertTrue(r.alerts().stream().anyMatch(a -> a.id().equals("temperature:cpu")
                && a.kind() == AlertKind.UNAVAILABLE));
    }

    @Test
    void semLeituraDosDiscosOSmartInformaQueNaoFoiAvaliado() {
        HealthReport r = evaluator.evaluate(TestData.input(TestData.memory(40), TestData.storage(),
                TestData.temperature(45)), NOW);
        Alert smart = r.alerts().stream().filter(a -> a.id().equals("smart")).findFirst().orElseThrow();
        assertEquals(Severity.INFO, smart.severity());
        assertEquals(AlertKind.UNAVAILABLE, smart.kind());
        assertTrue(smart.description().contains("NÃO foi avaliada"));
    }

    @Test
    void coletaIncompletaEInformativaENaoEFalhaFisica() {
        com.pccheck.agent.model.SystemSnapshot base = TestData.snapshot(TestData.memory(40), TestData.storage());
        com.pccheck.agent.model.SystemSnapshot s = new com.pccheck.agent.model.SystemSnapshot(base.collectedAt(),
                base.os(), CollectorResult.error("CpuCollector", "falha"), base.memory(),
                CollectorResult.unavailable("GpuCollector", "sem dado"), base.motherboard(), base.storage());
        HealthReport r = evaluator.evaluate(new HealthInput(s, TestData.temperature(45)), NOW);
        List<Alert> incompletos = r.alerts().stream().filter(a -> a.id().startsWith("collection:")).toList();
        assertEquals(2, incompletos.size());
        assertTrue(incompletos.stream().allMatch(a -> a.severity() == Severity.INFO
                && a.kind() == AlertKind.UNAVAILABLE));
        assertEquals(Severity.INFO, r.overall());
    }

    @Test
    void falhaEmUmDiagnosticoNaoImpedeOsDemais() {
        Diagnostic quebrado = new Diagnostic() {
            @Override
            public String name() {
                return "Quebrado";
            }

            @Override
            public List<Alert> evaluate(HealthInput input, java.time.Instant now) {
                throw new IllegalStateException("boom");
            }
        };
        HealthEvaluator e = new HealthEvaluator(HealthThresholds.defaults(),
                List.of(quebrado, new MemoryDiagnostic(HealthThresholds.defaults())));
        HealthReport r = e.evaluate(TestData.input(TestData.memory(40), TestData.storage(),
                TestData.temperature(45)), NOW);
        assertEquals(2, r.alerts().size());
        assertTrue(r.alerts().stream().anyMatch(a -> a.id().equals("memory:usage")));
        Alert erro = r.alerts().stream().filter(a -> a.id().startsWith("diagnostic-error:")).findFirst().orElseThrow();
        assertEquals(Severity.INFO, erro.severity());
        assertFalse(erro.description().contains("boom"));
    }

    @Test
    void relatorioInformaOrigemDosLimites() {
        HealthReport r = evaluator.evaluate(TestData.input(TestData.memory(40), TestData.storage(),
                TestData.temperature(45)), NOW);
        assertTrue(r.thresholdsSource().contains("não são limites oficiais do fabricante"));
        assertEquals(80.0, r.thresholds().cpuTempWarningC(), 0.0001);
    }

    @Test
    void limitesInvalidosSaoRejeitados() {
        assertThrows(IllegalArgumentException.class, () -> new HealthThresholds(90, 80, 15, 5, 85));
        assertThrows(IllegalArgumentException.class, () -> new HealthThresholds(80, 95, 5, 15, 85));
        assertThrows(IllegalArgumentException.class, () -> new HealthThresholds(80, 95, 15, 5, 0));
        assertThrows(IllegalArgumentException.class, () -> new HealthThresholds(80, 95, 150, 5, 85));
    }

    @Test
    void sensorOkSemValorNaoQuebra() {
        HealthReport r = evaluator.evaluate(TestData.input(TestData.memory(40), TestData.storage(),
                CollectorResult.ok("SensorCollector", new SensorInfo(null))), NOW);
        assertEquals(Severity.INFO, r.overall());
    }
}
