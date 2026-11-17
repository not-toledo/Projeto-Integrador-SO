package com.pccheck.monitor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class MonitorEngineTest {

    /** Relógio controlável pelo teste. */
    static final class TestClock extends Clock {
        Instant now = Instant.parse("2026-01-01T12:00:00Z");

        @Override
        public java.time.ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    private static <T> MonitorEngine<T> engine(Supplier<T> sampler, int capacity, TestClock clock) {
        return new MonitorEngine<>(sampler, capacity, Duration.ofSeconds(10), clock);
    }

    @Test
    void coletaBemSucedidaEntraNoHistoricoEAtualizaOEstado() {
        TestClock clock = new TestClock();
        int[] counter = {0};
        MonitorEngine<Integer> engine = engine(() -> ++counter[0], 5, clock);

        assertTrue(engine.tick());
        assertTrue(engine.tick());

        assertEquals(2, engine.latest());
        assertEquals(List.of(1, 2), engine.history(10));
        MonitorStatus s = engine.status(true, 2000);
        assertEquals(2, s.sampleCount());
        assertEquals(clock.now, s.lastSuccessAt());
        assertEquals(0, s.consecutiveFailures());
        assertNull(s.lastError());
        assertFalse(s.collecting());
    }

    @Test
    void historicoNuncaExcedeACapacidade() {
        int[] counter = {0};
        MonitorEngine<Integer> engine = engine(() -> ++counter[0], 3, new TestClock());
        for (int i = 0; i < 20; i++) {
            engine.tick();
        }
        assertEquals(List.of(18, 19, 20), engine.history(100));
        assertEquals(3, engine.status(true, 2000).sampleCount());
    }

    @Test
    void excecaoNaColetaEhRegistradaENaoImpedeAsProximas() {
        boolean[] fail = {true};
        MonitorEngine<String> engine = engine(() -> {
            if (fail[0]) {
                throw new IllegalStateException("sensor quebrou");
            }
            return "ok";
        }, 5, new TestClock());

        assertTrue(engine.tick());
        assertTrue(engine.tick());
        MonitorStatus falhando = engine.status(true, 2000);
        assertEquals(2, falhando.consecutiveFailures());
        assertEquals("IllegalStateException: sensor quebrou", falhando.lastError());
        assertEquals(0, falhando.sampleCount());
        assertNull(engine.latest());

        fail[0] = false;
        assertTrue(engine.tick());
        MonitorStatus recuperado = engine.status(true, 2000);
        assertEquals(0, recuperado.consecutiveFailures());
        assertNull(recuperado.lastError());
        assertEquals("ok", engine.latest());
    }

    @Test
    void falhaDeBibliotecaNativaTambemEhIsolada() {
        MonitorEngine<String> engine = engine(() -> {
            throw new UnsatisfiedLinkError("jna");
        }, 5, new TestClock());
        assertTrue(engine.tick());
        assertEquals(1, engine.status(true, 2000).consecutiveFailures());
    }

    @Test
    void coletaSemDadosContaComoFalha() {
        MonitorEngine<String> engine = engine(() -> null, 5, new TestClock());
        engine.tick();
        MonitorStatus s = engine.status(true, 2000);
        assertEquals(1, s.consecutiveFailures());
        assertEquals(0, s.sampleCount());
    }

    @Test
    void naoIniciaNovaColetaEnquantoAAnteriorEstaEmAndamento() {
        MonitorEngine<?>[] holder = new MonitorEngine<?>[1];
        boolean[] reentrada = {true};
        int[] execucoes = {0};
        MonitorEngine<Integer> engine = engine(() -> {
            execucoes[0]++;
            reentrada[0] = holder[0].tick(); // tenta iniciar outra coleta durante esta
            return execucoes[0];
        }, 5, new TestClock());
        holder[0] = engine;

        assertTrue(engine.tick());

        assertFalse(reentrada[0], "a coleta aninhada deve ser ignorada");
        assertEquals(1, execucoes[0]);
        assertEquals(1, engine.status(true, 2000).sampleCount());
    }

    @Test
    void coletaMuitoLongaEhSinalizadaComoTravada() {
        TestClock clock = new TestClock();
        MonitorStatus[] durante = new MonitorStatus[1];
        MonitorEngine<?>[] holder = new MonitorEngine<?>[1];
        MonitorEngine<Integer> engine = engine(() -> {
            clock.now = clock.now.plusSeconds(30); // simula coleta bloqueada por 30 s
            durante[0] = holder[0].status(true, 2000);
            return 1;
        }, 5, clock);
        holder[0] = engine;

        engine.tick();

        assertTrue(durante[0].collecting());
        assertTrue(durante[0].stalled());
        MonitorStatus depois = engine.status(true, 2000);
        assertFalse(depois.collecting());
        assertFalse(depois.stalled());
    }

    @Test
    void estadoReportaIntervaloECapacidade() {
        MonitorStatus s = engine(() -> 1, 7, new TestClock()).status(false, 1500);
        assertFalse(s.running());
        assertEquals(1500, s.intervalMs());
        assertEquals(7, s.historyCapacity());
        assertNull(s.lastSuccessAt());
    }
}
