package com.pccheck.agent;

import com.pccheck.agent.model.CollectorResult;
import com.pccheck.agent.model.CollectorStatus;
import com.pccheck.agent.model.CpuInfo;
import com.pccheck.agent.model.MemoryInfo;
import com.pccheck.agent.model.OsInfo;
import com.pccheck.agent.model.SystemSnapshot;
import java.util.Locale;

/**
 * Formata o resultado da coleta como texto. Usa apenas ASCII para nao depender
 * da codificacao do console. Dados ausentes aparecem como "Nao disponivel".
 */
public final class SnapshotPrinter {

    static final String UNAVAILABLE = "Nao disponivel";
    private static final double GIB = 1024.0 * 1024.0 * 1024.0;

    private SnapshotPrinter() {
    }

    public static String format(SystemSnapshot snapshot) {
        StringBuilder sb = new StringBuilder();
        sb.append("Coleta em: ").append(snapshot.collectedAt()).append(System.lineSeparator());

        section(sb, "Sistema operacional", snapshot.os());
        OsInfo os = snapshot.os().data();
        if (os != null) {
            line(sb, "Nome", os.name());
            line(sb, "Versao", os.version());
            line(sb, "Build", os.buildNumber());
            line(sb, "Arquitetura", os.architecture());
            line(sb, "Tempo de atividade", duration(os.uptimeSeconds()));
        }

        section(sb, "CPU", snapshot.cpu());
        CpuInfo cpu = snapshot.cpu().data();
        if (cpu != null) {
            line(sb, "Modelo", cpu.name());
            line(sb, "Fabricante", cpu.vendor());
            line(sb, "Nucleos fisicos", cpu.physicalCores() == null ? null : cpu.physicalCores().toString());
            line(sb, "Processadores logicos", cpu.logicalProcessors() == null ? null : cpu.logicalProcessors().toString());
            line(sb, "Frequencia nominal", ghz(cpu.nominalFrequencyHz()));
            line(sb, "Frequencia maxima", ghz(cpu.maxFrequencyHz()));
            line(sb, "Uso atual", percent(cpu.usagePercent()));
        }

        section(sb, "Memoria", snapshot.memory());
        MemoryInfo mem = snapshot.memory().data();
        if (mem != null) {
            line(sb, "Total", gib(mem.totalBytes()));
            line(sb, "Usada", gib(mem.usedBytes()));
            line(sb, "Disponivel", gib(mem.availableBytes()));
            line(sb, "Utilizacao", percent(mem.usedPercent()));
        }
        return sb.toString();
    }

    private static void section(StringBuilder sb, String title, CollectorResult<?> result) {
        sb.append(System.lineSeparator()).append("[").append(title).append("]");
        if (result.status() != CollectorStatus.OK) {
            String label = result.status() == CollectorStatus.ERROR ? "ERRO" : UNAVAILABLE;
            sb.append(" ").append(label).append(" - ").append(result.message());
        }
        sb.append(System.lineSeparator());
    }

    private static void line(StringBuilder sb, String label, String value) {
        sb.append("  ").append(label).append(": ").append(value == null ? UNAVAILABLE : value)
                .append(System.lineSeparator());
    }

    static String gib(Long bytes) {
        return bytes == null ? null : String.format(Locale.ROOT, "%.2f GiB", bytes / GIB);
    }

    static String ghz(Long hz) {
        return hz == null ? null : String.format(Locale.ROOT, "%.2f GHz", hz / 1_000_000_000.0);
    }

    static String percent(Double value) {
        return value == null ? null : String.format(Locale.ROOT, "%.1f%%", value);
    }

    static String duration(Long seconds) {
        if (seconds == null) {
            return null;
        }
        long d = seconds / 86400;
        long h = (seconds % 86400) / 3600;
        long m = (seconds % 3600) / 60;
        return d + "d " + h + "h " + m + "min";
    }
}
