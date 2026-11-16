package com.pccheck.service;

import com.pccheck.agent.collector.LiveSampler;
import com.pccheck.agent.lhm.LhmSource;
import com.pccheck.agent.model.LiveSample;
import com.pccheck.dto.LiveDto;
import com.pccheck.dto.LiveHistoryDto;
import com.pccheck.dto.LiveSampleDto;
import com.pccheck.monitor.MonitorEngine;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import oshi.SystemInfo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Coleta periódica em segundo plano (Módulo C). Uma única thread executa uma coleta por vez com
 * {@code scheduleWithFixedDelay}: a próxima só começa depois que a anterior termina, então não há
 * acúmulo de tarefas mesmo se uma coleta demorar. As requisições HTTP apenas leem o histórico em memória.
 *
 * O OSHI é inicializado na primeira coleta; se falhar, o erro aparece no estado e a próxima coleta tenta de novo.
 */
@Service
public class LiveMonitorService {

    static final long MIN_INTERVAL_MS = 500;
    static final int MIN_HISTORY = 10;
    static final int MAX_HISTORY = 3600;

    private final long intervalMs;
    private final LhmSource lhm;
    private final MonitorEngine<LiveSample> engine;
    private ScheduledExecutorService executor;
    private LiveSampler sampler;

    public LiveMonitorService(@Value("${pccheck.monitor.interval-ms:2000}") long intervalMs,
                              @Value("${pccheck.monitor.history-size:300}") int historySize,
                              LhmSource lhm) {
        if (intervalMs < MIN_INTERVAL_MS) {
            throw new IllegalArgumentException("pccheck.monitor.interval-ms deve ser pelo menos " + MIN_INTERVAL_MS + ".");
        }
        if (historySize < MIN_HISTORY || historySize > MAX_HISTORY) {
            throw new IllegalArgumentException(
                    "pccheck.monitor.history-size deve estar entre " + MIN_HISTORY + " e " + MAX_HISTORY + ".");
        }
        this.intervalMs = intervalMs;
        this.lhm = lhm;
        Duration stall = Duration.ofMillis(Math.max(10_000, intervalMs * 5));
        this.engine = new MonitorEngine<>(this::sampleOnce, historySize, stall, Clock.systemUTC());
    }

    @PostConstruct
    public synchronized void start() {
        if (executor != null) {
            return;
        }
        executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "pccheck-live-monitor");
            thread.setDaemon(true);
            return thread;
        });
        executor.scheduleWithFixedDelay(engine::tick, 0, intervalMs, TimeUnit.MILLISECONDS);
    }

    @PreDestroy
    public synchronized void stop() {
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }

    /** Executado somente pela thread do agendador (uma coleta por vez). */
    private LiveSample sampleOnce() {
        if (sampler == null) {
            sampler = new LiveSampler(new SystemInfo(), lhm);
        }
        return sampler.sample();
    }

    private synchronized boolean isRunning() {
        return executor != null;
    }

    public LiveDto live() {
        LiveSample latest = engine.latest();
        return new LiveDto(LiveDto.SOURCE, engine.status(isRunning(), intervalMs),
                latest == null ? null : LiveSampleDto.from(latest));
    }

    public LiveHistoryDto history(int limit) {
        List<LiveSampleDto> samples = engine.history(limit).stream().map(LiveSampleDto::from).toList();
        return new LiveHistoryDto(LiveDto.SOURCE, engine.status(isRunning(), intervalMs), samples);
    }
}
