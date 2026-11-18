package com.pccheck.agent.collector;

import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiQuery;
import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import oshi.util.platform.windows.WmiQueryHandler;
import oshi.util.platform.windows.WmiUtil;

/**
 * Tipo de mídia (SSD/HDD) e barramento de cada disco físico, informados pelo sistema operacional.
 * O OSHI não fornece esses dados; nada é deduzido do nome do modelo.
 *
 * <ul>
 *   <li><b>Windows:</b> WMI {@code MSFT_PhysicalDisk} (namespace ROOT\Microsoft\Windows\Storage), que não exige
 *       administrador. {@code DeviceId} é o número de {@code \\.\PHYSICALDRIVEn}.</li>
 *   <li><b>Linux:</b> {@code /sys/block/<disco>/queue/rotational} (0 = sem partes móveis, 1 = rotacional).</li>
 * </ul>
 */
final class DiskMediaTypes {

    /** Tipo e barramento de um disco; campos nulos quando o SO não informa. */
    record Media(String mediaType, String busType) {
    }

    private enum Property { DEVICEID, MEDIATYPE, BUSTYPE }

    private static final WmiQuery<Property> QUERY =
            new WmiQuery<>("ROOT\\Microsoft\\Windows\\Storage", "MSFT_PhysicalDisk", Property.class);
    private static final Pattern WINDOWS_DRIVE = Pattern.compile("(?i)^\\\\\\\\\\.\\\\PHYSICALDRIVE(\\d+)$");

    private DiskMediaTypes() {
    }

    /** Windows: consulta única ao WMI; mapa vazio se a consulta falhar (tipo fica não disponível). */
    static Map<String, Media> queryWindows() {
        Map<String, Media> result = new HashMap<>();
        try {
            WmiResult<Property> r = WmiQueryHandler.createInstance().queryWMI(QUERY);
            for (int i = 0; i < r.getResultCount(); i++) {
                String id = WmiUtil.getString(r, Property.DEVICEID, i);
                result.put(id, new Media(windowsMediaType(WmiUtil.getUint16(r, Property.MEDIATYPE, i)),
                        windowsBusType(WmiUtil.getUint16(r, Property.BUSTYPE, i))));
            }
        } catch (RuntimeException | LinkageError e) {
            result.clear();
        }
        return result;
    }

    /** Número do disco em "\\.\PHYSICALDRIVEn", ou null. */
    static String windowsDriveNumber(String name) {
        if (name == null) return null;
        Matcher m = WINDOWS_DRIVE.matcher(name.trim());
        return m.matches() ? m.group(1) : null;
    }

    /** MSFT_PhysicalDisk.MediaType: 3 = HDD, 4 = SSD; 0 (não especificado), 5 (SCM) e outros = null. */
    static String windowsMediaType(int code) {
        return switch (code) {
            case 3 -> "HDD";
            case 4 -> "SSD";
            default -> null;
        };
    }

    /** MSFT_PhysicalDisk.BusType, valores documentados pela Microsoft; desconhecidos = null. */
    static String windowsBusType(int code) {
        return switch (code) {
            case 1 -> "SCSI";
            case 3 -> "ATA";
            case 6 -> "Fibre Channel";
            case 7 -> "USB";
            case 8 -> "RAID";
            case 9 -> "iSCSI";
            case 10 -> "SAS";
            case 11 -> "SATA";
            case 12 -> "SD";
            case 13 -> "MMC";
            case 15 -> "Arquivo de disco virtual";
            case 16 -> "Espaços de Armazenamento";
            case 17 -> "NVMe";
            default -> null;
        };
    }

    /**
     * Linux: tipo pelo /sys/block/&lt;disco&gt;/queue/rotational; barramento só é informado para NVMe.
     * Dispositivos removíveis (pen drives, cartões) também têm rotational = 0, mas não são SSDs: ficam sem tipo.
     */
    static Media linux(String name, Path sysBlock) {
        if (name == null) return new Media(null, null);
        String device = Path.of(name.trim()).getFileName().toString();
        String bus = device.startsWith("nvme") ? "NVMe" : null;
        try {
            Path dir = sysBlock.resolve(device);
            Path removable = dir.resolve("removable");
            if (Files.exists(removable) && "1".equals(Files.readString(removable).trim())) {
                return new Media(null, bus);
            }
            return new Media(linuxMediaType(Files.readString(dir.resolve("queue").resolve("rotational")).trim()), bus);
        } catch (IOException | RuntimeException e) {
            return new Media(null, bus);
        }
    }

    static String linuxMediaType(String rotational) {
        return switch (rotational) {
            case "0" -> "SSD";
            case "1" -> "HDD";
            default -> null;
        };
    }
}
