package com.pccheck.compatibility;

import com.pccheck.agent.model.CpuInfo;
import com.pccheck.benchmark.BenchmarkCatalog;
import com.pccheck.agent.model.DiskInfo;
import com.pccheck.agent.model.GpuInfo;
import com.pccheck.agent.model.OsInfo;
import com.pccheck.agent.model.VolumeInfo;
import com.pccheck.dto.ComponentCompatibilityDto;
import com.pccheck.dto.ComponentCompatibilityDto.BenchmarkDto;
import com.pccheck.dto.ComponentDto;
import com.pccheck.dto.GameCompatibilityDto;
import com.pccheck.dto.GameRequirementsDto;
import com.pccheck.dto.InventoryDto;
import com.pccheck.dto.RequirementFieldDto;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Avaliação conservadora: só declara "atende" quando há comparação objetiva (número com número, o mesmo modelo de
 * CPU/GPU ou, para modelos diferentes, pontuações do benchmark do Blender na mesma versão e no mesmo backend).
 * Sem dado comparável, CPU/GPU ficam para revisão manual.
 */
@Component
public class GameCompatibilityEvaluator {

    static final String COMPATIBLE = "COMPATIBLE";
    static final String BELOW = "BELOW_MINIMUM";
    static final String MANUAL = "MANUAL_REVIEW";
    /**
     * Benchmark com confiança alta e diferença pequena (85% a 99% do exigido): desempenho parecido. Não é
     * "atende" (fica abaixo), nem "abaixo" (a diferença é pequena), nem falta de dado.
     */
    static final String NEAR = "NEAR_REQUIRED";
    static final String UNKNOWN = "UNKNOWN";
    private static final String NOT_AVAILABLE = "Não disponível";
    /**
     * A memória visível ao Windows é um pouco menor que a instalada (parte fica reservada para o sistema ou para
     * a GPU integrada). Entre 90% e 100% do requisito o resultado é revisão manual, e não "abaixo".
     */
    static final double RAM_RESERVED_MARGIN = 0.90;
    private static final Pattern WINDOWS_VERSION = Pattern.compile("(?i)windows\\s*(\\d+(?:\\.\\d+)?)");
    private static final Pattern REQUIRES_64_BIT = Pattern.compile("(?i)64[- ]?bits?|\\bx64\\b");

    /**
     * Decisão por benchmark (heurísticas do projeto). Confiança alta: atende com razão ≥ 1,0, abaixo com razão
     * &lt; 0,85. Confiança média: margens mais largas (≥ 1,15 e &lt; 0,70). Entre as margens: revisão manual.
     */
    static final double HIGH_OK = 1.0;
    static final double HIGH_BELOW = 0.85;
    static final double MEDIUM_OK = 1.15;
    static final double MEDIUM_BELOW = 0.70;
    /** Pontuação por núcleo físico local abaixo de 85% da exigida: o total maior não basta para atender. */
    static final double PER_CORE_WARNING = 0.85;

    private final BenchmarkCatalog benchmarks;

    /** Sem tabela de benchmarks (testes das regras básicas). */
    public GameCompatibilityEvaluator() {
        this(BenchmarkCatalog.empty());
    }

    @Autowired
    public GameCompatibilityEvaluator(BenchmarkCatalog benchmarks) {
        this.benchmarks = benchmarks;
    }

    public GameCompatibilityDto evaluate(GameRequirementsDto game, InventoryDto pc) {
        List<ComponentCompatibilityDto> min = compare(game.minimum(), pc);
        List<ComponentCompatibilityDto> rec = compare(game.recommended(), pc);
        List<String> warnings = new ArrayList<>(game.warnings());
        // O aviso sobre FPS fica no rodapé fixo da interface; aqui só entram limitações que afetaram este jogo.
        if (usesBenchmark(min) || usesBenchmark(rec)) {
            warnings.add("CPU e GPU de modelos diferentes foram comparadas pelo benchmark de renderização do Blender "
                    + "(Blender Open Data): mede desempenho relativo em renderização, não desempenho em jogos. Resultado "
                    + "abaixo não significa que o jogo não abre.");
        }
        if (hasCpuOrGpuReview(min) || hasCpuOrGpuReview(rec)) {
            warnings.add("CPU e GPU sem o mesmo modelo publicado nem dados de benchmark comparáveis (ou com "
                    + "desempenho próximo do exigido) ficam para revisão manual.");
        }
        return new GameCompatibilityDto(game.appId(), game.name(),
                overall(min, "mínimos"), overall(rec, "recomendados"), min, rec, game.minimumRawText(),
                game.recommendedRawText(), Instant.now(), game.source(), List.copyOf(warnings),
                CompatibilityVerdict.of(min, rec));
    }

