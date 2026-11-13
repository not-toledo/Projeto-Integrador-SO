package com.pccheck.agent.collector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pccheck.agent.model.CollectorResult;
import com.pccheck.agent.model.CollectorStatus;
import org.junit.jupiter.api.Test;

class CollectorRunnerTest {

    private static <T> Collector<T> collector(String name, java.util.concurrent.Callable<T> action) {
        return new Collector<>() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public T collect() throws Exception {
                return action.call();
            }
        };
    }

    @Test
    void retornaOkQuandoHaDados() {
        CollectorResult<String> r = CollectorRunner.run(collector("a", () -> "dado"));
        assertEquals(CollectorStatus.OK, r.status());
        assertEquals("dado", r.data());
        assertTrue(r.isOk());
    }

    @Test
    void retornaUnavailableQuandoNaoHaDados() {
        CollectorResult<String> r = CollectorRunner.run(collector("a", () -> null));
        assertEquals(CollectorStatus.UNAVAILABLE, r.status());
        assertNull(r.data());
    }

    @Test
    void falhaDeUmColetorViraErroSemPropagarExcecao() {
        CollectorResult<String> r = CollectorRunner.run(collector("a", () -> {
            throw new IllegalStateException("falhou");
        }));
        assertEquals(CollectorStatus.ERROR, r.status());
        assertTrue(r.message().contains("falhou"));
    }

    @Test
    void falhaDeBibliotecaNativaTambemEIsolada() {
        CollectorResult<String> r = CollectorRunner.run(collector("a", () -> {
            throw new UnsatisfiedLinkError("sem lib");
        }));
        assertEquals(CollectorStatus.ERROR, r.status());
    }

    @Test
    void umColetorComErroNaoAfetaOutro() {
        CollectorResult<String> falho = CollectorRunner.run(collector("a", () -> {
            throw new RuntimeException("x");
        }));
        CollectorResult<String> bom = CollectorRunner.run(collector("b", () -> "ok"));
        assertEquals(CollectorStatus.ERROR, falho.status());
        assertEquals(CollectorStatus.OK, bom.status());
    }
}
