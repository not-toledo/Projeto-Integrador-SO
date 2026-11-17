package com.pccheck.agent.model;

import java.util.List;

/**
 * Placa-mãe e identificação do computador. Campo nulo = não disponível.
 * Textos genéricos de fabricantes (ex.: "To be filled by O.E.M.") são descartados e o nome do campo vai
 * para {@code notReportedFields}: o firmware tem o campo, mas o fabricante não o preencheu.
 *
 * <p>Os dados do computador (systemManufacturer/systemModel) vêm de outra tabela do firmware e não são
 * usados para preencher os da placa-mãe: em PCs de marca e notebooks eles são diferentes.
 *
 * @param manufacturer       fabricante da placa-mãe
 * @param model              modelo da placa-mãe
 * @param version            versão (revisão) da placa-mãe
 * @param serialNumber       número de série da placa-mãe
 * @param systemManufacturer fabricante do computador (ex.: notebook de marca)
 * @param systemModel        modelo do computador
 * @param notReportedFields  nomes dos campos acima que o fabricante deixou com texto genérico
 */
public record MotherboardInfo(String manufacturer, String model, String version, String serialNumber,
                              String systemManufacturer, String systemModel, List<String> notReportedFields) {

    public MotherboardInfo {
        notReportedFields = notReportedFields == null ? List.of() : List.copyOf(notReportedFields);
    }

    /** Sem campos com texto genérico. */
    public MotherboardInfo(String manufacturer, String model, String version, String serialNumber,
                           String systemManufacturer, String systemModel) {
        this(manufacturer, model, version, serialNumber, systemManufacturer, systemModel, List.of());
    }
}
