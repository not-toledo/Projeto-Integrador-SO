package com.pccheck.history;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pccheck.agent.history.HistoryRecord;
import com.pccheck.agent.history.HistoryStore;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class HistoryServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-17T12:00:00Z"), ZoneOffset.UTC);

    @TempDir
    Path temp;

    private void write(String... instants) throws IOException {
        HistoryStore store = new HistoryStore(temp, ZoneOffset.UTC);
        for (String at : instants) {
            store.append(new HistoryRecord(Instant.parse(at), 10.0, 50.0, 60.0, 5.0, 1L, List.of()));
        }
    }

    @Test
    void devolveSoAsColetasDoPeriodoEmOrdem() throws Exception {
        write("2026-11-17T11:00:00Z", "2026-11-05T12:00:00Z", "2026-11-16T12:00:00Z");
        // "Linux" sem crontab no Windows: o estado do agendador fica UNKNOWN, sem impedir a resposta.
        HistoryDto dto = new HistoryService(temp, CLOCK, "Linux").history(7);
        assertEquals(2, dto.count());
        assertEquals(Instant.parse("2026-11-17T11:00:00Z"), dto.lastCollectedAt());
        assertEquals(Instant.parse("2026-11-16T12:00:00Z"), dto.records().get(0).collectedAt());
        assertTrue(dto.directory().endsWith("coletas"));
    }

    @Test
    void semColetasRespondeVazio() {
        HistoryDto dto = new HistoryService(temp, CLOCK, "Linux").history(1);
        assertEquals(0, dto.count());
        assertNull(dto.lastCollectedAt());
    }

    @Test
    void periodoForaDoLimiteERejeitado() {
        var service = new HistoryService(temp, CLOCK, "Linux");
        assertThrows(IllegalArgumentException.class, () -> service.history(0));
        assertThrows(IllegalArgumentException.class, () -> service.history(31));
    }
}
