package com.pccheck.agent.lhm;

import com.pccheck.agent.collector.Metrics;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Converte o {@code data.json} do servidor web do LibreHardwareMonitor em {@link LhmReading}.
 *
 * <p>A árvore é: computador → hardware → (sub-hardware) → grupo ("Temperatures", "Load", "Fans", "Data"...)
 * → sensor. Os valores vêm como texto formatado na cultura do Windows (ex.: "45,0 °C", "1,5 GB"). Versões
 * recentes trazem também {@code HardwareId}, {@code SensorId} e {@code Type}; as antigas não, então o tipo
 * do sensor e o tipo do hardware também são deduzidos dos nomes quando esses campos faltam.
 */
public final class LhmParser {

    private static final String[] CPU_TEMPERATURE_NAMES = {
        "CPU Package", "Core (Tctl/Tdie)", "Core (Tdie)", "Core (Tctl)", "CPU (Tctl/Tdie)", "Tdie", "Tctl",
        "Core Max", "Core Average"
    };
    private static final String[] GPU_LOAD_NAMES = {"GPU Core", "D3D 3D"};
    private static final String[] GPU_TEMPERATURE_NAMES = {"GPU Core", "GPU Hot Spot"};
    private static final String[] GPU_MEMORY_USED_NAMES = {"GPU Memory Used", "D3D Dedicated Memory Used"};
    private static final String[] GPU_MEMORY_TOTAL_NAMES = {"GPU Memory Total", "D3D Dedicated Memory Total"};

    private LhmParser() {
    }

    /** @throws IllegalArgumentException se o texto não for um JSON válido ou não tiver a estrutura esperada */
    public static LhmReading parse(String json) {
        Object root = MiniJson.parse(json);
        if (!(root instanceof Map<?, ?> rootMap)) {
            throw new IllegalArgumentException("Resposta inesperada: não é o data.json do LibreHardwareMonitor.");
        }
        Collected collected = new Collected();
        walk(rootMap, null, null, collected);

        Double cpuTemperature = null;
        if (collected.cpu != null) {
            cpuTemperature = pickCpuTemperature(collected.cpu);
        }
        List<LhmGpu> gpus = new ArrayList<>();
        for (HardwareSensors gpu : collected.gpus) {
            gpus.add(toGpu(gpu));
        }
        return new LhmReading(cpuTemperature, gpus, collected.fans);
    }

    // ---------------------------------------------------------------- percurso da árvore

    private enum Kind { CPU, GPU, OTHER }

    private record SensorValue(String name, Double number, String unit) {
    }

    private static final class HardwareSensors {
        final String name;
        final List<SensorValue> temperatures = new ArrayList<>();
        final List<SensorValue> loads = new ArrayList<>();
        final List<SensorValue> data = new ArrayList<>();

        HardwareSensors(String name) {
            this.name = name;
        }
    }

    private static final class Collected {
        HardwareSensors cpu;
        final List<HardwareSensors> gpus = new ArrayList<>();
        final List<Integer> fans = new ArrayList<>();
        private final java.util.IdentityHashMap<Map<?, ?>, HardwareSensors> byNode = new java.util.IdentityHashMap<>();

        /** Primeira CPU encontrada (sistemas com mais de um soquete são raros neste projeto). */
        HardwareSensors cpu(Map<?, ?> hardware) {
            HardwareSensors existing = byNode.get(hardware);
            if (existing != null) {
                return existing;
            }
            HardwareSensors created = new HardwareSensors(text(hardware.get("Text")));
            byNode.put(hardware, created);
            if (cpu == null) {
                cpu = created;
            }
            return created;
        }

        HardwareSensors gpu(Map<?, ?> hardware) {
            HardwareSensors existing = byNode.get(hardware);
            if (existing != null) {
                return existing;
            }
            HardwareSensors created = new HardwareSensors(text(hardware.get("Text")));
            byNode.put(hardware, created);
            gpus.add(created);
            return created;
        }
    }

    /** hardware = nó de hardware mais próximo acima do grupo; group = nome do grupo de sensores pai. */
    private static void walk(Map<?, ?> node, Map<?, ?> hardware, String group, Collected out) {
        List<?> children = node.get("Children") instanceof List<?> list ? list : List.of();
        String sensorId = text(node.get("SensorId"));
        String value = text(node.get("Value"));

        boolean isSensor = children.isEmpty() && value != null && !value.isBlank()
                && (sensorId != null || (hardware != null && group != null));
        if (isSensor) {
            addSensor(node, hardware, group, sensorId, value, out);
            return;
        }

        Map<?, ?> nextHardware = hardware;
        String nextGroup = group;
        if (isHardwareNode(node)) {
            nextHardware = node;
            nextGroup = null;
        } else if (hardware != null) {
            nextGroup = text(node.get("Text"));
        }
        for (Object child : children) {
            if (child instanceof Map<?, ?> childMap) {
                walk(childMap, nextHardware, nextGroup, out);
            }
        }
    }

