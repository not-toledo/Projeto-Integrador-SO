package com.pccheck.agent.model;

/**
 * Memória RAM. Campo nulo = não disponível.
 *
 * @param virtualMemory swap e memória comprometida (null se o SO não informou)
 * @param paging        páginas lidas/gravadas por segundo; só no Live Monitor, que compara duas amostras
 */
public record MemoryInfo(Long totalBytes, Long availableBytes, Long usedBytes, Double usedPercent,
                         VirtualMemoryInfo virtualMemory, PagingActivity paging) {

    /** Somente a RAM física. */
    public MemoryInfo(Long totalBytes, Long availableBytes, Long usedBytes, Double usedPercent) {
        this(totalBytes, availableBytes, usedBytes, usedPercent, null, null);
    }

    /** Cópia com a atividade de paginação calculada pelo Live Monitor. */
    public MemoryInfo withPaging(PagingActivity activity) {
        return new MemoryInfo(totalBytes, availableBytes, usedBytes, usedPercent, virtualMemory, activity);
    }
}
