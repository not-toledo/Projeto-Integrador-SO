package com.pccheck.dto;

import com.pccheck.monitor.MonitorStatus;
import java.util.List;

/** Resposta de GET /api/live/history: amostras da mais antiga para a mais recente. */
public record LiveHistoryDto(String source, MonitorStatus status, List<LiveSampleDto> samples) {
}
