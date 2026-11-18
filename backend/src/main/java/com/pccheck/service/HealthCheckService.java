package com.pccheck.service;

import com.pccheck.agent.collector.CollectorRunner;
import com.pccheck.agent.collector.DiskHealthCollector;
import com.pccheck.agent.collector.SensorCollector;
import com.pccheck.agent.collector.SystemCollector;
import com.pccheck.agent.model.CollectorResult;
import com.pccheck.agent.model.SensorInfo;
import com.pccheck.agent.model.SystemSnapshot;
import com.pccheck.agent.sensors.SensorSource;
import com.pccheck.diagnostic.DiskHealthThresholds;
import com.pccheck.diagnostic.HealthEvaluator;
import com.pccheck.diagnostic.HealthInput;
import com.pccheck.diagnostic.HealthReport;
import com.pccheck.diagnostic.HealthThresholds;
import com.pccheck.diagnostic.MemoryPressureThresholds;
import java.time.Instant;
import oshi.SystemInfo;

import org.springframework.stereotype.Service;

/**
 * Coleta os dados reais e executa o Health Check (Módulo B). Inicialização tardia do OSHI, como no
 * inventário: se falhar, o relatório informa dados indisponíveis em vez de impedir o backend de subir.
 */
@Service
public class HealthCheckService {

    private final HealthEvaluator evaluator;
    private final SensorSource sensors;
    private final LiveMonitorService liveMonitor;
    private final int pagingWindow;
    private SystemCollector collector;
    private SensorCollector sensorCollector;

    public HealthCheckService(HealthThresholds thresholds, MemoryPressureThresholds memoryPressure,
                              DiskHealthThresholds diskHealth, SensorSource sensors, LiveMonitorService liveMonitor) {
        this.evaluator = new HealthEvaluator(thresholds, memoryPressure, diskHealth);
        this.sensors = sensors;
        this.liveMonitor = liveMonitor;
        this.pagingWindow = memoryPressure.windowSamples();
    }

    /** Sincronizado para que requisições simultâneas não disparem várias coletas ao mesmo tempo. */
    public synchronized HealthReport check() {
        if (collector == null) {
            try {
                SystemInfo systemInfo = new SystemInfo();
                collector = new SystemCollector(systemInfo);
                sensorCollector = new SensorCollector(systemInfo, sensors);
            } catch (RuntimeException | LinkageError e) {
                collector = null;
                sensorCollector = null;
                SystemSnapshot failed = InventoryService.initializationFailure(e);
                return evaluator.evaluate(new HealthInput(failed,
                        CollectorResult.error("SensorCollector", "Falha ao inicializar a coleta (OSHI).")),
                        Instant.now());
            }
        }
        SystemSnapshot snapshot = collector.collect();
        CollectorResult<SensorInfo> sensors = CollectorRunner.run(sensorCollector);
        // A paginação é uma taxa: vem das amostras recentes do Live Monitor, não de uma leitura isolada.
        return evaluator.evaluate(new HealthInput(snapshot, sensors, liveMonitor.recentPaging(pagingWindow),
                CollectorRunner.run(new DiskHealthCollector(this.sensors))), Instant.now());
    }
}
