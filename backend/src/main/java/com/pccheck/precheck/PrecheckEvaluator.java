package com.pccheck.precheck;

import com.pccheck.diagnostic.Formatting;
import com.pccheck.diagnostic.HealthThresholds;
import com.pccheck.dto.GameRequirementsDto;
import com.pccheck.dto.InstalledGamesDto;
import com.pccheck.dto.RequirementFieldDto;
import com.pccheck.precheck.PrecheckDto.Item;
import com.pccheck.precheck.PrecheckDto.ProcessGroup;
import com.pccheck.precheck.SystemState.PowerPlan;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Regras da pré-checagem (função pura). Os limites são heurísticas do projeto, não valores oficiais:
 * <ul>
 *   <li>RAM: Atenção se a memória disponível agora for menor que {@value #RAM_FREE_ATTENTION_FRACTION} do mínimo
 *   publicado (o jogo disputaria memória com os programas abertos);</li>
 *   <li>CPU: Atenção se a CPU já estiver em {@value #CPU_BUSY_PERCENT}% ou mais sem o jogo;</li>
 *   <li>Espaço: o mesmo limite de espaço livre do Health Check.</li>
 * </ul>
 * Dado que não pôde ser lido fica "não verificado", nunca "ok".
 */
public final class PrecheckEvaluator {

    static final double RAM_FREE_ATTENTION_FRACTION = 0.5;
    static final double CPU_BUSY_PERCENT = 50;
    static final int TOP = 5;

    private PrecheckEvaluator() {
    }

    /**
     * @param requirements requisitos do jogo; null se não puderam ser obtidos ({@code requirementsError} diz o motivo)
     * @param installed    jogos instalados; null se a leitura falhou
     */
    public static PrecheckDto evaluate(long appId, GameRequirementsDto requirements, String requirementsError,
                                       InstalledGamesDto installed, SystemState state, HealthThresholds thresholds,
                                       Instant now) {
        InstalledGamesDto.Game game = installed == null ? null
                : installed.games().stream().filter(g -> g.appId() == appId).findFirst().orElse(null);
        InstalledGamesDto.Library library = game == null ? null
                : installed.libraries().stream().filter(l -> l.path().equals(game.libraryPath())).findFirst().orElse(null);
        String name = requirements != null && requirements.name() != null ? requirements.name()
                : game != null && game.name() != null ? game.name() : "AppID " + appId;

        List<Item> items = new ArrayList<>();
        items.add(ram(requirements, requirementsError, state));
        items.add(cpu(state));
        items.addAll(install(installed, game, library, requirements, thresholds));
        items.add(power(state.powerPlan()));
        items.add(battery(state));

        boolean attention = items.stream().anyMatch(i -> "ATTENTION".equals(i.status()));
        List<ProcessGroup> topCpu = byCpu(state).stream().filter(p -> p.cpuPercent() >= 0.1).limit(TOP).toList();
        List<ProcessGroup> topMemory = state.processes().stream()
                .sorted(Comparator.comparingLong(ProcessGroup::memoryBytes).reversed()).limit(TOP).toList();
        return new PrecheckDto(appId, name, attention ? "ATTENTION" : "READY",
                attention ? "Atenção antes de jogar" : "Nenhum ponto de atenção no estado atual do PC",
                List.copyOf(items), topCpu, topMemory, state.processError(), now);
    }

    private static List<ProcessGroup> byCpu(SystemState state) {
        return state.processes().stream().sorted(Comparator.comparingDouble(ProcessGroup::cpuPercent).reversed()).toList();
    }

    static Item ram(GameRequirementsDto requirements, String requirementsError, SystemState state) {
        String label = "Memória disponível agora";
        if (state.availableRamBytes() == null) {
            return new Item("ram", label, "UNAVAILABLE", null, "Não foi possível ler a memória disponível.");
        }
        String observed = Formatting.bytes(state.availableRamBytes()) + " disponíveis"
                + (state.totalRamBytes() != null ? " de " + Formatting.bytes(state.totalRamBytes()) : "");
        RequirementFieldDto field = requirements == null ? null : requirements.minimum().stream()
                .filter(f -> "RAM".equals(f.component()) && f.parsedBytes() != null).findFirst().orElse(null);
        Long minimum = field == null ? null : field.parsedBytes();
        if (minimum == null) {
            return new Item("ram", label, "INFO", observed, requirements == null
                    ? "Requisitos do jogo não obtidos (" + requirementsError + "); a memória não foi comparada."
                    : "A Steam não publicou um requisito mínimo de RAM interpretável; a memória não foi comparada.");
        }
        // Como a Steam publicou ("16 GB"); converter para GiB mostraria um número diferente do da loja.
        String need = "O mínimo publicado é " + (field.parsedValue() != null ? field.parsedValue()
                : Formatting.bytes(minimum)) + ".";
        if (state.availableRamBytes() >= minimum) {
            return new Item("ram", label, "OK", observed, need + " Há memória livre suficiente agora.");
        }
        if (state.availableRamBytes() >= minimum * RAM_FREE_ATTENTION_FRACTION) {
            return new Item("ram", label, "INFO", observed, need + " Parte da memória está com programas abertos; "
                    + "fechar os que não forem necessários libera memória para o jogo.");
        }
        return new Item("ram", label, "ATTENTION", observed, need + " Há pouca memória livre agora: feche programas "
                + "pesados (veja a lista abaixo) antes de jogar, ou o sistema vai paginar para o disco.");
    }

    static Item cpu(SystemState state) {
        String label = "CPU em uso agora";
        if (state.systemCpuPercent() == null) {
            return new Item("cpu", label, "UNAVAILABLE", null, "Não foi possível medir o uso da CPU.");
        }
        String observed = Formatting.percent(state.systemCpuPercent()) + " em uso (medido em cerca de 1 s)";
        if (state.systemCpuPercent() >= CPU_BUSY_PERCENT) {
            String top = byCpu(state).stream().limit(3).map(p -> p.name() + " (" + Formatting.percent(p.cpuPercent()) + ")")
                    .collect(Collectors.joining(", "));
            return new Item("cpu", label, "ATTENTION", observed, "A CPU já está ocupada sem o jogo"
                    + (top.isEmpty() ? "." : ": " + top + ".") + " Feche ou aguarde esses programas antes de jogar.");
        }
        return new Item("cpu", label, "OK", observed, "Sem uso alto de CPU por outros programas neste momento.");
    }

    static List<Item> install(InstalledGamesDto installed, InstalledGamesDto.Game game,
                              InstalledGamesDto.Library library, GameRequirementsDto requirements,
                              HealthThresholds thresholds) {
        if (installed == null || !installed.steamFound()) {
            return List.of(new Item("install", "Instalação do jogo", "UNAVAILABLE", null, installed == null
                    ? "Não foi possível ler as bibliotecas da Steam."
                    : "Steam não encontrada neste PC; o disco do jogo não foi verificado."));
        }
        if (game == null) {
            return List.of(new Item("install", "Instalação do jogo", "INFO", "Não instalado",
                    "O jogo não está nas bibliotecas da Steam deste PC; disco e atualizações não foram verificados."));
        }
        List<Item> items = new ArrayList<>();
        String where = game.mountPoint() == null ? game.libraryPath()
                : game.mountPoint() + (game.mediaType() != null ? " (" + game.mediaType() + ")" : "");
        items.add(switch (game.state()) {
            case "UPDATE_REQUIRED" -> new Item("install", "Instalação do jogo", "ATTENTION", "Atualização pendente",
                    "A Steam vai baixar e instalar a atualização antes de abrir o jogo; com a internet lenta, isso pode "
                            + "demorar. Instalado em " + where + ".");
            case "INSTALLED" -> new Item("install", "Instalação do jogo", "OK", "Instalado e atualizado",
                    "Instalado em " + where + ".");
            default -> new Item("install", "Instalação do jogo", "ATTENTION", "Instalação incompleta",
                    "A Steam informa que o download ou a instalação não terminaram.");
        });

        if (library == null || library.freeBytes() == null || library.totalBytes() == null || library.totalBytes() <= 0) {
            items.add(new Item("game-disk-space", "Espaço livre no disco do jogo", "UNAVAILABLE", null,
                    "O volume da biblioteca não foi identificado no inventário."));
        } else {
            double freePercent = library.freeBytes() * 100.0 / library.totalBytes();
            String observed = Formatting.bytes(library.freeBytes()) + " livres (" + Formatting.percent(freePercent)
                    + ") em " + library.mountPoint();
            items.add(freePercent < thresholds.diskFreeWarningPercent()
                    ? new Item("game-disk-space", "Espaço livre no disco do jogo", "ATTENTION", observed,
                    "Pouco espaço livre (Atenção abaixo de " + Formatting.percent(thresholds.diskFreeWarningPercent())
                            + "): atualizações, saves e arquivos temporários do jogo precisam de espaço.")
                    : new Item("game-disk-space", "Espaço livre no disco do jogo", "OK", observed,
                    "Espaço livre acima do limite de atenção (" + Formatting.percent(thresholds.diskFreeWarningPercent())
                            + ")."));
        }

        if (requiresSsd(requirements)) {
            items.add("HDD".equals(game.mediaType())
                    ? new Item("game-ssd", "SSD exigido pelo jogo", "ATTENTION", "Instalado num HDD (" + game.mountPoint() + ")",
                    "Os requisitos pedem SSD, e o jogo está num HDD: o carregamento pode ser lento ou travar. A Steam "
                            + "permite mover a pasta de instalação para um SSD nas propriedades do jogo.")
                    : "SSD".equals(game.mediaType())
                    ? new Item("game-ssd", "SSD exigido pelo jogo", "OK", "Instalado num SSD (" + game.mountPoint() + ")",
                    "O jogo está num SSD, como pedem os requisitos.")
                    : new Item("game-ssd", "SSD exigido pelo jogo", "UNAVAILABLE", null,
                    "Os requisitos pedem SSD, mas o SO não informou o tipo do disco onde o jogo está."));
        }
        return items;
    }

    private static boolean requiresSsd(GameRequirementsDto requirements) {
        return requirements != null && Stream.concat(requirements.minimum().stream(), requirements.recommended().stream())
                .anyMatch(f -> "SSD".equals(f.component()));
    }

    static Item power(PowerPlan plan) {
        String label = "Plano de energia";
        if (plan == null || "UNAVAILABLE".equals(plan.kind())) {
            return new Item("power", label, "UNAVAILABLE", null,
                    plan == null ? "Plano de energia não lido." : plan.error());
        }
        return switch (plan.kind()) {
            case "POWER_SAVER" -> new Item("power", label, "ATTENTION", plan.name(),
                    "O plano Economia de energia limita o desempenho do processador. Para jogar, use Equilibrado ou "
                            + "Alto desempenho (Painel de Controle > Opções de Energia).");
            case "BALANCED", "HIGH_PERFORMANCE" -> new Item("power", label, "OK", plan.name(),
                    "Plano padrão do Windows adequado para jogos.");
            case "LINUX_GOVERNOR" -> new Item("power", "Governador da CPU", "INFO", plan.name(),
                    "Política de frequência da CPU informada pelo kernel (cpufreq). O NEXCORE não avalia o governador: "
                            + "o efeito depende do driver (ex.: \"powersave\" no intel_pstate ainda acelera sob carga).");
            default -> new Item("power", label, "INFO", plan.name(),
                    "Plano personalizado ou do fabricante; o NEXCORE não avalia os ajustes dele.");
        };
    }

    static Item battery(SystemState state) {
        String label = "Bateria";
        if (state.onBattery() == null) {
            return new Item("battery", label, "UNAVAILABLE", null, "Não foi possível ler o estado da bateria.");
        }
        if (state.onBattery()) {
            return new Item("battery", label, "ATTENTION", state.batteryPercent() == null ? "Usando bateria"
                    : "Usando bateria (" + Formatting.percent(state.batteryPercent()) + ")",
                    "Na bateria, o sistema costuma reduzir o desempenho da CPU e da GPU. Conecte o carregador.");
        }
        return new Item("battery", label, "OK", "Não está usando bateria",
                "Ligado na tomada, ou computador sem bateria.");
    }
}
