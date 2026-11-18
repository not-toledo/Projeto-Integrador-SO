package com.pccheck.history;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
    static final long TIMEOUT_SECONDS = 5;

    /** Executa um comando e devolve a saída e o código de saída; null se não foi possível executar. */
    interface Runner {
        Result run(List<String> command) throws IOException, InterruptedException;
    }

    record Result(int exitCode, String output) {
    }

    public static SchedulerStatus query(String osName) {
        return query(osName, SchedulerStatus::runProcess);
    }

    static SchedulerStatus query(String osName, Runner runner) {
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

    private static Result runProcess(List<String> command) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        // A saída é lida em paralelo: se ninguém lê o pipe e ele enche, o processo filho trava.
        CompletableFuture<byte[]> reader = CompletableFuture.supplyAsync(() -> {
            try (InputStream in = process.getInputStream()) {
                return in.readAllBytes();
            } catch (IOException e) {
                return new byte[0];
            }
        });
        if (!process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            return null;
        }
        byte[] out;
        try {
            out = reader.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (ExecutionException | TimeoutException e) {
            out = new byte[0];
        }
        // schtasks escreve na página de código do console; o charset nativo do sistema a decodifica.
        return new Result(process.exitValue(), new String(out, Charset.forName(
                System.getProperty("native.encoding", Charset.defaultCharset().name()))));
    }
}
