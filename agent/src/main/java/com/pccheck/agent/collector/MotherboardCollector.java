package com.pccheck.agent.collector;

import com.pccheck.agent.model.MotherboardInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import oshi.PlatformEnum;
import oshi.SystemInfo;
import oshi.hardware.Baseboard;
import oshi.hardware.ComputerSystem;
import oshi.util.Memoizer;

/** Coleta fabricante, modelo e identificadores da placa-mãe e do computador. */
public class MotherboardCollector implements Collector<MotherboardInfo> {

    private final SystemInfo systemInfo;
    /** Modelo da placa lido por outra interface do SO; consultado uma única vez (a placa não muda). */
    private final Supplier<String> boardProduct;

    public MotherboardCollector(SystemInfo systemInfo) {
        this(systemInfo, SystemInfo.getCurrentPlatform() == PlatformEnum.WINDOWS
                ? Memoizer.memoize(WindowsBaseboardProduct::query)
                : () -> null);
    }

    MotherboardCollector(SystemInfo systemInfo, Supplier<String> boardProduct) {
        this.systemInfo = systemInfo;
        this.boardProduct = boardProduct;
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
                boardModel(board == null ? null : board.getModel(), boardProduct),
                board == null ? null : board.getVersion(),
                board == null ? null : board.getSerialNumber(),
                system.getManufacturer(),
                system.getModel());
    }

    /**
     * Modelo da placa: o valor do OSHI quando válido; senão o lido por {@code product} (no Windows,
     * Win32_BaseBoard.Product). O modelo do computador nunca é usado aqui.
     */
    static String boardModel(String oshiModel, Supplier<String> product) {
        if (Metrics.cleanOemText(oshiModel) != null) {
            return oshiModel;
        }
        String fallback = product.get();
        return Metrics.cleanText(fallback) != null ? fallback : oshiModel;
    }

    /**
     * Conversão pura (testável): descarta textos genéricos, registrando-os como "não informados pelo fabricante",
     * e retorna null se nenhum dado real restar.
     */
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
        List<String> notReported = new ArrayList<>();
        addIfPlaceholder(notReported, "manufacturer", manufacturer);
        addIfPlaceholder(notReported, "model", model);
        addIfPlaceholder(notReported, "version", version);
        addIfPlaceholder(notReported, "serialNumber", serial);
        addIfPlaceholder(notReported, "systemManufacturer", systemManufacturer);
        addIfPlaceholder(notReported, "systemModel", systemModel);
        return new MotherboardInfo(m, mo, ver, ser, sm, smo, notReported);
    }

    private static void addIfPlaceholder(List<String> fields, String field, String raw) {
        if (Metrics.isOemPlaceholder(raw)) {
            fields.add(field);
        }
    }
}
