package com.pccheck.agent.model;

import java.util.List;

/**
 * Uso de CPU entre duas amostras. Campo nulo = sem leitura.
 *
 * @param totalPercent   uso total (0 a 100)
 * @param perCorePercent uso por processador lógico (0 a 100); elemento nulo = sem leitura daquele núcleo; lista nula = não disponível
 */
public record CpuLive(Double totalPercent, List<Double> perCorePercent) {

    public CpuLive {
        perCorePercent = perCorePercent == null ? null : java.util.Collections.unmodifiableList(new java.util.ArrayList<>(perCorePercent));
    }
}
