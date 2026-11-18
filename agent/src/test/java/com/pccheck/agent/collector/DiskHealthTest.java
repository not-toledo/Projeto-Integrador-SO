package com.pccheck.agent.collector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pccheck.agent.model.DiskHealth;
import com.pccheck.agent.model.DiskInfo;
import com.pccheck.agent.sensors.SensorHostParser;
import com.pccheck.agent.sensors.SensorReading;
import com.pccheck.agent.sensors.SensorSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Tipo de mídia (SSD/HDD) e saúde dos discos (SMART pelo leitor de sensores). */
class DiskHealthTest {

    /** Trecho real do leitor de sensores (SSD SATA e HDD Seagate), com administrador. */
    static final String REAL_JSON = """
            {"version":1,"elevated":true,"driver":true,"error":null,"hardware":[
             {"type":"Cpu","id":"/intelcpu/0","name":"Intel Core i7-4790K","sensors":[{"type":"Temperature","name":"CPU Package","value":45}]},
             {"type":"Storage","id":"/ssd/0","name":"SSD-REDRAGON-SPARK-480","sensors":[
               {"type":"Temperature","name":"Temperature","value":48},{"type":"Level","name":"Life","value":70},
               {"type":"Data","name":"Data Read","value":31796},{"type":"Data","name":"Data Written","value":53880},
               {"type":"Factor","name":"Power On Count","value":1062},{"type":"Factor","name":"Power On Hours","value":7371},
               {"type":"Load","name":"Used Space","value":80.7465057},{"type":"Throughput","name":"Write Rate","value":10610384}]},
             {"type":"Storage","id":"/hdd/1","name":"ST1000DM003-1ER162","sensors":[
               {"type":"Temperature","name":"Temperature","value":28},{"type":"Factor","name":"Power On Count","value":4301},
               {"type":"Factor","name":"Power On Hours","value":50470},{"type":"Load","name":"Used Space","value":62.88}]},
             {"type":"Storage","id":"/hdd/2","name":"SanDisk Cruzer Blade","sensors":[{"type":"Load","name":"Used Space","value":3.1}]}
            ]}""";

    @TempDir
    Path temp;

    @Test
    void parserLeSaudeDosDiscosReais() {
        SensorReading reading = SensorHostParser.parse(REAL_JSON);
        List<DiskHealth> disks = reading.disks();
        // O pen drive só informa espaço usado: sem indicador de saúde, fica de fora.
        assertEquals(2, disks.size());

        DiskHealth ssd = disks.get(0);
        assertEquals("SSD-REDRAGON-SPARK-480", ssd.name());
        assertEquals(0, ssd.physicalIndex());
        assertEquals(48.0, ssd.temperatureCelsius());
        assertEquals(70.0, ssd.lifePercent());
        assertEquals(53880.0, ssd.dataWrittenGb());
        assertEquals(7371L, ssd.powerOnHours());
        assertEquals(1062L, ssd.powerOnCount());

        DiskHealth hdd = disks.get(1);
        assertEquals(1, hdd.physicalIndex());
        assertNull(hdd.lifePercent(), "HDD não tem vida útil restante");
        assertEquals(50470L, hdd.powerOnHours());
        // A CPU continua sendo lida normalmente.
        assertEquals(45.0, reading.cpuTemperatureCelsius());
    }

    @Test
    void valoresForaDaFaixaSaoDescartados() {
        String json = """
                {"version":1,"elevated":true,"driver":true,"error":null,"hardware":[
                 {"type":"Storage","id":"/nvme/3","name":"NVMe X","sensors":[
                   {"type":"Temperature","name":"Temperature","value":-40},{"type":"Level","name":"Life","value":250},
                   {"type":"Factor","name":"Power On Hours","value":10}]}]}""";
        DiskHealth d = SensorHostParser.parse(json).disks().get(0);
        assertNull(d.temperatureCelsius());
        assertNull(d.lifePercent());
        assertEquals(3, d.physicalIndex());
        assertEquals(10L, d.powerOnHours());
    }

    @Test
    void respostaAntigaSemIdNemDiscosContinuaValida() {
        String json = "{\"version\":1,\"elevated\":false,\"driver\":false,\"error\":null,\"hardware\":["
                + "{\"type\":\"Storage\",\"name\":\"Disco\",\"sensors\":[{\"type\":\"Temperature\",\"name\":\"Temperature\",\"value\":30}]}]}";
        DiskHealth d = SensorHostParser.parse(json).disks().get(0);
        assertNull(d.physicalIndex());
        assertEquals(30.0, d.temperatureCelsius());
    }

