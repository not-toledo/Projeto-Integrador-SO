package com.pccheck.compatibility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pccheck.agent.model.CpuInfo;
import com.pccheck.agent.model.GpuInfo;
import com.pccheck.agent.model.MemoryInfo;
import com.pccheck.benchmark.BenchmarkCatalog;
import com.pccheck.dto.ComponentCompatibilityDto;
import com.pccheck.dto.ComponentDto;
import com.pccheck.dto.GameCompatibilityDto;
import com.pccheck.dto.GameRequirementsDto;
import com.pccheck.dto.InventoryDto;
import com.pccheck.dto.RequirementFieldDto;
import java.io.StringReader;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/** CPU/GPU de modelos diferentes comparados pela tabela do Blender Open Data (valores reais do snapshot). */
class BenchmarkComparisonTest {

    static final long GIB = 1024L * 1024 * 1024;
    static final String CSV = """
            # Gerado de opendata-2026-11-18-000000+0000.jsonl (Blender Open Data).
            kind,key,backend,blender_version,median_score,p25,p75,samples,cores,device_name
            CPU,intel-core-i7-4790k,CPU,4.5.0,73.67,66.95,84.12,29,4,Intel Core i7-4790K CPU @ 4.00GHz
            CPU,intel-core-i7-7700k,CPU,4.5.0,88.89,85.39,94.47,39,4,Intel Core i7-7700K CPU @ 4.20GHz
            CPU,amd-ryzen-5-1600,CPU,4.5.0,96.71,93.98,104.28,12,6,AMD Ryzen 5 1600 Six-Core Processor
            CPU,intel-core-i5-4460,CPU,4.5.0,45.00,44.00,46.00,30,4,Intel Core i5-4460 CPU @ 3.20GHz
            CPU,amd-fx-8350,CPU,4.5.0,80.00,78.00,82.00,30,8,AMD FX(tm)-8350 Eight-Core Processor
            CPU,amd-ryzen-9-5950x,CPU,4.5.0,600.00,590.00,610.00,300,16,AMD Ryzen 9 5950X 16-Core Processor
            CPU,intel-core-i9-9900k,CPU,4.5.0,200.00,195.00,205.00,100,8,Intel Core i9-9900K CPU @ 3.60GHz
            CPU,intel-core-i5-8400,CPU,4.5.0,78.00,76.00,80.00,30,6,Intel Core i5-8400 CPU @ 2.80GHz
            GPU,nvidia-gtx-1050-ti,OPTIX,4.5.0,228.44,214.99,233.59,25,,NVIDIA GeForce GTX 1050 Ti
            GPU,nvidia-gtx-1060,OPTIX,4.5.0,382.97,359.29,395.06,108,,NVIDIA GeForce GTX 1060 6GB
            GPU,nvidia-gtx-960,OPTIX,4.5.0,150.00,145.00,155.00,40,,NVIDIA GeForce GTX 960
            GPU,nvidia-rtx-3060,OPTIX,4.5.0,2000.00,1950.00,2050.00,500,,NVIDIA GeForce RTX 3060
            """;

    private final GameCompatibilityEvaluator evaluator =
            new GameCompatibilityEvaluator(new BenchmarkCatalog(new StringReader(CSV)));

    static InventoryDto pc(String cpu, List<GpuInfo> gpus) {
        return new InventoryDto(Instant.now(), "test", new ComponentDto<>("UNAVAILABLE", "n/a", null),
                new ComponentDto<>("OK", null, new CpuInfo(cpu, null, 4, 8, null, null, null)),
                new ComponentDto<>("OK", null, new MemoryInfo(16 * GIB, 0L, 16 * GIB, 100.0)),
                gpus == null ? new ComponentDto<>("UNAVAILABLE", "n/a", null) : new ComponentDto<>("OK", null, gpus),
                new ComponentDto<>("UNAVAILABLE", "n/a", null), new ComponentDto<>("UNAVAILABLE", "n/a", null));
    }

    static GpuInfo gpu(String name, Long vram) {
        return new GpuInfo(name, null, null, vram);
    }

    static GameRequirementsDto game(RequirementFieldDto f) {
        return new GameRequirementsDto(1, "Game", null, "url", "", "", List.of(f), List.of(), Instant.now(), "Steam",
                List.of());
    }

