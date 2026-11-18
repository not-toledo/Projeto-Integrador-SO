package com.pccheck.agent;

import com.pccheck.agent.collector.SystemCollector;
import com.pccheck.agent.history.HistoryStore;
import com.pccheck.agent.history.ScheduledCollection;
import com.pccheck.agent.model.SystemSnapshot;
import java.time.ZoneId;
import oshi.SystemInfo;

/**
 * Ponto de entrada do agente local do PCCheck.
 *
 * Sem argumentos: coleta SO, CPU, memoria, GPU, placa-mae e armazenamento (via OSHI) e imprime o resultado.
 * --registrar-coleta: grava uma coleta no historico (usado pela tarefa agendada; ver scripts/agendar-coleta).
 * --onde: mostra a pasta do historico. Os dados sao lidos do sistema, nunca simulados.
 */
public final class PcCheckAgent {

    public static final String VERSION = "1.0.0";

    private PcCheckAgent() {
    }

    public static void main(String[] args) {
        if (args.length > 0) {
            switch (args[0]) {
                case "--registrar-coleta" -> {
                    // Usado pela tarefa agendada: uma coleta gravada no historico; o codigo de saida vai para o agendador.
                    System.exit(ScheduledCollection.run(System.out, System.err));
                    return;
                }
                case "--onde" -> {
                    System.out.println(new HistoryStore(ScheduledCollection.dataDir(), ZoneId.systemDefault()).directory());
                    return;
                }
                default -> {
                    System.err.println("Opcao desconhecida: " + args[0]);
                    System.err.println("Uso: java -jar pccheck-agent.jar [--registrar-coleta | --onde]");
                    System.exit(2);
                    return;
                }
            }
        }
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
