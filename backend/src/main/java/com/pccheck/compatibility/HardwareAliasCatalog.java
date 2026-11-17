package com.pccheck.compatibility;

import java.text.Normalizer;
import java.util.Map;

/**
 * Catálogo deliberadamente pequeno. Só equivalências explícitas e de mesmo modelo são aceitas;
 * modelos diferentes não são ordenados por frequência, geração ou benchmark presumido.
 */
final class HardwareAliasCatalog {
    private HardwareAliasCatalog() {}
    private static final Map<String, String> CPU = Map.of(
            "intel core i5 8400", "intel-core-i5-8400",
            "core i5 8400", "intel-core-i5-8400",
            "i5 8400", "intel-core-i5-8400",
            "amd ryzen 5 3600", "amd-ryzen-5-3600",
            "ryzen 5 3600", "amd-ryzen-5-3600");
    private static final Map<String, String> GPU = Map.of(
            "nvidia geforce gtx 1060", "nvidia-gtx-1060",
            "geforce gtx 1060", "nvidia-gtx-1060",
            "gtx 1060", "nvidia-gtx-1060",
            "nvidia geforce rtx 3060", "nvidia-rtx-3060",
            "geforce rtx 3060", "nvidia-rtx-3060",
            "rtx 3060", "nvidia-rtx-3060");

    static String canonical(String text, boolean gpu) {
        String normalized = normalizeForComparison(text);
        Map<String, String> aliases = gpu ? GPU : CPU;
        for (var entry : aliases.entrySet()) {
            if (normalized.contains(entry.getKey())) return entry.getValue();
        }
        return null;
    }
    static String normalizeForComparison(String value) {
        String plain = Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase();
        return plain.replaceAll("[^a-z0-9]+", " ").trim().replaceAll("\\s+", " ");
    }
}
