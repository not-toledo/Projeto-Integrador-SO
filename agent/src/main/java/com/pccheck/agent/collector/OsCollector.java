package com.pccheck.agent.collector;

import com.pccheck.agent.model.OsInfo;
import oshi.SystemInfo;
import oshi.software.os.OperatingSystem;

/** Coleta nome, versão, arquitetura e tempo de atividade do sistema operacional. */
public class OsCollector implements Collector<OsInfo> {

    private final SystemInfo systemInfo;

    public OsCollector(SystemInfo systemInfo) {
        this.systemInfo = systemInfo;
    }

    @Override
    public String name() {
        return "OsCollector";
    }

    @Override
    public OsInfo collect() {
        OperatingSystem os = systemInfo.getOperatingSystem();
        OperatingSystem.OSVersionInfo versionInfo = os.getVersionInfo();
        return new OsInfo(
                Metrics.cleanText(os.getFamily()),
                Metrics.cleanText(versionInfo.getVersion()),
                Metrics.cleanText(versionInfo.getBuildNumber()),
                Metrics.cleanText(System.getProperty("os.arch")),
                Metrics.positiveOrNull(os.getSystemUptime()));
    }
}
