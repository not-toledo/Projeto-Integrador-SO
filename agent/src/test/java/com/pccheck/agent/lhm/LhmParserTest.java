package com.pccheck.agent.lhm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Dados de exemplo FICTÍCIOS no formato do data.json do LibreHardwareMonitor (números em pt-BR). */
class LhmParserTest {

    /** Formato novo (HardwareId/SensorId/Type), números com vírgula decimal e ponto de milhar. */
    private static final String NEW = """
    {"id":0,"Text":"Sensor","Min":"Min","Value":"Value","Max":"Max","ImageURL":"","Children":[
     {"id":1,"Text":"DESKTOP-ABC","Min":"","Value":"","Max":"","ImageURL":"images_icon/computer.png","Children":[
      {"id":2,"Text":"AMD Ryzen 5 5600","HardwareId":"/amdcpu/0","Value":"","ImageURL":"images_icon/cpu.png","Children":[
        {"id":3,"Text":"Temperatures","Value":"","ImageURL":"images_icon/temperature.png","Children":[
          {"id":4,"Text":"Core (Tctl/Tdie)","SensorId":"/amdcpu/0/temperature/2","Type":"Temperature","Min":"38,0 \\u00b0C","Value":"52,4 \\u00b0C","Max":"80,1 \\u00b0C","Children":[]}
        ]},
        {"id":5,"Text":"Load","Value":"","Children":[
          {"id":6,"Text":"CPU Total","SensorId":"/amdcpu/0/load/0","Type":"Load","Value":"12,3 %","Children":[]}
        ]}
      ]},
      {"id":7,"Text":"NVIDIA GeForce RTX 3060","HardwareId":"/gpu-nvidia/0","Value":"","ImageURL":"images_icon/nvidia.png","Children":[
        {"id":8,"Text":"Temperatures","Value":"","Children":[
          {"id":9,"Text":"GPU Core","SensorId":"/gpu-nvidia/0/temperature/0","Type":"Temperature","Value":"47,0 °C","Children":[]}]},
        {"id":10,"Text":"Load","Value":"","Children":[
          {"id":11,"Text":"GPU Core","SensorId":"/gpu-nvidia/0/load/0","Type":"Load","Value":"31,0 %","Children":[]},
          {"id":12,"Text":"GPU Memory","SensorId":"/gpu-nvidia/0/load/3","Type":"Load","Value":"18,2 %","Children":[]}]},
        {"id":13,"Text":"Fans","Value":"","Children":[
          {"id":14,"Text":"GPU Fan 1","SensorId":"/gpu-nvidia/0/fan/1","Type":"Fan","Value":"0 RPM","Children":[]}]},
        {"id":15,"Text":"Data","Value":"","Children":[]},
        {"id":16,"Text":"Small Data","Value":"","Children":[
          {"id":17,"Text":"GPU Memory Free","SensorId":"/gpu-nvidia/0/smalldata/0","Type":"SmallData","Value":"9.800,5 MB","Children":[]},
          {"id":18,"Text":"GPU Memory Used","SensorId":"/gpu-nvidia/0/smalldata/1","Type":"SmallData","Value":"2.234,0 MB","Children":[]},
          {"id":19,"Text":"GPU Memory Total","SensorId":"/gpu-nvidia/0/smalldata/2","Type":"SmallData","Value":"12.288,0 MB","Children":[]}]}
      ]},
      {"id":20,"Text":"AMD Radeon(TM) Graphics","HardwareId":"/gpu-amd/1","Value":"","Children":[
        {"id":21,"Text":"Load","Value":"","Children":[{"id":22,"Text":"GPU Core","SensorId":"/gpu-amd/1/load/0","Type":"Load","Value":"1,0 %","Children":[]}]},
        {"id":23,"Text":"Small Data","Value":"","Children":[{"id":24,"Text":"GPU Memory Total","SensorId":"/gpu-amd/1/smalldata/2","Type":"SmallData","Value":"512,0 MB","Children":[]}]}
      ]},
      {"id":30,"Text":"Nuvoton NCT6798D","HardwareId":"/lpc/nct6798d","Value":"","Children":[
        {"id":31,"Text":"Fans","Value":"","Children":[
          {"id":32,"Text":"Fan #1","SensorId":"/lpc/nct6798d/0/fan/0","Type":"Fan","Value":"1245 RPM","Children":[]},
          {"id":33,"Text":"Fan #2","SensorId":"/lpc/nct6798d/0/fan/1","Type":"Fan","Value":"0 RPM","Children":[]}]},
        {"id":34,"Text":"Temperatures","Value":"","Children":[
          {"id":35,"Text":"CPU Core","SensorId":"/lpc/nct6798d/0/temperature/0","Type":"Temperature","Value":"41,0 °C","Children":[]}]}
      ]}
     ]}]}
            """;

