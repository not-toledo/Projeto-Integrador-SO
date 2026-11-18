package com.pccheck.processes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pccheck.processes.ProcessSampler.Snapshot;
import com.pccheck.processes.ProcessesDto.ProcessInfo;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProcessSamplerTest {

    private static Snapshot snap(int pid, String name, long busyMillis, long upMillis, long start) {
        return new Snapshot(pid, 1, name, "user", "RUNNING", 8, 4, busyMillis / 2, busyMillis - busyMillis / 2,
                upMillis, 100L * 1024 * 1024, 200L * 1024 * 1024, start);
    }

    @Test
    void cpuNoIntervaloDivididoPelosProcessadoresLogicos() {
        // 1000 ms decorridos, 2000 ms de CPU ganhos (2 núcleos inteiros) em 8 processadores lógicos = 25%.
        List<ProcessInfo> r = ProcessSampler.compute(
                List.of(snap(10, "jogo.exe", 5000, 10_000, 1)),
                List.of(snap(10, "jogo.exe", 7000, 11_000, 1)), 8, 99);
        assertEquals(25.0, r.get(0).cpuPercent(), 1e-9);
        assertEquals(100L * 1024 * 1024, r.get(0).memoryBytes());
        assertFalse(r.get(0).self());
    }

    @Test
    void processoNovoOuPidReaproveitadoFicaSemCpu() {
        List<ProcessInfo> r = ProcessSampler.compute(
                List.of(snap(20, "antigo", 100, 1000, 1)),
                List.of(snap(20, "novo", 50, 500, 2), snap(30, "recente", 10, 100, 3)), 4, 99);
        assertTrue(r.stream().allMatch(p -> p.cpuPercent() == null));
    }

    @Test
    void ignoraOProcessoOciosoEMarcaOProprioBackend() {
        List<ProcessInfo> r = ProcessSampler.compute(
                List.of(snap(0, "Idle", 0, 1000, 1), snap(99, "java.exe", 0, 1000, 1)),
                List.of(snap(0, "Idle", 900, 2000, 1), snap(99, "java.exe", 100, 2000, 1)), 1, 99);
        assertEquals(1, r.size());
        assertTrue(r.get(0).self());
        assertEquals(10.0, r.get(0).cpuPercent(), 1e-9);
    }

    @Test
    void usuarioDesconhecidoViraNuloEOrdenaPorCpu() {
        Snapshot semDono = new Snapshot(5, 1, "svc", "unknown", "SLEEPING", 8, 1, 0, 0, 1000, 1, 1, 0);
        Snapshot semDonoDepois = new Snapshot(5, 1, "svc", "unknown", "SLEEPING", 8, 1, 0, 0, 2000, 1, 1, 0);
        List<ProcessInfo> r = ProcessSampler.compute(
                List.of(semDono, snap(6, "ativo", 0, 1000, 1)),
                List.of(semDonoDepois, snap(6, "ativo", 500, 2000, 1)), 1, 99);
        assertEquals("ativo", r.get(0).name());
        ProcessInfo svc = r.get(1);
        assertNull(svc.user());
        assertNull(svc.startTime());
        assertEquals(0.0, svc.cpuPercent(), 1e-9);
    }
}