    @Test
    void coletorDevolveOsDiscosOuOMotivo() throws Exception {
        SensorReading reading = SensorHostParser.parse(REAL_JSON);
        assertEquals(2, new DiskHealthCollector(() -> reading).collect().disks().size());

        // Sem administrador: o motivo é o dos discos, não o texto da CPU.
        SensorReading semDiscos = new SensorReading(45.0, List.of(), List.of(), "texto da CPU", List.of(),
                "Sem administrador para o SMART.");
        var e = assertThrows(CollectorUnavailableException.class, () -> new DiskHealthCollector(() -> semDiscos).collect());
        assertEquals("Sem administrador para o SMART.", e.getMessage());

        SensorReading semMotivo = new SensorReading(45.0, List.of(), List.of(), null);
        assertEquals(DiskHealthCollector.NO_DISKS, assertThrows(CollectorUnavailableException.class,
                () -> new DiskHealthCollector(() -> semMotivo).collect()).getMessage());

        SensorSource linux = SensorSource.unavailable("Leitor de sensores não suportado neste sistema.");
        assertEquals("Leitor de sensores não suportado neste sistema.", assertThrows(CollectorUnavailableException.class,
                () -> new DiskHealthCollector(linux).collect()).getMessage());
    }

    @Test
    void motivoDosDiscosDependeSoDaElevacao() {
        String semAdmin = "{\"version\":1,\"elevated\":false,\"driver\":true,\"error\":null,\"hardware\":[]}";
        assertEquals(SensorHostParser.DISKS_NOT_ELEVATED, SensorHostParser.parse(semAdmin).diskLimitation());
        // Sem o driver PawnIO, mas com administrador: o SMART é lido normalmente (o driver é só da CPU/placa-mãe).
        String semDriver = "{\"version\":1,\"elevated\":true,\"driver\":false,\"error\":null,\"hardware\":[]}";
        SensorReading r = SensorHostParser.parse(semDriver);
        assertNull(r.diskLimitation());
        assertTrue(r.limitation().contains("PawnIO"));
    }

    @Test
    void tipoDeMidiaDoWindows() {
        assertEquals("SSD", DiskMediaTypes.windowsMediaType(4));
        assertEquals("HDD", DiskMediaTypes.windowsMediaType(3));
        assertNull(DiskMediaTypes.windowsMediaType(0), "não especificado (ex.: pen drive)");
        assertNull(DiskMediaTypes.windowsMediaType(5), "SCM não é tratado como SSD");
        assertEquals("NVMe", DiskMediaTypes.windowsBusType(17));
        assertEquals("SATA", DiskMediaTypes.windowsBusType(11));
        assertEquals("USB", DiskMediaTypes.windowsBusType(7));
        assertNull(DiskMediaTypes.windowsBusType(99));
        assertEquals("0", DiskMediaTypes.windowsDriveNumber("\\\\.\\PHYSICALDRIVE0"));
        assertEquals("12", DiskMediaTypes.windowsDriveNumber("\\\\.\\PhysicalDrive12"));
        assertNull(DiskMediaTypes.windowsDriveNumber("/dev/sda"));
    }

    @Test
    void tipoDeMidiaDoLinuxPeloSysBlock() throws Exception {
        disk("sda", "1", "0");
        disk("nvme0n1", "0", "0");
        disk("sdb", "0", "1"); // pen drive: não rotacional, mas removível
        assertEquals(new DiskMediaTypes.Media("HDD", null), DiskMediaTypes.linux("/dev/sda", temp));
        assertEquals(new DiskMediaTypes.Media("SSD", "NVMe"), DiskMediaTypes.linux("/dev/nvme0n1", temp));
        assertEquals(new DiskMediaTypes.Media(null, null), DiskMediaTypes.linux("/dev/sdb", temp));
        assertEquals(new DiskMediaTypes.Media(null, null), DiskMediaTypes.linux("/dev/sdz", temp));
    }

    @Test
    void discoGuardaTipoEBarramento() {
        DiskInfo d = DiskCollector.toDisk("\\\\.\\PHYSICALDRIVE0", "SSD X", 480_000_000_000L, "SSD", "SATA");
        assertEquals("SSD", d.mediaType());
        assertEquals("SATA", d.busType());
        assertTrue(DiskCollector.toDisk("d", "m", 1L).mediaType() == null);
    }

    private void disk(String name, String rotational, String removable) throws Exception {
        Path queue = Files.createDirectories(temp.resolve(name).resolve("queue"));
        Files.writeString(queue.resolve("rotational"), rotational + "\n");
        Files.writeString(temp.resolve(name).resolve("removable"), removable + "\n");
    }
}
