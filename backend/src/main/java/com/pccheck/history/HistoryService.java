package com.pccheck.history;

import com.pccheck.agent.history.HistoryRecord;
import com.pccheck.agent.history.HistoryStore;
import com.pccheck.agent.history.ScheduledCollection;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Lê o histórico gravado pela coleta agendada (mesmos arquivos e mesmo formato do agente) e informa se a tarefa está
 * instalada no agendador do SO.
 */
@Service
public class HistoryService {

    static final int MAX_DAYS = 30;

    private final HistoryStore store;
    private final Clock clock;
    private final String osName;

    @Autowired
    public HistoryService(@Value("${nexcore.history.data-dir:}") String dataDir) {
        this(dataDir == null || dataDir.isBlank() ? ScheduledCollection.dataDir() : Path.of(dataDir.trim()),
                Clock.systemDefaultZone(), System.getProperty("os.name"));
    }

    HistoryService(Path dataDir, Clock clock, String osName) {
        this.store = new HistoryStore(dataDir, clock.getZone());
        this.clock = clock;
        this.osName = osName;
    }

    /** Coletas dos últimos {@code days} dias (1 a {@value #MAX_DAYS}). */
    public HistoryDto history(int days) {
        if (days < 1 || days > MAX_DAYS) {
            throw new IllegalArgumentException("O período deve ser de 1 a " + MAX_DAYS + " dias.");
        }
        Instant from = clock.instant().minus(Duration.ofDays(days));
        HistoryStore.ReadResult result = store.read(from);
        List<HistoryRecord> records = result.records();
        Instant last = records.isEmpty() ? null : records.get(records.size() - 1).collectedAt();
        return new HistoryDto(store.directory().toString(), days, records.size(), last,
                SchedulerStatus.query(osName), records, result.problems());
    }
}
