package com.pccheck.agent.collector;

import java.util.Locale;
import java.util.Set;

/**
 * Conversões e validações puras (sem dependência do OSHI), para tratar valores
 * ausentes, inválidos ou sentinelas da biblioteca (ex.: -1) como "não disponível".
 */
public final class Metrics {

    /** Textos genéricos que fabricantes de placas/BIOS deixam preenchidos; não identificam o componente. */
    private static final Set<String> OEM_PLACEHOLDERS = Set.of(
            "to be filled by o.e.m.", "default string", "not specified", "not applicable", "n/a", "none",
            "o.e.m.", "system serial number", "system product name", "system manufacturer",
            "system version", "base board serial number");

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

    /** Como cleanText, mas também descarta textos genéricos de fabricantes (ex.: "To be filled by O.E.M."). */
    public static String cleanOemText(String text) {
        String cleaned = cleanText(text);
        if (cleaned == null || OEM_PLACEHOLDERS.contains(cleaned.toLowerCase(Locale.ROOT))) {
            return null;
        }
        return cleaned;
    }

    /** Espaço usado = total - livre; null se algum valor for ausente ou incoerente. */
    public static Long usedBytes(Long total, Long free) {
        if (total == null || free == null || total <= 0 || free < 0 || free > total) {
            return null;
        }
        return total - free;
    }

    /**
     * Temperatura em °C. O OSHI devolve 0 (ou NaN) quando não há sensor; valores fora de uma faixa
     * física plausível (acima de 0 °C e até 150 °C) não são medições válidas.
     */
    public static Double validTemperature(double celsius) {
        if (Double.isNaN(celsius) || Double.isInfinite(celsius) || celsius <= 0.0 || celsius > 150.0) {
            return null;
        }
        return celsius;
    }

    /**
     * Taxa por segundo entre dois contadores acumulados. Retorna null se não houver valor anterior,
     * se o contador diminuir (reinício/estouro) ou se o intervalo for inválido.
     */
    public static Double ratePerSecond(long previous, long current, long elapsedMs) {
        if (previous < 0 || current < previous || elapsedMs <= 0) {
            return null;
        }
        return (current - previous) * 1000.0 / elapsedMs;
    }

    /**
     * Percentual de tempo ocupado a partir de um contador de milissegundos acumulados.
     * Limitado a 100 (leituras podem exceder levemente por arredondamento); null se inválido.
     */
    public static Double busyPercent(long previousMs, long currentMs, long elapsedMs) {
        if (previousMs < 0 || currentMs < previousMs || elapsedMs <= 0) {
            return null;
        }
        return Math.min(100.0, (currentMs - previousMs) * 100.0 / elapsedMs);
    }
}
