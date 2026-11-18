package com.pccheck.precheck;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pccheck.diagnostic.HealthThresholds;
import com.pccheck.dto.GameRequirementsDto;
import com.pccheck.dto.InstalledGamesDto;
import com.pccheck.dto.InstalledGamesDto.Game;
import com.pccheck.dto.InstalledGamesDto.Library;
import com.pccheck.dto.RequirementFieldDto;
import com.pccheck.os.CommandRunner;
import com.pccheck.precheck.PrecheckDto.Item;
import com.pccheck.precheck.PrecheckDto.ProcessGroup;
import com.pccheck.precheck.SystemProbe.ProcessLoad;
import com.pccheck.precheck.SystemState.PowerPlan;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class PrecheckTest {

    static final long GIB = 1024L * 1024 * 1024;
    static final Instant NOW = Instant.parse("2026-11-18T12:00:00Z");
    static final HealthThresholds T = HealthThresholds.defaults();

    static GameRequirementsDto requirements(long ramBytes, boolean ssd) {
        List<RequirementFieldDto> min = new java.util.ArrayList<>(List.of(
                new RequirementFieldDto("RAM", "8 GB RAM", ramBytes, "8 GB", 0.9, "PARSED")));
        if (ssd) {
            min.add(new RequirementFieldDto("SSD", "SSD required", null, "SSD", 0.8, "PARSED"));
        }
        return new GameRequirementsDto(900001, "Aventura de Teste", null, null, "", "", min, List.of(), NOW, "teste",
                List.of());
    }

    static InstalledGamesDto installed(String state, String media, long free, long total) {
        Library lib = new Library("D:\\SteamLibrary", "D:\\", free, total, media, true, 1);
        Game game = new Game(900001, "Aventura de Teste", "Aventura", "D:\\SteamLibrary", "D:\\", media, 45 * GIB,
                state, null, "https://store.steampowered.com/app/900001/");
        return new InstalledGamesDto(true, "C:\\Steam", List.of(lib), List.of(game), List.of(), NOW);
    }

    static SystemState state(long availableRam, double cpu, PowerPlan plan, Boolean battery) {
        return new SystemState(16 * GIB, availableRam, cpu,
                List.of(new ProcessGroup("brave", 12, 3.2, 2 * GIB), new ProcessGroup("java", 2, 9.0, 300 * 1024 * 1024L),
                        new ProcessGroup("Memory Compression", 1, 0.0, 582 * 1024 * 1024L)),
                null, plan, battery, null);
    }

    static Item item(PrecheckDto dto, String id) {
        return dto.items().stream().filter(i -> i.id().equals(id)).findFirst().orElseThrow();
    }

    static final PowerPlan BALANCED = new PowerPlan("BALANCED", "Balanced", null);

    @Test
    void pcLivreComJogoAtualizadoNumSsdEstaPronto() {
        PrecheckDto dto = PrecheckEvaluator.evaluate(900001, requirements(8 * GIB, true), null,
                installed("INSTALLED", "SSD", 300 * GIB, 1000 * GIB), state(10 * GIB, 12, BALANCED, false), T, NOW);
        assertEquals("READY", dto.overall());
        assertEquals("OK", item(dto, "ram").status());
        assertEquals("OK", item(dto, "game-ssd").status());
        assertEquals("Aventura de Teste", dto.gameName());
        // Ranking por CPU sem processos parados; ranking por memória com todos.
        assertEquals(List.of("java", "brave"), dto.topCpu().stream().map(ProcessGroup::name).toList());
        assertEquals("brave", dto.topMemory().get(0).name());
    }

    @Test
    void cadaProblemaViraAtencaoComOMotivo() {
        PrecheckDto dto = PrecheckEvaluator.evaluate(900001, requirements(8 * GIB, true), null,
                installed("UPDATE_REQUIRED", "HDD", 50 * GIB, 1000 * GIB),
                state(3 * GIB, 75, new PowerPlan("POWER_SAVER", "Economia de energia", null), true), T, NOW);
        assertEquals("ATTENTION", dto.overall());
        assertEquals("ATTENTION", item(dto, "ram").status(), "3 GiB livres < 50% de 8 GiB");
        assertEquals("ATTENTION", item(dto, "cpu").status());
        assertTrue(item(dto, "cpu").detail().contains("brave"));
        assertEquals("ATTENTION", item(dto, "install").status());
        assertEquals("ATTENTION", item(dto, "game-disk-space").status(), "5% livre < 15%");
        assertEquals("ATTENTION", item(dto, "game-ssd").status());
        assertEquals("ATTENTION", item(dto, "power").status());
        assertEquals("ATTENTION", item(dto, "battery").status());
    }

    @Test
    void memoriaEntreMetadeEOMinimoSoInforma() {
        Item ram = PrecheckEvaluator.ram(requirements(8 * GIB, false), null, state(5 * GIB, 10, BALANCED, false));
        assertEquals("INFO", ram.status());
        assertEquals("5,0 GiB disponíveis de 16,0 GiB", ram.observed());
        assertTrue(ram.detail().startsWith("O mínimo publicado é 8 GB."), "valor como a Steam publicou");
    }

    @Test
    void semRequisitosOuSemLeituraNadaEInventado() {
        PrecheckDto dto = PrecheckEvaluator.evaluate(900001, null, "A Steam não respondeu.", null,
                new SystemState(null, null, null, List.of(), "Sem acesso aos processos.",
                        new PowerPlan("UNAVAILABLE", null, "O powercfg não respondeu."), null, null), T, NOW);
        assertEquals("READY", dto.overall(), "nada lido não vira atenção, mas também não vira ok");
        for (String id : List.of("ram", "cpu", "install", "power", "battery")) {
            assertEquals("UNAVAILABLE", item(dto, id).status(), id);
        }
        assertEquals("AppID 900001", dto.gameName());
        assertEquals("Sem acesso aos processos.", dto.processError());
    }

    @Test
    void requisitosNaoObtidosMasMemoriaLida() {
        Item ram = PrecheckEvaluator.ram(null, "A Steam não respondeu.", state(6 * GIB, 10, BALANCED, false));
        assertEquals("INFO", ram.status());
        assertTrue(ram.detail().contains("A Steam não respondeu."));
    }

    @Test
    void jogoNaoInstaladoSoInforma() {
        InstalledGamesDto vazio = new InstalledGamesDto(true, "C:\\Steam", List.of(), List.of(), List.of(), NOW);
        PrecheckDto dto = PrecheckEvaluator.evaluate(900001, requirements(8 * GIB, true), null, vazio,
                state(10 * GIB, 10, BALANCED, false), T, NOW);
        assertEquals("INFO", item(dto, "install").status());
        assertTrue(dto.items().stream().noneMatch(i -> i.id().equals("game-ssd")));
    }

    @Test
    void planoDeEnergiaPelaSaidaRealDoPowercfg() {
        // Saída real (Windows em inglês) e a mesma em português.
        PowerPlan en = SystemProbe.parseWindowsPlan(
                "Power Scheme GUID: 381b4222-f694-41f0-9685-ff5bb260df2e  (Balanced)\r\n");
        assertEquals("BALANCED", en.kind());
        assertEquals("Balanced", en.name());
        PowerPlan pt = SystemProbe.parseWindowsPlan(
                "GUID do Esquema de Energia: a1841308-3541-4fab-bc81-f71556f20b4a  (Economia de energia)");
        assertEquals("POWER_SAVER", pt.kind());
        PowerPlan oem = SystemProbe.parseWindowsPlan("Power Scheme GUID: 11111111-2222-3333-4444-555555555555  (Gamer OEM)");
        assertEquals("OTHER", oem.kind());
        assertEquals("Gamer OEM", oem.name());
        assertNull(SystemProbe.parseWindowsPlan("Invalid Parameters -- try \"/?\" for help"));
        assertEquals("INFO", PrecheckEvaluator.power(oem).status());
        assertEquals("INFO", PrecheckEvaluator.power(new PowerPlan("LINUX_GOVERNOR", "powersave", null)).status(),
                "o governador do Linux não é julgado");
    }

    @Test
    void powercfgSemRespostaOuComFalha() {
        SystemProbe semResposta = new SystemProbe(true, cmd -> null);
        SystemProbe falha = new SystemProbe(true, cmd -> new CommandRunner.Result(1, "Invalid Parameters"));
        // Só o plano de energia é testado aqui (sample() também lê o sistema real).
        assertEquals("UNAVAILABLE", semResposta.sample().powerPlan().kind());
        assertEquals("UNAVAILABLE", falha.sample().powerPlan().kind());
    }

    @Test
    void processosAgrupadosEmPercentualDoTotalDaCpu() {
        // Valores reais medidos (8 processadores lógicos): carga somada entre os núcleos.
        List<ProcessLoad> loads = List.of(
                new ProcessLoad(0, "Idle", 5.395, 0),
                new ProcessLoad(5628, "java", 0.717, 119L << 20),
                new ProcessLoad(10124, "brave", 0.255, 1093L << 20),
                new ProcessLoad(17424, "Brave", 0.134, 88L << 20),
                new ProcessLoad(42, "pccheck-backend", 0.5, 400L << 20));
        List<ProcessGroup> groups = SystemProbe.group(loads, 8, 42, true);
        assertEquals(2, groups.size(), "sem o processo ocioso e sem o próprio PCCheck");
        assertEquals("java", groups.get(0).name());
        assertEquals(0.717 / 8 * 100, groups.get(0).cpuPercent(), 1e-9);
        ProcessGroup brave = groups.get(1);
        assertEquals(2, brave.processCount(), "brave e Brave são o mesmo aplicativo no Windows");
        assertEquals((1093L + 88L) << 20, brave.memoryBytes());
    }
}
