package com.pccheck.agent.model;

/**
 * Memória virtual. Campo nulo = não disponível.
 *
 * <p>Swap: arquivo de paginação (Windows) ou área de swap (Linux). Memória comprometida ("commit"): total que os
 * programas reservaram e que o SO garantiu poder fornecer, somando RAM e swap. No Windows, ao atingir o limite,
 * novas alocações falham. No Linux, o kernel permite ultrapassar o limite por padrão (overcommit), então o
 * percentual pode passar de 100% sem indicar falha.
 *
 * @param swapTotalBytes   tamanho do arquivo de paginação/swap
 * @param swapUsedBytes    parte em uso
 * @param swapUsedPercent  uso do swap (0 a 100)
 * @param commitLimitBytes limite de memória comprometida (Windows: Commit Limit; Linux: CommitLimit)
 * @param commitUsedBytes  memória comprometida (Windows: Committed Bytes; Linux: Committed_AS)
 * @param commitPercent    commitUsed / commitLimit, em % (pode passar de 100 no Linux)
 */
public record VirtualMemoryInfo(Long swapTotalBytes, Long swapUsedBytes, Double swapUsedPercent,
                                Long commitLimitBytes, Long commitUsedBytes, Double commitPercent) {
}
