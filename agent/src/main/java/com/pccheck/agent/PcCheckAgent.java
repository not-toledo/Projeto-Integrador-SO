package com.pccheck.agent;

import com.pccheck.agent.collector.SystemCollector;
import com.pccheck.agent.model.SystemSnapshot;
import oshi.SystemInfo;

/**
 * Ponto de entrada do agente local do PCCheck.
 *
 * V0.3: coleta SO, CPU, memoria, GPU, placa-mae e armazenamento do computador local (via OSHI)
 * e imprime o resultado. Os dados sao lidos do sistema, nunca simulados.
 */
public final class PcCheckAgent {

    public static final String VERSION = "0.8.0";

    private PcCheckAgent() {
    }

    public static void main(String[] args) {
        System.out.println("PCCheck Agent v" + VERSION);
        System.out.println("Java: " + System.getProperty("java.version")
                + " (" + System.getProperty("java.vendor") + ")");

        SystemInfo systemInfo;
        try {
            systemInfo = new SystemInfo();
        } catch (RuntimeException | LinkageError e) {
            System.err.println("Falha ao inicializar a biblioteca de coleta (OSHI): " + e);
            System.exit(1);
            return;
        }

        SystemSnapshot snapshot = new SystemCollector(systemInfo).collect();
        System.out.print(SnapshotPrinter.format(snapshot));
    }
}