    private static boolean usesBenchmark(List<ComponentCompatibilityDto> results) {
        return results.stream().anyMatch(r -> r.benchmark() != null);
    }

    private static boolean hasCpuOrGpuReview(List<ComponentCompatibilityDto> results) {
        return results.stream().anyMatch(r -> MANUAL.equals(r.status())
                && ("CPU".equals(r.component()) || "GPU".equals(r.component())));
    }

    /** Linhas sem comparação possível (DirectX, Rede, Notas...) ficam só no texto original. */
    private List<ComponentCompatibilityDto> compare(List<RequirementFieldDto> fields, InventoryDto pc) {
        List<ComponentCompatibilityDto> result = new ArrayList<>();
        for (RequirementFieldDto field : fields) {
            switch (field.component()) {
                case "RAM" -> result.add(compareRam(field, pc));
                case "STORAGE" -> result.add(compareStorage(field, pc));
                case "CPU" -> result.add(compareCpu(field, pc));
                case "GPU" -> result.add(compareGpu(field, pc));
                case "OS" -> result.add(compareOs(field, pc));
                case "SSD" -> result.add(compareSsd(field, pc));
                default -> {
                    // não comparável
                }
            }
        }
        return List.copyOf(result);
    }

    private ComponentCompatibilityDto compareRam(RequirementFieldDto f, InventoryDto pc) {
        var memory = data(pc.memory());
        Long detected = memory == null ? null : memory.totalBytes();
        String shown = detected == null ? NOT_AVAILABLE : bytes(detected);
        if (f.parsedBytes() == null || detected == null) {
            return row("RAM", f, shown, UNKNOWN, "Não foi possível comparar numericamente o requisito e a memória detectada.");
        }
        if (detected >= f.parsedBytes()) {
            return row("RAM", f, shown, COMPATIBLE, "A memória detectada atende ao valor publicado.");
        }
        if (detected >= f.parsedBytes() * RAM_RESERVED_MARGIN) {
            return row("RAM", f, shown, MANUAL, "A memória visível ao Windows está um pouco abaixo do valor publicado. "
                    + "Isso é comum quando parte da RAM fica reservada (por exemplo, para a GPU integrada); "
                    + "confira a memória instalada.");
        }
        return row("RAM", f, shown, BELOW, "A memória detectada está abaixo do requisito.");
    }

    /**
     * "SSD required": atende se há um SSD. Abaixo só quando todos os discos internos têm tipo conhecido e nenhum é
     * SSD; disco interno de tipo desconhecido deixa o item sem confirmação. Discos USB não contam como SSD.
     */
    private ComponentCompatibilityDto compareSsd(RequirementFieldDto f, InventoryDto pc) {
        var storage = data(pc.storage());
        List<DiskInfo> drives = storage == null || storage.drives() == null ? List.of() : storage.drives();
        List<DiskInfo> internal = drives.stream().filter(d -> !"USB".equals(d.busType())).toList();
        List<String> ssds = internal.stream().filter(d -> "SSD".equals(d.mediaType()))
                .map(d -> d.model() != null ? d.model() : d.name()).toList();
        if (!ssds.isEmpty()) {
            return row("Armazenamento (SSD)", f, "SSD: " + String.join("; ", ssds), COMPATIBLE,
                    "Há um SSD neste computador. Instale o jogo nele para atender ao requisito.");
        }
        if (!internal.isEmpty() && internal.stream().allMatch(d -> d.mediaType() != null)) {
            return row("Armazenamento (SSD)", f, "Somente HDD", BELOW,
                    "Nenhum SSD foi encontrado: os discos internos são HDD, e o jogo exige SSD.");
        }
        return row("Armazenamento (SSD)", f, NOT_AVAILABLE, UNKNOWN,
                "O sistema não informou o tipo (SSD/HDD) de todos os discos internos.");
    }

