package com.pccheck.service;

import com.pccheck.agent.collector.SystemCollector;
import com.pccheck.agent.model.CollectorResult;
import com.pccheck.agent.model.SystemSnapshot;
import com.pccheck.dto.InventoryDto;
import java.time.Instant;
import oshi.SystemInfo;

import org.springframework.stereotype.Service;

/**
 * Executa a coleta do inventário usando os coletores do agente, no mesmo processo.
 * A inicialização do OSHI é tardia: se falhar, a API responde com ERROR em todos os componentes
 * (e tenta de novo na próxima chamada) em vez de impedir o backend de subir.
 */
@Service
public class InventoryService {

    private SystemCollector collector;

    /** Sincronizado para que requisições simultâneas não disparem várias coletas ao mesmo tempo. */
    public synchronized InventoryDto collect() {
        if (collector == null) {
            try {
                collector = new SystemCollector(new SystemInfo());
            } catch (RuntimeException | LinkageError e) {
                return InventoryDto.from(initializationFailure(e));
            }
        }
        return InventoryDto.from(collector.collect());
    }

    static SystemSnapshot initializationFailure(Throwable cause) {
        String message = "Falha ao inicializar a biblioteca de coleta (OSHI): " + cause;
        return new SystemSnapshot(
                Instant.now(),
                CollectorResult.error("OsCollector", message),
                CollectorResult.error("CpuCollector", message),
                CollectorResult.error("RamCollector", message),
                CollectorResult.error("GpuCollector", message),
                CollectorResult.error("MotherboardCollector", message),
                CollectorResult.error("DiskCollector", message));
    }
}
