package com.pccheck.compatibility;

import java.text.Normalizer;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Identifica modelos exatos de CPU e GPU em texto livre ("Intel Core i5-8400 / AMD Ryzen 5 3600",
 * "NVIDIA GeForce GTX 1050 Ti"). Serve só para reconhecer o MESMO modelo: modelos diferentes não são
 * ordenados por desempenho (não há medição confiável para isso), então ficam para revisão manual.
 *
 * <p>Sufixos fazem parte do modelo: i5-8400 ≠ i5-8400T, RTX 3060 ≠ RTX 3060 Ti, RX 6700 ≠ RX 6700 XT.
 */
final class HardwareModels {

    private record Rule(Pattern pattern, Function<MatchResult, String> key) {
    }

    private static final List<Rule> CPU = List.of(
            rule("\\bi([3579]) (\\d{3,5})([a-z]{0,3})\\b", m -> "intel-core-i" + m.group(1) + "-" + m.group(2) + m.group(3)),
            rule("\\bcore ultra ([579]) (\\d{3})([a-z]{0,2})\\b", m -> "intel-core-ultra-" + m.group(1) + "-" + m.group(2) + m.group(3)),
            rule("\\bryzen ([3579]) (\\d{4})([a-z0-9]{0,3})\\b", m -> "amd-ryzen-" + m.group(1) + "-" + m.group(2) + m.group(3)),
            rule("\\bfx ?(\\d{4})\\b", m -> "amd-fx-" + m.group(1)));

    private static final List<Rule> GPU = List.of(
            rule("\\b(gtx|rtx|gt) ?(\\d{3,4})(?: (ti|super))?\\b",
                    m -> "nvidia-" + m.group(1) + "-" + m.group(2) + (m.group(3) == null ? "" : "-" + m.group(3))),
            rule("\\brx ?(\\d{3,4}) ?(xtx|xt)?\\b",
                    m -> "amd-rx-" + m.group(1) + (m.group(2) == null ? "" : "-" + m.group(2))),
            rule("\\b(r[579]) (\\d{3})(x?)\\b", m -> "amd-" + m.group(1) + "-" + m.group(2) + m.group(3)),
            rule("\\bradeon hd (\\d{4})\\b", m -> "amd-hd-" + m.group(1)),
            rule("\\b(uhd|hd) graphics (\\d{3,4})\\b", m -> "intel-" + m.group(1) + "-" + m.group(2)),
            rule("\\barc (a\\d{3})\\b", m -> "intel-arc-" + m.group(1)));

    private HardwareModels() {
    }

    private static Rule rule(String regex, Function<MatchResult, String> key) {
        return new Rule(Pattern.compile(regex), key);
    }

    /** Modelos de CPU citados no texto, na ordem em que aparecem (vazio se nenhum modelo exato). */
    static Set<String> cpuModels(String text) {
        return find(text, CPU);
    }

    /** Modelos de GPU citados no texto, na ordem em que aparecem (vazio se nenhum modelo exato). */
    static Set<String> gpuModels(String text) {
        return find(text, GPU);
    }

    private static Set<String> find(String text, List<Rule> rules) {
        String normalized = normalize(text);
        Set<String> models = new LinkedHashSet<>();
        for (Rule rule : rules) {
            Matcher m = rule.pattern().matcher(normalized);
            while (m.find()) {
                models.add(rule.key().apply(m.toMatchResult()));
            }
        }
        return models;
    }

    /** Minúsculas, sem acentos e com toda pontuação (inclusive "-", "(R)", "™") trocada por espaço. */
    static String normalize(String value) {
        String plain = Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT)
                .replace("(r)", " ").replace("(tm)", " ");
        return plain.replaceAll("[^a-z0-9]+", " ").trim().replaceAll("\\s+", " ");
    }
}