    private ComponentCompatibilityDto compareStorage(RequirementFieldDto f, InventoryDto pc) {
        Long largest = null;
        var storage = data(pc.storage());
        if (storage != null && storage.volumes() != null) {
            for (VolumeInfo v : storage.volumes()) {
                if (v.freeBytes() != null && (largest == null || v.freeBytes() > largest)) largest = v.freeBytes();
            }
        }
        String shown = largest == null ? NOT_AVAILABLE : bytes(largest) + " livres (maior volume)";
        if (f.parsedBytes() == null || largest == null) {
            return row("Armazenamento", f, shown, UNKNOWN, "Não foi possível comparar o espaço livre com o requisito.");
        }
        boolean ok = largest >= f.parsedBytes();
        return row("Armazenamento", f, shown, ok ? COMPATIBLE : BELOW,
                ok ? "O maior volume tem espaço livre suficiente." : "Nenhum volume tem espaço livre suficiente.");
    }

    private ComponentCompatibilityDto compareCpu(RequirementFieldDto f, InventoryDto pc) {
        CpuInfo cpu = data(pc.cpu());
        String detected = cpu == null ? null : cpu.name();
        if (detected == null) {
            return row("CPU", f, NOT_AVAILABLE, UNKNOWN, "O processador local não foi identificado.");
        }
        Set<String> required = HardwareModels.cpuModels(f.rawText());
        Set<String> local = HardwareModels.cpuModels(detected);
        if (!required.isEmpty() && !local.isEmpty() && Collections.disjoint(required, local)) {
            ComponentCompatibilityDto byBenchmark = byBenchmark("CPU", "CPU", f, detected, local, required, null);
            if (byBenchmark != null) return byBenchmark;
        }
        return compareModels("CPU", f, detected, required, local);
    }

    private ComponentCompatibilityDto compareGpu(RequirementFieldDto f, InventoryDto pc) {
        List<GpuInfo> gpus = data(pc.gpu());
        List<GpuInfo> named = gpus == null ? List.of()
                : gpus.stream().filter(g -> g.name() != null).toList();
        if (named.isEmpty()) {
            return row("GPU", f, NOT_AVAILABLE, UNKNOWN, "O inventário não identificou uma placa de vídeo.");
        }
        Set<String> required = HardwareModels.gpuModels(f.rawText());
        for (GpuInfo gpu : named) {
            if (Collections.disjoint(required, HardwareModels.gpuModels(gpu.name()))) continue;
            if (f.parsedBytes() == null) {
                return row("GPU", f, gpu.name(), COMPATIBLE, "A placa detectada é um dos modelos publicados.");
            }
            if (gpu.vramBytes() == null) {
                return row("GPU", f, gpu.name(), UNKNOWN,
                        "O modelo corresponde, mas a memória de vídeo detectada não está disponível para comparação.");
            }
            boolean enough = gpu.vramBytes() >= f.parsedBytes();
            return row("GPU", f, gpu.name() + " — " + bytes(gpu.vramBytes()), enough ? COMPATIBLE : BELOW,
                    enough ? "Modelo e memória de vídeo atendem ao requisito publicado."
                            : "O modelo corresponde, mas a memória de vídeo está abaixo do valor publicado.");
        }
        String detected = String.join("; ", named.stream().map(GpuInfo::name).toList());
        if (!required.isEmpty()) {
            // A placa com mais memória de vídeo primeiro (em geral a dedicada).
            List<GpuInfo> ordered = named.stream().sorted((a, b) -> Long.compare(
                    b.vramBytes() == null ? 0 : b.vramBytes(), a.vramBytes() == null ? 0 : a.vramBytes())).toList();
            for (GpuInfo gpu : ordered) {
                Set<String> local = HardwareModels.gpuModels(gpu.name());
                if (local.isEmpty()) continue;
                ComponentCompatibilityDto byBenchmark = byBenchmark("GPU", "GPU", f, gpu.name(), local, required, gpu);
                if (byBenchmark != null) return byBenchmark;
            }
        }
        return compareModels("GPU", f, detected, required, Set.of());
    }

