package com.pccheck.compatibility;

import com.pccheck.agent.model.CpuInfo;
import com.pccheck.agent.model.GpuInfo;
import com.pccheck.agent.model.MemoryInfo;
import com.pccheck.agent.model.OsInfo;
import com.pccheck.agent.model.StorageInfo;
import com.pccheck.agent.model.VolumeInfo;
import com.pccheck.dto.ComponentCompatibilityDto;
import com.pccheck.dto.GameCompatibilityDto;
import com.pccheck.dto.GameRequirementsDto;
import com.pccheck.dto.RequirementFieldDto;
import com.pccheck.dto.InventoryDto;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/** Avaliação conservadora: só declara compatível quando há comparação objetiva. */
@Component
public class GameCompatibilityEvaluator {
    public GameCompatibilityDto evaluate(GameRequirementsDto game, InventoryDto pc) {
        List<ComponentCompatibilityDto> min = compare(game.minimum(), pc);
        List<ComponentCompatibilityDto> rec = compare(game.recommended(), pc);
        List<String> warnings = new ArrayList<>(game.warnings());
        warnings.add("CPU e GPU em texto livre exigem revisão manual quando não há equivalência segura.");
        warnings.add("A avaliação não estima FPS, estabilidade ou desempenho real.");
        return new GameCompatibilityDto(game.appId(), game.name(),
                overall(min, "mínimos"), overall(rec, "recomendados"), min, rec, game.minimumRawText(),
                game.recommendedRawText(), Instant.now(), game.source(), List.copyOf(warnings));
    }

    private List<ComponentCompatibilityDto> compare(List<RequirementFieldDto> fields, InventoryDto pc) {
        List<ComponentCompatibilityDto> result = new ArrayList<>();
        for (RequirementFieldDto field : fields) {
            switch (field.component()) {
                case "RAM" -> result.add(compareRam(field, pc));
                case "STORAGE" -> result.add(compareStorage(field, pc));
                case "CPU" -> result.add(compareModel("CPU", field.rawText(), cpu(pc), false));
                case "GPU" -> result.add(compareGpu(field, pc));
                case "OS" -> result.add(compareOs(field, pc));
                default -> result.add(new ComponentCompatibilityDto(field.component(), field.rawText(),
                        "Não disponível", "UNKNOWN", "O requisito não corresponde a um campo comparável."));
            }
        }
        return List.copyOf(result);
    }

    private ComponentCompatibilityDto compareRam(RequirementFieldDto f, InventoryDto pc) {
        Long detected = pc.memory() != null && "OK".equals(pc.memory().status()) && pc.memory().data() != null
                ? pc.memory().data().totalBytes() : null;
        if (f.parsedBytes() == null || detected == null)
            return new ComponentCompatibilityDto("RAM", f.rawText(), detected == null ? "Não disponível" : bytes(detected),
                    "UNKNOWN", "Não foi possível comparar numericamente o requisito e a memória detectada.");
        boolean ok = detected >= f.parsedBytes();
        return new ComponentCompatibilityDto("RAM", f.rawText(), bytes(detected), ok ? "COMPATIBLE" : "BELOW_MINIMUM",
                ok ? "A memória física detectada atende ao valor publicado." : "A memória física detectada está abaixo do requisito.");
    }

    private ComponentCompatibilityDto compareStorage(RequirementFieldDto f, InventoryDto pc) {
        Long largest = null;
        if (pc.storage() != null && "OK".equals(pc.storage().status()) && pc.storage().data() != null) {
            List<VolumeInfo> volumes = pc.storage().data().volumes();
            if (volumes != null) for (VolumeInfo v : volumes)
                if (v.freeBytes() != null && (largest == null || v.freeBytes() > largest)) largest = v.freeBytes();
        }
        if (f.parsedBytes() == null || largest == null)
            return new ComponentCompatibilityDto("Armazenamento", f.rawText(), largest == null ? "Não disponível" : bytes(largest),
                    "UNKNOWN", "Não foi possível comparar o espaço livre com o requisito.");
        boolean ok = largest >= f.parsedBytes();
        return new ComponentCompatibilityDto("Armazenamento", f.rawText(), bytes(largest), ok ? "COMPATIBLE" : "BELOW_MINIMUM",
                ok ? "O maior volume elegível possui espaço livre suficiente." : "Nenhum volume individual possui espaço livre suficiente.");
    }

