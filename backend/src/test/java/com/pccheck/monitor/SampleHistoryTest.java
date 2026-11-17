package com.pccheck.monitor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class SampleHistoryTest {

    @Test
    void descartaAsMaisAntigasAoAtingirACapacidade() {
        SampleHistory<Integer> history = new SampleHistory<>(3);
        for (int i = 1; i <= 5; i++) {
            history.add(i);
        }
        assertEquals(3, history.size());
        assertEquals(List.of(3, 4, 5), history.snapshot());
        assertEquals(5, history.latest());
    }

    @Test
    void lastRetornaAsMaisRecentesEmOrdemCronologica() {
        SampleHistory<Integer> history = new SampleHistory<>(10);
        for (int i = 1; i <= 6; i++) {
            history.add(i);
        }
        assertEquals(List.of(5, 6), history.last(2));
        assertEquals(List.of(1, 2, 3, 4, 5, 6), history.last(100));
        assertTrue(history.last(0).isEmpty());
        assertTrue(history.last(-1).isEmpty());
    }

    @Test
    void vazioNaoTemUltimaAmostra() {
        SampleHistory<Integer> history = new SampleHistory<>(2);
        assertNull(history.latest());
        assertEquals(0, history.size());
    }

    @Test
    void snapshotEUmaCopiaIndependente() {
        SampleHistory<Integer> history = new SampleHistory<>(2);
        history.add(1);
        List<Integer> copy = history.snapshot();
        history.add(2);
        assertEquals(1, copy.size());
    }

    @Test
    void capacidadeInvalidaERejeitada() {
        assertThrows(IllegalArgumentException.class, () -> new SampleHistory<Integer>(0));
    }
}
