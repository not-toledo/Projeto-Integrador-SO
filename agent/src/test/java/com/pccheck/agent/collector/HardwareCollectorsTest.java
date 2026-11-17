package com.pccheck.agent.collector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.pccheck.agent.model.DiskInfo;
import com.pccheck.agent.model.GpuInfo;
import com.pccheck.agent.model.MotherboardInfo;
import com.pccheck.agent.model.StorageInfo;
import com.pccheck.agent.model.VolumeInfo;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Testa as conversões puras dos coletores (a leitura real do hardware exige teste em máquina real). */
class HardwareCollectorsTest {

    @Test
    void gpuConverteDadosValidos() {
        GpuInfo gpu = GpuCollector.toInfo("GPU X", "Vendor", "0x1234", 4_294_967_296L);
        assertEquals("GPU X", gpu.name());
        assertEquals("Vendor", gpu.vendor());
        assertEquals("0x1234", gpu.deviceId());
        assertEquals(4_294_967_296L, gpu.vramBytes());
    }

    @Test
    void gpuTrataSentinelaDeVramEMantemOsOutrosDados() {
        GpuInfo gpu = GpuCollector.toInfo("GPU X", "unknown", "", -1L);
        assertNotNull(gpu);
        assertEquals("GPU X", gpu.name());
        assertNull(gpu.vendor());
        assertNull(gpu.deviceId());
        assertNull(gpu.vramBytes());
    }

    @Test
    void gpuSemNenhumDadoRetornaNull() {
        assertNull(GpuCollector.toInfo(null, "unknown", " ", 0L));
    }

    @Test
    void placaMaeDescartaTextoGenericoDoFabricante() {
        MotherboardInfo mb = MotherboardCollector.build("ASUSTeK", "To be filled by O.E.M.", "Rev 1.xx",
                "Default string", "System manufacturer", "System Product Name");
        assertEquals("ASUSTeK", mb.manufacturer());
        assertNull(mb.model());
        assertEquals("Rev 1.xx", mb.version());
        assertNull(mb.serialNumber());
        assertNull(mb.systemManufacturer());
        assertNull(mb.systemModel());
    }

    @Test
    void placaMaeSemNenhumDadoRetornaNull() {
        assertNull(MotherboardCollector.build("To be filled by O.E.M.", null, "", "None", "unknown", "N/A"));
    }

    @Test
    void placaMaeDescartaVersaoPlaceholderComoXPonto() {
        MotherboardInfo mb = MotherboardCollector.build("Gigabyte Technology Co., Ltd.", "H81M-S1", "x.x",
                null, null, null);
        assertEquals("Gigabyte Technology Co., Ltd.", mb.manufacturer());
        assertEquals("H81M-S1", mb.model());
        assertNull(mb.version());
        assertNull(MotherboardCollector.build(null, null, "X.XX", null, null, null));
    }

    @Test
    void discoConverteEDescartaInvalidos() {
        DiskInfo disk = DiskCollector.toDisk("\\\\.\\PHYSICALDRIVE0", "Modelo D", 500_107_862_016L);
        assertEquals("Modelo D", disk.model());
        assertEquals(500_107_862_016L, disk.sizeBytes());
        assertNull(DiskCollector.toDisk(null, "unknown", -1L));
        assertNull(DiskCollector.toDisk("d", null, 0L).sizeBytes());
    }

    @Test
    void volumeCalculaUsoAPartirDoEspacoDisponivel() {
        VolumeInfo v = DiskCollector.toVolume("C:\\", "Sistema", "NTFS", 1000L, 250L);
        assertEquals(1000L, v.totalBytes());
        assertEquals(250L, v.freeBytes());
        assertEquals(750L, v.usedBytes());
        assertEquals(75.0, v.usedPercent(), 0.0001);
    }

    @Test
    void volumeSemCapacidadeValidaEIgnorado() {
        assertNull(DiskCollector.toVolume("X:\\", null, null, 0L, 0L));
        assertNull(DiskCollector.toVolume("X:\\", null, null, -1L, 5L));
    }

    @Test
    void volumeComEspacoLivreInvalidoMantemCapacidadeSemInventarUso() {
        VolumeInfo v = DiskCollector.toVolume("D:\\", null, "NTFS", 1000L, -1L);
        assertEquals(1000L, v.totalBytes());
        assertNull(v.freeBytes());
        assertNull(v.usedBytes());
        assertNull(v.usedPercent());
    }

    @Test
    void storageInfoCopiaListasEAceitaNulas() {
        StorageInfo s = new StorageInfo(null, List.of());
        assertNull(s.drives());
        assertEquals(0, s.volumes().size());
    }
}
