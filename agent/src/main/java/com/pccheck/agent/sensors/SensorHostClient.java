package com.pccheck.agent.sensors;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.Executor;

/**
 * Inicia, controla e lê o leitor de sensores ({@code sensor-host/}, executável .NET com a
 * LibreHardwareMonitorLib). O usuário não instala nem abre outro programa: o backend inicia o leitor ao
 * subir, pedindo permissão de administrador ao Windows (necessária para temperatura da CPU e ventoinhas).
 *
 * <ul>
 *   <li>Se o usuário recusar a elevação, o leitor roda sem ela: GPU continua com leitura; CPU e ventoinhas
 *       da placa-mãe ficam "Sem leitura" com a explicação.</li>
 *   <li>Comunicação por pipe nomeado local com nome aleatório, somente leitura para o NEXCORE. Nenhuma porta
 *       de rede é aberta.</li>
 *   <li>O leitor termina sozinho quando o backend termina. Se ele cair, é reiniciado (até {@value #MAX_RESTARTS} vezes).</li>
 *   <li>Uma leitura recente (menos de 1 s) é reaproveitada: Live Monitor e Health Check não consultam duas vezes.</li>
 * </ul>
 */
public final class SensorHostClient implements SensorSource, AutoCloseable {

    public static final String EXE_NAME = "NEXCORE.SensorHost.exe";
    static final String PIPE_PREFIX = "nexcore-sensors-";
    static final long READ_TIMEOUT_MS = 3000;
    static final long CACHE_MS = 1000;
    static final int MAX_RESTARTS = 3;

    /** Inicia o processo do leitor. */
    public interface HostLauncher {
        /** @throws ElevationDeclinedException se {@code elevate} e o usuário recusou o aviso do Windows */
        HostProcess launch(Path exe, List<String> args, boolean elevate) throws IOException;
    }

    /** Processo do leitor em execução. */
    public interface HostProcess {
        boolean isAlive();

        void stop();
    }

    /** Lê uma linha do pipe nomeado do leitor. */
    public interface PipeReader {
        /** @throws PipeNotFoundException se o pipe ainda não existe (leitor carregando ou encerrado) */
        String readLine(String pipeName, long timeoutMs) throws IOException;
    }

    public static class ElevationDeclinedException extends IOException {
        public ElevationDeclinedException() {
            super("A permissão de administrador foi recusada.");
        }
    }

    public static class PipeNotFoundException extends IOException {
        public PipeNotFoundException() {
            super("O canal do leitor de sensores não existe.");
        }
    }

    private enum State { NEW, STARTING, RUNNING, FAILED, CLOSED }

    private final Path exe;
    private final boolean elevateRequested;
    private final HostLauncher launcher;
    private final PipeReader pipe;
    private final Executor launchExecutor;

    private State state = State.NEW;
    private String problem;
    private HostProcess process;
    private String pipeName;
    private boolean elevationDeclined;
    private int restarts;
    private SensorReading cached;
    private long cachedAtNanos;

    SensorHostClient(Path exe, boolean elevate, HostLauncher launcher, PipeReader pipe, Executor launchExecutor) {
        this.exe = exe;
        this.elevateRequested = elevate;
        this.launcher = launcher;
        this.pipe = pipe;
        this.launchExecutor = launchExecutor;
    }

    /**
     * Cria a fonte de sensores e já inicia o leitor (em segundo plano: o aviso de administrador do Windows
     * não trava o backend). Sem Windows ou sem o executável, devolve uma fonte que explica o motivo.
     *
     * @param configuredPath caminho do executável; em branco, procura em {@code sensor-host/dist/}
     * @param elevate        pedir permissão de administrador ao iniciar
     */
    public static SensorSource start(String configuredPath, boolean elevate) {
        if (!System.getProperty("os.name", "").toLowerCase(Locale.ROOT).startsWith("windows")) {
            return SensorSource.unavailable("A leitura de sensores e GPU está disponível somente no Windows.");
        }
        Path exe = locate(configuredPath);
        if (exe == null) {
            return SensorSource.unavailable("O leitor de sensores (" + EXE_NAME + ") não foi encontrado. "
                    + "Compile-o com scripts\\build-sensor-host.bat ou ajuste nexcore.sensors.host-path.");
        }
        WindowsSensorHost windows = new WindowsSensorHost();
        SensorHostClient client = new SensorHostClient(exe, elevate, windows, windows, task -> {
            Thread thread = new Thread(task, "nexcore-sensor-host-launch");
            thread.setDaemon(true);
            thread.start();
        });
        client.start();
        return client;
    }

