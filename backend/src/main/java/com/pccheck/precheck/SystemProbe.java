package com.pccheck.precheck;

import com.pccheck.os.CommandRunner;
import com.pccheck.precheck.PrecheckDto.ProcessGroup;
import com.pccheck.precheck.SystemState.PowerPlan;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.PowerSource;
import oshi.software.os.OSProcess;

/**
 * Lê o estado do sistema para a pré-checagem: memória (OSHI), uso de CPU e processos (duas leituras do OSHI com
 * cerca de 1 s de intervalo), plano de energia ({@code powercfg /getactivescheme} no Windows; governador em
 * {@code /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor} no Linux) e bateria (OSHI).
 */
public class SystemProbe {

    static final long SAMPLE_MILLIS = 1000;
    static final Path LINUX_GOVERNOR = Path.of("/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor");

    /** GUIDs dos planos padrão do Windows (conferidos com {@code powercfg /list}). */
    static final Map<String, String> WINDOWS_PLANS = Map.of(
            "381b4222-f694-41f0-9685-ff5bb260df2e", "BALANCED",
            "8c5e7fda-e8bf-4a96-9a85-a6e23a8c635c", "HIGH_PERFORMANCE",
            "a1841308-3541-4fab-bc81-f71556f20b4a", "POWER_SAVER");
    private static final Pattern SCHEME = Pattern.compile(
            "([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})\\s*(?:\\((.*)\\))?");

    private final boolean windows;
    private final CommandRunner runner;
    private SystemInfo systemInfo;

    public SystemProbe() {
        this(System.getProperty("os.name", "").toLowerCase(Locale.ROOT).startsWith("windows"), CommandRunner.system());
    }

    SystemProbe(boolean windows, CommandRunner runner) {
        this.windows = windows;
        this.runner = runner;
    }

    /** Uma leitura por vez: cada uma já leva cerca de 1 s. */
    public synchronized SystemState sample() {
        Long total = null;
        Long available = null;
        Double cpu = null;
        List<ProcessGroup> groups = List.of();
        String processError = null;
        Boolean onBattery = null;
        Double batteryPercent = null;
        try {
            if (systemInfo == null) {
                systemInfo = new SystemInfo();
            }
            GlobalMemory memory = systemInfo.getHardware().getMemory();
            total = memory.getTotal() > 0 ? memory.getTotal() : null;
            available = memory.getAvailable() >= 0 ? memory.getAvailable() : null;
        } catch (RuntimeException | LinkageError e) {
            processError = "Não foi possível ler a memória e os processos: " + e.getMessage();
        }
        if (systemInfo != null) {
            try {
                CentralProcessor processor = systemInfo.getHardware().getProcessor();
                List<OSProcess> before = systemInfo.getOperatingSystem().getProcesses();
                long[] ticks = processor.getSystemCpuLoadTicks();
                Thread.sleep(SAMPLE_MILLIS);
                List<OSProcess> after = systemInfo.getOperatingSystem().getProcesses();
                double load = processor.getSystemCpuLoadBetweenTicks(ticks);
                cpu = load >= 0 && load <= 1 ? load * 100 : null;
                groups = group(loads(before, after), processor.getLogicalProcessorCount(), ProcessHandle.current().pid(),
                        windows);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                processError = "Leitura dos processos interrompida.";
            } catch (RuntimeException e) {
                processError = "Não foi possível ler os processos: " + e.getMessage();
            }
            try {
                double discharging = -1;
                for (PowerSource source : systemInfo.getHardware().getPowerSources()) {
                    // Desktops podem informar uma "System Battery" fictícia (sem tomada e sem descarga):
                    // só "descarregando" indica uso real da bateria.
                    if (source.isDischarging()) {
                        discharging = Math.max(discharging, source.getRemainingCapacityPercent());
                    }
                }
                onBattery = discharging >= 0;
                batteryPercent = discharging >= 0 && discharging <= 1 ? discharging * 100 : null;
            } catch (RuntimeException e) {
                onBattery = null;
            }
        }
        return new SystemState(total, available, cpu, groups, processError, powerPlan(), onBattery, batteryPercent);
    }

    /** Uso de um processo no intervalo: carga somada entre os núcleos (pode passar de 1) e memória (RSS). */
    record ProcessLoad(long pid, String name, double load, long rssBytes) {
    }

