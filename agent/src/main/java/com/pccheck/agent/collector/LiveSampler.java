package com.pccheck.agent.collector;

import com.pccheck.agent.model.CollectorResult;
import com.pccheck.agent.model.CpuLive;
import com.pccheck.agent.model.DiskActivity;
import com.pccheck.agent.model.DiskLive;
import com.pccheck.agent.model.GpuLive;
import com.pccheck.agent.model.LiveSample;
import com.pccheck.agent.model.MemoryInfo;
import com.pccheck.agent.model.SensorLive;
import com.pccheck.agent.model.VolumeInfo;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.HWDiskStore;
import oshi.hardware.Sensors;
import oshi.software.os.OSFileStore;

/**
 * Coleta uma amostra do Live Monitor. Mantém estado entre chamadas (contadores anteriores) para calcular
 * uso de CPU e taxas de disco SEM bloquear: ao contrário do inventário, não espera 500 ms medindo a CPU;
 * usa a diferença entre a amostra atual e a anterior.
 *
 * <p>Não é thread-safe: deve ser chamado por uma coleta por vez (garantido pelo agendador do backend).
 * Cada grupo de métricas é isolado por {@link CollectorRunner}.
 */
public class LiveSampler {

    /** Mensagem exibida para a GPU: o OSHI não fornece telemetria de GPU. */
    static final String GPU_UNSUPPORTED =
            "Não suportado: a biblioteca de coleta (OSHI) não fornece utilização nem memória de vídeo em uso da GPU.";

    private final SystemInfo systemInfo;
    private long[] previousCpuTicks;
    private long[][] previousCoreTicks;
    private final Map<String, DiskCounters> previousDisk = new HashMap<>();

    public LiveSampler(SystemInfo systemInfo) {
        this.systemInfo = systemInfo;
        CentralProcessor processor = systemInfo.getHardware().getProcessor();
        // Referência inicial: a primeira amostra mede o uso desde a criação do amostrador.
        this.previousCpuTicks = processor.getSystemCpuLoadTicks();
        this.previousCoreTicks = processor.getProcessorCpuLoadTicks();
    }

    public LiveSample sample() {
        long start = System.nanoTime();
        CollectorResult<CpuLive> cpu = CollectorRunner.run(new Named<>("LiveCpu", this::readCpu));
        CollectorResult<MemoryInfo> memory = CollectorRunner.run(new RamCollector(systemInfo));
        CollectorResult<GpuLive> gpu = CollectorResult.unavailable("LiveGpu", GPU_UNSUPPORTED);
        CollectorResult<DiskLive> disks = CollectorRunner.run(new Named<>("LiveDisk", this::readDisks));
        CollectorResult<SensorLive> sensors = CollectorRunner.run(new Named<>("LiveSensors", this::readSensors));
        long durationMs = (System.nanoTime() - start) / 1_000_000L;
        return new LiveSample(Instant.now(), durationMs, cpu, memory, gpu, disks, sensors);
    }

    private CpuLive readCpu() {
        CentralProcessor processor = systemInfo.getHardware().getProcessor();
        long[] ticks = processor.getSystemCpuLoadTicks();
        long[][] coreTicks = processor.getProcessorCpuLoadTicks();
        Double total = Metrics.cpuLoadToPercent(processor.getSystemCpuLoadBetweenTicks(previousCpuTicks));
        double[] perCore = processor.getProcessorCpuLoadBetweenTicks(previousCoreTicks);
        previousCpuTicks = ticks;
        previousCoreTicks = coreTicks;

        List<Double> cores = new ArrayList<>();
        boolean anyCore = false;
        for (double load : perCore) {
            Double percent = Metrics.cpuLoadToPercent(load);
            anyCore |= percent != null;
            cores.add(percent);
        }
        if (total == null && !anyCore) {
            return null;
        }
        return new CpuLive(total, anyCore ? cores : null);
    }

    private DiskLive readDisks() {
        List<DiskActivity> drives = null;
        List<VolumeInfo> volumes = null;
        RuntimeException failure = null;
        try {
            drives = readDriveActivity();
        } catch (RuntimeException e) {
            failure = e;
        }
        try {
            volumes = readVolumes();
        } catch (RuntimeException e) {
            if (failure == null) {
                failure = e;
            }
        }
        if (drives == null && volumes == null) {
            if (failure != null) {
                throw failure;
            }
            return null;
        }
        return new DiskLive(drives, volumes);
    }

    private List<DiskActivity> readDriveActivity() {
        List<DiskActivity> result = new ArrayList<>();
        for (HWDiskStore store : systemInfo.getHardware().getDiskStores()) {
            store.updateAttributes();
            String name = Metrics.cleanText(store.getName());
            if (name == null) {
                continue;
            }
            DiskCounters now = new DiskCounters(store.getReadBytes(), store.getWriteBytes(),
                    store.getTransferTime(), store.getTimeStamp());
            DiskCounters before = previousDisk.put(name, now);
            if (before == null) {
                // Primeira leitura desta unidade: ainda não há valor anterior para calcular a taxa.
                result.add(new DiskActivity(name, null, null, null));
                continue;
            }
            long elapsed = now.timestampMs() - before.timestampMs();
            result.add(new DiskActivity(name,
                    Metrics.ratePerSecond(before.readBytes(), now.readBytes(), elapsed),
                    Metrics.ratePerSecond(before.writeBytes(), now.writeBytes(), elapsed),
                    Metrics.busyPercent(before.transferMs(), now.transferMs(), elapsed)));
        }
        return result.isEmpty() ? null : result;
    }

    private List<VolumeInfo> readVolumes() {
        List<VolumeInfo> result = new ArrayList<>();
        for (OSFileStore store : systemInfo.getOperatingSystem().getFileSystem().getFileStores(true)) {
            VolumeInfo info = DiskCollector.toVolume(store.getMount(), store.getLabel(), store.getType(),
                    store.getTotalSpace(), store.getUsableSpace());
            if (info != null) {
                result.add(info);
            }
        }
        return result.isEmpty() ? null : result;
    }

    private SensorLive readSensors() {
        Sensors sensors = systemInfo.getHardware().getSensors();
        Double temperature = Metrics.validTemperature(sensors.getCpuTemperature());
        List<Integer> fans = new ArrayList<>();
        for (int rpm : sensors.getFanSpeeds()) {
            Integer valid = Metrics.positiveOrNull(rpm);
            if (valid != null) {
                fans.add(valid);
            }
        }
        if (temperature == null && fans.isEmpty()) {
            return null;
        }
        return new SensorLive(temperature, fans.isEmpty() ? null : fans);
    }

    private record DiskCounters(long readBytes, long writeBytes, long transferMs, long timestampMs) {
    }

    /** Adapta uma leitura a {@link Collector} para reaproveitar o isolamento de falhas do {@link CollectorRunner}. */
    private static final class Named<T> implements Collector<T> {

        interface Reader<T> {
            T read() throws Exception;
        }

        private final String name;
        private final Reader<T> reader;

        Named(String name, Reader<T> reader) {
            this.name = name;
            this.reader = reader;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public T collect() throws Exception {
            return reader.read();
        }
    }
}
