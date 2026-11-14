package com.pccheck.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.pccheck.agent.model.CollectorResult;
import com.pccheck.agent.model.CpuLive;
import com.pccheck.agent.model.LiveSample;
import com.pccheck.agent.model.MemoryInfo;
import java.time.Instant;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class LiveSampleDtoTest {

    @Test
    void mapeiaEstadoPorGrupoSemUmAfetarOOutro() {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        LiveSample sample = new LiveSample(now, 12,
                CollectorResult.ok("LiveCpu", new CpuLive(40.0, Arrays.asList(10.0, null))),
                CollectorResult.ok("RamCollector", new MemoryInfo(100L, 40L, 60L, 60.0)),
                CollectorResult.unavailable("LiveGpu", "Não suportado"),
                CollectorResult.error("LiveDisk", "falha"),
                CollectorResult.unavailable("LiveSensors", "sem sensor"));

        LiveSampleDto dto = LiveSampleDto.from(sample);

        assertEquals(now, dto.collectedAt());
        assertEquals(12, dto.durationMs());
        assertEquals("OK", dto.cpu().status());
        assertEquals(40.0, dto.cpu().data().totalPercent(), 0.0001);
        assertNull(dto.cpu().data().perCorePercent().get(1));
        assertEquals("OK", dto.memory().status());
        assertEquals("UNAVAILABLE", dto.gpu().status());
        assertEquals("Não suportado", dto.gpu().message());
        assertEquals("ERROR", dto.disks().status());
        assertEquals("UNAVAILABLE", dto.sensors().status());
    }
}
