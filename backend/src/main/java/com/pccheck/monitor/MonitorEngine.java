package com.pccheck.monitor;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * Executa coletas e guarda o histórico limitado. Não conhece threads nem Spring: o agendador chama
 * {@link #tick()}.
 *
 * <ul>
 *   <li>Nunca executa duas coletas ao mesmo tempo: um {@code tick()} chamado enquanto outro está em andamento
 *       é ignorado (sem acúmulo de tarefas).</li>
 *   <li>Qualquer exceção da coleta é registrada e não interrompe as próximas tentativas.</li>
 * </ul>
 */
public class MonitorEngine<T> {

    private final Supplier<T> sampler;
    private final SampleHistory<T> history;
    private final Clock clock;
    private final Duration stallThreshold;
    private final AtomicBoolean collecting = new AtomicBoolean(false);

    private volatile Instant collectingSince;
    private volatile Instant lastSuccessAt;
    private volatile int consecutiveFailures;
    private volatile String lastError;

    public MonitorEngine(Supplier<T> sampler, int historyCapacity, Duration stallThreshold, Clock clock) {
        this.sampler = sampler;
        this.history = new SampleHistory<>(historyCapacity);
        this.stallThreshold = stallThreshold;
        this.clock = clock;
    }

    /**
     * Tenta uma coleta.
     *
     * @return true se uma coleta foi executada (com ou sem sucesso); false se foi ignorada por haver outra em andamento
     */
    public boolean tick() {
        if (!collecting.compareAndSet(false, true)) {
            return false;
        }
        collectingSince = clock.instant();
        try {
            T sample = sampler.get();
            if (sample == null) {
                recordFailure("A coleta não retornou dados.");
            } else {
                history.add(sample);
                lastSuccessAt = clock.instant();
                consecutiveFailures = 0;
                lastError = null;
            }
        } catch (Exception | LinkageError e) {
            // LinkageError cobre falhas de bibliotecas nativas (ex.: JNA) sem derrubar o agendador.
            recordFailure(e.getClass().getSimpleName() + ": " + e.getMessage());
        } finally {
            collectingSince = null;
            collecting.set(false);
        }
        return true;
    }

    private void recordFailure(String message) {
        consecutiveFailures++;
        lastError = message;
    }

    public T latest() {
        return history.latest();
    }

    public List<T> history(int limit) {
        return history.last(limit);
    }

    public MonitorStatus status(boolean running, long intervalMs) {
        Instant since = collectingSince;
        boolean isCollecting = collecting.get();
        boolean stalled = isCollecting && since != null
                && Duration.between(since, clock.instant()).compareTo(stallThreshold) > 0;
        return new MonitorStatus(running, isCollecting, stalled, intervalMs, history.capacity(), history.size(),
                lastSuccessAt, consecutiveFailures, lastError);
    }
}