    static RequirementFieldDto field(String component, String raw, Long bytes) {
        return new RequirementFieldDto(component, raw, bytes, null, .9, "PARSED");
    }

    ComponentCompatibilityDto first(RequirementFieldDto f, InventoryDto pc) {
        return evaluator.evaluate(game(f), pc).minimumResults().get(0);
    }

    @Test
    void casoRealI7_4790kContraI7_7700kFicaAbaixo() {
        // Where Winds Meet: "Core i7-7700K / Ryzen 5 1600". 73,67 ÷ 88,89 = 83% → abaixo de 85% com confiança alta.
        var r = first(field("CPU", "Processador: Core i7-7700K / Ryzen 5 1600", null),
                pc("Intel(R) Core(TM) i7-4790K CPU @ 4.00GHz", null));
        assertNotNull(r.benchmark());
        assertEquals("Intel Core i7-7700K CPU @ 4.20GHz", r.benchmark().requiredModel(),
                "a alternativa do mesmo fabricante (Intel) é a usada");
        assertEquals("HIGH", r.benchmark().confidence());
        assertEquals("BELOW_MINIMUM", r.status());
        assertTrue(r.explanation().contains("83% do exigido"));
    }

    @Test
    void casoRealGtx1050TiContraGtx1060() {
        var r = first(field("GPU", "Placa de vídeo: GTX 1060(6GB) / RX 480(8GB)", 6_000_000_000L),
                pc("x", List.of(gpu("NVIDIA GeForce GTX 1050 Ti", 4 * GIB), gpu("Intel(R) HD Graphics 4600", GIB))));
        assertEquals("BELOW_MINIMUM", r.status(), "228 ÷ 383 = 60%");
        assertEquals("OPTIX", r.benchmark().backend());
        assertTrue(r.detected().startsWith("NVIDIA GeForce GTX 1050 Ti"));
    }

    @Test
    void gpuMaisForteDaMesmaGeracaoAtendeMasVramMenorVaiParaRevisao() {
        var semVram = field("GPU", "Graphics: NVIDIA GeForce GTX 960", null);
        assertEquals("COMPATIBLE", first(semVram, pc("x", List.of(gpu("NVIDIA GeForce GTX 1060 6GB", 6 * GIB)))).status());
        var comVram = field("GPU", "Graphics: NVIDIA GeForce GTX 960 (8 GB)", 8 * GIB);
        var r = first(comVram, pc("x", List.of(gpu("NVIDIA GeForce GTX 1060 6GB", 6 * GIB))));
        assertEquals("MANUAL_REVIEW", r.status());
        assertTrue(r.explanation().contains("memória de vídeo detectada é menor"));
    }

    @Test
    void gpusDeGeracoesDiferentesNaoSaoDecididasPorBenchmark() {
        // RTX 3060 (Ampere) × GTX 960 (Maxwell): a renderização exagera a diferença dos jogos.
        var r = first(field("GPU", "Graphics: NVIDIA GeForce GTX 960", null),
                pc("x", List.of(gpu("NVIDIA GeForce RTX 3060", 12 * GIB))));
        assertEquals("MANUAL_REVIEW", r.status());
        assertNull(r.benchmark());
        assertTrue(r.explanation().contains("gerações diferentes"));
    }

    @Test
    void cpuComTotalMaiorMasMaisFracaPorNucleoVaiParaRevisao() {
        // 5950X (16 núcleos) × i9-9900K (8 núcleos): 300% no total e 150% por núcleo → atende, sem alerta.
        var ok = first(field("CPU", "Processor: Intel Core i9-9900K", null), pc("AMD Ryzen 9 5950X 16-Core Processor", null));
        assertEquals("COMPATIBLE", ok.status());
        assertTrue(!ok.explanation().contains("por núcleo"));
        // Ryzen 5 1600 (6 núcleos, 96,7) × i7-7700K (4 núcleos, 88,9): 109% no total, mas por núcleo
        // 16,1 ÷ 22,2 = 73% → revisão manual (jogos dependem mais do desempenho por núcleo).
        var r = first(field("CPU", "Processor: Intel Core i7-7700K", null),
                pc("AMD Ryzen 5 1600 Six-Core Processor", null));
        assertEquals("MANUAL_REVIEW", r.status(), "total maior, mas mais fraco por núcleo");
        assertTrue(r.explanation().contains("por núcleo é 73% do exigido (6 núcleos × 4)"), r.explanation());
    }

