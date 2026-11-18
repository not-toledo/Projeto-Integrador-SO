package com.pccheck.agent.model;

/**
 * Indicadores de saúde de um disco lidos do SMART pelo leitor de sensores. Campo nulo = o disco ou o controlador
 * não informou (por exemplo, HDDs não têm "vida útil restante").
 *
 * @param name             modelo informado pelo disco
 * @param physicalIndex    número do disco no SO (\\.\PHYSICALDRIVEn), para ligar ao inventário; null se desconhecido
 * @param temperatureCelsius temperatura do disco
 * @param lifePercent      vida útil restante em % (SSDs; 100 = novo)
 * @param dataWrittenGb    total gravado ao longo da vida, em GB, como informado pelo disco
 * @param powerOnHours     horas ligado ao longo da vida
 * @param powerOnCount     quantidade de vezes que foi ligado
 */
public record DiskHealth(String name, Integer physicalIndex, Double temperatureCelsius, Double lifePercent,
                         Double dataWrittenGb, Long powerOnHours, Long powerOnCount) {
}
