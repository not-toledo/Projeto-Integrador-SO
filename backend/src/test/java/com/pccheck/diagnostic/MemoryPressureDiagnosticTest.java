package com.pccheck.diagnostic;

import static com.pccheck.diagnostic.TestData.GIB;
import static com.pccheck.diagnostic.TestData.NOW;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pccheck.agent.model.CollectorResult;
import com.pccheck.agent.model.MemoryInfo;
import com.pccheck.agent.model.OsInfo;
import com.pccheck.agent.model.PagingActivity;
import com.pccheck.agent.model.SystemSnapshot;
import com.pccheck.agent.model.VirtualMemoryInfo;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class MemoryPressureDiagnosticTest {

    private final MemoryPressureDiagnostic diagnostic =
            new MemoryPressureDiagnostic(HealthThresholds.defaults(), MemoryPressureThresholds.defaults());

    /** RAM de 16 GiB no percentual dado, com memória comprometida em commitPercent de um limite de 24 GiB. */
    private static CollectorResult<MemoryInfo> memory(double ramPercent, Double commitPercent) {
        long total = 16 * GIB;
        long used = (long) (total * ramPercent / 100.0);
        VirtualMemoryInfo vm = commitPercent == null ? null : new VirtualMemoryInfo(8 * GIB, GIB, 12.5,
                24 * GIB, (long) (24 * GIB * commitPercent / 100.0), commitPercent);
        return CollectorResult.ok("RamCollector", new MemoryInfo(total, total - used, used, ramPercent, vm, null));
    }

    private static HealthInput input(CollectorResult<MemoryInfo> memory, String os, List<PagingActivity> paging) {
        SystemSnapshot s = TestData.snapshot(memory, TestData.storage(TestData.volume("C:\\", "NTFS", 100 * GIB, 50 * GIB)));
        SystemSnapshot withOs = new SystemSnapshot(s.collectedAt(),
                CollectorResult.ok("OsCollector", new OsInfo(os, "10", null, "amd64", 1L)),
                s.cpu(), s.memory(), s.gpu(), s.motherboard(), s.storage());
        return new HealthInput(withOs, TestData.temperature(45), paging);
    }

    /** n amostras gravando bytesPerSecond em disco (páginas de 4 KiB). */
    private static List<PagingActivity> paging(int n, double bytesPerSecond) {
        return Collections.nCopies(n, new PagingActivity(0.0, bytesPerSecond / 4096, 4096L));
    }

    private Alert alert(List<Alert> alerts, String id) {
        return alerts.stream().filter(a -> a.id().equals(id)).findFirst().orElseThrow();
    }

    @Test
    void memoriaComprometidaPertoDoLimiteNoWindowsGeraAviso() {
        var alerts = diagnostic.evaluate(input(memory(60, 92.0), "Windows", List.of()), NOW);
        Alert commit = alert(alerts, MemoryPressureDiagnostic.COMMIT_ID);
        assertEquals(Severity.WARNING, commit.severity());
        assertTrue(commit.observedValue().contains("92,0%"));
    }

    @Test
    void memoriaComprometidaAbaixoDoLimiteENormal() {
        var alerts = diagnostic.evaluate(input(memory(60, 88.0), "Windows", List.of()), NOW);
        assertEquals(AlertKind.NORMAL, alert(alerts, MemoryPressureDiagnostic.COMMIT_ID).kind());
    }

    @Test
    void noLinuxMemoriaComprometidaEInformativaMesmoAcimaDoLimite() {
        // Overcommit: no Linux o percentual pode passar de 100% sem indicar falha.
        var alerts = diagnostic.evaluate(input(memory(60, 140.0), "Linux", List.of()), NOW);
        Alert commit = alert(alerts, MemoryPressureDiagnostic.COMMIT_ID);
        assertEquals(Severity.INFO, commit.severity());
        assertEquals("Memória comprometida (informativo)", commit.title());
    }

    @Test
    void semMemoriaVirtualOItemFicaIndisponivel() {
        var alerts = diagnostic.evaluate(input(memory(60, null), "Windows", List.of()), NOW);
        assertEquals(AlertKind.UNAVAILABLE, alert(alerts, MemoryPressureDiagnostic.COMMIT_ID).kind());
    }

    @Test
    void paginacaoSustentadaComRamCheiaGeraAviso() {
        var alerts = diagnostic.evaluate(input(memory(92, 70.0), "Windows", paging(30, 5_000_000)), NOW);
        Alert paging = alert(alerts, MemoryPressureDiagnostic.PAGING_ID);
        assertEquals(Severity.WARNING, paging.severity());
        assertEquals("Pressão de memória: paginação constante", paging.title());
        assertTrue(paging.observedValue().contains("30 amostras"));
    }

    @Test
    void paginacaoSemRamCheiaNaoEPressaoDeMemoria() {
        var alerts = diagnostic.evaluate(input(memory(50, 70.0), "Windows", paging(30, 5_000_000)), NOW);
        Alert paging = alert(alerts, MemoryPressureDiagnostic.PAGING_ID);
        assertEquals(Severity.INFO, paging.severity());
        assertEquals("Gravação de páginas sem RAM cheia", paging.title());
    }

    @Test
    void ramCheiaSemPaginacaoNaoEPressaoDeMemoria() {
        var alerts = diagnostic.evaluate(input(memory(92, 70.0), "Windows", paging(30, 10_000)), NOW);
        assertEquals("Sem paginação sustentada", alert(alerts, MemoryPressureDiagnostic.PAGING_ID).title());
    }

    @Test
    void picoIsoladoNaoBastaParaAviso() {
        // 29 amostras sem paginação e 1 pico de 20 MB/s: média abaixo de 1 MB/s.
        List<PagingActivity> recent = new java.util.ArrayList<>(paging(29, 0));
        recent.add(new PagingActivity(0.0, 20_000_000.0 / 4096, 4096L));
        var alerts = diagnostic.evaluate(input(memory(92, 70.0), "Windows", recent), NOW);
        assertEquals(Severity.INFO, alert(alerts, MemoryPressureDiagnostic.PAGING_ID).severity());
    }

    @Test
    void poucasAmostrasNaoSaoAvaliadas() {
        var alerts = diagnostic.evaluate(input(memory(92, 70.0), "Windows", paging(5, 5_000_000)), NOW);
        Alert paging = alert(alerts, MemoryPressureDiagnostic.PAGING_ID);
        assertEquals(AlertKind.UNAVAILABLE, paging.kind());
        assertTrue(paging.description().contains("há 5"));
    }

    @Test
    void usaSoAsAmostrasMaisRecentesDaJanela() {
        // 30 amostras antigas com paginação forte e 30 recentes sem: só as recentes contam.
        List<PagingActivity> recent = new java.util.ArrayList<>(paging(30, 5_000_000));
        recent.addAll(paging(30, 0));
        var alerts = diagnostic.evaluate(input(memory(92, 70.0), "Windows", recent), NOW);
        assertEquals("Sem paginação sustentada", alert(alerts, MemoryPressureDiagnostic.PAGING_ID).title());
    }

    @Test
    void limitesInvalidosSaoRejeitados() {
        assertThrows(IllegalArgumentException.class, () -> new MemoryPressureThresholds(0, 1, 30, 10));
        assertThrows(IllegalArgumentException.class, () -> new MemoryPressureThresholds(90, 0, 30, 10));
        assertThrows(IllegalArgumentException.class, () -> new MemoryPressureThresholds(90, 1, 5, 10));
    }
}
