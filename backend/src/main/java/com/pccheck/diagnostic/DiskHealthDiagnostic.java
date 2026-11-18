package com.pccheck.diagnostic;

import com.pccheck.agent.model.CollectorResult;
import com.pccheck.agent.model.DiskHealth;
import com.pccheck.agent.model.DiskHealthInfo;
import com.pccheck.agent.model.DiskInfo;
import com.pccheck.agent.model.StorageInfo;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Saúde dos discos (SMART), lida pelo leitor de sensores com permissão de administrador.
 *
 * <p>Avalia o que o disco informa: vida útil restante (SSDs) e temperatura. O PCCheck não lê setores realocados,
 * setores pendentes nem erros de leitura, então "indicadores normais" não garante ausência de falhas. Sem leitura,
 * o alerta diz o motivo e que a saúde física NÃO foi avaliada.
 */
public class DiskHealthDiagnostic implements Diagnostic {

    static final String ID = "smart";
    private static final Locale PT_BR = Locale.of("pt", "BR");

    private final DiskHealthThresholds thresholds;

    public DiskHealthDiagnostic(DiskHealthThresholds thresholds) {
        this.thresholds = thresholds;
    }

    @Override
    public String name() {
        return "DiskHealthDiagnostic";
    }

    @Override
    public List<Alert> evaluate(HealthInput input, Instant now) {
        CollectorResult<DiskHealthInfo> health = input.diskHealth();
        if (health == null || !health.isOk() || health.data() == null || health.data().disks().isEmpty()) {
            String reason = health == null ? "A saúde dos discos não foi coletada."
                    : health.message() != null ? health.message() : "O leitor de sensores não informou discos.";
            return List.of(new Alert(ID, "Discos (SMART)", Severity.INFO, AlertKind.UNAVAILABLE,
                    "Saúde dos discos não lida",
                    reason + " A saúde física dos discos NÃO foi avaliada; isso não significa que estejam saudáveis "
                            + "nem defeituosos.",
                    null, null, now));
        }
        List<Alert> alerts = new ArrayList<>();
        int position = 0;
        for (DiskHealth disk : health.data().disks()) {
            alerts.add(evaluate(disk, mediaType(input, disk), position++, now));
        }
        return alerts;
    }

    private Alert evaluate(DiskHealth d, String mediaType, int position, Instant now) {
        String id = ID + ":" + (d.physicalIndex() != null ? d.physicalIndex() : "p" + position);
        String component = "Disco: " + (d.name() != null ? d.name() : "sem nome")
                + (mediaType != null ? " (" + mediaType + ")" : "");
        boolean hdd = "HDD".equals(mediaType);
        double tempLimit = hdd ? thresholds.hddTempWarningC() : thresholds.ssdTempWarningC();
        String observed = observed(d);
        if (d.lifePercent() == null && d.temperatureCelsius() == null) {
            return new Alert(id, component, Severity.INFO, AlertKind.UNAVAILABLE,
                    "Indicadores de saúde não informados",
                    "O disco não informou vida útil nem temperatura, então a saúde dele NÃO foi avaliada; isso não "
                            + "significa que esteja saudável nem defeituoso.",
                    observed, null, now);
        }
        // Só os limites dos indicadores que o disco informou (HDDs não informam vida útil).
        List<String> limitParts = new ArrayList<>();
        if (d.lifePercent() != null) {
            limitParts.add("Vida útil: Atenção ≤ " + Formatting.percent(thresholds.lifeWarningPercent()) + ", Crítico ≤ "
                    + Formatting.percent(thresholds.lifeCriticalPercent()));
        }
        if (d.temperatureCelsius() != null) {
            limitParts.add("Temperatura: Atenção ≥ " + Formatting.celsius(tempLimit)
                    + (hdd ? " (HDD)" : mediaType == null ? " (tipo desconhecido)" : ""));
        }
        String limits = String.join(" · ", limitParts);

        if (d.lifePercent() != null && d.lifePercent() <= thresholds.lifeCriticalPercent()) {
            return new Alert(id, component, Severity.CRITICAL, AlertKind.PREVENTIVE,
                    "Vida útil do SSD praticamente esgotada",
                    "O próprio disco informa que resta pouca vida útil para gravação. Faça backup dos dados e planeje "
                            + "a troca do disco.",
                    observed, limits, now);
        }
        boolean lowLife = d.lifePercent() != null && d.lifePercent() <= thresholds.lifeWarningPercent();
        boolean hot = d.temperatureCelsius() != null && d.temperatureCelsius() >= tempLimit;
        if (lowLife || hot) {
            List<String> reasons = new ArrayList<>();
            if (lowLife) reasons.add("a vida útil restante informada pelo disco está baixa (mantenha backup em dia)");
            if (hot) reasons.add("a temperatura está acima do recomendado (verifique a ventilação do gabinete)");
            return new Alert(id, component, Severity.WARNING, AlertKind.PREVENTIVE,
                    lowLife ? "Vida útil do SSD baixa" : "Disco com temperatura elevada",
                    "Atenção: " + String.join("; ", reasons) + ".", observed, limits, now);
        }
        String evaluated = d.lifePercent() != null && d.temperatureCelsius() != null
                ? "A vida útil e a temperatura informadas pelo disco estão dentro dos limites."
                : d.lifePercent() != null ? "A vida útil informada pelo disco está dentro dos limites (o disco não "
                        + "informou temperatura)."
                : "A temperatura informada pelo disco está dentro do limite" + (hdd ? " (HDDs não informam vida útil)." : ".");
        return new Alert(id, component, Severity.INFO, AlertKind.NORMAL,
                "Indicadores do disco normais",
                evaluated + " O PCCheck não lê setores realocados nem erros de leitura; por isso, isto não garante que "
                        + "o disco esteja livre de falhas.",
                observed, limits, now);
    }

    private static String observed(DiskHealth d) {
        List<String> parts = new ArrayList<>();
        if (d.lifePercent() != null) parts.add("Vida útil restante " + Formatting.percent(d.lifePercent()));
        if (d.temperatureCelsius() != null) parts.add(Formatting.celsius(d.temperatureCelsius()));
        if (d.powerOnHours() != null) parts.add(String.format(PT_BR, "%,d h ligado", d.powerOnHours()));
        if (d.dataWrittenGb() != null) parts.add(String.format(PT_BR, "%,.0f GB gravados", d.dataWrittenGb()));
        return parts.isEmpty() ? null : String.join(" · ", parts);
    }

    /** Tipo (SSD/HDD) do inventário, ligando pelo número do disco físico (\\.\PHYSICALDRIVEn). */
    private static String mediaType(HealthInput input, DiskHealth disk) {
        CollectorResult<StorageInfo> storage = input.snapshot().storage();
        if (disk.physicalIndex() == null || storage == null || !storage.isOk() || storage.data() == null
                || storage.data().drives() == null) {
            return null;
        }
        String suffix = "PHYSICALDRIVE" + disk.physicalIndex();
        for (DiskInfo drive : storage.data().drives()) {
            if (drive.name() != null && drive.name().toUpperCase(Locale.ROOT).endsWith(suffix)) {
                return drive.mediaType();
            }
        }
        return null;
    }
}
