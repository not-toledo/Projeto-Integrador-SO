package com.pccheck.benchmark;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class BenchmarkTest {

    static final String CSV = """
            # Gerado de opendata-2026-11-18-000000+0000.jsonl (Blender Open Data, https://opendata.blender.org, licença CC0).
            kind,key,backend,blender_version,median_score,p25,p75,samples,cores,device_name
            CPU,intel-core-i7-4790k,CPU,4.5.0,73.67,66.95,84.12,29,8,Intel Core i7-4790K CPU @ 4.00GHz
            CPU,intel-core-i7-7700k,CPU,4.5.0,88.89,85.39,94.47,39,8,Intel Core i7-7700K CPU @ 4.20GHz
            CPU,intel-core-i7-4790k,CPU,5.1.1,78.72,78.15,80.98,3,8,Intel Core i7-4790K CPU @ 4.00GHz
            CPU,intel-core-i7-7700k,CPU,5.1.1,85.92,84.26,89.76,3,8,Intel Core i7-7700K CPU @ 4.20GHz
            GPU,nvidia-gtx-1060,OPTIX,4.5.0,360.00,350.00,370.00,80,,"NVIDIA GeForce GTX 1060 6GB"
            GPU,nvidia-gtx-1050-ti,CUDA,4.5.0,277.85,269.33,280.47,3,,NVIDIA GeForce GTX 1050 Ti
            """;

    @Test
    void catalogoEscolheOParComMaisExecucoesNaMesmaVersaoEBackend() {
        BenchmarkCatalog c = new BenchmarkCatalog(new StringReader(CSV));
        BenchmarkCatalog.Match m = c.match("CPU", "intel-core-i7-4790k", "intel-core-i7-7700k");
        assertEquals("4.5.0", m.local().version(), "29/39 execuções vencem 3/3 da versão mais nova");
        assertEquals("HIGH", m.confidence());
        assertEquals(73.67 / 88.89, m.ratio(), 1e-9);
        assertTrue(m.source().contains("2026-11-18"));
        // Backends diferentes (CUDA × OPTIX) não são comparados.
        assertNull(c.match("GPU", "nvidia-gtx-1050-ti", "nvidia-gtx-1060"));
        assertNull(c.match("CPU", "intel-core-i7-4790k", "amd-ryzen-5-1600"));
    }

    @Test
    void gtxERtxNaoSaoComparadasNoOptix() {
        String csv = "kind,key,backend,blender_version,median_score,p25,p75,samples,cores,device_name\n"
                + "GPU,nvidia-gtx-1660,OPTIX,4.0.0,700,690,710,50,,GTX 1660\n"
                + "GPU,nvidia-rtx-2060,OPTIX,4.0.0,1900,1890,1910,50,,RTX 2060\n"
                + "GPU,nvidia-gtx-1660,CUDA,3.3.0,650,640,660,12,,GTX 1660\n"
                + "GPU,nvidia-rtx-2060,CUDA,3.3.0,980,970,990,12,,RTX 2060\n";
        BenchmarkCatalog c = new BenchmarkCatalog(new StringReader(csv));
        BenchmarkCatalog.Match m = c.match("GPU", "nvidia-gtx-1660", "nvidia-rtx-2060");
        assertEquals("CUDA", m.local().backend(), "os núcleos de ray tracing só aceleram o OptiX");
        assertEquals("NVIDIA Turing", BenchmarkCatalog.gpuGeneration("nvidia-gtx-1660"));
        assertEquals("NVIDIA Turing", BenchmarkCatalog.gpuGeneration("nvidia-rtx-2060-super"));
        assertEquals("NVIDIA Maxwell/Pascal", BenchmarkCatalog.gpuGeneration("nvidia-gtx-1050-ti"));
        assertEquals("AMD RDNA 2", BenchmarkCatalog.gpuGeneration("amd-rx-6700-xt"));
        assertNull(BenchmarkCatalog.gpuGeneration("amd-rx-580"));
    }

    @Test
    void confiancaMediaComPoucasExecucoesOuDispersaoAlta() {
        String csv = "kind,key,backend,blender_version,median_score,p25,p75,samples,cores,device_name\n"
                + "CPU,a,CPU,4.0.0,100,99,101,4,8,A\nCPU,b,CPU,4.0.0,100,99,101,50,8,B\n"
                + "CPU,c,CPU,4.0.0,100,60,140,50,8,C\n";
        BenchmarkCatalog c = new BenchmarkCatalog(new StringReader(csv));
        assertEquals("MEDIUM", c.match("CPU", "a", "b").confidence(), "só 4 execuções");
        assertEquals("MEDIUM", c.match("CPU", "c", "b").confidence(), "intervalo interquartil de 80%");
    }

    @Test
    void versoesComparadasNumericamente() {
        assertTrue(BenchmarkCatalog.compareVersions("4.10.0", "4.5.0") > 0);
        assertTrue(BenchmarkCatalog.compareVersions("5.2.0 LTS", "5.1.1") > 0);
        assertEquals(0, BenchmarkCatalog.compareVersions("4.5.0", "4.5.0"));
        assertNull(BenchmarkCatalog.parse("CPU,x,CPU,4.0.0,abc,1,2,3,4,X"));
        assertEquals("NVIDIA \"Especial\", Ed", BenchmarkCatalog.parse(
                "GPU,k,OPTIX,4.0.0,1,1,1,3,,\"NVIDIA \"\"Especial\"\", Ed\"").deviceName());
    }

    @Test
    void tabelaRealDoProjetoCarrega() {
        BenchmarkCatalog real = new BenchmarkCatalog();
        assertFalse(real.isEmpty());
        assertTrue(real.has("CPU", "intel-core-i7-4790k"));
        assertTrue(real.has("GPU", "nvidia-gtx-1060"));
        assertFalse(real.has("GPU", "amd-rx-480"), "Polaris não roda o benchmark atual (sem HIP)");
        assertTrue(BenchmarkCatalog.empty().isEmpty());
    }

    // ---- Gerador: registros no formato real (v4) do Blender Open Data ----

    static final ObjectMapper JSON = new ObjectMapper();

    static String run(String device, String type, String version, double... spm) {
        String[] scenes = {"monster", "junkshop", "classroom"};
        StringBuilder data = new StringBuilder();
        for (int i = 0; i < spm.length; i++) {
            if (i > 0) data.append(',');
            data.append("{\"blender_version\":{\"version\":\"").append(version).append("\"},")
                    .append("\"device_info\":{\"compute_devices\":[{\"name\":\"").append(device).append("\",\"type\":\"")
                    .append(type).append("\"}],\"device_type\":\"").append(type).append("\"},")
                    .append("\"scene\":{\"label\":\"").append(scenes[i]).append("\"},")
                    .append("\"stats\":{\"samples_per_minute\":").append(spm[i]).append("},")
                    .append("\"system_info\":{\"num_cpu_sockets\":1,\"num_cpu_cores\":4}}");
        }
        return "{\"schema_version\":\"v4\",\"data\":[" + data + "]}";
    }

    @Test
    void geradorSomaAsTresCenasEFiltraExecucoesFora() throws Exception {
        Map<String, BlenderDatasetBuilder.Group> g = new HashMap<>();
        assertEquals(1, BlenderDatasetBuilder.accept(JSON.readTree(
                run("Intel Core i7-4790K CPU @ 4.00GHz", "CPU", "4.5.0", 20, 30, 25)), g));
        assertEquals(List.of(75.0), g.get("CPU,intel-core-i7-4790k,CPU,4.5.0").scores);

        assertEquals(0, BlenderDatasetBuilder.accept(JSON.readTree(
                run("Intel Core i7-4790K CPU @ 4.00GHz", "CPU", "4.5.0", 20, 30)), g), "faltou uma cena");
        assertEquals(0, BlenderDatasetBuilder.accept(JSON.readTree(
                run("NVIDIA GeForce RTX 3060 Laptop GPU", "OPTIX", "4.5.0", 1, 1, 1)), g), "notebook");
        assertEquals(0, BlenderDatasetBuilder.accept(JSON.readTree(
                run("AMD Radeon RX 580 Series", "CUDA", "4.5.0", 1, 1, 1)), g), "AMD no backend CUDA (ZLUDA)");
        assertEquals(0, BlenderDatasetBuilder.accept(JSON.readTree(
                run("Apple M1", "METAL", "4.5.0", 1, 1, 1)), g), "backend fora do escopo");
        assertEquals(0, BlenderDatasetBuilder.accept(JSON.readTree(
                run("Intel Xeon E5-2680 v4", "CPU", "4.5.0", 1, 1, 1)), g), "sem modelo exato");
        assertEquals(0, BlenderDatasetBuilder.accept(JSON.readTree(
                "{\"schema_version\":\"v3\",\"data\":[]}"), g), "formato antigo");
        assertEquals(1, BlenderDatasetBuilder.accept(JSON.readTree(
                run("NVIDIA GeForce GTX 1060 6GB", "OPTIX", "4.5.0", 100, 150, 110)), g));
    }

    @Test
    void geradorExigeTresExecucoesECalculaQuartis() {
        Map<String, BlenderDatasetBuilder.Group> g = new HashMap<>();
        BlenderDatasetBuilder.Group a = new BlenderDatasetBuilder.Group();
        a.scores.addAll(List.of(10.0, 30.0, 20.0, 40.0));
        a.names.put("CPU A", 4);
        a.cores.put(8, 4);
        g.put("CPU,a,CPU,4.0.0", a);
        BlenderDatasetBuilder.Group b = new BlenderDatasetBuilder.Group();
        b.scores.addAll(List.of(1.0, 2.0));
        b.names.put("CPU B", 2);
        g.put("CPU,b,CPU,4.0.0", b);
        assertEquals(List.of("CPU,a,CPU,4.0.0,25.00,17.50,32.50,4,8,CPU A"), BlenderDatasetBuilder.rows(g));
    }
}
