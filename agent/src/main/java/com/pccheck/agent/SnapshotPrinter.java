package com.pccheck.agent;

import com.pccheck.agent.model.CollectorResult;
import com.pccheck.agent.model.CollectorStatus;
import com.pccheck.agent.model.CpuInfo;
import com.pccheck.agent.model.DiskInfo;
import com.pccheck.agent.model.GpuInfo;
import com.pccheck.agent.model.MemoryInfo;
import com.pccheck.agent.model.MotherboardInfo;
import com.pccheck.agent.model.OsInfo;
import com.pccheck.agent.model.StorageInfo;
import com.pccheck.agent.model.SystemSnapshot;
import com.pccheck.agent.model.VolumeInfo;
import java.util.List;
import java.util.Locale;

/**
 * Formata o resultado da coleta como texto. Usa apenas ASCII para nao depender
 * da codificacao do console. Dados ausentes aparecem como "Nao disponivel".
 */
public final class SnapshotPrinter {

    static final String UNAVAILABLE = "Nao disponivel";
    static final String NOT_REPORTED = "Nao informado pelo fabricante";
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

        section(sb, "Placa de video", snapshot.gpu());
        List<GpuInfo> gpus = snapshot.gpu().data();
        if (gpus != null) {
            int i = 1;
            for (GpuInfo gpu : gpus) {
                sb.append("  Placa ").append(i++).append(":").append(System.lineSeparator());
                line(sb, "  Nome", gpu.name());
                line(sb, "  Fabricante", gpu.vendor());
                line(sb, "  ID do dispositivo", gpu.deviceId());
                line(sb, "  VRAM informada", gib(gpu.vramBytes()));
            }
        }

        section(sb, "Placa-mae", snapshot.motherboard());
        MotherboardInfo board = snapshot.motherboard().data();
        if (board != null) {
            boardLine(sb, board, "Fabricante", "manufacturer", board.manufacturer());
            boardLine(sb, board, "Modelo", "model", board.model());
            boardLine(sb, board, "Versao", "version", board.version());
            boardLine(sb, board, "Numero de serie", "serialNumber", board.serialNumber());
            boardLine(sb, board, "Computador (fabricante)", "systemManufacturer", board.systemManufacturer());
            boardLine(sb, board, "Computador (modelo)", "systemModel", board.systemModel());
        }

        section(sb, "Armazenamento", snapshot.storage());
        StorageInfo storage = snapshot.storage().data();
        if (storage != null) {
            appendDrives(sb, storage.drives());
            appendVolumes(sb, storage.volumes());
        }
        return sb.toString();
    }

    private static void appendDrives(StringBuilder sb, List<DiskInfo> drives) {
        if (drives == null) {
            line(sb, "Unidades fisicas", null);
            return;
        }
        sb.append("  Unidades fisicas:").append(System.lineSeparator());
        int i = 1;
        for (DiskInfo d : drives) {
            sb.append("    Unidade ").append(i++).append(":").append(System.lineSeparator());
            line(sb, "    Nome", d.name());
            line(sb, "    Modelo", d.model());
            line(sb, "    Capacidade", gib(d.sizeBytes()));
        }
    }

    private static void appendVolumes(StringBuilder sb, List<VolumeInfo> volumes) {
        if (volumes == null) {
            line(sb, "Volumes", null);
            return;
        }
        sb.append("  Volumes:").append(System.lineSeparator());
        int i = 1;
        for (VolumeInfo v : volumes) {
            sb.append("    Volume ").append(i++).append(":").append(System.lineSeparator());
            line(sb, "    Ponto de montagem", v.mountPoint());
            line(sb, "    Rotulo", v.label());
            line(sb, "    Sistema de arquivos", v.fileSystem());
            line(sb, "    Capacidade", gib(v.totalBytes()));
            line(sb, "    Livre", gib(v.freeBytes()));
            line(sb, "    Usado", gib(v.usedBytes()));
            line(sb, "    Utilizacao", percent(v.usedPercent()));
        }
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

    /** Campo da placa-mae: diferencia "nao informado pelo fabricante" (texto generico) de "nao disponivel". */
    private static void boardLine(StringBuilder sb, MotherboardInfo board, String label, String field, String value) {
        line(sb, label, value == null && board.notReportedFields().contains(field) ? NOT_REPORTED : value);
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
