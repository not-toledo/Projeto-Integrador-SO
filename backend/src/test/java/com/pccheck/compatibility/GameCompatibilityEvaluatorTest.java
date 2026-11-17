package com.pccheck.compatibility;

import static org.junit.jupiter.api.Assertions.*;

import com.pccheck.agent.model.CpuInfo;
import com.pccheck.agent.model.GpuInfo;
import com.pccheck.agent.model.MemoryInfo;
import com.pccheck.agent.model.OsInfo;
import com.pccheck.agent.model.StorageInfo;
import com.pccheck.agent.model.VolumeInfo;
import com.pccheck.dto.*;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class GameCompatibilityEvaluatorTest {
    private static final long GIB = 1024L * 1024 * 1024;
    private final GameCompatibilityEvaluator evaluator = new GameCompatibilityEvaluator();

    private InventoryDto pc(long ram, long free) {
        return pc(ram, free, null, null, null);
    }

    private InventoryDto pc(long ram, long free, String cpu, List<GpuInfo> gpus, OsInfo os) {
        return new InventoryDto(Instant.now(), "test",
                os == null ? new ComponentDto<>("UNAVAILABLE", "n/a", null) : new ComponentDto<>("OK", null, os),
                cpu == null ? new ComponentDto<>("UNAVAILABLE", "n/a", null)
                        : new ComponentDto<>("OK", null, new CpuInfo(cpu, null, 4, 8, null, null, null)),
                new ComponentDto<>("OK", null, new MemoryInfo(ram, 0L, ram, 100.0)),
                gpus == null ? new ComponentDto<>("UNAVAILABLE", "n/a", null) : new ComponentDto<>("OK", null, gpus),
                new ComponentDto<>("UNAVAILABLE", "n/a", null),
                new ComponentDto<>("OK", null, new StorageInfo(null,
                        List.of(new VolumeInfo("C:", null, null, free, free, 0L, 0.0)))));
    }

    private static GameRequirementsDto game(RequirementFieldDto... minimum) {
        return new GameRequirementsDto(1, "Game", null, "url", "", "", List.of(minimum), List.of(),
                Instant.now(), "Steam", List.of());
    }

    private static RequirementFieldDto field(String component, String raw, Long bytes, String parsed) {
        return new RequirementFieldDto(component, raw, bytes, parsed, .9, "PARSED");
    }

    private ComponentCompatibilityDto first(GameRequirementsDto game, InventoryDto pc) {
        return evaluator.evaluate(game, pc).minimumResults().get(0);
    }

    @Test
    void reportsBelowMinimumWhenRamIsInsufficient() {
        var ram = new RequirementFieldDto("RAM", "8 GB RAM", 8L * GIB, "8 GB", .95, "PARSED");
        var result = evaluator.evaluate(game(ram), pc(4L * GIB, 20L * GIB));
        assertEquals("Abaixo dos mínimos", result.overallMinimum());
        assertEquals("BELOW_MINIMUM", result.minimumResults().get(0).status());
    }

    @Test
    void neverTreatsUnknownAsCompatible() {
        var cpu = new RequirementFieldDto("CPU", "Intel Core i5", null, null, .3, "MANUAL_REVIEW");
        var result = evaluator.evaluate(game(cpu), pc(16L * GIB, 20L * GIB, "Intel Core i5-8400", null, null));
        assertEquals("Análise incompleta", result.overallMinimum());
        assertEquals("MANUAL_REVIEW", result.minimumResults().get(0).status());
    }

    @Test
    void ramReservadaPeloSistemaVaiParaRevisaoENaoParaAbaixo() {
        // PC com 8 GB instalados e ~7,4 GiB visíveis (parte reservada para a GPU integrada).
        var ram = field("RAM", "Memory: 8 GB RAM", 8_000_000_000L, "8 GB");
        assertEquals("MANUAL_REVIEW", first(game(ram), pc((long) (7.4 * GIB), 20L * GIB)).status());
    }

    @Test
    void cpuDoMesmoModeloAtendeEntreAlternativas() {
        var cpu = field("CPU", "Processor: Intel Core i5-8400 / AMD Ryzen 5 3600", null, null);
        var r = first(game(cpu), pc(16L * GIB, 20L * GIB, "AMD Ryzen 5 3600 6-Core Processor", null, null));
        assertEquals("COMPATIBLE", r.status());
    }

    @Test
    void cpuComSufixoDiferenteNaoEOMesmoModelo() {
        var cpu = field("CPU", "Processor: Intel Core i5-8400", null, null);
        var r = first(game(cpu), pc(16L * GIB, 20L * GIB, "Intel(R) Core(TM) i5-8400T CPU", null, null));
        assertEquals("MANUAL_REVIEW", r.status());
    }

    @Test
    void gpuTiNaoEOMesmoModeloDaVersaoComum() {
        var gpu = field("GPU", "Graphics: GeForce RTX 3060", null, null);
        var gpus = List.of(new GpuInfo("NVIDIA GeForce RTX 3060 Ti", "NVIDIA", null, 8L * GIB));
        assertEquals("MANUAL_REVIEW", first(game(gpu), pc(16L * GIB, 20L * GIB, null, gpus, null)).status());
    }

    @Test
    void gpuDoMesmoModeloComparaAMemoriaDeVideo() {
        var gpu = field("GPU", "Graphics: GTX 1060 6GB", 6_000_000_000L, null);
        var gpus = List.of(new GpuInfo("Intel HD Graphics 630", "Intel", null, GIB),
                new GpuInfo("NVIDIA GeForce GTX 1060", "NVIDIA", null, 3L * GIB));
        assertEquals("BELOW_MINIMUM", first(game(gpu), pc(16L * GIB, 20L * GIB, null, gpus, null)).status());
    }

    @Test
    void windows11AtendeAWindows10E7FicaAbaixo() {
        var os = field("OS", "OS: Windows 10 64-bit", null, "Windows 10");
        assertEquals("COMPATIBLE", first(game(os),
                pc(16L * GIB, 20L * GIB, null, null, new OsInfo("Windows", "11", null, "amd64", 1L))).status());
        assertEquals("BELOW_MINIMUM", first(game(os),
                pc(16L * GIB, 20L * GIB, null, null, new OsInfo("Windows", "7", null, "amd64", 1L))).status());
        assertEquals("BELOW_MINIMUM", first(game(os),
                pc(16L * GIB, 20L * GIB, null, null, new OsInfo("Windows", "10", null, "x86", 1L))).status());
    }

    @Test
    void linhasNaoComparaveisNaoEntramNoResultado() {
        var other = new RequirementFieldDto("OTHER", "DirectX: Version 12", null, null, 0, "UNPARSED");
        var ram = field("RAM", "Memory: 8 GB RAM", 8_000_000_000L, "8 GB");
        var result = evaluator.evaluate(game(other, ram), pc(16L * GIB, 20L * GIB));
        assertEquals(1, result.minimumResults().size());
        assertEquals("Atende aos mínimos", result.overallMinimum());
    }

    @Test
    void resumoParcialDizOQueFaltaRevisar() {
        var ram = field("RAM", "Memory: 8 GB RAM", 8_000_000_000L, "8 GB");
        var cpu = field("CPU", "Processor: Intel Core i5-8400", null, null);
        var result = evaluator.evaluate(game(ram, cpu), pc(16L * GIB, 20L * GIB, "AMD Ryzen 7 5800X", null, null));
        assertEquals("Nenhum item abaixo dos mínimos; revisar manualmente: CPU", result.overallMinimum());
    }
}
