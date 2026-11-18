package com.pccheck.precheck;

import com.pccheck.precheck.PrecheckDto.ProcessGroup;
import java.util.List;

/**
 * Estado do sistema num instante. Campo nulo = não lido (o motivo fica no campo de erro correspondente).
 *
 * @param systemCpuPercent uso total da CPU no intervalo medido
 * @param processes        grupos de processos (sem o próprio PCCheck nem o processo ocioso do sistema)
 * @param onBattery        true só quando uma bateria informa que está descarregando
 */
public record SystemState(Long totalRamBytes, Long availableRamBytes, Double systemCpuPercent,
                          List<ProcessGroup> processes, String processError, PowerPlan powerPlan, Boolean onBattery,
                          Double batteryPercent) {

    public SystemState {
        processes = processes == null ? List.of() : List.copyOf(processes);
    }

    /**
     * Plano de energia (Windows) ou governador da CPU (Linux).
     *
     * @param kind  {@code POWER_SAVER}, {@code BALANCED}, {@code HIGH_PERFORMANCE}, {@code OTHER} (plano
     *              personalizado ou do fabricante), {@code LINUX_GOVERNOR} ou {@code UNAVAILABLE}
     * @param name  nome informado pelo SO (ex.: "Balanced", "powersave")
     * @param error motivo, quando {@code UNAVAILABLE}
     */
    public record PowerPlan(String kind, String name, String error) {
    }
}
