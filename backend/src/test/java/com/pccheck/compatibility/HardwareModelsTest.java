package com.pccheck.compatibility;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class HardwareModelsTest {

    @Test
    void reconheceNomesComoOWindowsInforma() {
        assertEquals(Set.of("intel-core-i7-4790k"), HardwareModels.cpuModels("Intel(R) Core(TM) i7-4790K CPU @ 4.00GHz"));
        assertEquals(Set.of("amd-ryzen-5-3600"), HardwareModels.cpuModels("AMD Ryzen 5 3600 6-Core Processor"));
        assertEquals(Set.of("nvidia-gtx-1050-ti"), HardwareModels.gpuModels("NVIDIA GeForce GTX 1050 Ti"));
        assertEquals(Set.of("amd-rx-580"), HardwareModels.gpuModels("Radeon RX 580 Series"));
        assertEquals(Set.of("intel-hd-4600"), HardwareModels.gpuModels("Intel(R) HD Graphics 4600"));
    }

    @Test
    void sufixosFazemParteDoModelo() {
        assertNotEquals(HardwareModels.cpuModels("i5-8400"), HardwareModels.cpuModels("i5-8400T"));
        assertNotEquals(HardwareModels.gpuModels("RTX 3060"), HardwareModels.gpuModels("RTX 3060 Ti"));
        assertNotEquals(HardwareModels.gpuModels("RX 6700"), HardwareModels.gpuModels("RX 6700 XT"));
        assertEquals(Set.of("amd-rx-5700-xt"), HardwareModels.gpuModels("RX 5700XT"));
    }

    @Test
    void alternativasSaoTodasReconhecidasEmOrdem() {
        assertEquals(List.of("intel-core-i5-8400", "amd-ryzen-5-3600"),
                List.copyOf(HardwareModels.cpuModels("Intel Core i5-8400 / AMD Ryzen 5 3600")));
        assertEquals(List.of("nvidia-gtx-1060", "amd-rx-580"),
                List.copyOf(HardwareModels.gpuModels("NVIDIA GTX 1060 6GB or AMD RX 580 8GB")));
    }

    @Test
    void textoSemModeloExatoNaoGeraModelo() {
        assertTrue(HardwareModels.cpuModels("Intel Core i5 or equivalent").isEmpty());
        assertTrue(HardwareModels.gpuModels("DirectX 11 compatible graphics card").isEmpty());
        assertTrue(HardwareModels.cpuModels(null).isEmpty());
    }
}
