package com.pccheck.agent.sensors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.Test;

/** Máquina de estados do cliente, sem processos nem pipes reais. */
class SensorHostClientTest {

    private static final String JSON = "{\"elevated\":true,\"driver\":true,\"hardware\":[{\"type\":\"Cpu\","
            + "\"name\":\"c\",\"sensors\":[{\"type\":\"Temperature\",\"name\":\"CPU Package\",\"value\":47}]}]}";
    private static final Executor INLINE = Runnable::run;

    private static final class FakeProcess implements SensorHostClient.HostProcess {
        boolean alive = true;
        boolean stopped;

        @Override
        public boolean isAlive() {
            return alive;
        }

        @Override
        public void stop() {
            stopped = true;
            alive = false;
        }
    }

    private static final class FakeLauncher implements SensorHostClient.HostLauncher {
        final List<Boolean> elevations = new ArrayList<>();
        final List<FakeProcess> processes = new ArrayList<>();
        boolean declineElevation;
        boolean fail;

        @Override
        public SensorHostClient.HostProcess launch(Path exe, List<String> args, boolean elevate) throws IOException {
            elevations.add(elevate);
            assertEquals("--pipe", args.get(0));
            assertTrue(args.get(1).startsWith(SensorHostClient.PIPE_PREFIX));
            if (fail) {
                throw new IOException("arquivo bloqueado");
            }
            if (elevate && declineElevation) {
                throw new SensorHostClient.ElevationDeclinedException();
            }
            FakeProcess p = new FakeProcess();
            processes.add(p);
            return p;
        }
    }

    private static SensorHostClient client(FakeLauncher launcher, SensorHostClient.PipeReader pipe, Executor executor) {
        return new SensorHostClient(Path.of("PCCheck.SensorHost.exe"), true, launcher, pipe, executor);
    }

    @Test
    void iniciaElevadoELeAPrimeiraAmostra() throws Exception {
        FakeLauncher launcher = new FakeLauncher();
        SensorHostClient c = client(launcher, (name, timeout) -> JSON, INLINE);
        assertEquals(47.0, c.read().cpuTemperatureCelsius());
        assertEquals(List.of(true), launcher.elevations);
    }

    @Test
    void reaproveitaLeituraRecente() throws Exception {
        int[] calls = {0};
        SensorHostClient c = client(new FakeLauncher(), (name, timeout) -> {
            calls[0]++;
            return JSON;
        }, INLINE);
        SensorReading first = c.read();
        assertSame(first, c.read());
        assertEquals(1, calls[0]);
    }

    @Test
    void enquantoIniciaInformaQueEstaAguardandoOAviso() {
        List<Runnable> pending = new ArrayList<>();
        SensorHostClient c = client(new FakeLauncher(), (name, timeout) -> JSON, pending::add);
        SensorsUnavailableException e = assertThrows(SensorsUnavailableException.class, c::read);
        assertTrue(e.getMessage().contains("administrador"));
        assertEquals(1, pending.size(), "a inicialização roda fora da thread de leitura");
    }

    @Test
    void elevacaoRecusadaIniciaSemElevacao() throws Exception {
        FakeLauncher launcher = new FakeLauncher();
        launcher.declineElevation = true;
        String noAdmin = JSON.replace("\"elevated\":true", "\"elevated\":false")
                .replace("\"value\":47", "\"value\":\"x\"");
        SensorHostClient c = client(launcher, (name, timeout) -> noAdmin, INLINE);
        SensorReading r = c.read();
        assertEquals(List.of(true, false), launcher.elevations);
        assertNull(r.cpuTemperatureCelsius());
        assertEquals(SensorHostParser.NOT_ELEVATED, r.limitation());
    }

    @Test
    void falhaAoIniciarViraMensagem() {
        FakeLauncher launcher = new FakeLauncher();
        launcher.fail = true;
        SensorHostClient c = client(launcher, (name, timeout) -> JSON, INLINE);
        SensorsUnavailableException e = assertThrows(SensorsUnavailableException.class, c::read);
        assertTrue(e.getMessage().contains("arquivo bloqueado"));
    }

    @Test
    void pipeAindaInexistenteComProcessoVivoSignificaCarregando() {
        SensorHostClient c = client(new FakeLauncher(), (name, timeout) -> {
            throw new SensorHostClient.PipeNotFoundException();
        }, INLINE);
        SensorsUnavailableException e = assertThrows(SensorsUnavailableException.class, c::read);
        assertTrue(e.getMessage().contains("carregando"));
    }

    @Test
    void reiniciaQuandoOLeitorCaiComLimite() {
        FakeLauncher launcher = new FakeLauncher();
        SensorHostClient c = client(launcher, (name, timeout) -> {
            launcher.processes.get(launcher.processes.size() - 1).alive = false;
            throw new SensorHostClient.PipeNotFoundException();
        }, INLINE);
        for (int i = 0; i < SensorHostClient.MAX_RESTARTS; i++) {
            SensorsUnavailableException e = assertThrows(SensorsUnavailableException.class, c::read);
            assertTrue(e.getMessage().contains("reiniciando"));
        }
        SensorsUnavailableException last = assertThrows(SensorsUnavailableException.class, c::read);
        assertTrue(last.getMessage().contains("não será reiniciado"));
        assertEquals(1 + SensorHostClient.MAX_RESTARTS, launcher.elevations.size());
    }

    @Test
    void respostaInvalidaNaoDerrubaOCliente() {
        SensorHostClient c = client(new FakeLauncher(), (name, timeout) -> "lixo", INLINE);
        assertThrows(SensorsUnavailableException.class, c::read);
    }

    @Test
    void closeEncerraOLeitor() {
        FakeLauncher launcher = new FakeLauncher();
        SensorHostClient c = client(launcher, (name, timeout) -> JSON, INLINE);
        c.start();
        c.close();
        assertTrue(launcher.processes.get(0).stopped);
        assertFalse(launcher.processes.get(0).alive);
        assertThrows(SensorsUnavailableException.class, c::read);
    }

    @Test
    void localizaSomenteArquivoExistente() {
        assertNull(SensorHostClient.locate("caminho/que/nao/existe.exe"));
    }

    @Test
    void fonteIndisponivelExplicaOMotivo() {
        SensorsUnavailableException e = assertThrows(SensorsUnavailableException.class,
                () -> SensorSource.unavailable("desativado").read());
        assertEquals("desativado", e.getMessage());
    }
}
