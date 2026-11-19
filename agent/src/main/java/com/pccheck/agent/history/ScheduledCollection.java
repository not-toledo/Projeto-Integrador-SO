package com.pccheck.agent.history;

import com.pccheck.agent.Env;
import com.pccheck.agent.collector.SystemCollector;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import oshi.SystemInfo;

/**
 * Execução feita pela tarefa agendada (schtasks no Windows, cron no Linux): uma coleta, gravada no histórico,
 * seguida da limpeza dos arquivos antigos. O código de saída informa o resultado ao agendador
 * (0 = sucesso, 1 = falha), que o registra como "Último resultado" da tarefa.
 */
public final class ScheduledCollection {

    public static final String ENV_RETENTION_DAYS = "NEXCORE_RETENCAO_DIAS";

    private ScheduledCollection() {
    }

    /** Dias de retenção: {@value #ENV_RETENTION_DAYS} se for um inteiro de 1 a 365; senão o padrão. */
    public static int retentionDays(Map<String, String> env) {
        String value = Env.get(env, ENV_RETENTION_DAYS);
        if (value == null || value.isBlank()) return HistoryStore.DEFAULT_RETENTION_DAYS;
        try {
            int days = Integer.parseInt(value.trim());
            return days >= 1 && days <= 365 ? days : HistoryStore.DEFAULT_RETENTION_DAYS;
        } catch (NumberFormatException e) {
            return HistoryStore.DEFAULT_RETENTION_DAYS;
        }
    }

    /** Pasta de dados; na primeira vez, copia as coletas da pasta do nome antigo (PCCheck), se houver. */
    public static Path dataDir() {
        Map<String, String> env = System.getenv();
        String os = System.getProperty("os.name");
        String home = System.getProperty("user.home");
        Path dir = HistoryStore.defaultDataDir(env, os, home);
        try {
            HistoryStore.migrateLegacy(dir, HistoryStore.legacyDataDir(env, os, home));
        } catch (IOException | RuntimeException e) {
            // A migração é uma conveniência: sem ela, a pasta antiga continua intacta e nada é perdido.
        }
        return dir;
    }

    /** Executa a coleta agendada. Retorna o código de saída do processo. */
    public static int run(PrintStream out, PrintStream err) {
        Map<String, String> env = System.getenv();
        HistoryStore store = new HistoryStore(dataDir(), ZoneId.systemDefault());
        try {
            HistoryRecord record = HistoryRecord.from(new SystemCollector(new SystemInfo()).collect());
            Path file = store.append(record);
            int days = retentionDays(env);
            List<Path> deleted = store.deleteOlderThan(days, LocalDate.now(ZoneId.systemDefault()));
            out.println("Coleta registrada em " + file + " (retencao: " + days + " dias"
                    + (deleted.isEmpty() ? "" : "; arquivos antigos removidos: " + deleted.size()) + ").");
            return 0;
        } catch (Exception | LinkageError e) {
            err.println("Falha na coleta agendada: " + e);
            return 1;
        }
    }
}