    /** Mesmo modelo → atende; requisito sem modelo exato ou modelos diferentes → revisão manual. */
    private ComponentCompatibilityDto compareModels(String component, RequirementFieldDto f, String detected,
                                                    Set<String> required, Set<String> found) {
        if (required.isEmpty()) {
            return row(component, f, detected, MANUAL, "O requisito não cita um modelo exato (ex.: \"Intel Core i5\"); "
                    + "compare manualmente.");
        }
        if (!Collections.disjoint(required, found)) {
            return row(component, f, detected, COMPATIBLE, "O modelo detectado é um dos modelos publicados.");
        }
        return row(component, f, detected, MANUAL, "Modelos diferentes, sem comparação por benchmark possível (sem "
                + "dados dos dois modelos na mesma versão do Blender, ou GPUs de gerações diferentes, em que a "
                + "renderização não representa a diferença em jogos); compare manualmente.");
    }

    /**
     * Compara pelo benchmark com a alternativa publicada do mesmo fabricante (os requisitos da Steam já trazem a
     * equivalência declarada pelo desenvolvedor, ex.: "GTX 1060 / RX 580"); sem ela, com as demais alternativas.
     * Null se nenhum par tiver dados comparáveis.
     */
    private ComponentCompatibilityDto byBenchmark(String kind, String component, RequirementFieldDto f, String detected,
                                                  Set<String> localKeys, Set<String> requiredKeys, GpuInfo gpu) {
        if (benchmarks.isEmpty()) return null;
        for (String localKey : localKeys) {
            String vendor = vendor(localKey);
            List<String> ordered = new ArrayList<>(requiredKeys.stream().filter(k -> vendor(k).equals(vendor)).toList());
            requiredKeys.stream().filter(k -> !vendor(k).equals(vendor)).forEach(ordered::add);
            for (String requiredKey : ordered) {
                if ("GPU".equals(kind)) {
                    // Só GPUs da mesma geração: entre gerações, a renderização exagera a diferença dos jogos.
                    String generation = BenchmarkCatalog.gpuGeneration(localKey);
                    if (generation == null || !generation.equals(BenchmarkCatalog.gpuGeneration(requiredKey))) continue;
                }
                BenchmarkCatalog.Match m = benchmarks.match(kind, localKey, requiredKey);
                if (m != null) return decide(component, f, detected, m, gpu);
            }
        }
        return null;
    }

    private static String vendor(String key) {
        int dash = key.indexOf('-');
        return dash < 0 ? key : key.substring(0, dash);
    }