    private static List<ProcessLoad> loads(List<OSProcess> before, List<OSProcess> after) {
        Map<Integer, OSProcess> prior = new HashMap<>();
        for (OSProcess p : before) {
            prior.put(p.getProcessID(), p);
        }
        List<ProcessLoad> result = new ArrayList<>();
        for (OSProcess p : after) {
            OSProcess old = prior.get(p.getProcessID());
            double load = old == null ? 0 : p.getProcessCpuLoadBetweenTicks(old);
            result.add(new ProcessLoad(p.getProcessID(), p.getName(), load, p.getResidentSetSize()));
        }
        return result;
    }

    /**
     * Agrupa por nome (sem diferenciar maiúsculas no Windows) e converte a carga em % do total da CPU (dividindo
     * pelos processadores lógicos). Exclui o processo ocioso (PID 0) e o próprio NEXCORE.
     */
    static List<ProcessGroup> group(List<ProcessLoad> loads, int logicalProcessors, long selfPid, boolean windows) {
        int cpus = Math.max(1, logicalProcessors);
        Map<String, double[]> acc = new LinkedHashMap<>(); // [cpu, memória, quantidade]
        Map<String, String> names = new HashMap<>();
        for (ProcessLoad p : loads) {
            if (p.pid() == 0 || p.pid() == selfPid || p.name() == null || p.name().isBlank()) {
                continue;
            }
            String key = windows ? p.name().toLowerCase(Locale.ROOT) : p.name();
            names.putIfAbsent(key, p.name());
            double[] a = acc.computeIfAbsent(key, k -> new double[3]);
            a[0] += Math.max(0, p.load());
            a[1] += Math.max(0, p.rssBytes());
            a[2]++;
        }
        List<ProcessGroup> result = new ArrayList<>();
        acc.forEach((key, a) -> result.add(new ProcessGroup(names.get(key), (int) a[2],
                Math.min(100, a[0] / cpus * 100), (long) a[1])));
        result.sort(Comparator.comparingDouble(ProcessGroup::cpuPercent).reversed()
                .thenComparing(Comparator.comparingLong(ProcessGroup::memoryBytes).reversed()));
        return result;
    }

    private PowerPlan powerPlan() {
        if (!windows) {
            try {
                String governor = Files.isReadable(LINUX_GOVERNOR) ? Files.readString(LINUX_GOVERNOR).strip() : null;
                return governor == null || governor.isEmpty()
                        ? new PowerPlan("UNAVAILABLE", null, "O kernel não expõe o governador da CPU (cpufreq).")
                        : new PowerPlan("LINUX_GOVERNOR", governor, null);
            } catch (IOException e) {
                return new PowerPlan("UNAVAILABLE", null, "Não foi possível ler o governador da CPU: " + e.getMessage());
            }
        }
        try {
            CommandRunner.Result r = runner.run(List.of("powercfg", "/getactivescheme"));
            if (r == null) {
                return new PowerPlan("UNAVAILABLE", null, "O powercfg não respondeu.");
            }
            PowerPlan plan = r.exitCode() == 0 ? parseWindowsPlan(r.output()) : null;
            return plan != null ? plan : new PowerPlan("UNAVAILABLE", null, "Saída do powercfg não reconhecida.");
        } catch (IOException e) {
            return new PowerPlan("UNAVAILABLE", null, "Não foi possível executar o powercfg: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new PowerPlan("UNAVAILABLE", null, "Consulta ao powercfg interrompida.");
        }
    }

    /** "GUID do Esquema de Energia: 381b4222-...  (Equilibrado)" → BALANCED, "Equilibrado". Null se não houver GUID. */
    static PowerPlan parseWindowsPlan(String output) {
        if (output == null) {
            return null;
        }
        Matcher m = SCHEME.matcher(output);
        if (!m.find()) {
            return null;
        }
        String guid = m.group(1).toLowerCase(Locale.ROOT);
        String name = m.group(2) == null ? null : m.group(2).strip();
        return new PowerPlan(WINDOWS_PLANS.getOrDefault(guid, "OTHER"), name == null || name.isEmpty() ? guid : name,
                null);
    }
}
