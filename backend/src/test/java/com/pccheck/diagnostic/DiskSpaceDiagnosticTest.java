package com.pccheck.diagnostic;

import static com.pccheck.diagnostic.TestData.GIB;
import static com.pccheck.diagnostic.TestData.NOW;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pccheck.agent.model.CollectorResult;
import com.pccheck.agent.model.StorageInfo;
import com.pccheck.agent.model.VolumeInfo;
import java.util.List;
import org.junit.jupiter.api.Test;

class DiskSpaceDiagnosticTest {

    private final DiskSpaceDiagnostic diagnostic = new DiskSpaceDiagnostic(HealthThresholds.defaults());

    private List<Alert> run(CollectorResult<StorageInfo> storage) {
        return diagnostic.evaluate(TestData.input(TestData.memory(10), storage, TestData.temperature(40)), NOW);
    }

    @Test
    void espacoAdequadoGeraInformacaoComValorUnidadeEPercentual() {
        Alert a = run(TestData.storage(TestData.volume("C:", "NTFS", 100 * GIB, 50 * GIB))).get(0);
        assertEquals(Severity.INFO, a.severity());
        assertEquals(AlertKind.NORMAL, a.kind());
        assertEquals("disk-space:C:", a.id());
        assertEquals("50,0 GiB livres de 100,0 GiB (50,0% livre)", a.observedValue());
    }

    @Test
    void abaixoDe15PorCentoLivreGeraAtencao() {
        Alert a = run(TestData.storage(TestData.volume("C:", "NTFS", 100 * GIB, 10 * GIB))).get(0);
        assertEquals(Severity.WARNING, a.severity());
        assertEquals(AlertKind.PREVENTIVE, a.kind());
        assertTrue(a.threshold().contains("15,0%"));
    }

    @Test
    void exatamente15PorCentoAindaENormal() {
        Alert a = run(TestData.storage(TestData.volume("C:", "NTFS", 100 * GIB, 15 * GIB))).get(0);
        assertEquals(Severity.INFO, a.severity());
    }

    @Test
    void abaixoDe5PorCentoLivreGeraCritico() {
        Alert a = run(TestData.storage(TestData.volume("C:", "NTFS", 100 * GIB, 4 * GIB))).get(0);
        assertEquals(Severity.CRITICAL, a.severity());
        assertTrue(a.description().contains("Não indica defeito da unidade"));
    }

    @Test
    void cadaVolumeTemSeuProprioAlerta() {
        List<Alert> alerts = run(TestData.storage(
                TestData.volume("C:", "NTFS", 100 * GIB, 50 * GIB),
                TestData.volume("D:", "NTFS", 200 * GIB, 2 * GIB)));
        assertEquals(2, alerts.size());
        assertEquals(Severity.INFO, alerts.get(0).severity());
        assertEquals(Severity.CRITICAL, alerts.get(1).severity());
    }

    @Test
    void imagensSomenteLeituraSaoIgnoradas() {
        List<Alert> alerts = run(TestData.storage(
                TestData.volume("/snap/core/1", "squashfs", 100 * GIB, 0),
                TestData.volume("/", "ext4", 100 * GIB, 60 * GIB)));
        assertEquals(1, alerts.size());
        assertEquals("disk-space:/", alerts.get(0).id());
    }

    @Test
    void volumeSemEspacoLivreValidoEIndisponivel() {
        VolumeInfo semLivre = new VolumeInfo("E:", null, "NTFS", 100 * GIB, null, null, null);
        Alert a = run(TestData.storage(semLivre)).get(0);
        assertEquals(Severity.INFO, a.severity());
        assertEquals(AlertKind.UNAVAILABLE, a.kind());
    }

    @Test
    void armazenamentoComErroOuSemVolumesEIndisponivel() {
        Alert erro = run(CollectorResult.error("DiskCollector", "falha")).get(0);
        assertEquals(AlertKind.UNAVAILABLE, erro.kind());
        Alert semVolumes = run(CollectorResult.ok("DiskCollector", new StorageInfo(null, null))).get(0);
        assertEquals(AlertKind.UNAVAILABLE, semVolumes.kind());
    }
}