    private static ComponentCompatibilityDto decide(String component, RequirementFieldDto f, String detected,
                                                    BenchmarkCatalog.Match m, GpuInfo gpu) {
        double ratio = m.ratio();
        boolean high = "HIGH".equals(m.confidence());
        String status = ratio >= (high ? HIGH_OK : MEDIUM_OK) ? COMPATIBLE
                : ratio < (high ? HIGH_BELOW : MEDIUM_BELOW) ? BELOW : high ? NEAR : MANUAL;
        String numbers = String.format(Locale.forLanguageTag("pt-BR"),
                "Benchmark do Blender %s (%s): %s %.1f pontos (%d execuções) × %s %.1f pontos (%d execuções) = %.0f%% "
                        + "do exigido. Confiança %s.",
                m.local().version(), m.local().backend(), m.local().deviceName(), m.local().median(),
                m.local().samples(), m.required().deviceName(), m.required().median(), m.required().samples(),
                ratio * 100, high ? "alta" : "média");
        String verdict = switch (status) {
            case COMPATIBLE -> " O desempenho de renderização é igual ou maior que o do modelo exigido.";
            case BELOW -> " O desempenho de renderização fica claramente abaixo do modelo exigido.";
            case NEAR -> " Diferença pequena: na prática o desempenho é parecido, e o jogo deve rodar, com um pouco "
                    + "menos de folga que no modelo exigido.";
            default -> ratio >= 1 ? " O seu parece mais rápido, mas há poucos dados para garantir; revise manualmente."
                    : " O seu parece um pouco mais lento, mas há poucos dados para decidir; revise manualmente.";
        };
        String extra = "";
        // A renderização usa todos os núcleos; jogos dependem mais do desempenho por núcleo. Quando o total e o
        // desempenho por núcleo apontam em direções opostas, a decisão fica para revisão manual.
        if ("CPU".equals(component) && m.local().cores() != null && m.required().cores() != null) {
            // Por núcleo físico, e não por thread: dividir por threads penalizaria quem tem Hyper-Threading/SMT.
            double perCore = (m.local().median() / m.local().cores())
                    / (m.required().median() / m.required().cores());
            String detail = String.format(Locale.forLanguageTag("pt-BR"), "o desempenho estimado por núcleo é %.0f%% "
                    + "do exigido (%d núcleos × %d)", perCore * 100, m.local().cores(), m.required().cores());
            if (COMPATIBLE.equals(status) && perCore < PER_CORE_WARNING) {
                status = MANUAL;
                verdict = " O total é maior, mas " + detail + ": jogos que dependem de poucos núcleos podem ficar "
                        + "abaixo do exigido; revise manualmente.";
            } else if (BELOW.equals(status) && perCore >= 1.0) {
                status = MANUAL;
                verdict = " O total é menor (menos núcleos), mas " + detail + ": em jogos que usam poucos núcleos o "
                        + "processador local pode atender; revise manualmente.";
            } else if (perCore < PER_CORE_WARNING && !BELOW.equals(status)) {
                extra = " Atenção: " + detail + "; jogos que dependem de poucos núcleos podem ser limitados.";
                // Total pouco abaixo e por núcleo também abaixo: a diferença pode ser maior em jogos.
                if (NEAR.equals(status)) {
                    status = MANUAL;
                    verdict = " A diferença no total é pequena, mas o desempenho por núcleo também fica abaixo; "
                            + "revise manualmente.";
                }
            }
        }
        String shown = detected;
        if (gpu != null && f.parsedBytes() != null) {
            if (gpu.vramBytes() == null) {
                extra += " A memória de vídeo detectada não está disponível para comparar com a publicada.";
                if (COMPATIBLE.equals(status) || NEAR.equals(status)) status = MANUAL;
            } else {
                shown = detected + " — " + bytes(gpu.vramBytes());
                if (gpu.vramBytes() < f.parsedBytes()) {
                    extra += " A memória de vídeo detectada é menor que a publicada; revise manualmente (o Windows "
                            + "pode informar até 4 GiB para placas com mais memória).";
                    if (COMPATIBLE.equals(status) || NEAR.equals(status)) status = MANUAL;
                }
            }
        }
        BenchmarkDto benchmark = new BenchmarkDto(m.local().deviceName(), m.required().deviceName(),
                m.local().median(), m.required().median(), m.local().samples(), m.required().samples(),
                m.local().backend(), m.local().version(), ratio, m.confidence(), m.source());
        return new ComponentCompatibilityDto(component, f.rawText(), shown, status,
                plain(component, ratio, m.required().deviceName()) + " " + numbers + verdict + extra, benchmark);
    }

    /**
     * Frase direta de quem é melhor, antes dos números: "Seu processador é 62% mais rápido que o exigido (...)" ou
     * "Seu processador rende 94% do exigido (...): 6% a menos". Diferenças abaixo de 3% contam como iguais.
     */
    static String plain(String component, double ratio, String required) {
        String subject = "GPU".equals(component) ? "Sua placa de vídeo" : "Seu processador";
        Locale pt = Locale.forLanguageTag("pt-BR");
        if (Math.abs(ratio - 1) < 0.03) {
            return String.format(pt, "%s e o exigido (%s) são praticamente iguais em desempenho.", subject, required);
        }
        if (ratio > 1) {
            return String.format(pt, "%s é %.0f%% mais rápido que o exigido (%s).", subject, (ratio - 1) * 100,
                    required);
        }
        return String.format(pt, "%s rende %.0f%% do exigido (%s): %.0f%% a menos.", subject, ratio * 100, required,
                (1 - ratio) * 100);
    }

