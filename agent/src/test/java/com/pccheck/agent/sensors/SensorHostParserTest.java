package com.pccheck.agent.sensors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class SensorHostParserTest {

    /** Formato real do leitor (valores de teste). */
    private static final String FULL = """
            {"version":1,"elevated":true,"driver":true,"error":null,"hardware":[
              {"type":"Motherboard","name":"Placa de teste","sensors":[]},
              {"type":"SuperIO","name":"Nuvoton NCT6798D","sensors":[
                {"type":"Fan","name":"Fan #1","value":1245.5},
                {"type":"Fan","name":"Fan #2","value":0},
                {"type":"Voltage","name":"Vcore","value":1.2}]},
              {"type":"Cpu","name":"CPU de teste","sensors":[
                {"type":"Load","name":"CPU Total","value":12.5},
                {"type":"Temperature","name":"Core #1","value":50},
                {"type":"Temperature","name":"CPU Package","value":55.25}]},
              {"type":"GpuIntel","name":"GPU integrada","sensors":[
                {"type":"Load","name":"D3D 3D","value":3},
                {"type":"SmallData","name":"D3D Shared Memory Used","value":120}]},
              {"type":"GpuNvidia","name":"GPU dedicada","sensors":[
                {"type":"Temperature","name":"GPU Core","value":43},
                {"type":"Fan","name":"GPU","value":1072},
                {"type":"Load","name":"GPU Core","value":21},
                {"type":"SmallData","name":"GPU Memory Used","value":742.5},
                {"type":"SmallData","name":"GPU Memory Total","value":4096}]}
            ]}""";

    @Test
    void temperaturaDaCpuPrefereOPacote() {
        assertEquals(55.25, SensorHostParser.parse(FULL).cpuTemperatureCelsius());
    }

    @Test
    void semPacoteUsaAMaiorTemperaturaDeNucleoValida() {
        String json = """
                {"elevated":true,"driver":true,"hardware":[{"type":"Cpu","name":"c","sensors":[
                  {"type":"Temperature","name":"Core #1","value":48},
                  {"type":"Temperature","name":"Core #2","value":52},
                  {"type":"Temperature","name":"Distance to TjMax","value":-1}]}]}""";
        assertEquals(52.0, SensorHostParser.parse(json).cpuTemperatureCelsius());
    }

    @Test
    void gpuPrincipalEAQueTemMaisMemoriaDeVideo() {
        SensorReading r = SensorHostParser.parse(FULL);
        assertEquals(2, r.gpus().size());
        GpuReading gpu = r.primaryGpu();
        assertEquals("GPU dedicada", gpu.name());
        assertEquals(21.0, gpu.loadPercent());
        assertEquals(43.0, gpu.temperatureCelsius());
        assertEquals(Math.round(742.5 * 1024 * 1024), gpu.vramUsedBytes());
        assertEquals(4096L * 1024 * 1024, gpu.vramTotalBytes());
    }

    @Test
    void ventoinhasSomenteComRotacaoValida() {
        assertEquals(List.of(1246, 1072), SensorHostParser.parse(FULL).fanSpeedsRpm());
    }

    @Test
    void semLimitacaoQuandoElevadoEComDriver() {
        assertNull(SensorHostParser.parse(FULL).limitation());
    }

    @Test
    void explicaQuandoFaltaAdministradorOuDriver() {
        String noAdmin = FULL.replace("\"elevated\":true", "\"elevated\":false");
        assertEquals(SensorHostParser.NOT_ELEVATED, SensorHostParser.parse(noAdmin).limitation());
        String noDriver = FULL.replace("\"driver\":true", "\"driver\":false");
        assertEquals(SensorHostParser.NO_DRIVER, SensorHostParser.parse(noDriver).limitation());
    }

    @Test
    void erroDoLeitorViraLimitacaoESemValores() {
        SensorReading r = SensorHostParser.parse(
                "{\"elevated\":true,\"driver\":true,\"error\":\"Falha X\",\"hardware\":[]}");
        assertNull(r.cpuTemperatureCelsius());
        assertNull(r.primaryGpu());
        assertTrue(r.fanSpeedsRpm().isEmpty());
        assertTrue(r.limitation().contains("Falha X"));
    }

    @Test
    void valoresForaDaFaixaSaoDescartados() {
        String json = """
                {"elevated":true,"driver":true,"hardware":[{"type":"GpuAmd","name":"g","sensors":[
                  {"type":"Load","name":"GPU Core","value":140},
                  {"type":"Temperature","name":"GPU Core","value":-5}]}]}""";
        GpuReading gpu = SensorHostParser.parse(json).primaryGpu();
        assertNull(gpu.loadPercent());
        assertNull(gpu.temperatureCelsius());
    }

    @Test
    void memoriaEmGbQuandoOTipoEData() {
        String json = """
                {"elevated":true,"driver":true,"hardware":[{"type":"GpuAmd","name":"g","sensors":[
                  {"type":"Data","name":"GPU Memory Total","value":8}]}]}""";
        assertEquals(8L * 1024 * 1024 * 1024, SensorHostParser.parse(json).primaryGpu().vramTotalBytes());
    }

    @Test
    void respostaInvalidaGeraErro() {
        assertThrows(IllegalArgumentException.class, () -> SensorHostParser.parse("<html>erro</html>"));
        assertThrows(IllegalArgumentException.class, () -> SensorHostParser.parse("[1, 2]"));
        assertThrows(IllegalArgumentException.class, () -> SensorHostParser.parse("{\"elevated\":true}"));
        assertThrows(IllegalArgumentException.class, () -> SensorHostParser.parse(""));
    }
}
