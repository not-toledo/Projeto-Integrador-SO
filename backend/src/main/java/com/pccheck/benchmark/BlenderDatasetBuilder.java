package com.pccheck.benchmark;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pccheck.compatibility.HardwareModels;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Gera {@code benchmarks/blender-opendata.csv} a partir do snapshot do Blender Open Data (licença CC0).
 * Executado pelo script {@code scripts/atualizar-benchmarks}, nunca durante o uso do PCCheck.
 *
 * <p>Regras (as mesmas do site do Blender Open Data, versão 4 do formato):
 * <ul>
 *   <li>pontuação de uma execução = soma das amostras por minuto das três cenas (monster, junkshop, classroom);
 *   execuções sem as três cenas são descartadas;</li>
 *   <li>CPU: um único soquete; GPU: um único dispositivo e do mesmo backend da execução (sem CPU junto) e sem
 *   variantes de notebook ("Laptop", "Mobile", "Max-Q"), que têm o mesmo nome e outra potência;</li>
 *   <li>o modelo é identificado pelas mesmas regras do avaliador ({@link HardwareModels}); nomes sem modelo exato
 *   (Xeon, Apple M1...) ficam de fora;</li>
 *   <li>uma linha por modelo, backend e versão do Blender, com pelo menos {@value #MIN_SAMPLES} execuções: mediana,
 *   quartis e quantidade. Versões diferentes usam cenas diferentes e não são comparáveis entre si.</li>
 * </ul>
 */
public final class BlenderDatasetBuilder {

    static final int MIN_SAMPLES = 3;
    static final Set<String> SCENES = Set.of("monster", "junkshop", "classroom");
    /** Backend de GPU e o fabricante que ele atende nativamente (prefixo da chave do modelo). */
    static final Map<String, String> VENDOR_OF_BACKEND = Map.of(
            "OPTIX", "nvidia-", "CUDA", "nvidia-", "HIP", "amd-", "ONEAPI", "intel-");
    static final Set<String> GPU_BACKENDS = VENDOR_OF_BACKEND.keySet();
    private static final ObjectMapper JSON = new ObjectMapper();

    private BlenderDatasetBuilder() {
    }

    /** Uso: {@code BlenderDatasetBuilder <opendata.zip> <saída.csv>}. */
    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.err.println("Uso: BlenderDatasetBuilder <opendata-latest.zip> <saida.csv>");
            System.exit(2);
        }
        Path zip = Path.of(args[0]);
        Path out = Path.of(args[1]);
        Map<String, Group> groups = new HashMap<>();
        long[] counts = new long[3]; // linhas, execuções aceitas, linhas inválidas
        String snapshot;
        try (ZipFile z = new ZipFile(zip.toFile())) {
            ZipEntry entry = z.stream().filter(e -> e.getName().endsWith(".jsonl")).findFirst()
                    .orElseThrow(() -> new IOException("O zip não tem o arquivo .jsonl do Blender Open Data."));
            snapshot = entry.getName();
            try (BufferedReader in = new BufferedReader(new InputStreamReader(z.getInputStream(entry),
                    StandardCharsets.UTF_8), 1 << 16)) {
                String line;
                while ((line = in.readLine()) != null) {
                    counts[0]++;
                    try {
                        counts[1] += accept(JSON.readTree(line), groups);
                    } catch (IOException | RuntimeException e) {
                        counts[2]++;
                    }
                }
            }
        }
        List<String> rows = rows(groups);
        Files.createDirectories(out.toAbsolutePath().getParent());
        try (Writer w = Files.newBufferedWriter(out, StandardCharsets.UTF_8)) {
            w.write("# Gerado de " + snapshot + " (Blender Open Data, https://opendata.blender.org, licença CC0).\n");
            w.write("# Pontuação = soma das amostras por minuto de monster, junkshop e classroom. "
                    + "Não altere à mão: rode scripts/atualizar-benchmarks.\n");
            w.write("kind,key,backend,blender_version,median_score,p25,p75,samples,cores,device_name\n");
            for (String row : rows) {
                w.write(row);
                w.write('\n');
            }
        }
        System.out.printf("Linhas lidas: %d; execuções aceitas: %d; linhas inválidas: %d; linhas no CSV: %d%n",
                counts[0], counts[1], counts[2], rows.size());
    }

    /** Pontuações de um modelo num backend e numa versão do Blender. */
    static final class Group {
        final List<Double> scores = new ArrayList<>();
        final Map<String, Integer> names = new HashMap<>();
        final Map<Integer, Integer> cores = new HashMap<>();
    }

    /** Uma execução (documento) do Blender Open Data; devolve 1 se aceita. */
    static int accept(JsonNode doc, Map<String, Group> groups) {
        if (!"v4".equals(doc.path("schema_version").asText())) {
            return 0;
        }
        String backend = null;
        String device = null;
        String version = null;
        int cores = 0;
        double score = 0;
        Set<String> scenes = new java.util.HashSet<>();
        for (JsonNode d : doc.path("data")) {
            JsonNode info = d.path("device_info");
            String type = info.path("device_type").asText();
            JsonNode devices = info.path("compute_devices");
            String name = single(devices, type);
            String v = d.path("blender_version").path("version").asText();
            double spm = d.path("stats").path("samples_per_minute").asDouble(-1);
            if (name == null || v.isEmpty() || !(spm > 0) || d.path("system_info").path("num_cpu_sockets").asInt(1) > 1) {
                return 0;
            }
            if (backend == null) {
                backend = type;
                device = name;
                version = v;
                cores = d.path("system_info").path("num_cpu_cores").asInt(0);
            } else if (!backend.equals(type) || !device.equals(name) || !version.equals(v)) {
                return 0; // execução misturando dispositivos ou versões
            }
            if (!scenes.add(d.path("scene").path("label").asText())) {
                return 0;
            }
            score += spm;
        }
        if (backend == null || !scenes.equals(SCENES)) {
            return 0;
        }
        String kind;
        Set<String> keys;
        if ("CPU".equals(backend)) {
            kind = "CPU";
            keys = HardwareModels.cpuModels(device);
        } else if (GPU_BACKENDS.contains(backend)) {
            String n = HardwareModels.normalize(device);
            if (n.contains("laptop") || n.contains("mobile") || n.contains("max q")) {
                return 0;
            }
            kind = "GPU";
            keys = HardwareModels.gpuModels(device);
        } else {
            return 0;
        }
        if (keys.size() != 1) {
            return 0; // sem modelo exato, ou nome ambíguo
        }
        String model = keys.iterator().next();
        if ("GPU".equals(kind) && !model.startsWith(VENDOR_OF_BACKEND.get(backend))) {
            return 0; // ex.: placa AMD no backend CUDA via camada de compatibilidade (ZLUDA): fora do padrão
        }
        String key = kind + "," + model + "," + backend + "," + version;
        Group g = groups.computeIfAbsent(key, k -> new Group());
        g.scores.add(score);
        g.names.merge(device.strip(), 1, Integer::sum);
        if ("CPU".equals(kind) && cores > 0) {
            g.cores.merge(cores, 1, Integer::sum);
        }
        return 1;
    }

    /** Nome do único dispositivo da execução, do tipo dela; null se houver mais de um ou de outro tipo. */
    private static String single(JsonNode devices, String type) {
        String name = null;
        for (JsonNode dev : devices) {
            String n = dev.path("name").asText();
            if (!type.equals(dev.path("type").asText()) || n.isBlank()) {
                return null;
            }
            if (name == null) {
                name = n;
            } else if (!name.equals(n) || !"CPU".equals(type)) {
                return null; // CPU pode aparecer repetida por núcleo; duas GPUs, não
            }
        }
        return name;
    }

    static List<String> rows(Map<String, Group> groups) {
        List<String> rows = new ArrayList<>();
        for (Map.Entry<String, Group> e : new TreeMap<>(groups).entrySet()) {
            Group g = e.getValue();
            if (g.scores.size() < MIN_SAMPLES) {
                continue;
            }
            List<Double> s = new ArrayList<>(g.scores);
            Collections.sort(s);
            String name = Collections.max(g.names.entrySet(), Map.Entry.comparingByValue()).getKey();
            String cores = g.cores.isEmpty() ? ""
                    : String.valueOf(Collections.max(g.cores.entrySet(), Map.Entry.comparingByValue()).getKey());
            rows.add(String.join(",", e.getKey(), fmt(quantile(s, 0.5)), fmt(quantile(s, 0.25)), fmt(quantile(s, 0.75)),
                    String.valueOf(s.size()), cores, csv(name)));
        }
        return rows;
    }

    /** Quantil com interpolação linear (a mesma da mediana usual). */
    static double quantile(List<Double> sorted, double q) {
        double pos = (sorted.size() - 1) * q;
        int lo = (int) Math.floor(pos);
        int hi = (int) Math.ceil(pos);
        return sorted.get(lo) + (sorted.get(hi) - sorted.get(lo)) * (pos - lo);
    }

    private static String fmt(double v) {
        return String.format(Locale.ROOT, "%.2f", v);
    }

    private static String csv(String text) {
        String t = text.replace("\"", "\"\"");
        return t.contains(",") || t.contains("\"") ? "\"" + t + "\"" : t;
    }
}
