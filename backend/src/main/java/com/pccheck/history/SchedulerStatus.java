package com.pccheck.history;

import com.pccheck.os.CommandRunner;
import com.pccheck.os.CommandRunner.Result;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Consulta o agendador do sistema operacional para saber se a coleta agendada está instalada.
 * Windows: {@code schtasks /Query /TN PCCheck\Coleta /FO CSV /NH}; Linux: {@code crontab -l}, procurando a linha
 * marcada pelo script {@code scripts/agendar-coleta.sh}. Os comandos são fixos (nada vem do usuário) e têm tempo
 * limite.
 *
 * @param state   INSTALLED, NOT_INSTALLED ou UNKNOWN (agendador indisponível ou comando falhou)
 * @param details texto do agendador (por exemplo, próxima execução e estado), quando houver
 */
public record SchedulerStatus(String state, String details) {

    public static final String TASK_NAME = "PCCheck\\Coleta";
    public static final String CRON_MARKER = "# PCCheck: coleta agendada";
    public static SchedulerStatus query(String osName) {
        return query(osName, CommandRunner.system());
    }

    static SchedulerStatus query(String osName, CommandRunner runner) {
        boolean windows = osName != null && osName.toLowerCase(Locale.ROOT).startsWith("windows");
        try {
            if (windows) {
                Result r = runner.run(List.of("schtasks", "/Query", "/TN", TASK_NAME, "/FO", "CSV", "/NH"));
                if (r == null) return unknown("O agendador de tarefas não respondeu.");
                if (r.exitCode() != 0) return new SchedulerStatus("NOT_INSTALLED", null);
                return new SchedulerStatus("INSTALLED", fromCsv(r.output()));
            }
            Result r = runner.run(List.of("crontab", "-l"));
            if (r == null) return unknown("O cron não respondeu.");
            // "crontab -l" sai com código 1 quando o usuário ainda não tem crontab: também é "não instalado".
            for (String line : r.output().split("\\R")) {
                if (line.contains(CRON_MARKER)) {
                    return new SchedulerStatus("INSTALLED", line.substring(0, line.indexOf(CRON_MARKER)).trim());
                }
            }
            return new SchedulerStatus("NOT_INSTALLED", null);
        } catch (IOException e) {
            return unknown("Não foi possível executar o agendador do sistema: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return unknown("Consulta ao agendador interrompida.");
        }
    }

    /** "\"\\PCCheck\\Coleta\",\"18/11/2026 10:15:00\",\"Pronto\"" → "Próxima execução: 18/11/2026 10:15:00 · Pronto". */
    static String fromCsv(String output) {
        for (String line : output.split("\\R")) {
            String[] cols = line.trim().split("\",\"");
            if (cols.length >= 3) {
                String next = cols[1].replace("\"", "").trim();
                String status = cols[2].replace("\"", "").trim();
                return "Próxima execução: " + next + " · Estado: " + status;
            }
        }
        return null;
    }

    private static SchedulerStatus unknown(String why) {
        return new SchedulerStatus("UNKNOWN", why);
    }
}
