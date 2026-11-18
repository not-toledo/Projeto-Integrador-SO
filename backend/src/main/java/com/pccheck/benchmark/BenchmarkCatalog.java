package com.pccheck.benchmark;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Tabela de pontuações do Blender Open Data ({@code benchmarks/blender-opendata.csv}, gerada por
 * {@link BlenderDatasetBuilder}). Só compara pontuações da mesma versão do Blender e do mesmo backend; sem um par
 * comparável, não há resultado (o avaliador mantém a revisão manual).
 */
@Component
public class BenchmarkCatalog {

    static final String RESOURCE = "/benchmarks/blender-opendata.csv";
    /** Confiança alta: 10+ execuções de cada lado e intervalo interquartil até 25% da mediana. */
    static final int HIGH_MIN_SAMPLES = 10;
    static final double HIGH_MAX_SPREAD = 0.25;
    private static final Pattern SNAPSHOT = Pattern.compile("opendata-(\\d{4}-\\d{2}-\\d{2})");

    /** Uma linha do CSV. */
    public record Entry(String kind, String key, String backend, String version, double median, double p25,
                        double p75, int samples, Integer cores, String deviceName) {
        double spread() {
            return median > 0 ? (p75 - p25) / median : Double.MAX_VALUE;
        }
    }

    /** Par comparável escolhido. */
    public record Match(Entry local, Entry required, String confidence, String source) {
        public double ratio() {
            return local.median() / required.median();
        }
    }

    private final Map<String, List<Entry>> byModel; // "CPU|chave" → linhas
    private final String source;

    public BenchmarkCatalog() {
        this(open());
    }

    /** Tabela lida de {@code csv} (testes); null = vazia. */
    public BenchmarkCatalog(Reader csv) {
        Map<String, List<Entry>> map = new HashMap<>();
        String snapshot = null;
        if (csv != null) {
            try (BufferedReader in = new BufferedReader(csv)) {
                String line;
                while ((line = in.readLine()) != null) {
                    if (line.startsWith("#")) {
                        Matcher m = SNAPSHOT.matcher(line);
                        if (snapshot == null && m.find()) snapshot = m.group(1);
                        continue;
                    }
                    Entry e = parse(line);
                    if (e != null) map.computeIfAbsent(e.kind() + "|" + e.key(), k -> new ArrayList<>()).add(e);
                }
            } catch (IOException e) {
                map.clear();
            }
        }
        this.byModel = map;
        this.source = "Blender Open Data (opendata.blender.org, CC0)"
                + (snapshot != null ? ", snapshot de " + snapshot : "");
    }

    /** Catálogo vazio: nenhuma comparação por benchmark (CPU/GPU diferentes ficam para revisão manual). */
    public static BenchmarkCatalog empty() {
        return new BenchmarkCatalog((Reader) null);
    }

    private static Reader open() {
        InputStream in = BenchmarkCatalog.class.getResourceAsStream(RESOURCE);
        return in == null ? null : new InputStreamReader(in, StandardCharsets.UTF_8);
    }

    public boolean isEmpty() {
        return byModel.isEmpty();
    }

    public boolean has(String kind, String key) {
        return byModel.containsKey(kind + "|" + key);
    }

    /**
     * Melhor par (mesmo backend e mesma versão) entre o modelo local e o exigido: o que tem mais execuções do lado
     * com menos execuções; no empate, a versão mais recente. Null se não houver par.
     */
    public Match match(String kind, String localKey, String requiredKey) {
        List<Entry> local = byModel.getOrDefault(kind + "|" + localKey, List.of());
        List<Entry> required = byModel.getOrDefault(kind + "|" + requiredKey, List.of());
        Entry bestLocal = null;
        Entry bestRequired = null;
        boolean sameRtClass = rtCores(localKey) == rtCores(requiredKey);
        for (Entry l : local) {
            for (Entry r : required) {
                if (!l.backend().equals(r.backend()) || !l.version().equals(r.version())) continue;
                // No OptiX, as RTX usam núcleos de ray tracing que aceleram muito a renderização e pouco os jogos:
                // GTX × RTX só é comparado no CUDA, que não usa esses núcleos.
                if ("OPTIX".equals(l.backend()) && !sameRtClass) continue;
                if (bestLocal == null || better(l, r, bestLocal, bestRequired)) {
                    bestLocal = l;
                    bestRequired = r;
                }
            }
        }
        if (bestLocal == null) return null;
        boolean high = Math.min(bestLocal.samples(), bestRequired.samples()) >= HIGH_MIN_SAMPLES
                && bestLocal.spread() <= HIGH_MAX_SPREAD && bestRequired.spread() <= HIGH_MAX_SPREAD;
        return new Match(bestLocal, bestRequired, high ? "HIGH" : "MEDIUM", source);
    }