    private static final java.util.Set<String> GROUP_ICONS = java.util.Set.of(
            "temperature", "load", "fan", "clock", "power", "voltage", "data", "smalldata", "factor", "control",
            "level", "flow", "throughput", "energy", "humidity", "noise", "conductivity", "frequency", "current");

    /** Nome do arquivo de ícone sem pasta nem extensão: "images_icon/cpu.png" -> "cpu". */
    private static String iconName(Map<?, ?> node) {
        String image = lower(text(node.get("ImageURL")));
        if (image == null || image.isBlank()) {
            return null;
        }
        String file = image.substring(Math.max(image.lastIndexOf('/'), image.lastIndexOf('\\')) + 1);
        int dot = file.lastIndexOf('.');
        return dot > 0 ? file.substring(0, dot) : file;
    }

    /**
     * Nó de hardware: tem HardwareId (versões novas) ou, nas antigas, um ícone que não é de grupo de
     * sensores nem de sensor ("transparent").
     */
    private static boolean isHardwareNode(Map<?, ?> node) {
        if (text(node.get("HardwareId")) != null) {
            return true;
        }
        String icon = iconName(node);
        if (icon == null || icon.equals("transparent") || GROUP_ICONS.contains(icon)) {
            return false;
        }
        return node.get("Children") instanceof List<?> list && !list.isEmpty();
    }

    private static void addSensor(Map<?, ?> sensor, Map<?, ?> hardware, String group, String sensorId,
                                  String valueText, Collected out) {
        String type = sensorType(sensor, group, sensorId);
        if (type == null) {
            return;
        }
        String name = text(sensor.get("Text"));
        Parsed parsed = parseValue(valueText);
        if (name == null || parsed == null) {
            return;
        }
        SensorValue sv = new SensorValue(name, parsed.number(), parsed.unit());

        if (type.equals("fan")) {
            if (parsed.number() > 0 && parsed.number() < 100_000) {
                out.fans.add((int) Math.round(parsed.number()));
            }
            return;
        }
        if (hardware == null) {
            return;
        }
        Kind kind = hardwareKind(hardware);
        if (kind == Kind.OTHER) {
            return;
        }
        HardwareSensors target = kind == Kind.CPU ? out.cpu(hardware) : out.gpu(hardware);
        switch (type) {
            case "temperature" -> target.temperatures.add(sv);
            case "load" -> target.loads.add(sv);
            case "data" -> target.data.add(withDefaultMemoryUnit(sv, sensor, group));
            default -> {
                // outros tipos (tensão, clock, potência...) não são usados
            }
        }
    }

    /** "temperature", "load", "fan", "data" ou null (tipo ignorado). */
    private static String sensorType(Map<?, ?> sensor, String group, String sensorId) {
        String explicit = text(sensor.get("Type"));
        String raw = explicit != null ? explicit : group;
        if (raw == null && sensorId != null) {
            // "/gpu-nvidia/0/temperature/0" -> "temperature"
            String[] parts = sensorId.split("/");
            raw = parts.length >= 4 ? parts[parts.length - 2] : null;
        }
        if (raw == null) {
            return null;
        }
        String r = raw.toLowerCase(Locale.ROOT).replace(" ", "");
        if (r.startsWith("temperature")) {
            return "temperature";
        }
        if (r.startsWith("load")) {
            return "load";
        }
        if (r.equals("fan") || r.equals("fans")) {
            return "fan";
        }
        if (r.equals("data") || r.equals("smalldata")) {
            return "data";
        }
        return null;
    }

    private static Kind hardwareKind(Map<?, ?> hardware) {
        String id = lower(text(hardware.get("HardwareId")));
        String icon = iconName(hardware);
        String name = lower(text(hardware.get("Text")));
        if (id != null) {
            if (id.startsWith("/amdcpu") || id.startsWith("/intelcpu")) {
                return Kind.CPU;
            }
            if (id.startsWith("/gpu")) {
                return Kind.GPU;
            }
            return Kind.OTHER;
        }
        if (icon != null) {
            if (icon.equals("cpu")) {
                return Kind.CPU;
            }
            if (icon.equals("nvidia") || icon.equals("ati") || icon.equals("amd") || icon.equals("gpu")) {
                return Kind.GPU;
            }
            if (java.util.Set.of("mainboard", "chip", "hdd", "ram", "nic", "battery", "computer").contains(icon)) {
                return Kind.OTHER;
            }
        }
        if (name != null && (name.contains("nvidia") || name.contains("geforce") || name.contains("radeon")
                || name.contains("graphics") || name.contains("arc "))) {
            return Kind.GPU;
        }
        return Kind.OTHER;
    }

    // ---------------------------------------------------------------- seleção dos valores

    private static Double pickCpuTemperature(HardwareSensors cpu) {
        for (String wanted : CPU_TEMPERATURE_NAMES) {
            Double v = find(cpu.temperatures, wanted);
            if (v != null && Metrics.validTemperature(v) != null) {
                return v;
            }
        }
        // Sem sensor de pacote: usa a maior temperatura de núcleo válida.
        Double max = null;
        for (SensorValue sv : cpu.temperatures) {
            if (Metrics.validTemperature(sv.number()) != null && (max == null || sv.number() > max)) {
                max = sv.number();
            }
        }
        return max;
    }

