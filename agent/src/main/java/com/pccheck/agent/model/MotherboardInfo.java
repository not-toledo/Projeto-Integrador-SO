package com.pccheck.agent.model;

/**
 * Placa-mãe e identificação do computador. Campo nulo = não disponível.
 * Textos genéricos de fabricantes (ex.: "To be filled by O.E.M.") são descartados.
 *
 * @param manufacturer       fabricante da placa-mãe
 * @param model              modelo da placa-mãe
 * @param version            versão da placa-mãe
 * @param serialNumber       número de série da placa-mãe
 * @param systemManufacturer fabricante do computador (ex.: notebook de marca)
 * @param systemModel        modelo do computador
 */
public record MotherboardInfo(String manufacturer, String model, String version, String serialNumber,
                              String systemManufacturer, String systemModel) {
}
