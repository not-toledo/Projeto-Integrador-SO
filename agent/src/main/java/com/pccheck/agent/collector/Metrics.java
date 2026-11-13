package com.pccheck.agent.collector;

/**
 * Conversões e validações puras (sem dependência do OSHI), para tratar valores
 * ausentes, inválidos ou sentinelas da biblioteca (ex.: -1) como "não disponível".
 */
public final class Metrics {

    private Metrics() {
    }

    /** Percentual de part sobre total (0 a 100), ou null se os valores forem inválidos. */
    public static Double percent(Long part, Long total) {
        if (part == null || total == null || total <= 0 || part < 0 || part > total) {
            return null;
        }
        return part * 100.0 / total;
    }

    /** Valores menores ou iguais a zero (ex.: -1 do OSHI) não são medições válidas. */
    public static Long positiveOrNull(long value) {
        return value > 0 ? value : null;
    }

    public static Integer positiveOrNull(int value) {
        return value > 0 ? value : null;
    }

    /** Texto vazio ou "unknown" vira null. */
    public static String cleanText(String text) {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        if (trimmed.isEmpty() || trimmed.equalsIgnoreCase("unknown")) {
            return null;
        }
        return trimmed;
    }

    /** Converte carga 0.0 a 1.0 em percentual; NaN, infinito ou fora da faixa viram null. */
    public static Double cpuLoadToPercent(double load) {
        if (Double.isNaN(load) || Double.isInfinite(load) || load < 0.0 || load > 1.0) {
            return null;
        }
        return load * 100.0;
    }
}
