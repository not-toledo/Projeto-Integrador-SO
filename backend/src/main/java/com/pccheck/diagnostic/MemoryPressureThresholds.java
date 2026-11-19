package com.pccheck.diagnostic;

/**
 * Limites da análise de pressão de memória, configuráveis em application.properties
 * ({@code nexcore.health.memory-pressure.*}).
 *
 * <p>Valores padrão definidos pelo projeto (heurísticas conservadoras, não limites oficiais do Windows ou do
 * Linux): aviso com memória comprometida a partir de 90% do limite, e paginação considerada sustentada quando a
 * média de gravação em disco no último minuto passa de 1 MiB/s com a RAM acima do limite de aviso.
 *
 * @param commitWarningPercent       memória comprometida (% do limite) a partir da qual há aviso (só Windows)
 * @param pageOutWarningBytesPerSec  média de páginas gravadas (bytes/s) que caracteriza paginação sustentada
 * @param windowSamples              amostras do Live Monitor usadas na média (padrão 30 = cerca de 1 minuto)
 * @param minSamples                 mínimo de amostras com dado para avaliar a paginação
 */
public record MemoryPressureThresholds(double commitWarningPercent, double pageOutWarningBytesPerSec,
                                       int windowSamples, int minSamples) {

    public MemoryPressureThresholds {
        if (!(commitWarningPercent > 0 && commitWarningPercent <= 100)) {
            throw new IllegalArgumentException("Memória comprometida: o limite de aviso deve estar entre 0 e 100.");
        }
        if (!(pageOutWarningBytesPerSec > 0)) {
            throw new IllegalArgumentException("Paginação: o limite deve ser positivo.");
        }
        if (!(minSamples > 0 && windowSamples >= minSamples)) {
            throw new IllegalArgumentException("Paginação: é preciso 0 < amostras mínimas <= janela.");
        }
    }

    public static MemoryPressureThresholds defaults() {
        return new MemoryPressureThresholds(90.0, 1_048_576.0, 30, 10);
    }
}