    @Test
    void cpuComMenosNucleosMasMaisRapidaPorNucleoNaoFicaAbaixo() {
        // i5-4460 (4 núcleos, 45) × FX-8350 (8 núcleos, 80): total 56%, por núcleo 113%.
        var r = first(field("CPU", "Processor: AMD FX-8350", null), pc("Intel Core i5-4460 CPU @ 3.20GHz", null));
        assertEquals("MANUAL_REVIEW", r.status());
        assertTrue(r.explanation().contains("O total é menor (menos núcleos)"), r.explanation());
    }

    @Test
    void hyperThreadingNaoContaComoNucleo() {
        // i7-4790K (4 núcleos/8 threads, 73,7) × i5-4460 (4 núcleos/4 threads, 45): atende sem alerta por núcleo.
        var r = first(field("CPU", "Processor: Intel Core i5-4460", null), pc("Intel Core i7-4790K CPU @ 4.00GHz", null));
        assertEquals("COMPATIBLE", r.status());
        assertTrue(!r.explanation().contains("por núcleo"));
    }

    @Test
    void diferencaPequenaComConfiancaAltaFicaProximoDoExigido() {
        // 73,67 ÷ 78,00 = 94%: abaixo de 100%, mas acima de 85% com confiança alta; por núcleo, o i7 (4) é maior.
        RequirementFieldDto cpu = field("CPU", "Processador: Intel Core i5-8400 or AMD Ryzen 3 3300X", null);
        InventoryDto local = pc("Intel(R) Core(TM) i7-4790K CPU @ 4.00GHz", null);
        var r = first(cpu, local);
        assertEquals("NEAR_REQUIRED", r.status());
        assertTrue(r.explanation().startsWith("Seu processador rende 94% do exigido (Intel Core i5-8400"),
                r.explanation());
        assertTrue(r.explanation().contains(": 6% a menos."), r.explanation());
        GameCompatibilityDto all = evaluator.evaluate(game(cpu), local);
        assertEquals("LIMITED", all.verdict().code(), "deve rodar, com pouca folga");
        assertTrue(all.verdict().pendingReview().isEmpty(), "próximo do exigido não é falta de dado");
        assertTrue(all.overallMinimum().startsWith("Praticamente atende"), all.overallMinimum());
    }

    @Test
    void fraseDiretaDizQuemEMelhor() {
        assertEquals("Seu processador é 62% mais rápido que o exigido (X).", GameCompatibilityEvaluator.plain("CPU", 1.62, "X"));
        assertEquals("Sua placa de vídeo rende 59% do exigido (Y): 41% a menos.",
                GameCompatibilityEvaluator.plain("GPU", 0.59, "Y"));
        assertEquals("Seu processador e o exigido (Z) são praticamente iguais em desempenho.",
                GameCompatibilityEvaluator.plain("CPU", 0.98, "Z"));
    }

    @Test
    void semDadoComparavelContinuaRevisaoManual() {
        var r = first(field("CPU", "Processor: Intel Core i5-12400F", null), pc("Intel Core i7-4790K", null));
        assertEquals("MANUAL_REVIEW", r.status());
        assertNull(r.benchmark());
        GameCompatibilityDto all = evaluator.evaluate(game(field("CPU", "Processor: Intel Core i5-12400F", null)),
                pc("Intel Core i7-4790K", null));
        assertTrue(all.warnings().stream().anyMatch(w -> w.contains("revisão manual")));
    }

    @Test
    void usoDoBenchmarkApareceNosAvisos() {
        GameCompatibilityDto all = evaluator.evaluate(game(field("CPU", "Processador: Core i7-7700K", null)),
                pc("Intel Core i7-4790K CPU @ 4.00GHz", null));
        assertTrue(all.warnings().stream().anyMatch(w -> w.contains("Blender")));
        assertTrue(all.warnings().stream().noneMatch(w -> w.contains("FPS")), "o aviso de FPS fica no rodapé");
    }
}
