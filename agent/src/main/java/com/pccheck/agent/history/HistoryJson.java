package com.pccheck.agent.history;

import com.pccheck.agent.history.HistoryRecord.VolumeRecord;
import com.pccheck.agent.sensors.MiniJson;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Uma coleta por linha, em JSON (formato "JSON Lines"): fácil de acrescentar, ler e inspecionar com um editor. */
public final class HistoryJson {

    private HistoryJson() {
    }

    public static String toLine(HistoryRecord r) {
        StringBuilder sb = new StringBuilder(256).append('{');
        field(sb, "collectedAt", r.collectedAt() == null ? null : r.collectedAt().toString()).append(',');
        number(sb, "cpuPercent", r.cpuPercent()).append(',');
        number(sb, "ramUsedPercent", r.ramUsedPercent()).append(',');
        number(sb, "commitPercent", r.commitPercent()).append(',');
        number(sb, "swapUsedPercent", r.swapUsedPercent()).append(',');
        number(sb, "uptimeSeconds", r.uptimeSeconds()).append(",\"volumes\":[");
        for (int i = 0; i < r.volumes().size(); i++) {
            VolumeRecord v = r.volumes().get(i);
            if (i > 0) sb.append(',');
            sb.append('{');
            field(sb, "mountPoint", v.mountPoint()).append(',');
            number(sb, "totalBytes", v.totalBytes()).append(',');
            number(sb, "freeBytes", v.freeBytes()).append('}');
        }
        return sb.append("]}").toString();
    }

    /** Lê uma linha; null se ela estiver incompleta ou inválida (por exemplo, gravação interrompida). */
    public static HistoryRecord fromLine(String line) {
        if (line == null || line.isBlank()) return null;
        try {
            if (!(MiniJson.parse(line.trim()) instanceof Map<?, ?> m)) return null;
            Instant at = Instant.parse((String) m.get("collectedAt"));
            List<VolumeRecord> volumes = new ArrayList<>();
            if (m.get("volumes") instanceof List<?> list) {
                for (Object o : list) {
                    if (o instanceof Map<?, ?> v) {
                        volumes.add(new VolumeRecord((String) v.get("mountPoint"), asLong(v.get("totalBytes")),
                                asLong(v.get("freeBytes"))));
                    }
                }
            }
            return new HistoryRecord(at, asDouble(m.get("cpuPercent")), asDouble(m.get("ramUsedPercent")),
                    asDouble(m.get("commitPercent")), asDouble(m.get("swapUsedPercent")),
                    asLong(m.get("uptimeSeconds")), volumes);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static StringBuilder field(StringBuilder sb, String name, String value) {
        sb.append('"').append(name).append("\":");
        if (value == null) return sb.append("null");
        sb.append('"');
        for (char c : value.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) sb.append(String.format(Locale.ROOT, "\\u%04x", (int) c));
                    else sb.append(c);
                }
            }
        }
        return sb.append('"');
    }

    private static StringBuilder number(StringBuilder sb, String name, Number value) {
        sb.append('"').append(name).append("\":");
        boolean valid = value != null && !(value instanceof Double d && (d.isNaN() || d.isInfinite()));
        return sb.append(valid ? value.toString() : "null");
    }

    private static Double asDouble(Object o) {
        return o instanceof Number n ? n.doubleValue() : null;
    }

    private static Long asLong(Object o) {
        return o instanceof Number n ? n.longValue() : null;
    }
}
