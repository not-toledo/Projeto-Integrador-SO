package com.pccheck.agent.collector;

import com.pccheck.agent.model.CollectorResult;
import com.pccheck.agent.model.CpuLive;
import com.pccheck.agent.model.DiskActivity;
import com.pccheck.agent.model.DiskLive;
import com.pccheck.agent.model.GpuLive;
import com.pccheck.agent.model.LiveSample;
import com.pccheck.agent.model.MemoryInfo;
import com.pccheck.agent.model.PagingActivity;
import com.pccheck.agent.model.SensorLive;
import com.pccheck.agent.model.VolumeInfo;
import com.pccheck.agent.sensors.GpuReading;
import com.pccheck.agent.sensors.SensorReading;
import com.pccheck.agent.sensors.SensorSource;
import com.pccheck.agent.sensors.SensorsUnavailableException;
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

    private final SystemInfo systemInfo;
    private final SensorSource sensorSource;
    private long[] previousCpuTicks;
    private long[][] previousCoreTicks;
    private final Map<String, DiskCounters> previousDisk = new HashMap<>();
    private PagingCounters previousPaging;

    public LiveSampler(SystemInfo systemInfo, SensorSource sensorSource) {
        this.systemInfo = systemInfo;
        this.sensorSource = sensorSource;
        CentralProcessor processor = systemInfo.getHardware().getProcessor();
        // Referência inicial: a primeira amostra mede o uso desde a criação do amostrador.
        this.previousCpuTicks = processor.getSystemCpuLoadTicks();
        this.previousCoreTicks = processor.getProcessorCpuLoadTicks();
    }

    public LiveSample sample() {
        long start = System.nanoTime();
        CollectorResult<CpuLive> cpu = CollectorRunner.run(new Named<>("LiveCpu", this::readCpu));
        CollectorResult<MemoryInfo> memory = withPaging(CollectorRunner.run(new RamCollector(systemInfo)));
        // Uma única consulta ao leitor de sensores por amostra, compartilhada por GPU e sensores.
        SensorOutcome sensorOutcome = readSensorSource();
        CollectorResult<GpuLive> gpu = readGpu(sensorOutcome);
        CollectorResult<DiskLive> disks = CollectorRunner.run(new Named<>("LiveDisk", this::readDisks));
        CollectorResult<SensorLive> sensors = CollectorRunner.run(new Named<>("LiveSensors", () -> readSensors(sensorOutcome)));
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

    /** Acrescenta à memória as páginas lidas/gravadas por segundo desde a amostra anterior. */
    private CollectorResult<MemoryInfo> withPaging(CollectorResult<MemoryInfo> memory) {
        if (!memory.isOk() || memory.data() == null) {
            return memory;
        }
        try {
            var hw = systemInfo.getHardware().getMemory();
            var vm = hw.getVirtualMemory();
            PagingCounters now = new PagingCounters(vm.getSwapPagesIn(), vm.getSwapPagesOut(), System.currentTimeMillis());
            PagingCounters before = previousPaging;
            previousPaging = now;
            return CollectorResult.ok(memory.collector(),
                    memory.data().withPaging(pagingBetween(before, now, Metrics.positiveOrNull(hw.getPageSize()))));
        } catch (RuntimeException e) {
            // Sem contadores de paginação: a RAM e a memória virtual continuam válidas.
            return memory;
        }
    }

    /** Conversão pura (testável): null na primeira amostra; taxa null se o contador for inválido ou recomeçar. */
    static PagingActivity pagingBetween(PagingCounters before, PagingCounters now, Long pageSize) {
        if (before == null) {
            return null;
        }
        long elapsed = now.timestampMs() - before.timestampMs();
        Double in = Metrics.ratePerSecond(before.pagesIn(), now.pagesIn(), elapsed);
        Double out = Metrics.ratePerSecond(before.pagesOut(), now.pagesOut(), elapsed);
        return in == null && out == null ? null : new PagingActivity(in, out, pageSize);
    }

    record PagingCounters(long pagesIn, long pagesOut, long timestampMs) {
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

    /** Resultado da consulta ao leitor de sensores: leitura ou o motivo de não haver. */
    private record SensorOutcome(SensorReading reading, String problem) {
    }

    private SensorOutcome readSensorSource() {
        try {
            return new SensorOutcome(sensorSource.read(), null);
        } catch (SensorsUnavailableException e) {
            return new SensorOutcome(null, e.getMessage());
        } catch (RuntimeException e) {
            return new SensorOutcome(null, "Falha ao ler os sensores: " + e.getMessage());
        }
    }

    /** GPU: UNAVAILABLE (com o motivo) quando não há leitura da placa; nunca um valor inventado. */
    private CollectorResult<GpuLive> readGpu(SensorOutcome outcome) {
        final String collector = "LiveGpu";
        try {
            if (outcome.reading() == null) {
                return CollectorResult.unavailable(collector, outcome.problem());
            }
            GpuReading gpu = outcome.reading().primaryGpu();
            if (gpu == null) {
                return CollectorResult.unavailable(collector,
                        "Nenhuma placa de vídeo com sensores foi encontrada (driver ausente ou não suportado).");
            }
            if (gpu.loadPercent() == null && gpu.vramUsedBytes() == null && gpu.temperatureCelsius() == null) {
                return CollectorResult.unavailable(collector,
                        "A placa de vídeo foi encontrada, mas o driver não informou uso, memória nem temperatura.");
            }
            return CollectorResult.ok(collector, new GpuLive(gpu.loadPercent(), gpu.vramUsedBytes(),
                    gpu.vramTotalBytes(), gpu.temperatureCelsius(), gpu.name()));
        } catch (RuntimeException e) {
            return CollectorResult.error(collector, e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private SensorLive readSensors(SensorOutcome outcome) throws CollectorUnavailableException {
        Sensors sensors = systemInfo.getHardware().getSensors();
        SensorReading reading = outcome.reading();

        // CPU: o leitor de sensores lê o sensor real; o OSHI fica como alternativa.
        Double temperature = reading == null || reading.cpuTemperatureCelsius() == null ? null
                : Metrics.validTemperature(reading.cpuTemperatureCelsius());
        if (temperature == null) {
            temperature = Metrics.validTemperature(sensors.getCpuTemperature());
        }

        List<Integer> fans = new ArrayList<>();
        if (reading != null) {
            fans.addAll(reading.fanSpeedsRpm());
        }
        if (fans.isEmpty()) {
            for (int rpm : sensors.getFanSpeeds()) {
                Integer valid = Metrics.positiveOrNull(rpm);
                if (valid != null) {
                    fans.add(valid);
                }
            }
        }

        Double gpuTemperature = null;
        if (reading != null && reading.primaryGpu() != null) {
            gpuTemperature = reading.primaryGpu().temperatureCelsius();
        }
        if (temperature == null && fans.isEmpty() && gpuTemperature == null) {
            String reason = reading != null ? reading.limitation() : outcome.problem();
            throw new CollectorUnavailableException(reason != null ? reason
                    : "O hardware não informou temperaturas nem ventoinhas.");
        }
        return new SensorLive(temperature, fans.isEmpty() ? null : fans, gpuTemperature);
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