    /** Formato antigo (sem HardwareId/SensorId/Type), números com ponto decimal. */
    private static final String OLD = """
    {"id":0,"Text":"Sensor","Children":[
     {"id":1,"Text":"PC","ImageURL":"images_icon/computer.png","Children":[
      {"id":2,"Text":"Intel Core i5-9400F","ImageURL":"images_icon/cpu.png","Children":[
        {"id":3,"Text":"Temperatures","ImageURL":"images_icon/temperature.png","Children":[
          {"id":4,"Text":"CPU Core #1","Value":"40.0 °C","ImageURL":"images/transparent.png","Children":[]},
          {"id":5,"Text":"CPU Core #2","Value":"44.0 °C","ImageURL":"images/transparent.png","Children":[]},
          {"id":6,"Text":"CPU Package","Value":"46.0 °C","ImageURL":"images/transparent.png","Children":[]}]}]},
      {"id":7,"Text":"NVIDIA GeForce GTX 1660","ImageURL":"images_icon/nvidia.png","Children":[
        {"id":8,"Text":"Load","ImageURL":"images_icon/load.png","Children":[
          {"id":9,"Text":"GPU Core","Value":"7.0 %","ImageURL":"images/transparent.png","Children":[]}]},
        {"id":10,"Text":"Data","ImageURL":"images_icon/data.png","Children":[
          {"id":11,"Text":"GPU Memory Used","Value":"1.2 GB","ImageURL":"images/transparent.png","Children":[]},
          {"id":12,"Text":"GPU Memory Total","Value":"6.0 GB","ImageURL":"images/transparent.png","Children":[]}]}]}
     ]}]}
            """;

    @Test
    void leTemperaturaDaCpuEmFormatoPtBr() {
        LhmReading r = LhmParser.parse(NEW);
        assertEquals(52.4, r.cpuTemperatureCelsius());
    }

    @Test
    void escolheComoGpuPrincipalAQueTemMaisMemoriaDeVideo() {
        LhmReading r = LhmParser.parse(NEW);
        assertEquals(2, r.gpus().size());
        LhmGpu gpu = r.primaryGpu();
        assertNotNull(gpu);
        assertTrue(gpu.name().contains("RTX 3060"));
        assertEquals(31.0, gpu.loadPercent());
        assertEquals(47.0, gpu.temperatureCelsius());
        assertEquals(Math.round(2234.0 * 1024 * 1024), gpu.vramUsedBytes());
        assertEquals(12288L * 1024 * 1024, gpu.vramTotalBytes());
    }

    @Test
    void ventoinhasIgnoramLeiturasZeradas() {
        assertEquals(List.of(1245), LhmParser.parse(NEW).fanSpeedsRpm());
    }

    @Test
    void entendeOFormatoAntigoSemIdsETipos() {
        LhmReading r = LhmParser.parse(OLD);
        assertEquals(46.0, r.cpuTemperatureCelsius());
        assertEquals(1, r.gpus().size());
        assertEquals(7.0, r.gpus().get(0).loadPercent());
        assertEquals(6L * 1024 * 1024 * 1024, r.gpus().get(0).vramTotalBytes());
    }

    @Test
    void arvoreSemSensoresNaoInventaValores() {
        LhmReading r = LhmParser.parse("{\"Text\":\"Sensor\",\"Children\":[]}");
        assertNull(r.cpuTemperatureCelsius());
        assertTrue(r.gpus().isEmpty());
        assertNull(r.primaryGpu());
        assertTrue(r.fanSpeedsRpm().isEmpty());
    }

    @Test
    void respostaQueNaoEJsonDoLibreHardwareMonitorEhRejeitada() {
        assertThrows(IllegalArgumentException.class, () -> LhmParser.parse("<html>erro</html>"));
        assertThrows(IllegalArgumentException.class, () -> LhmParser.parse("[1, 2]"));
    }

    @Test
    void separadoresDecimaisEDeMilharSaoInterpretados() {
        assertEquals(45.0, LhmParser.parseValue("45,0 \u00b0C").number());
        assertEquals("\u00b0C", LhmParser.parseValue("45,0 \u00b0C").unit());
        assertEquals(1234.5, LhmParser.parseValue("1.234,5 MB").number());
        assertEquals(1234.5, LhmParser.parseValue("1,234.5 MB").number());
        assertEquals(1450.0, LhmParser.parseValue("1450 RPM").number());
        assertNull(LhmParser.parseValue("sem numero"));
        assertNull(LhmParser.parseValue(""));
    }

    @Test
    void conversaoDeUnidadesDeMemoria() {
        assertEquals(1024L * 1024, LhmParser.toBytes(1.0, "MB"));
        assertEquals(1024L * 1024 * 1024, LhmParser.toBytes(1.0, "GB"));
        assertNull(LhmParser.toBytes(1.0, "RPM"));
        assertNull(LhmParser.toBytes(-1.0, "MB"));
    }
}
