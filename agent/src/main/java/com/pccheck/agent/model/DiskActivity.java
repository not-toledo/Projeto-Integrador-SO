package com.pccheck.agent.model;

/**
 * Atividade de uma unidade física entre duas amostras. Campo nulo = sem leitura
 * (por exemplo, na primeira amostra ainda não há valor anterior para calcular a taxa).
 *
 * @param busyPercent tempo ocupado (0 a 100), quando o sistema informa o tempo de transferência
 */
public record DiskActivity(String name, Double readBytesPerSecond, Double writeBytesPerSecond, Double busyPercent) {
}
