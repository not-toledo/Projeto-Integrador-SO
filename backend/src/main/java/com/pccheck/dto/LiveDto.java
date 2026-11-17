package com.pccheck.dto;

import com.pccheck.monitor.MonitorStatus;

/** Resposta de GET /api/live: estado da coleta e a amostra mais recente (nula se ainda não houve nenhuma). */
public record LiveDto(String source, MonitorStatus status, LiveSampleDto latest) {

    public static final String SOURCE = "Coleta periódica local via OSHI (dados lidos do proprio computador)";
}
