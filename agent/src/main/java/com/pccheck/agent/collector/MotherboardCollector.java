package com.pccheck.agent.collector;

import com.pccheck.agent.model.MotherboardInfo;
import oshi.SystemInfo;
import oshi.hardware.Baseboard;
import oshi.hardware.ComputerSystem;

/** Coleta fabricante, modelo e identificadores da placa-mãe e do computador. */
public class MotherboardCollector implements Collector<MotherboardInfo> {

    private final SystemInfo systemInfo;

    public MotherboardCollector(SystemInfo systemInfo) {
        this.systemInfo = systemInfo;
    }

    @Override
    public String name() {
        return "MotherboardCollector";
    }

    @Override
    public MotherboardInfo collect() {
        ComputerSystem system = systemInfo.getHardware().getComputerSystem();
        Baseboard board = system.getBaseboard();
        return build(
                board == null ? null : board.getManufacturer(),
                board == null ? null : board.getModel(),
                board == null ? null : board.getVersion(),
                board == null ? null : board.getSerialNumber(),
                system.getManufacturer(),
                system.getModel());
    }

    /** Conversão pura (testável): descarta textos genéricos e retorna null se nada restar. */
    static MotherboardInfo build(String manufacturer, String model, String version, String serial,
                                 String systemManufacturer, String systemModel) {
        String m = Metrics.cleanOemText(manufacturer);
        String mo = Metrics.cleanOemText(model);
        String ver = Metrics.cleanOemText(version);
        String ser = Metrics.cleanOemText(serial);
        String sm = Metrics.cleanOemText(systemManufacturer);
        String smo = Metrics.cleanOemText(systemModel);
        if (m == null && mo == null && ver == null && ser == null && sm == null && smo == null) {
            return null;
        }
        return new MotherboardInfo(m, mo, ver, ser, sm, smo);
    }
}
