package com.pccheck.processes;

import com.pccheck.processes.ProcessesDto.ProcessInfo;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import oshi.software.os.OSProcess;

/**
 * Cálculo do uso de CPU por processo a partir de duas leituras, separado do OSHI para ser testável.
 * Uso no intervalo = (tempo de CPU em modo usuário + núcleo) ganho ÷ tempo decorrido ÷ processadores lógicos.
 */
final class ProcessSampler {

    private ProcessSampler() {
    }

    /** O que se precisa de um processo em cada leitura. Tempos em milissegundos, como o OSHI informa. */
    record Snapshot(int pid, int parentPid, String name, String user, String state, int priority, int threads,
                    long kernelMillis, long userMillis, long upMillis, long rssBytes, long virtualBytes,
                    long startEpochMillis) {

        static Snapshot of(OSProcess p) {
            return new Snapshot(p.getProcessID(), p.getParentProcessID(), p.getName(), p.getUser(),
                    p.getState() == null ? null : p.getState().name(), p.getPriority(), p.getThreadCount(),
                    p.getKernelTime(), p.getUserTime(), p.getUpTime(), p.getResidentSetSize(), p.getVirtualSize(),
                    p.getStartTime());
        }
    }

    /**
     * Junta as duas leituras pelo PID. Exclui o processo ocioso do sistema (PID 0), que no Windows aparece com o
     * tempo livre da CPU. Ordena por CPU e depois por memória.
     */
    static List<ProcessInfo> compute(List<Snapshot> before, List<Snapshot> after, int logicalProcessors, long selfPid) {
        int cpus = Math.max(1, logicalProcessors);
        Map<Integer, Snapshot> prior = new HashMap<>();
        for (Snapshot s : before) {
            prior.put(s.pid(), s);
        }
        List<ProcessInfo> result = new ArrayList<>();
        for (Snapshot s : after) {
            if (s.pid() == 0) {
                continue;
            }
            Snapshot old = prior.get(s.pid());
            Double cpu = null;
            // Mesmo PID com início diferente é outro processo (PID reaproveitado): sem comparação.
            if (old != null && old.startEpochMillis() == s.startEpochMillis()) {
                long elapsed = s.upMillis() - old.upMillis();
                long busy = (s.kernelMillis() + s.userMillis()) - (old.kernelMillis() + old.userMillis());
                if (elapsed > 0 && busy >= 0) {
                    cpu = Math.min(100.0, (double) busy / elapsed / cpus * 100);
                }
            }
            String user = s.user() == null || s.user().isBlank() || "unknown".equalsIgnoreCase(s.user()) ? null : s.user();
            Instant start = s.startEpochMillis() > 0 ? Instant.ofEpochMilli(s.startEpochMillis()) : null;
            String name = s.name() == null || s.name().isBlank() ? null : s.name();
            result.add(new ProcessInfo(s.pid(), s.parentPid(), name, user, s.state(), s.priority(), s.threads(), cpu,
                    Math.max(0, s.rssBytes()), Math.max(0, s.virtualBytes()), start, s.pid() == selfPid));
        }
        result.sort(Comparator.comparing((ProcessInfo p) -> p.cpuPercent() == null ? -1 : p.cpuPercent()).reversed()
                .thenComparing(Comparator.comparingLong(ProcessInfo::memoryBytes).reversed()));
        return result;
    }
}