    private static final List<Map.Entry<Pattern, String>> GPU_GENERATIONS = List.of(
            Map.entry(Pattern.compile("^nvidia-(gtx|gt)-(7[0-9]{2}|9[0-9]{2}|10[0-9]{2})(-.*)?$"), "NVIDIA Maxwell/Pascal"),
            Map.entry(Pattern.compile("^nvidia-(gtx-16[0-9]{2}|rtx-20[0-9]{2})(-.*)?$"), "NVIDIA Turing"),
            Map.entry(Pattern.compile("^nvidia-rtx-30[0-9]{2}(-.*)?$"), "NVIDIA Ampere"),
            Map.entry(Pattern.compile("^nvidia-rtx-40[0-9]{2}(-.*)?$"), "NVIDIA Ada Lovelace"),
            Map.entry(Pattern.compile("^nvidia-rtx-50[0-9]{2}(-.*)?$"), "NVIDIA Blackwell"),
            Map.entry(Pattern.compile("^amd-rx-5[0-9]{3}(-.*)?$"), "AMD RDNA"),
            Map.entry(Pattern.compile("^amd-rx-6[0-9]{3}(-.*)?$"), "AMD RDNA 2"),
            Map.entry(Pattern.compile("^amd-rx-7[0-9]{3}(-.*)?$"), "AMD RDNA 3"),
            Map.entry(Pattern.compile("^amd-rx-9[0-9]{3}(-.*)?$"), "AMD RDNA 4"),
            Map.entry(Pattern.compile("^intel-arc-a[0-9]{3}$"), "Intel Arc Alchemist"));

    /**
     * Geração de arquitetura da GPU, pela chave do modelo; null se desconhecida. Renderização e jogos escalam de
     * forma diferente entre gerações (as mais novas ganham muito mais na renderização), então GPUs só são comparadas
     * por benchmark dentro da mesma geração.
     */
    public static String gpuGeneration(String key) {
        for (Map.Entry<Pattern, String> g : GPU_GENERATIONS) {
            if (g.getKey().matcher(key).matches()) return g.getValue();
        }
        return null;
    }

    /** Placas NVIDIA RTX têm núcleos de ray tracing (as GTX e GT, não). */
    static boolean rtCores(String key) {
        return key.startsWith("nvidia-rtx-");
    }

    private static boolean better(Entry l, Entry r, Entry bl, Entry br) {
        int n = Math.min(l.samples(), r.samples());
        int best = Math.min(bl.samples(), br.samples());
        if (n != best) return n > best;
        return compareVersions(l.version(), bl.version()) > 0;
    }

    /** "4.10.0" > "4.5.0"; sufixos como " LTS" são ignorados. */
    static int compareVersions(String a, String b) {
        String[] x = a.replaceAll("[^0-9.].*$", "").split("\\.");
        String[] y = b.replaceAll("[^0-9.].*$", "").split("\\.");
        for (int i = 0; i < Math.max(x.length, y.length); i++) {
            int xi = i < x.length && !x[i].isEmpty() ? Integer.parseInt(x[i]) : 0;
            int yi = i < y.length && !y[i].isEmpty() ? Integer.parseInt(y[i]) : 0;
            if (xi != yi) return Integer.compare(xi, yi);
        }
        return 0;
    }

    /** Linha CSV (o nome do dispositivo pode estar entre aspas); null se inválida. */
    static Entry parse(String line) {
        List<String> cols = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (quoted) {
                if (c == '"' && i + 1 < line.length() && line.charAt(i + 1) == '"') { cur.append('"'); i++; }
                else if (c == '"') quoted = false;
                else cur.append(c);
            } else if (c == '"') {
                quoted = true;
            } else if (c == ',') {
                cols.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        cols.add(cur.toString());
        if (cols.size() != 10 || "kind".equals(cols.get(0))) return null;
        try {
            return new Entry(cols.get(0), cols.get(1), cols.get(2), cols.get(3), Double.parseDouble(cols.get(4)),
                    Double.parseDouble(cols.get(5)), Double.parseDouble(cols.get(6)), Integer.parseInt(cols.get(7)),
                    cols.get(8).isEmpty() ? null : Integer.valueOf(cols.get(8)), cols.get(9));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
