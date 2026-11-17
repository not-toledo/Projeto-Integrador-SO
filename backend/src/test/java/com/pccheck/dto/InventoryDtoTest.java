package com.pccheck.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.pccheck.agent.model.CollectorResult;
import com.pccheck.agent.model.GpuInfo;
import com.pccheck.agent.model.OsInfo;
import com.pccheck.agent.model.SystemSnapshot;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class InventoryDtoTest {

    @Test
    void mapeiaStatusPorComponenteSemUmAfetarOOutro() {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        SystemSnapshot snapshot = new SystemSnapshot(now,
                CollectorResult.ok("OsCollector", new OsInfo("Windows", "11", null, "amd64", 10L)),
                CollectorResult.error("CpuCollector", "falha"),
                CollectorResult.unavailable("RamCollector", "sem dado"),
                CollectorResult.ok("GpuCollector", List.of(new GpuInfo("GPU X", null, null, null))),
                CollectorResult.unavailable("MotherboardCollector", "sem dado"),
                CollectorResult.error("DiskCollector", "falha"));

        InventoryDto dto = InventoryDto.from(snapshot);

        assertEquals(now, dto.collectedAt());
        assertEquals(InventoryDto.SOURCE, dto.source());
        assertEquals("OK", dto.os().status());
        assertEquals("Windows", dto.os().data().name());
        assertEquals("ERROR", dto.cpu().status());
        assertEquals("falha", dto.cpu().message());
        assertNull(dto.cpu().data());
        assertEquals("UNAVAILABLE", dto.memory().status());
        assertEquals("OK", dto.gpu().status());
        assertEquals("GPU X", dto.gpu().data().get(0).name());
        assertEquals("UNAVAILABLE", dto.motherboard().status());
        assertEquals("ERROR", dto.storage().status());
    }
}
