package com.pccheck.agent.history;

import com.pccheck.agent.model.CollectorResult;
import com.pccheck.agent.model.MemoryInfo;
import com.pccheck.agent.model.StorageInfo;
import com.pccheck.agent.model.SystemSnapshot;
import com.pccheck.agent.model.VirtualMemoryInfo;
import com.pccheck.agent.model.VolumeInfo;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Uma coleta agendada: os indicadores que fazem sentido acompanhar ao longo de dias. Campo nulo = não disponível.
 * Temperaturas ficam de fora: a tarefa agendada roda sem privilégio de administrador, e sem ele o leitor de
 * sensores não lê a CPU.
 *
 * @param collectedAt     momento da coleta
 * @param cpuPercent      uso de CPU (medido em 0,5 s)
 * @param ramUsedPercent  uso de RAM
 * @param commitPercent   memória comprometida em relação ao limite
 * @param swapUsedPercent uso do arquivo de paginação/swap
 * @param uptimeSeconds   tempo de atividade do SO
 * @param volumes         espaço de cada volume
 */
public record HistoryRecord(Instant collectedAt, Double cpuPercent, Double ramUsedPercent, Double commitPercent,
                            Double swapUsedPercent, Long uptimeSeconds, List<VolumeRecord> volumes) {

    public HistoryRecord {
        volumes = volumes == null ? List.of() : List.copyOf(volumes);
    }

    /** Espaço de um volume. */
    public record VolumeRecord(String mountPoint, Long totalBytes, Long freeBytes) {
    }

    /** Extrai os indicadores de uma coleta completa; componentes com falha viram campos nulos. */
    public static HistoryRecord from(SystemSnapshot s) {
        MemoryInfo memory = data(s.memory());
        VirtualMemoryInfo vm = memory == null ? null : memory.virtualMemory();
        StorageInfo storage = data(s.storage());
        List<VolumeRecord> volumes = new ArrayList<>();
        if (storage != null && storage.volumes() != null) {
            for (VolumeInfo v : storage.volumes()) {
                volumes.add(new VolumeRecord(v.mountPoint(), v.totalBytes(), v.freeBytes()));
            }
        }
        return new HistoryRecord(s.collectedAt(),
                s.cpu().isOk() && s.cpu().data() != null ? s.cpu().data().usagePercent() : null,
                memory == null ? null : memory.usedPercent(),
                vm == null ? null : vm.commitPercent(),
                vm == null ? null : vm.swapUsedPercent(),
                s.os().isOk() && s.os().data() != null ? s.os().data().uptimeSeconds() : null,
                volumes);
    }

    private static <T> T data(CollectorResult<T> result) {
        return result != null && result.isOk() ? result.data() : null;
    }
}
