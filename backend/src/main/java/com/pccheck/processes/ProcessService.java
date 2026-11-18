package com.pccheck.processes;

import com.pccheck.processes.ProcessSampler.Snapshot;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;

/**
 * Lista de processos com uso de CPU medido em um intervalo curto. Uma leitura por vez (cada uma leva cerca de 1 s);
 * pedidos que chegam logo depois recebem a última leitura, para várias abas não multiplicarem o custo.
 */
@Service
public class ProcessService {

    static final long SAMPLE_MILLIS = 1000;
    static final Duration REUSE = Duration.ofSeconds(2);

    private final Clock clock;
    private final String osFamily =
            System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT).startsWith("windows") ? "windows" : "linux";
    private SystemInfo systemInfo;
    private ProcessesDto last;

    public ProcessService() {
        this(Clock.systemUTC());
    }

    ProcessService(Clock clock) {
        this.clock = clock;
    }

    public synchronized ProcessesDto processes() {
        Instant now = clock.instant();
        if (last != null && last.error() == null && Duration.between(last.collectedAt(), now).compareTo(REUSE) < 0) {
            return last;
        }
        last = read();
        return last;
    }

    private ProcessesDto read() {
        int logical = 0;
        Long totalMemory = null;
        try {
            if (systemInfo == null) {
                systemInfo = new SystemInfo();
            }
            OperatingSystem os = systemInfo.getOperatingSystem();
            CentralProcessor processor = systemInfo.getHardware().getProcessor();
            logical = processor.getLogicalProcessorCount();
            long total = systemInfo.getHardware().getMemory().getTotal();
            totalMemory = total > 0 ? total : null;

            List<Snapshot> before = os.getProcesses().stream().map(Snapshot::of).toList();
            Thread.sleep(SAMPLE_MILLIS);
            List<OSProcess> current = os.getProcesses();
            List<Snapshot> after = current.stream().map(Snapshot::of).toList();

            int processCount = os.getProcessCount();
            int threadCount = os.getThreadCount();
            return new ProcessesDto(clock.instant(), SAMPLE_MILLIS, logical, totalMemory,
                    processCount > 0 ? processCount : null, threadCount > 0 ? threadCount : null,
                    ProcessSampler.compute(before, after, logical, ProcessHandle.current().pid()), null, osFamily);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return failure(logical, totalMemory, "Leitura dos processos interrompida.");
        } catch (RuntimeException | LinkageError e) {
            return failure(logical, totalMemory, "Não foi possível ler os processos: " + e.getMessage());
        }
    }

    private ProcessesDto failure(int logical, Long totalMemory, String error) {
        return new ProcessesDto(clock.instant(), SAMPLE_MILLIS, logical, totalMemory, null, null, List.of(), error,
                osFamily);
    }
}
