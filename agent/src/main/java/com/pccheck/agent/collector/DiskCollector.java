package com.pccheck.agent.collector;

import com.pccheck.agent.model.DiskInfo;
import com.pccheck.agent.model.StorageInfo;
import com.pccheck.agent.model.VolumeInfo;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import oshi.PlatformEnum;
import oshi.SystemInfo;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HWPartition;
import oshi.software.os.OSFileStore;
import oshi.util.Memoizer;

/**
 * Coleta unidades físicas e volumes montados (com espaço livre).
 * Falha em uma das partes não impede a outra; só falha por inteiro se ambas falharem.
 */
public class DiskCollector implements Collector<StorageInfo> {

    private static final Path SYS_BLOCK = Path.of("/sys/block");
    /** Discos podem ser conectados e removidos: o mapa do WMI é reaproveitado por 60 s. */
    private static final long MEDIA_CACHE_NANOS = 60_000_000_000L;

    private final SystemInfo systemInfo;
    private final Supplier<Map<String, DiskMediaTypes.Media>> windowsMedia =
            Memoizer.memoize(DiskMediaTypes::queryWindows, MEDIA_CACHE_NANOS);

    public DiskCollector(SystemInfo systemInfo) {
        this.systemInfo = systemInfo;
    }

    @Override
    public String name() {
        return "DiskCollector";
    }

    @Override
    public StorageInfo collect() {
        List<DiskInfo> drives = null;
        List<VolumeInfo> volumes = null;
        RuntimeException failure = null;

        try {
            drives = readDrives();
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
        return new StorageInfo(drives, volumes);
    }

    private List<DiskInfo> readDrives() {
        List<DiskInfo> result = new ArrayList<>();
        for (HWDiskStore disk : systemInfo.getHardware().getDiskStores()) {
            DiskMediaTypes.Media media = mediaOf(disk.getName());
            DiskInfo info = toDisk(disk.getName(), disk.getModel(), disk.getSize(), media.mediaType(), media.busType(),
                    mountPoints(disk));
            if (info != null) {
                result.add(info);
            }
        }
        return result.isEmpty() ? null : result;
    }

    private List<VolumeInfo> readVolumes() {
        List<VolumeInfo> result = new ArrayList<>();
        // true = somente volumes locais (exclui unidades de rede)
        for (OSFileStore store : systemInfo.getOperatingSystem().getFileSystem().getFileStores(true)) {
            VolumeInfo info = toVolume(store.getMount(), store.getLabel(), store.getType(),
                    store.getTotalSpace(), store.getUsableSpace());
            if (info != null) {
                result.add(info);
            }
        }
        return result.isEmpty() ? null : result;
    }

    /** Tipo e barramento informados pelo SO; nulos quando não informados ou se a leitura falhar. */
    private DiskMediaTypes.Media mediaOf(String name) {
        try {
            if (SystemInfo.getCurrentPlatform() == PlatformEnum.WINDOWS) {
                String number = DiskMediaTypes.windowsDriveNumber(name);
                DiskMediaTypes.Media media = number == null ? null : windowsMedia.get().get(number);
                return media == null ? new DiskMediaTypes.Media(null, null) : media;
            }
            if (SystemInfo.getCurrentPlatform() == PlatformEnum.LINUX) {
                return DiskMediaTypes.linux(name, SYS_BLOCK);
            }
        } catch (RuntimeException e) {
            // O tipo é complementar: sem ele, o disco continua listado.
        }
        return new DiskMediaTypes.Media(null, null);
    }

    /** Pontos de montagem das partições (Windows: "C:\"; Linux: "/", "/home"). Vazio se a leitura falhar. */
    private static List<String> mountPoints(HWDiskStore disk) {
        try {
            List<String> result = new ArrayList<>();
            for (HWPartition partition : disk.getPartitions()) {
                String mount = Metrics.cleanText(partition.getMountPoint());
                if (mount != null && !result.contains(mount)) {
                    result.add(mount);
                }
            }
            return result;
        } catch (RuntimeException e) {
            return List.of();
        }
    }

    /** Conversão pura (testável). */
    static DiskInfo toDisk(String name, String model, long sizeBytes) {
        return toDisk(name, model, sizeBytes, null, null, List.of());
    }

    static DiskInfo toDisk(String name, String model, long sizeBytes, String mediaType, String busType) {
        return toDisk(name, model, sizeBytes, mediaType, busType, List.of());
    }

    static DiskInfo toDisk(String name, String model, long sizeBytes, String mediaType, String busType,
                           List<String> mountPoints) {
        String n = Metrics.cleanText(name);
        String m = Metrics.cleanText(model);
        Long size = Metrics.positiveOrNull(sizeBytes);
        if (n == null && m == null && size == null) {
            return null;
        }
        return new DiskInfo(n, m, size, mediaType, busType, mountPoints);
    }

    /** Conversão pura (testável): volumes sem capacidade válida são ignorados. */
    static VolumeInfo toVolume(String mount, String label, String fileSystem, long totalBytes, long usableBytes) {
        Long total = Metrics.positiveOrNull(totalBytes);
        if (total == null) {
            return null;
        }
        Long free = usableBytes >= 0 ? usableBytes : null;
        Long used = Metrics.usedBytes(total, free);
        return new VolumeInfo(Metrics.cleanText(mount), Metrics.cleanText(label), Metrics.cleanText(fileSystem),
                total, free, used, Metrics.percent(used, total));
    }
}
