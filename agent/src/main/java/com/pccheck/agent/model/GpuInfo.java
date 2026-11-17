package com.pccheck.agent.model;

/**
 * Placa de vídeo. Campo nulo = não disponível.
 *
 * @param vramBytes memória de vídeo conforme informada pelo sistema (o OSHI não distingue dedicada de compartilhada)
 */
public record GpuInfo(String name, String vendor, String deviceId, Long vramBytes) {
}
