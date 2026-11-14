package com.pccheck.diagnostic;

import com.pccheck.agent.model.CollectorResult;
import com.pccheck.agent.model.StorageInfo;
import com.pccheck.agent.model.VolumeInfo;
import com.pccheck.agent.collector.Metrics;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Avalia o espaço livre de cada volume (valor, unidade e percentual). */
public class DiskSpaceDiagnostic implements Diagnostic {

    static final String COMPONENT = "Disco";

    /** Imagens somente leitura (ex.: pacotes snap, ISO) ficam sempre 100% cheias por construção. */
    private static final Set<String> READ_ONLY_IMAGE_TYPES = Set.of("squashfs", "iso9660", "udf", "cdfs");

    private final HealthThresholds thresholds;

    public DiskSpaceDiagnostic(HealthThresholds thresholds) {
        this.thresholds = thresholds;
    }

    @Override
    public String name() {
        return "DiskSpaceDiagnostic";
    }

    @Override
    public List<Alert> evaluate(HealthInput input, Instant now) {
        CollectorResult<StorageInfo> storage = input.snapshot().storage();
        List<VolumeInfo> volumes = (storage.isOk() && storage.data() != null) ? storage.data().volumes() : null;

        if (volumes == null || volumes.isEmpty()) {
            return List.of(new Alert("disk-space", COMPONENT, Severity.INFO, AlertKind.UNAVAILABLE,
                    "Espaço em disco não disponível",
                    "O sistema não informou os volumes montados; o espaço livre não pôde ser avaliado. "
                            + "Isso não indica falha física.",
                    null, null, now));
        }

        List<Alert> alerts = new ArrayList<>();
        for (VolumeInfo volume : volumes) {
            if (volume.fileSystem() != null
                    && READ_ONLY_IMAGE_TYPES.contains(volume.fileSystem().toLowerCase(Locale.ROOT))) {
                continue;
            }
            alerts.add(evaluateVolume(volume, now));
        }
        return alerts;
    }

    private Alert evaluateVolume(VolumeInfo v, Instant now) {
        String name = v.mountPoint() != null ? v.mountPoint() : (v.label() != null ? v.label() : "volume sem nome");
        String id = "disk-space:" + name;
        Double freePercent = Metrics.percent(v.freeBytes(), v.totalBytes());

        if (freePercent == null) {
            return new Alert(id, COMPONENT, Severity.INFO, AlertKind.UNAVAILABLE,
                    "Espaço livre de " + name + " não disponível",
                    "O sistema não informou capacidade ou espaço livre válidos para este volume.",
                    null, null, now);
        }

        String observed = Formatting.bytes(v.freeBytes()) + " livres de " + Formatting.bytes(v.totalBytes())
                + " (" + Formatting.percent(freePercent) + " livre)";
        if (freePercent < thresholds.diskFreeCriticalPercent()) {
            return new Alert(id, COMPONENT, Severity.CRITICAL, AlertKind.PREVENTIVE,
                    "Pouco espaço livre em " + name,
                    "O espaço livre está abaixo do nível crítico configurado. O sistema e os programas podem "
                            + "ficar lentos ou falhar ao gravar arquivos. Não indica defeito da unidade.",
                    observed, "Crítico abaixo de " + Formatting.percent(thresholds.diskFreeCriticalPercent())
                            + " livre", now);
        }
        if (freePercent < thresholds.diskFreeWarningPercent()) {
            return new Alert(id, COMPONENT, Severity.WARNING, AlertKind.PREVENTIVE,
                    "Espaço livre baixo em " + name,
                    "O espaço livre está abaixo do limite de atenção configurado. Considere liberar espaço. "
                            + "Não indica defeito da unidade.",
                    observed, "Atenção abaixo de " + Formatting.percent(thresholds.diskFreeWarningPercent())
                            + " livre", now);
        }
        return new Alert(id, COMPONENT, Severity.INFO, AlertKind.NORMAL,
                "Espaço livre adequado em " + name,
                "O espaço livre está acima do limite de atenção configurado.",
                observed, "Atenção abaixo de " + Formatting.percent(thresholds.diskFreeWarningPercent())
                        + " livre", now);
    }
}
