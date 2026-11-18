package com.pccheck.os;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Executa um comando fixo do sistema (ex.: {@code schtasks}, {@code powercfg}) com tempo limite. Os comandos nunca
 * vêm do usuário. Interface para que os testes usem saídas gravadas no lugar do comando real.
 */
@FunctionalInterface
public interface CommandRunner {

    long TIMEOUT_SECONDS = 5;

    /** Saída e código de saída; null se o comando não terminou dentro do tempo limite. */
    Result run(List<String> command) throws IOException, InterruptedException;

    record Result(int exitCode, String output) {
    }

    /** Execução real, como processo filho do backend. */
    static CommandRunner system() {
        return CommandRunner::runProcess;
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
        // Os comandos do Windows escrevem na página de código do sistema; o charset nativo a decodifica.
        return new Result(process.exitValue(), new String(out, Charset.forName(
                System.getProperty("native.encoding", Charset.defaultCharset().name()))));
    }
}
