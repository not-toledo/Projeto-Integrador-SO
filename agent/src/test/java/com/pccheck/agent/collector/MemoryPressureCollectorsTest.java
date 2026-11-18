package com.pccheck.agent.collector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.pccheck.agent.collector.LiveSampler.PagingCounters;
import com.pccheck.agent.model.MemoryInfo;
import com.pccheck.agent.model.PagingActivity;
import com.pccheck.agent.model.VirtualMemoryInfo;
import org.junit.jupiter.api.Test;

/** Memória virtual e paginação: conversões puras (a leitura real exige máquina real). */
class MemoryPressureCollectorsTest {

    private static final long GB = 1_000_000_000L;

    @Test
    void memoriaVirtualConverteValoresReais() {
        // Valores lidos de uma máquina Windows com 16 GB de RAM.
        VirtualMemoryInfo vm = RamCollector.toVirtual(8_081_989_632L, 435_970_048L, 24_117_821_440L,
                21_213_360_128L);
        assertEquals(8_081_989_632L, vm.swapTotalBytes());
        assertEquals(5.39, vm.swapUsedPercent(), 0.01);
        assertEquals(87.96, vm.commitPercent(), 0.01);
    }

    @Test
    void memoriaComprometidaPodePassarDoLimiteNoLinux() {
        // Overcommit: Committed_AS acima de CommitLimit é permitido pelo kernel.
        VirtualMemoryInfo vm = RamCollector.toVirtual(2 * GB, 0, 10 * GB, 15 * GB);
        assertEquals(150.0, vm.commitPercent(), 0.001);
    }

    @Test
    void memoriaVirtualDescartaValoresInvalidos() {
        assertNull(RamCollector.toVirtual(-1, -1, 0, -1));
        VirtualMemoryInfo semSwap = RamCollector.toVirtual(0, 0, 16 * GB, 8 * GB);
        assertEquals(0L, semSwap.swapTotalBytes());
        assertNull(semSwap.swapUsedPercent());
        // Uso maior que o total é incoerente: descartado.
        assertNull(RamCollector.toVirtual(GB, 2 * GB, 16 * GB, 8 * GB).swapUsedBytes());
    }

    @Test
    void paginacaoCalculaTaxaEntreAmostras() {
        PagingActivity p = LiveSampler.pagingBetween(new PagingCounters(1000, 50, 10_000),
                new PagingCounters(1400, 250, 12_000), 4096L);
        assertEquals(200.0, p.pagesInPerSecond(), 0.001);
        assertEquals(100.0, p.pagesOutPerSecond(), 0.001);
        assertEquals(409_600.0, p.pagesOutBytesPerSecond(), 0.001);
    }

    @Test
    void paginacaoSemAmostraAnteriorOuComContadorReiniciado() {
        assertNull(LiveSampler.pagingBetween(null, new PagingCounters(1, 1, 1), 4096L));
        // Contador menor que o anterior (reinício): taxa não é inventada.
        assertNull(LiveSampler.pagingBetween(new PagingCounters(500, 500, 0), new PagingCounters(10, 10, 2000), 4096L));
        PagingActivity semTamanho = LiveSampler.pagingBetween(new PagingCounters(0, 0, 0),
                new PagingCounters(10, 10, 1000), null);
        assertNull(semTamanho.pagesOutBytesPerSecond());
    }

    @Test
    void memoriaComPaginacaoPreservaOsDemaisCampos() {
        MemoryInfo base = new MemoryInfo(16 * GB, 4 * GB, 12 * GB, 75.0,
                new VirtualMemoryInfo(null, null, null, 24 * GB, 20 * GB, 83.3), null);
        MemoryInfo live = base.withPaging(new PagingActivity(1.0, 2.0, 4096L));
        assertEquals(75.0, live.usedPercent());
        assertEquals(20 * GB, live.virtualMemory().commitUsedBytes());
        assertEquals(2.0, live.paging().pagesOutPerSecond());
    }
}