    /** Caminho configurado ou, em branco, {@code sensor-host/dist/} a partir da pasta atual ou da pasta acima. */
    static Path locate(String configuredPath) {
        if (configuredPath != null && !configuredPath.isBlank()) {
            Path p = Path.of(configuredPath.trim()).toAbsolutePath().normalize();
            return Files.isRegularFile(p) ? p : null;
        }
        for (String candidate : List.of("sensor-host/dist/" + EXE_NAME, "../sensor-host/dist/" + EXE_NAME)) {
            Path p = Path.of(candidate).toAbsolutePath().normalize();
            if (Files.isRegularFile(p)) {
                return p;
            }
        }
        return null;
    }

    /** Inicia o leitor, se ainda não foi iniciado. Não bloqueia. */
    public synchronized void start() {
        if (state != State.NEW) {
            return;
        }
        state = State.STARTING;
        problem = null;
        String name = PIPE_PREFIX + UUID.randomUUID();
        boolean elevate = elevateRequested && !elevationDeclined;
        launchExecutor.execute(() -> launch(name, elevate));
    }

    private void launch(String name, boolean elevate) {
        List<String> args = List.of("--pipe", name, "--parent", Long.toString(ProcessHandle.current().pid()));
        HostProcess started = null;
        String failure = null;
        boolean declined = false;
        try {
            started = launcher.launch(exe, args, elevate);
        } catch (ElevationDeclinedException e) {
            // Sem administrador ainda há leituras úteis (GPU): inicia sem elevação.
            declined = true;
            try {
                started = launcher.launch(exe, args, false);
            } catch (IOException | RuntimeException e2) {
                failure = "Não foi possível iniciar o leitor de sensores: " + e2.getMessage();
            }
        } catch (IOException | RuntimeException e) {
            failure = "Não foi possível iniciar o leitor de sensores: " + e.getMessage();
        }
        synchronized (this) {
            if (declined) {
                elevationDeclined = true;
            }
            if (state == State.CLOSED) {
                if (started != null) {
                    started.stop();
                }
                return;
            }
            if (started == null) {
                state = State.FAILED;
                problem = failure;
                return;
            }
            process = started;
            pipeName = name;
            state = State.RUNNING;
        }
    }

    @Override
    public synchronized SensorReading read() throws SensorsUnavailableException {
        start();
        long now = System.nanoTime();
        if (cached != null && (now - cachedAtNanos) / 1_000_000L < CACHE_MS) {
            return cached;
        }
        switch (state) {
            case STARTING -> throw new SensorsUnavailableException(elevateRequested && !elevationDeclined
                    ? "Iniciando o leitor de sensores (confirme o aviso de administrador do Windows, se aparecer)."
                    : "Iniciando o leitor de sensores.");
            case FAILED -> throw new SensorsUnavailableException(problem);
            case CLOSED -> throw new SensorsUnavailableException("O leitor de sensores foi encerrado.");
            default -> {
                // RUNNING
            }
        }
        String line;
        try {
            line = pipe.readLine(pipeName, READ_TIMEOUT_MS);
        } catch (PipeNotFoundException e) {
            if (process.isAlive()) {
                throw new SensorsUnavailableException(
                        "O leitor de sensores está carregando; as leituras aparecem em instantes.");
            }
            throw restartAfterExit();
        } catch (IOException e) {
            throw new SensorsUnavailableException("O leitor de sensores não respondeu: " + e.getMessage(), e);
        }
        SensorReading reading;
        try {
            reading = SensorHostParser.parse(line);
        } catch (IllegalArgumentException e) {
            throw new SensorsUnavailableException("Resposta inesperada do leitor de sensores: " + e.getMessage(), e);
        }
        cached = reading;
        cachedAtNanos = now;
        return reading;
    }

    private SensorsUnavailableException restartAfterExit() {
        process = null;
        if (restarts >= MAX_RESTARTS) {
            state = State.FAILED;
            problem = "O leitor de sensores foi encerrado várias vezes e não será reiniciado. Reinicie o NEXCORE.";
            return new SensorsUnavailableException(problem);
        }
        restarts++;
        state = State.NEW;
        start();
        return new SensorsUnavailableException("O leitor de sensores foi encerrado; reiniciando.");
    }

    @Override
    public synchronized void close() {
        state = State.CLOSED;
        if (process != null) {
            process.stop();
            process = null;
        }
    }
}