    /** Windows: compara a versão numérica (Windows 11 atende a "Windows 10") e a exigência de 64 bits. */
    private ComponentCompatibilityDto compareOs(RequirementFieldDto f, InventoryDto pc) {
        OsInfo os = data(pc.os());
        if (os == null || os.name() == null || os.name().isBlank()) {
            return row("Sistema operacional", f, NOT_AVAILABLE, UNKNOWN, "O sistema operacional local não foi identificado.");
        }
        String detected = String.join(" ", os.name(), Objects.requireNonNullElse(os.version(), "")).trim()
                + (os.architecture() == null ? "" : " (" + os.architecture() + ")");
        Matcher required = WINDOWS_VERSION.matcher(Objects.requireNonNullElse(f.parsedValue(), ""));
        Double localVersion = number(os.version());
        if (!required.find() || !os.name().toLowerCase(Locale.ROOT).contains("windows") || localVersion == null) {
            return row("Sistema operacional", f, detected, MANUAL,
                    "Não foi possível comparar a versão automaticamente; revise manualmente.");
        }
        double requiredVersion = Double.parseDouble(required.group(1));
        if (localVersion < requiredVersion) {
            return row("Sistema operacional", f, detected, BELOW, "A versão do Windows é anterior à publicada.");
        }
        boolean needs64 = REQUIRES_64_BIT.matcher(f.rawText()).find();
        if (needs64 && os.architecture() != null && !os.architecture().contains("64")) {
            return row("Sistema operacional", f, detected, BELOW, "O jogo exige sistema de 64 bits.");
        }
        return row("Sistema operacional", f, detected, COMPATIBLE,
                "A versão do Windows é igual ou mais recente que a publicada.");
    }

    /**
     * Resumo: "abaixo" se algum item comparado ficou abaixo; "atende" só se todos foram confirmados; caso
     * contrário diz o que foi confirmado e o que falta revisar.
     */
    static String overall(List<ComponentCompatibilityDto> results, String label) {
        if (results.isEmpty()) return "Análise incompleta";
        if (results.stream().anyMatch(r -> BELOW.equals(r.status()))) return "Abaixo dos " + label;
        Set<String> pending = new LinkedHashSet<>();
        Set<String> near = new LinkedHashSet<>();
        boolean anyConfirmed = false;
        for (ComponentCompatibilityDto r : results) {
            if (COMPATIBLE.equals(r.status())) anyConfirmed = true;
            else if (NEAR.equals(r.status())) near.add(r.component());
            else pending.add(r.component());
        }
        if (pending.isEmpty() && near.isEmpty()) return "Atende aos " + label;
        if (pending.isEmpty()) return "Praticamente atende aos " + label + " (próximo do exigido: "
                + String.join(", ", near) + ")";
        if (!anyConfirmed) return "Análise incompleta";
        return "Nenhum item abaixo dos " + label + "; revisar manualmente: " + String.join(", ", pending);
    }

    private static ComponentCompatibilityDto row(String component, RequirementFieldDto f, String detected,
                                                 String status, String explanation) {
        return new ComponentCompatibilityDto(component, f.rawText(), detected, status, explanation);
    }

    private static <T> T data(ComponentDto<T> component) {
        return component != null && "OK".equals(component.status()) ? component.data() : null;
    }

    private static Double number(String text) {
        if (text == null) return null;
        Matcher m = Pattern.compile("\\d+(?:\\.\\d+)?").matcher(text);
        return m.find() ? Double.valueOf(m.group()) : null;
    }

    private static String bytes(long value) {
        return String.format(Locale.forLanguageTag("pt-BR"), "%.1f GiB", value / 1073741824.0);
    }
}
