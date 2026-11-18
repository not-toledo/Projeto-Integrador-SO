package com.pccheck.diagnostic;

import com.pccheck.agent.model.CollectorResult;
import com.pccheck.agent.model.MemoryInfo;
import com.pccheck.agent.model.OsInfo;
import com.pccheck.agent.model.PagingActivity;
import com.pccheck.agent.model.VirtualMemoryInfo;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Pressão de memória: vai além do percentual de RAM em uso.
 *
 * <ul>
 *   <li><b>Memória comprometida</b> ({@code memory:commit}): no Windows, o total reservado pelos programas não pode
 *       passar do limite (RAM + arquivo de paginação); perto dele, alocações podem falhar. No Linux o kernel permite
 *       ultrapassar o limite (overcommit), então o valor é só informativo.</li>
 *   <li><b>Paginação sustentada</b> ({@code memory:paging}): o SO gravando páginas no disco de forma contínua
 *       enquanto a RAM está cheia é o sinal clássico de falta de memória. Usa a média das amostras recentes do Live
 *       Monitor, nunca uma leitura isolada.</li>
 * </ul>
 * Nenhum dos dois indica defeito no módulo de RAM.
 */
public class MemoryPressureDiagnostic implements Diagnostic {

    static final String COMPONENT = "Memória virtual";
    static final String COMMIT_ID = "memory:commit";
    static final String PAGING_ID = "memory:paging";

    private final HealthThresholds health;
    private final MemoryPressureThresholds thresholds;

    public MemoryPressureDiagnostic(HealthThresholds health, MemoryPressureThresholds thresholds) {
        this.health = health;
        this.thresholds = thresholds;
    }

    @Override
    public String name() {
        return "MemoryPressureDiagnostic";
    }

    @Override
    public List<Alert> evaluate(HealthInput input, Instant now) {
        CollectorResult<MemoryInfo> memory = input.snapshot().memory();
        MemoryInfo m = memory.isOk() ? memory.data() : null;
        return List.of(commit(m, isWindows(input), now), paging(m, input.recentPaging(), now));
    }

    private Alert commit(MemoryInfo m, boolean windows, Instant now) {
        VirtualMemoryInfo vm = m == null ? null : m.virtualMemory();
        if (vm == null || vm.commitPercent() == null) {
            return new Alert(COMMIT_ID, COMPONENT, Severity.INFO, AlertKind.UNAVAILABLE,
                    "Memória comprometida não disponível",
                    "O sistema não informou a memória comprometida e o seu limite; este item não foi avaliado.",
                    null, null, now);
        }
        String observed = Formatting.bytes(vm.commitUsedBytes()) + " de " + Formatting.bytes(vm.commitLimitBytes())
                + " (" + Formatting.percent(vm.commitPercent()) + ")";
        String limit = "Aviso a partir de " + Formatting.percent(thresholds.commitWarningPercent()) + " (Windows)";
        if (!windows) {
            return new Alert(COMMIT_ID, COMPONENT, Severity.INFO, AlertKind.NORMAL,
                    "Memória comprometida (informativo)",
                    "No Linux, o kernel permite reservar mais memória do que o limite (overcommit); este valor não "
                            + "indica, sozinho, falta de memória. A paginação sustentada é o indicador usado.",
                    observed, limit, now);
        }
        if (vm.commitPercent() >= thresholds.commitWarningPercent()) {
            return new Alert(COMMIT_ID, COMPONENT, Severity.WARNING, AlertKind.PREVENTIVE,
                    "Memória comprometida perto do limite",
                    "Os programas abertos reservaram quase toda a memória que o Windows pode garantir (RAM + arquivo "
                            + "de paginação). No limite, o Windows aumenta o arquivo de paginação ou programas podem "
                            + "falhar ao alocar memória. Fechar programas ou ter mais RAM reduz o problema. Não indica "
                            + "defeito no hardware.",
                    observed, limit, now);
        }
        return new Alert(COMMIT_ID, COMPONENT, Severity.INFO, AlertKind.NORMAL,
                "Memória comprometida normal",
                "A memória reservada pelos programas está abaixo do limite de aviso.",
                observed, limit, now);
    }

    private Alert paging(MemoryInfo m, List<PagingActivity> recent, Instant now) {
        List<Double> rates = recent.stream()
                .skip(Math.max(0, recent.size() - thresholds.windowSamples()))
                .map(PagingActivity::pagesOutBytesPerSecond)
                .filter(Objects::nonNull)
                .toList();
        String limit = "Aviso com média a partir de " + Formatting.bytes((long) thresholds.pageOutWarningBytesPerSec())
                + "/s e RAM a partir de " + Formatting.percent(health.ramUsedWarningPercent());
        if (rates.size() < thresholds.minSamples()) {
            return new Alert(PAGING_ID, COMPONENT, Severity.INFO, AlertKind.UNAVAILABLE,
                    "Paginação ainda não avaliada",
                    "A paginação é medida pelas amostras do Live Monitor; são necessárias pelo menos "
                            + thresholds.minSamples() + " amostras (há " + rates.size() + "). Tente novamente em "
                            + "alguns segundos.",
                    null, limit, now);
        }
        double average = rates.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        Double ramUsed = m == null ? null : m.usedPercent();
        String observed = String.format(Locale.ROOT, "Média de %s/s gravados em disco (%d amostras)",
                Formatting.bytes((long) average), rates.size())
                + (ramUsed == null ? "" : "; RAM em " + Formatting.percent(ramUsed));
        boolean heavyPaging = average >= thresholds.pageOutWarningBytesPerSec();
        boolean ramFull = ramUsed != null && ramUsed >= health.ramUsedWarningPercent();
        if (heavyPaging && ramFull) {
            return new Alert(PAGING_ID, COMPONENT, Severity.WARNING, AlertKind.PREVENTIVE,
                    "Pressão de memória: paginação constante",
                    "Com a RAM quase cheia, o sistema está gravando páginas de memória no disco continuamente para "
                            + "liberar espaço. Isso deixa o computador mais lento, porque o disco é muito mais lento que "
                            + "a RAM. Fechar programas ou ter mais RAM resolve. Não indica defeito no hardware.",
                    observed, limit, now);
        }
        if (heavyPaging) {
            return new Alert(PAGING_ID, COMPONENT, Severity.INFO, AlertKind.NORMAL,
                    "Gravação de páginas sem RAM cheia",
                    "Há gravação de páginas em disco, mas a RAM não está cheia: no Windows isso inclui gravação de "
                            + "arquivos mapeados em memória e não caracteriza falta de RAM.",
                    observed, limit, now);
        }
        return new Alert(PAGING_ID, COMPONENT, Severity.INFO, AlertKind.NORMAL,
                "Sem paginação sustentada",
                "O sistema não está gravando páginas de memória no disco de forma contínua.",
                observed, limit, now);
    }

    private static boolean isWindows(HealthInput input) {
        CollectorResult<OsInfo> os = input.snapshot().os();
        return os != null && os.isOk() && os.data() != null && os.data().name() != null
                && os.data().name().toLowerCase(Locale.ROOT).contains("windows");
    }
}