    private static LhmGpu toGpu(HardwareSensors gpu) {
        Double load = firstOf(gpu.loads, GPU_LOAD_NAMES);
        if (load != null && (load < 0 || load > 100)) {
            load = null;
        }
        Double temperature = firstOf(gpu.temperatures, GPU_TEMPERATURE_NAMES);
        if (temperature != null && Metrics.validTemperature(temperature) == null) {
            temperature = null;
        }
        Long used = bytes(gpu.data, GPU_MEMORY_USED_NAMES);
        Long total = bytes(gpu.data, GPU_MEMORY_TOTAL_NAMES);
        return new LhmGpu(Metrics.cleanText(gpu.name), load, temperature, used, total);
    }

    private static Double firstOf(List<SensorValue> values, String[] names) {
        for (String wanted : names) {
            Double v = find(values, wanted);
            if (v != null) {
                return v;
            }
        }
        return null;
    }

    private static Double find(List<SensorValue> values, String name) {
        for (SensorValue sv : values) {
            if (sv.name().equalsIgnoreCase(name)) {
                return sv.number();
            }
        }
        return null;
    }

    private static Long bytes(List<SensorValue> values, String[] names) {
        for (String wanted : names) {
            for (SensorValue sv : values) {
                if (sv.name().equalsIgnoreCase(wanted)) {
                    Long b = toBytes(sv.number(), sv.unit());
                    if (b != null) {
                        return b;
                    }
                }
            }
        }
        return null;
    }

    // ---------------------------------------------------------------- valores em texto

    /** Sem unidade no texto, assume a do tipo: "SmallData" = MB, "Data" = GB (como o LibreHardwareMonitor). */
    private static SensorValue withDefaultMemoryUnit(SensorValue sv, Map<?, ?> sensor, String group) {
        if (!sv.unit().isEmpty()) {
            return sv;
        }
        String type = lower(text(sensor.get("Type")));
        String g = lower(group);
        boolean small = "smalldata".equals(type) || (g != null && g.replace(" ", "").equals("smalldata"));
        return new SensorValue(sv.name(), sv.number(), small ? "MB" : "GB");
    }

    static Long toBytes(Double number, String unit) {
        if (number == null || number < 0) {
            return null;
        }
        String u = unit == null ? "" : unit.trim().toUpperCase(Locale.ROOT).replace("IB", "B");
        double factor;
        switch (u) {
            case "B" -> factor = 1;
            case "KB" -> factor = 1024.0;
            case "MB" -> factor = 1024.0 * 1024;
            case "GB" -> factor = 1024.0 * 1024 * 1024;
            case "TB" -> factor = 1024.0 * 1024 * 1024 * 1024;
            default -> {
                return null;
            }
        }
        return Math.round(number * factor);
    }

    record Parsed(double number, String unit) {
    }

    /**
     * Separa número e unidade de textos como "45,0 °C", "1.234,5 MB" ou "1450 RPM". O separador decimal
     * depende da cultura do Windows: com vírgula e ponto juntos, o último é o decimal; com um só tipo
     * repetido, é separador de milhar; com um só separador, é decimal (o programa não agrupa milhares).
     */
    static Parsed parseValue(String text) {
        if (text == null) {
            return null;
        }
        String t = text.trim();
        int i = 0;
        if (i < t.length() && (t.charAt(i) == '-' || t.charAt(i) == '+')) {
            i++;
        }
        int digitsStart = i;
        while (i < t.length() && (Character.isDigit(t.charAt(i)) || t.charAt(i) == ',' || t.charAt(i) == '.')) {
            i++;
        }
        if (i == digitsStart) {
            return null;
        }
        String number = t.substring(0, i);
        String unit = t.substring(i).trim();

        int lastComma = number.lastIndexOf(',');
        int lastDot = number.lastIndexOf('.');
        String normalized;
        if (lastComma >= 0 && lastDot >= 0) {
            char decimal = lastComma > lastDot ? ',' : '.';
            char group = decimal == ',' ? '.' : ',';
            normalized = number.replace(String.valueOf(group), "").replace(decimal, '.');
        } else if (lastComma >= 0 || lastDot >= 0) {
            char sep = lastComma >= 0 ? ',' : '.';
            int count = number.length() - number.replace(String.valueOf(sep), "").length();
            normalized = count > 1 ? number.replace(String.valueOf(sep), "") : number.replace(sep, '.');
        } else {
            normalized = number;
        }
        try {
            double v = Double.parseDouble(normalized);
            if (Double.isNaN(v) || Double.isInfinite(v)) {
                return null;
            }
            return new Parsed(v, unit);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String text(Object o) {
        return o instanceof String s ? s : null;
    }

    private static String lower(String s) {
        return s == null ? null : s.toLowerCase(Locale.ROOT);
    }
}
