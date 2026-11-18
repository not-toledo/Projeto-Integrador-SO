package com.pccheck.diagnostic;

import static com.pccheck.diagnostic.TestData.GIB;
import static com.pccheck.diagnostic.TestData.NOW;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pccheck.agent.model.CollectorResult;
import com.pccheck.agent.model.DiskHealth;
import com.pccheck.agent.model.DiskHealthInfo;
import com.pccheck.agent.model.DiskInfo;
import com.pccheck.agent.model.StorageInfo;
import com.pccheck.agent.model.SystemSnapshot;
import java.util.List;
import org.junit.jupiter.api.Test;

class DiskHealthDiagnosticTest {

    private final DiskHealthDiagnostic diagnostic = new DiskHealthDiagnostic(DiskHealthThresholds.defaults());

    /** Inventário com o SSD em PHYSICALDRIVE0 e o HDD em PHYSICALDRIVE1, como nesta máquina de teste. */
    private static HealthInput input(CollectorResult<DiskHealthInfo> health) {
        SystemSnapshot base = TestData.snapshot(TestData.memory(40), TestData.storage());
        StorageInfo storage = new StorageInfo(List.of(
                new DiskInfo("\\\\.\\PHYSICALDRIVE0", "SSD-REDRAGON-SPARK-480", 480 * GIB, "SSD", "SATA"),
                new DiskInfo("\\\\.\\PHYSICALDRIVE1", "ST1000DM003-1ER162", 1000 * GIB, "HDD", "SATA")), List.of());
        SystemSnapshot s = new SystemSnapshot(base.collectedAt(), base.os(), base.cpu(), base.memory(), base.gpu(),
                base.motherboard(), CollectorResult.ok("DiskCollector", storage));
        return new HealthInput(s, TestData.temperature(45), List.of(), health);
    }

    private static CollectorResult<DiskHealthInfo> disks(DiskHealth... disks) {
        return CollectorResult.ok("DiskHealthCollector", new DiskHealthInfo(List.of(disks), null));
    }

    private static DiskHealth ssd(Double life, Double temp) {
        return new DiskHealth("SSD-REDRAGON-SPARK-480", 0, temp, life, 53880.0, 7371L, 1062L);
    }

    private static DiskHealth hdd(Double temp) {
        return new DiskHealth("ST1000DM003-1ER162", 1, temp, null, null, 50470L, 4301L);
    }

    @Test
    void discosReaisComIndicadoresNormais() {
        List<Alert> alerts = diagnostic.evaluate(input(disks(ssd(70.0, 48.0), hdd(28.0))), NOW);
        assertEquals(2, alerts.size());
        Alert ssd = alerts.get(0);
        assertEquals("smart:0", ssd.id());
        assertEquals("Disco: SSD-REDRAGON-SPARK-480 (SSD)", ssd.component());
        assertEquals(AlertKind.NORMAL, ssd.kind());
        assertEquals("Vida útil restante 70,0% · 48,0 °C · 7.371 h ligado · 53.880 GB gravados", ssd.observedValue());
        // Honestidade: "normal" não é "sem falhas".
        assertTrue(ssd.description().contains("não garante"));
        assertEquals("Disco: ST1000DM003-1ER162 (HDD)", alerts.get(1).component());
        assertTrue(alerts.get(1).threshold().contains("55,0 °C (HDD)"));
    }

    @Test
    void vidaUtilBaixaEAtencaoEMuitoBaixaECritico() {
        assertEquals(Severity.WARNING, diagnostic.evaluate(input(disks(ssd(18.0, 40.0))), NOW).get(0).severity());
        Alert critical = diagnostic.evaluate(input(disks(ssd(4.0, 40.0))), NOW).get(0);
        assertEquals(Severity.CRITICAL, critical.severity());
        assertTrue(critical.description().contains("backup"));
    }

    @Test
    void temperaturaUsaOLimiteDoTipoDeDisco() {
        // 58 °C: acima do limite de HDD (55), abaixo do de SSD (70).
        assertEquals(Severity.WARNING, diagnostic.evaluate(input(disks(hdd(58.0))), NOW).get(0).severity());
        assertEquals(Severity.INFO, diagnostic.evaluate(input(disks(ssd(90.0, 58.0))), NOW).get(0).severity());
        assertEquals(Severity.WARNING, diagnostic.evaluate(input(disks(ssd(90.0, 72.0))), NOW).get(0).severity());
    }

    @Test
    void discoSemNumeroFisicoUsaLimiteDeTipoDesconhecido() {
        DiskHealth semIndice = new DiskHealth("Disco X", null, 60.0, null, null, null, null);
        Alert a = diagnostic.evaluate(input(disks(semIndice)), NOW).get(0);
        assertEquals("smart:p0", a.id());
        assertEquals("Disco: Disco X", a.component());
        assertEquals(Severity.INFO, a.severity());
        assertTrue(a.threshold().contains("tipo desconhecido"));
    }

    @Test
    void semLeituraInformaOMotivoENaoAvalia() {
        Alert a = diagnostic.evaluate(input(CollectorResult.unavailable("DiskHealthCollector",
                "A leitura exige administrador.")), NOW).get(0);
        assertEquals("smart", a.id());
        assertEquals(AlertKind.UNAVAILABLE, a.kind());
        assertTrue(a.description().startsWith("A leitura exige administrador."));
        assertTrue(a.description().contains("NÃO foi avaliada"));

        Alert naoColetado = diagnostic.evaluate(input(null), NOW).get(0);
        assertEquals(AlertKind.UNAVAILABLE, naoColetado.kind());
    }

    @Test
    void limitesInvalidosSaoRejeitados() {
        assertThrows(IllegalArgumentException.class, () -> new DiskHealthThresholds(5, 20, 55, 70));
        assertThrows(IllegalArgumentException.class, () -> new DiskHealthThresholds(20, 5, 0, 70));
    }
}
