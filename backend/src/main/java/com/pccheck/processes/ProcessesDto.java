package com.pccheck.processes;

import java.time.Instant;
import java.util.List;

/**
 * Processos em execução (GET /api/processes), lidos pelo OSHI em duas leituras com {@code sampleMillis} de intervalo.
 * Somente leitura: o NEXCORE não encerra nem altera processos.
 *
 * @param processCount quantidade informada pelo SO (null se não informou)
 * @param threadCount  threads somadas de todos os processos, informada pelo SO (null se não informou)
 * @param error        motivo quando a leitura falhou; a lista vem vazia
 * @param osFamily     "windows" ou "linux": a escala de prioridade e o significado do estado dependem do SO
 */
public record ProcessesDto(Instant collectedAt, long sampleMillis, int logicalProcessors, Long totalMemoryBytes,
                           Integer processCount, Integer threadCount, List<ProcessInfo> processes, String error,
                           String osFamily) {

    public ProcessesDto {
        processes = processes == null ? List.of() : List.copyOf(processes);
    }

    /**
     * Um processo.
     *
     * @param cpuPercent  uso no intervalo, em % da CPU inteira (0 a 100); null para processos que surgiram no intervalo
     * @param memoryBytes memória física em uso (RSS / conjunto de trabalho)
     * @param state       estado informado pelo SO (RUNNING, SLEEPING, WAITING, ZOMBIE, STOPPED, SUSPENDED, OTHER...)
     * @param priority    prioridade como o SO informa (Windows: prioridade base, 8 = normal; Linux: valor do kernel)
     * @param user        dono do processo; null quando o SO não informa (comum sem permissão de administrador)
     * @param startTime   início do processo; null quando o SO não informa
     * @param self        o próprio backend do NEXCORE
     */
    public record ProcessInfo(int pid, int parentPid, String name, String user, String state, int priority,
                              int threads, Double cpuPercent, long memoryBytes, long virtualBytes, Instant startTime,
                              boolean self) {
    }
}