    private String cpu(InventoryDto pc) {
        CpuInfo d = pc.cpu() != null && "OK".equals(pc.cpu().status()) ? pc.cpu().data() : null;
        return d == null || d.name() == null ? "Não disponível" : d.name();
    }
    private String gpu(InventoryDto pc) {
        if (pc.gpu() == null || !"OK".equals(pc.gpu().status()) || pc.gpu().data() == null) return "Não disponível";
        return pc.gpu().data().stream().map(GpuInfo::name).filter(n -> n != null).reduce((a,b) -> a + "; " + b).orElse("Não disponível");
    }
    private String os(InventoryDto pc) {
        OsInfo d = pc.os() != null && "OK".equals(pc.os().status()) ? pc.os().data() : null;
        return d == null || d.name() == null || d.name().isBlank()
                ? "Não disponível" : String.join(" ", d.name(), safe(d.version()));
    }
    private ComponentCompatibilityDto compareGpu(RequirementFieldDto field, InventoryDto pc) {
        if (pc.gpu() == null || !"OK".equals(pc.gpu().status()) || pc.gpu().data() == null
                || pc.gpu().data().isEmpty())
            return new ComponentCompatibilityDto("GPU", field.rawText(), "Não disponível", "UNKNOWN",
                    "O inventário não identificou uma GPU comparável.");
        String requiredModel = HardwareAliasCatalog.canonical(field.rawText(), true);
        if (requiredModel == null)
            return new ComponentCompatibilityDto("GPU", field.rawText(), gpu(pc), "MANUAL_REVIEW",
                    "O modelo publicado não possui equivalência segura no catálogo atual.");
        for (GpuInfo detected : pc.gpu().data()) {
            if (!requiredModel.equals(HardwareAliasCatalog.canonical(detected.name(), true))) continue;
            if (field.parsedBytes() != null) {
                if (detected.vramBytes() == null)
                    return new ComponentCompatibilityDto("GPU", field.rawText(), detected.name(),
                            "UNKNOWN", "O modelo corresponde, mas a VRAM detectada não está disponível para comparação.");
                boolean enough = detected.vramBytes() >= field.parsedBytes();
                return new ComponentCompatibilityDto("GPU", field.rawText(),
                        detected.name() + " — " + bytes(detected.vramBytes()),
                        enough ? "COMPATIBLE" : "BELOW_MINIMUM",
                        enough ? "Modelo e VRAM detectados atendem ao requisito publicado."
                                : "A VRAM detectada está abaixo do valor publicado.");
            }
            return new ComponentCompatibilityDto("GPU", field.rawText(), detected.name(), "COMPATIBLE",
                    "O modelo detectado corresponde ao mesmo modelo identificado no catálogo explícito de aliases.");
        }
        return new ComponentCompatibilityDto("GPU", field.rawText(), gpu(pc), "MANUAL_REVIEW",
                "Não existe correspondência segura entre a GPU publicada e as GPUs detectadas.");
    }

    private ComponentCompatibilityDto compareOs(RequirementFieldDto field, InventoryDto pc) {
        String detected = os(pc);
        if ("Não disponível".equals(detected))
            return new ComponentCompatibilityDto("Sistema operacional", field.rawText(), detected, "UNKNOWN",
                    "O sistema operacional local não foi identificado.");
        String required = HardwareAliasCatalog.normalizeForComparison(field.parsedValue());
        String actual = HardwareAliasCatalog.normalizeForComparison(detected);
        if (required != null && !required.isBlank() && actual.contains(required))
            return new ComponentCompatibilityDto("Sistema operacional", field.rawText(), detected, "COMPATIBLE",
                    "A plataforma e a versão identificadas correspondem ao requisito publicado.");
        return new ComponentCompatibilityDto("Sistema operacional", field.rawText(), detected, "MANUAL_REVIEW",
                "A versão detectada não corresponde exatamente ao requisito extraído; revise a compatibilidade manualmente.");
    }

    private ComponentCompatibilityDto compareModel(String component, String requirement, String detected, boolean gpu) {
        String requiredModel = HardwareAliasCatalog.canonical(requirement, gpu);
        String detectedModel = HardwareAliasCatalog.canonical(detected, gpu);
        if (requiredModel != null && requiredModel.equals(detectedModel))
            return new ComponentCompatibilityDto(component, requirement, detected, "COMPATIBLE",
                    "O modelo detectado corresponde ao mesmo modelo identificado no catálogo explícito de aliases.");
        return new ComponentCompatibilityDto(component, requirement, detected, "MANUAL_REVIEW",
                "Não existe equivalência segura entre os modelos no catálogo atual; revise manualmente.");
    }

    private String overall(List<ComponentCompatibilityDto> results, String label) {
        if (results.isEmpty()) return "Análise incompleta";
        if (results.stream().anyMatch(r -> "BELOW_MINIMUM".equals(r.status()))) return "Abaixo dos " + label;
        if (results.stream().allMatch(r -> "COMPATIBLE".equals(r.status()))) return "Atende aos " + label;
        return "Análise incompleta";
    }
    private String bytes(long value) { return String.format(java.util.Locale.forLanguageTag("pt-BR"), "%.1f GiB", value / 1073741824.0); }
    private String safe(String s) { return s == null ? "" : s; }
}
