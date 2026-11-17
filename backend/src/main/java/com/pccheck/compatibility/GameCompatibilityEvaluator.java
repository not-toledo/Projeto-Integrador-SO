package com.pccheck.compatibility;

import com.pccheck.agent.model.CpuInfo;
import com.pccheck.agent.model.GpuInfo;
import com.pccheck.agent.model.OsInfo;
import com.pccheck.agent.model.VolumeInfo;
import com.pccheck.dto.ComponentCompatibilityDto;
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
import org.springframework.stereotype.Component;

/**
 * Avaliação conservadora: só declara "atende" quando há comparação objetiva (número com número ou o mesmo
 * modelo de CPU/GPU). Modelos diferentes não são ordenados por desempenho; ficam para revisão manual.
 */
@Component
public class GameCompatibilityEvaluator {

    static final String COMPATIBLE = "COMPATIBLE";
    static final String BELOW = "BELOW_MINIMUM";
    static final String MANUAL = "MANUAL_REVIEW";
    static final String UNKNOWN = "UNKNOWN";
    private static final String NOT_AVAILABLE = "Não disponível";
    /**
     * A memória visível ao Windows é um pouco menor que a instalada (parte fica reservada para o sistema ou para
     * a GPU integrada). Entre 90% e 100% do requisito o resultado é revisão manual, e não "abaixo".
     */
    static final double RAM_RESERVED_MARGIN = 0.90;
    private static final Pattern WINDOWS_VERSION = Pattern.compile("(?i)windows\\s*(\\d+(?:\\.\\d+)?)");
    private static final Pattern REQUIRES_64_BIT = Pattern.compile("(?i)64[- ]?bits?|\\bx64\\b");

    public GameCompatibilityDto evaluate(GameRequirementsDto game, InventoryDto pc) {
        List<ComponentCompatibilityDto> min = compare(game.minimum(), pc);
        List<ComponentCompatibilityDto> rec = compare(game.recommended(), pc);
        List<String> warnings = new ArrayList<>(game.warnings());
        // O aviso sobre FPS fica no rodapé fixo da interface; aqui só entram limitações que afetaram este jogo.
        if (hasCpuOrGpuReview(min) || hasCpuOrGpuReview(rec)) {
            warnings.add("CPU e GPU só são aprovadas quando o modelo detectado é o mesmo publicado; modelos "
                    + "diferentes ficam para revisão manual (o PCCheck não estima desempenho entre modelos).");
        }
        return new GameCompatibilityDto(game.appId(), game.name(),
                overall(min, "mínimos"), overall(rec, "recomendados"), min, rec, game.minimumRawText(),
                game.recommendedRawText(), Instant.now(), game.source(), List.copyOf(warnings),
                CompatibilityVerdict.of(min, rec));
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
        return compareModels("CPU", f, detected, HardwareModels.cpuModels(f.rawText()),
                HardwareModels.cpuModels(detected));
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
        return row(component, f, detected, MANUAL, "Modelos diferentes. O PCCheck não compara desempenho entre "
                + "modelos diferentes; confira em uma comparação de desempenho (benchmark).");
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
        boolean anyConfirmed = false;
        for (ComponentCompatibilityDto r : results) {
            if (COMPATIBLE.equals(r.status())) anyConfirmed = true;
            else pending.add(r.component());
        }
        if (pending.isEmpty()) return "Atende aos " + label;
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
