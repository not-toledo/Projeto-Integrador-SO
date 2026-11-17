package com.pccheck.compatibility;

import com.pccheck.dto.ComponentCompatibilityDto;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Classificação geral de um jogo a partir dos resultados por componente (mínimos e recomendados).
 *
 * <ul>
 *   <li>{@code NOT_RECOMMENDED}: algum item ficou comprovadamente abaixo do mínimo;</li>
 *   <li>{@code COMPATIBLE}: todos os itens dos mínimos e dos recomendados foram confirmados (ou a Steam não
 *       publicou recomendados);</li>
 *   <li>{@code LIMITED}: os mínimos foram confirmados, mas os recomendados não (abaixo ou não verificáveis);</li>
 *   <li>{@code INSUFFICIENT_DATA}: nenhum item abaixo do mínimo, mas algum item dos mínimos não pôde ser
 *       confirmado. Falta de dado nunca é tratada como incompatibilidade nem como compatibilidade.</li>
 * </ul>
 *
 * @param code             código estável para a interface
 * @param label            rótulo em português
 * @param reasons          motivos que justificam a classificação
 * @param belowMinimum     componentes comprovadamente abaixo do mínimo
 * @param belowRecommended componentes comprovadamente abaixo do recomendado
 * @param pendingReview    componentes que não puderam ser confirmados (revisão manual ou dado ausente)
 */
public record CompatibilityVerdict(String code, String label, List<String> reasons, List<String> belowMinimum,
                                   List<String> belowRecommended, List<String> pendingReview) {

    public static final String COMPATIBLE = "COMPATIBLE";
    public static final String LIMITED = "LIMITED";
    public static final String NOT_RECOMMENDED = "NOT_RECOMMENDED";
    public static final String INSUFFICIENT_DATA = "INSUFFICIENT_DATA";

    public CompatibilityVerdict {
        reasons = List.copyOf(reasons);
        belowMinimum = List.copyOf(belowMinimum);
        belowRecommended = List.copyOf(belowRecommended);
        pendingReview = List.copyOf(pendingReview);
    }

    static CompatibilityVerdict of(List<ComponentCompatibilityDto> minimum, List<ComponentCompatibilityDto> recommended) {
        List<String> belowMin = components(minimum, GameCompatibilityEvaluator.BELOW);
        List<String> belowRec = components(recommended, GameCompatibilityEvaluator.BELOW);
        List<String> pendingMin = pending(minimum);
        List<String> pendingRec = pending(recommended);
        Set<String> pendingAll = new LinkedHashSet<>(pendingMin);
        pendingAll.addAll(pendingRec);
        List<String> pendingList = List.copyOf(pendingAll);

        List<String> reasons = new ArrayList<>();
        if (minimum.isEmpty()) {
            reasons.add("A Steam não publicou requisitos mínimos comparáveis para este jogo.");
            return new CompatibilityVerdict(INSUFFICIENT_DATA, "Dados insuficientes", reasons, belowMin, belowRec,
                    pendingList);
        }
        if (!belowMin.isEmpty()) {
            for (ComponentCompatibilityDto r : minimum) {
                if (GameCompatibilityEvaluator.BELOW.equals(r.status())) {
                    reasons.add("Não atende ao mínimo de " + r.component() + ": " + r.explanation());
                }
            }
            if (!pendingMin.isEmpty()) {
                reasons.add("Também não foi possível confirmar: " + String.join(", ", pendingMin) + ".");
            }
            return new CompatibilityVerdict(NOT_RECOMMENDED, "Não recomendado", reasons, belowMin, belowRec,
                    pendingList);
        }
        if (!pendingMin.isEmpty()) {
            reasons.add("Nenhum item ficou abaixo do mínimo, mas não foi possível confirmar: "
                    + String.join(", ", pendingMin) + ".");
            if (!belowRec.isEmpty()) {
                reasons.add("Abaixo do recomendado: " + String.join(", ", belowRec) + ".");
            }
            return new CompatibilityVerdict(INSUFFICIENT_DATA, "Dados insuficientes", reasons, belowMin, belowRec,
                    pendingList);
        }
        reasons.add("Todos os itens comparáveis dos requisitos mínimos foram atendidos.");
        if (recommended.isEmpty()) {
            reasons.add("A Steam não publicou requisitos recomendados comparáveis.");
            return new CompatibilityVerdict(COMPATIBLE, "Compatível", reasons, belowMin, belowRec, pendingList);
        }
        if (!belowRec.isEmpty()) {
            for (ComponentCompatibilityDto r : recommended) {
                if (GameCompatibilityEvaluator.BELOW.equals(r.status())) {
                    reasons.add("Abaixo do recomendado em " + r.component() + ": " + r.explanation());
                }
            }
            return new CompatibilityVerdict(LIMITED, "Compatibilidade limitada", reasons, belowMin, belowRec,
                    pendingList);
        }
        if (!pendingRec.isEmpty()) {
            reasons.add("Não foi possível confirmar os recomendados de: " + String.join(", ", pendingRec) + ".");
            return new CompatibilityVerdict(LIMITED, "Compatibilidade limitada", reasons, belowMin, belowRec,
                    pendingList);
        }
        reasons.add("Todos os itens comparáveis dos requisitos recomendados foram atendidos.");
        return new CompatibilityVerdict(COMPATIBLE, "Compatível", reasons, belowMin, belowRec, pendingList);
    }

    private static List<String> components(List<ComponentCompatibilityDto> results, String status) {
        return results.stream().filter(r -> status.equals(r.status()))
                .map(ComponentCompatibilityDto::component).distinct().toList();
    }

    private static List<String> pending(List<ComponentCompatibilityDto> results) {
        return results.stream()
                .filter(r -> !GameCompatibilityEvaluator.COMPATIBLE.equals(r.status())
                        && !GameCompatibilityEvaluator.BELOW.equals(r.status()))
                .map(ComponentCompatibilityDto::component).distinct().toList();
    }
}
