package com.pccheck.agent.collector;

import com.pccheck.agent.model.DiskInfo;
import com.pccheck.agent.model.StorageInfo;
import com.pccheck.agent.model.VolumeInfo;
import java.util.ArrayList;
import java.util.List;
import oshi.SystemInfo;
import oshi.hardware.HWDiskStore;
import oshi.software.os.OSFileStore;

/**
 * Coleta unidades físicas e volumes montados (com espaço livre).
 * Falha em uma das partes não impede a outra; só falha por inteiro se ambas falharem.
 */
public class DiskCollector implements Collector<StorageInfo> {

    private final SystemInfo systemInfo;

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
            DiskInfo info = toDisk(disk.getName(), disk.getModel(), disk.getSize());
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

    /** Conversão pura (testável). */
    static DiskInfo toDisk(String name, String model, long sizeBytes) {
        String n = Metrics.cleanText(name);
        String m = Metrics.cleanText(model);
        Long size = Metrics.positiveOrNull(sizeBytes);
        if (n == null && m == null && size == null) {
            return null;
        }
        return new DiskInfo(n, m, size);
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
