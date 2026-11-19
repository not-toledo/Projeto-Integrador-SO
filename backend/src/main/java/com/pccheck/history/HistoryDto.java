package com.pccheck.history;

import com.pccheck.agent.history.HistoryRecord;
import java.time.Instant;
import java.util.List;

/**
 * Resposta de {@code GET /api/history}.
 *
 * @param directory      pasta lida (configurável por NEXCORE_DATA_DIR ou nexcore.history.data-dir)
 * @param days           período consultado
 * @param count          quantidade de coletas no período
 * @param lastCollectedAt última coleta do período, ou null
 * @param scheduler      situação da tarefa no agendador do SO
 * @param records        coletas em ordem cronológica
 * @param problems       arquivos que não puderam ser lidos (as demais coletas continuam)
 */
public record HistoryDto(String directory, int days, int count, Instant lastCollectedAt, SchedulerStatus scheduler,
                         List<HistoryRecord> records, List<String> problems) {
}
