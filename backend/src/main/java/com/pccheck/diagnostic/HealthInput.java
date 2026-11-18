package com.pccheck.diagnostic;

import com.pccheck.agent.model.CollectorResult;
import com.pccheck.agent.model.DiskHealthInfo;
import com.pccheck.agent.model.PagingActivity;
import com.pccheck.agent.model.SensorInfo;
import com.pccheck.agent.model.SystemSnapshot;
import java.util.List;

/**
 * Dados coletados que alimentam as regras de diagnóstico.
 *
 * @param recentPaging atividade de paginação das amostras recentes do Live Monitor (mais antiga primeiro);
 *                     vazia se o monitor ainda não coletou
 * @param diskHealth   saúde dos discos (SMART); null = não coletada
 */
public record HealthInput(SystemSnapshot snapshot, CollectorResult<SensorInfo> sensors,
                          List<PagingActivity> recentPaging, CollectorResult<DiskHealthInfo> diskHealth) {

    public HealthInput {
        recentPaging = recentPaging == null ? List.of() : List.copyOf(recentPaging);
    }

    public HealthInput(SystemSnapshot snapshot, CollectorResult<SensorInfo> sensors) {
        this(snapshot, sensors, List.of(), null);
    }

    public HealthInput(SystemSnapshot snapshot, CollectorResult<SensorInfo> sensors, List<PagingActivity> recentPaging) {
        this(snapshot, sensors, recentPaging, null);
    }
}
