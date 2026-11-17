package com.pccheck.diagnostic;

import com.pccheck.agent.model.CollectorResult;
import com.pccheck.agent.model.SensorInfo;
import com.pccheck.agent.model.SystemSnapshot;

/** Dados coletados que alimentam as regras de diagnóstico. */
public record HealthInput(SystemSnapshot snapshot, CollectorResult<SensorInfo> sensors) {
}
