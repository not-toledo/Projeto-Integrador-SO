package com.pccheck.agent.model;

/**
 * Unidade de armazenamento física. Campo nulo = não disponível.
 *
 * @param name           identificador do SO (ex.: \\.\PHYSICALDRIVE0, /dev/sda)
 * @param model          modelo
 * @param sizeBytes      capacidade
 * @param mediaType      "SSD" ou "HDD", somente quando o SO informa (Windows: MSFT_PhysicalDisk.MediaType;
 *                       Linux: /sys/block/&lt;disco&gt;/queue/rotational); nunca deduzido do nome do modelo
 * @param busType        barramento informado pelo SO (ex.: "SATA", "NVMe", "USB")
 */
public record DiskInfo(String name, String model, Long sizeBytes, String mediaType, String busType) {

    /** Sem tipo de mídia nem barramento. */
    public DiskInfo(String name, String model, Long sizeBytes) {
        this(name, model, sizeBytes, null, null);
    }
}
