package com.pccheck.agent.model;

/**
 * Atividade de paginação entre duas amostras do Live Monitor. Campo nulo = não disponível (por exemplo, na
 * primeira amostra, que ainda não tem valor anterior).
 *
 * <p>Windows: "Pages Input/sec" e "Pages Output/sec"; incluem também leitura de programas e arquivos mapeados
 * em memória, não só o arquivo de paginação. Linux: pswpin/pswpout de /proc/vmstat (somente swap).
 *
 * @param pagesInPerSecond  páginas lidas do disco por segundo para atender falhas de página graves
 * @param pagesOutPerSecond páginas gravadas no disco por segundo para liberar RAM
 * @param pageSizeBytes     tamanho da página de memória
 */
public record PagingActivity(Double pagesInPerSecond, Double pagesOutPerSecond, Long pageSizeBytes) {

    /** Páginas gravadas por segundo convertidas em bytes por segundo, ou null. */
    public Double pagesOutBytesPerSecond() {
        return pagesOutPerSecond == null || pageSizeBytes == null ? null : pagesOutPerSecond * pageSizeBytes;
    }
}
