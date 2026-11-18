package com.pccheck.precheck;

import java.time.Instant;
import java.util.List;

/**
 * Pré-checagem antes de jogar: o estado do PC agora (memória livre, processos, disco do jogo, energia), diferente da
 * compatibilidade, que compara o hardware instalado com os requisitos.
 *
 * @param overall      {@code READY} (nenhum ponto de atenção agora) ou {@code ATTENTION}
 * @param items        verificações, cada uma com estado próprio
 * @param topCpu       aplicativos que mais usam CPU agora (processos agrupados pelo nome)
 * @param topMemory    aplicativos que mais usam memória agora
 * @param processError motivo, se os processos não puderam ser lidos
 */
public record PrecheckDto(long appId, String gameName, String overall, String overallLabel, List<Item> items,
                          List<ProcessGroup> topCpu, List<ProcessGroup> topMemory, String processError,
                          Instant checkedAt) {

    /**
     * @param status {@code OK}, {@code ATTENTION}, {@code INFO} (só informa) ou {@code UNAVAILABLE} (não verificado)
     */
    public record Item(String id, String label, String status, String observed, String detail) {
    }

    /**
     * Processos com o mesmo nome somados (ex.: as várias abas de um navegador).
     *
     * @param cpuPercent  % do total da CPU (todos os núcleos), medido em cerca de 1 s
     * @param memoryBytes soma do conjunto de trabalho (RSS); páginas compartilhadas podem ser contadas mais de uma vez
     */
    public record ProcessGroup(String name, int processCount, double cpuPercent, long memoryBytes) {
    }
}
