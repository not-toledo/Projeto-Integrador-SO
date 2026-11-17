package com.pccheck.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pccheck.agent.model.CollectorResult;
import com.pccheck.agent.model.CpuInfo;
import com.pccheck.agent.model.DiskInfo;
import com.pccheck.agent.model.GpuInfo;
import com.pccheck.agent.model.MemoryInfo;
import com.pccheck.agent.model.MotherboardInfo;
import com.pccheck.agent.model.OsInfo;
import com.pccheck.agent.model.StorageInfo;
import com.pccheck.agent.model.SystemSnapshot;
import com.pccheck.agent.model.VolumeInfo;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class SnapshotPrinterTest {

    @Test
    void formatadoresConvertemUnidades() {
        assertEquals("16.00 GiB", SnapshotPrinter.gib(17_179_869_184L));
        assertEquals("3.60 GHz", SnapshotPrinter.ghz(3_600_000_000L));
        assertEquals("42.5%", SnapshotPrinter.percent(42.5));
        assertEquals("1d 2h 3min", SnapshotPrinter.duration(93_780L));
        assertNull(SnapshotPrinter.gib(null));
        assertNull(SnapshotPrinter.ghz(null));
        assertNull(SnapshotPrinter.percent(null));
        assertNull(SnapshotPrinter.duration(null));
    }

    @Test
    void camposAusentesApareceComoNaoDisponivelENaoSaoInventados() {
        SystemSnapshot snapshot = new SystemSnapshot(
                Instant.parse("2026-01-01T00:00:00Z"),
                CollectorResult.ok("OsCollector", new OsInfo("Windows", null, null, "amd64", null)),
                CollectorResult.error("CpuCollector", "falha"),
                CollectorResult.unavailable("RamCollector", "sem dado"),
                CollectorResult.error("GpuCollector", "driver"),
                CollectorResult.unavailable("MotherboardCollector", "sem placa"),
                CollectorResult.unavailable("DiskCollector", "sem disco"));
        String text = SnapshotPrinter.format(snapshot);

        assertTrue(text.contains("[Sistema operacional]"));
        assertTrue(text.contains("Versao: Nao disponivel"));
        assertTrue(text.contains("Arquitetura: amd64"));
        assertTrue(text.contains("[CPU] ERRO - falha"));
        assertTrue(text.contains("[Memoria] Nao disponivel - sem dado"));
        assertTrue(text.contains("[Placa de video] ERRO - driver"));
        assertTrue(text.contains("[Placa-mae] Nao disponivel - sem placa"));
        assertTrue(text.contains("[Armazenamento] Nao disponivel - sem disco"));
    }

    @Test
    void imprimeDadosQuandoDisponiveis() {
        SystemSnapshot snapshot = new SystemSnapshot(
                Instant.parse("2026-01-01T00:00:00Z"),
                CollectorResult.ok("OsCollector", new OsInfo("Linux", "6.8", null, "amd64", 60L)),
                CollectorResult.ok("CpuCollector", new CpuInfo("CPU X", "Vendor", 4, 8, 3_000_000_000L, null, 10.0)),
                CollectorResult.ok("RamCollector", new MemoryInfo(8_589_934_592L, 4_294_967_296L, 4_294_967_296L, 50.0)),
                CollectorResult.ok("GpuCollector", List.of(new GpuInfo("GPU X", "Vendor", null, 2_147_483_648L))),
                CollectorResult.ok("MotherboardCollector", new MotherboardInfo("Fab", "Mod", null, null, null, null,
                        List.of("serialNumber"))),
                CollectorResult.ok("DiskCollector", new StorageInfo(
                        List.of(new DiskInfo("disco0", "Modelo D", 500_107_862_016L)),
                        List.of(new VolumeInfo("C:\\", null, "NTFS", 500_000_000_000L, 250_000_000_000L,
                                250_000_000_000L, 50.0)))));
        String text = SnapshotPrinter.format(snapshot);

        assertTrue(text.contains("Nucleos fisicos: 4"));
        assertTrue(text.contains("Frequencia nominal: 3.00 GHz"));
        assertTrue(text.contains("Frequencia maxima: Nao disponivel"));
        assertTrue(text.contains("Total: 8.00 GiB"));
        assertTrue(text.contains("Utilizacao: 50.0%"));
        assertTrue(text.contains("Nome: GPU X"));
        assertTrue(text.contains("ID do dispositivo: Nao disponivel"));
        assertTrue(text.contains("VRAM informada: 2.00 GiB"));
        assertTrue(text.contains("Fabricante: Fab"));
        assertTrue(text.contains("Versao: Nao disponivel"));
        assertTrue(text.contains("Numero de serie: Nao informado pelo fabricante"));
        assertTrue(text.contains("Modelo: Modelo D"));
        assertTrue(text.contains("Sistema de arquivos: NTFS"));
        assertTrue(text.contains("Rotulo: Nao disponivel"));
    }
}
