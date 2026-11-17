package com.pccheck.agent.sensors;

import com.pccheck.agent.collector.Metrics;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Converte a linha JSON do leitor de sensores ({@code sensor-host/}) em {@link SensorReading}.
 *
 * <p>Formato (os tipos são os nomes dos enums da LibreHardwareMonitorLib; valores numéricos já em
 * cultura invariável, sem unidade no texto):
 * <pre>
 * {"version":1, "elevated":true, "driver":true, "error":null,
 *  "hardware":[{"type":"Cpu", "name":"...", "sensors":[{"type":"Temperature", "name":"CPU Package", "value":45.5}]}]}
 * </pre>
 * A escolha de qual sensor representa cada métrica fica aqui, e não no leitor, para ser testada em Java.
 */
public final class SensorHostParser {

    static final String NOT_ELEVATED =
            "A temperatura da CPU e as ventoinhas da placa-mãe exigem permissão de administrador. "
                    + "Reinicie o PCCheck e confirme o aviso do Windows.";
    static final String NO_DRIVER =
            "O driver PawnIO não está instalado; sem ele o Windows não permite ler a temperatura da CPU e as "
                    + "ventoinhas da placa-mãe. Instale-o uma vez (https://pawnio.eu) e reinicie o PCCheck.";

    private static final String[] CPU_TEMPERATURE_NAMES = {
        "CPU Package", "Core (Tctl/Tdie)", "Core (Tdie)", "Core (Tctl)", "CPU (Tctl/Tdie)", "Tdie", "Tctl",
        "Core Max", "Core Average"
    };
    private static final String[] GPU_LOAD_NAMES = {"GPU Core", "D3D 3D"};
    private static final String[] GPU_TEMPERATURE_NAMES = {"GPU Core", "GPU Hot Spot"};
    private static final String[] GPU_MEMORY_USED_NAMES = {"GPU Memory Used", "D3D Dedicated Memory Used"};
    private static final String[] GPU_MEMORY_TOTAL_NAMES = {"GPU Memory Total", "D3D Dedicated Memory Total"};
    private static final long MIB = 1024L * 1024;
    private static final long GIB = MIB * 1024;

    private SensorHostParser() {
    }

    private record Sensor(String type, String name, double value) {
    }

    /** @throws IllegalArgumentException se o texto não for um JSON válido do leitor de sensores */
    public static SensorReading parse(String json) {
        Object root = MiniJson.parse(json == null ? "" : json);
        if (!(root instanceof Map<?, ?> map) || !(map.get("hardware") instanceof List<?> hardware)) {
            throw new IllegalArgumentException("Resposta inesperada do leitor de sensores.");
        }
        Double cpuTemperature = null;
        boolean cpuSeen = false;
        List<GpuReading> gpus = new ArrayList<>();
        List<Integer> fans = new ArrayList<>();

        for (Object item : hardware) {
            if (!(item instanceof Map<?, ?> hw)) {
                continue;
            }
            String type = text(hw.get("type"));
            List<Sensor> sensors = sensors(hw.get("sensors"));
            for (Sensor s : sensors) {
                if (s.type().equals("Fan") && s.value() > 0 && s.value() < 100_000) {
                    fans.add((int) Math.round(s.value()));
                }
            }
            if ("Cpu".equals(type) && !cpuSeen) {
                // Primeira CPU (sistemas com mais de um soquete são raros neste projeto).
                cpuSeen = true;
                cpuTemperature = pickCpuTemperature(sensors);
            } else if (type != null && type.startsWith("Gpu")) {
                gpus.add(toGpu(text(hw.get("name")), sensors));
            }
        }
        return new SensorReading(cpuTemperature, gpus, fans, limitation(map));
    }

    /** Explica o que falta para ler os sensores que dependem do driver (CPU e placa-mãe). */
    private static String limitation(Map<?, ?> root) {
        String error = text(root.get("error"));
        if (error != null) {
            return "O leitor de sensores não conseguiu acessar o hardware: " + error;
        }
        if (!Boolean.TRUE.equals(root.get("elevated"))) {
            return NOT_ELEVATED;
        }
        if (!Boolean.TRUE.equals(root.get("driver"))) {
            return NO_DRIVER;
        }
        return null;
    }

    private static List<Sensor> sensors(Object raw) {
        List<Sensor> result = new ArrayList<>();
        if (!(raw instanceof List<?> list)) {
            return result;
        }
        for (Object item : list) {
            if (item instanceof Map<?, ?> s && s.get("value") instanceof Double value
                    && !value.isNaN() && !value.isInfinite()) {
                String type = text(s.get("type"));
                String name = text(s.get("name"));
                if (type != null && name != null) {
                    result.add(new Sensor(type, name, value));
                }
            }
        }
        return result;
    }

    private static Double pickCpuTemperature(List<Sensor> sensors) {
        for (String wanted : CPU_TEMPERATURE_NAMES) {
            Double v = find(sensors, "Temperature", wanted);
            if (v != null && Metrics.validTemperature(v) != null) {
                return v;
            }
        }
        // Sem sensor de pacote: usa a maior temperatura de núcleo válida.
        Double max = null;
        for (Sensor s : sensors) {
            if (s.type().equals("Temperature") && Metrics.validTemperature(s.value()) != null
                    && (max == null || s.value() > max)) {
                max = s.value();
            }
        }
        return max;
    }

    private static GpuReading toGpu(String name, List<Sensor> sensors) {
        Double load = firstOf(sensors, "Load", GPU_LOAD_NAMES);
        if (load != null && (load < 0 || load > 100)) {
            load = null;
        }
        Double temperature = firstOf(sensors, "Temperature", GPU_TEMPERATURE_NAMES);
        if (temperature != null && Metrics.validTemperature(temperature) == null) {
            temperature = null;
        }
        return new GpuReading(Metrics.cleanText(name), load, temperature,
                memoryBytes(sensors, GPU_MEMORY_USED_NAMES), memoryBytes(sensors, GPU_MEMORY_TOTAL_NAMES));
    }

    private static Double firstOf(List<Sensor> sensors, String type, String[] names) {
        for (String wanted : names) {
            Double v = find(sensors, type, wanted);
            if (v != null) {
                return v;
            }
        }
        return null;
    }

    private static Double find(List<Sensor> sensors, String type, String name) {
        for (Sensor s : sensors) {
            if (s.type().equals(type) && s.name().equalsIgnoreCase(name)) {
                return s.value();
            }
        }
        return null;
    }

    /** Na LibreHardwareMonitorLib, "SmallData" é em MB e "Data" em GB. */
    private static Long memoryBytes(List<Sensor> sensors, String[] names) {
        for (String wanted : names) {
            for (Sensor s : sensors) {
                if (!s.name().equalsIgnoreCase(wanted) || s.value() < 0) {
                    continue;
                }
                if (s.type().equals("SmallData")) {
                    return Math.round(s.value() * MIB);
                }
                if (s.type().equals("Data")) {
                    return Math.round(s.value() * GIB);
                }
            }
        }
        return null;
    }

    private static String text(Object o) {
        return o instanceof String s ? s : null;
    }
}
