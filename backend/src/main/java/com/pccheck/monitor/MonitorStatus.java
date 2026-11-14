package com.pccheck.monitor;

import java.time.Instant;

/**
 * Estado da coleta periódica.
 *
 * @param running             true se o agendador está ativo
 * @param collecting          true enquanto uma coleta está em andamento
 * @param stalled             true se a coleta em andamento está demorando muito mais que o esperado (possível bloqueio)
 * @param intervalMs          intervalo entre o fim de uma coleta e o início da próxima
 * @param historyCapacity     máximo de amostras mantidas em memória
 * @param sampleCount         amostras atualmente no histórico
 * @param lastSuccessAt       horário da última amostra bem-sucedida (nulo se nenhuma)
 * @param consecutiveFailures falhas seguidas desde a última amostra bem-sucedida
 * @param lastError           mensagem da última falha (nulo se a última coleta funcionou)
 */
public record MonitorStatus(boolean running, boolean collecting, boolean stalled, long intervalMs,
                            int historyCapacity, int sampleCount, Instant lastSuccessAt,
                            int consecutiveFailures, String lastError) {
}
