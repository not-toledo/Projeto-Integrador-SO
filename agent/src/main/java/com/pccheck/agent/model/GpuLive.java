package com.pccheck.agent.model;

/**
 * Telemetria de GPU em tempo real. Reservado: a biblioteca de coleta atual (OSHI) não fornece
 * utilização nem memória de vídeo em uso, então nenhum valor é produzido nesta versão.
 */
public record GpuLive(Double utilizationPercent, Long vramUsedBytes) {
}
