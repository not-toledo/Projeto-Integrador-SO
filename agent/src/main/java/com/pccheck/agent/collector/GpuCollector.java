package com.pccheck.agent.collector;

import com.pccheck.agent.model.GpuInfo;
import java.util.ArrayList;
import java.util.List;
import oshi.SystemInfo;
import oshi.hardware.GraphicsCard;

/** Coleta as placas de vídeo identificadas pelo sistema. Telemetria (uso/VRAM usada) fica para a V0.7. */
public class GpuCollector implements Collector<List<GpuInfo>> {

    private final SystemInfo systemInfo;

    public GpuCollector(SystemInfo systemInfo) {
        this.systemInfo = systemInfo;
    }

    @Override
    public String name() {
        return "GpuCollector";
    }

    @Override
    public List<GpuInfo> collect() {
        List<GpuInfo> result = new ArrayList<>();
        for (GraphicsCard card : systemInfo.getHardware().getGraphicsCards()) {
            GpuInfo info = toInfo(card.getName(), card.getVendor(), card.getDeviceId(), card.getVRam());
            if (info != null) {
                result.add(info);
            }
        }
        return result.isEmpty() ? null : result;
    }

    /** Conversão pura (testável): null quando nenhum dado útil foi informado. */
    static GpuInfo toInfo(String name, String vendor, String deviceId, long vramBytes) {
        String n = Metrics.cleanText(name);
        String v = Metrics.cleanText(vendor);
        String d = Metrics.cleanText(deviceId);
        Long vram = Metrics.positiveOrNull(vramBytes);
        if (n == null && v == null && d == null && vram == null) {
            return null;
        }
        return new GpuInfo(n, v, d, vram);
    }
}
