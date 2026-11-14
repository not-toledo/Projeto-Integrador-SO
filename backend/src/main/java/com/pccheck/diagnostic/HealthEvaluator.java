package com.pccheck.diagnostic;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Executa os diagnósticos de forma isolada: a falha de uma regra vira um alerta informativo
 * e não impede as demais.
 */
public class HealthEvaluator {

    private final HealthThresholds thresholds;
    private final List<Diagnostic> diagnostics;

    public HealthEvaluator(HealthThresholds thresholds) {
        this(thresholds, List.of(
                new TemperatureDiagnostic(thresholds),
                new DiskSpaceDiagnostic(thresholds),
                new MemoryDiagnostic(thresholds),
                new SmartDiagnostic(),
                new CollectionDiagnostic()));
    }

    public HealthEvaluator(HealthThresholds thresholds, List<Diagnostic> diagnostics) {
        this.thresholds = thresholds;
        this.diagnostics = List.copyOf(diagnostics);
    }

    public HealthReport evaluate(HealthInput input, Instant now) {
        List<Alert> alerts = new ArrayList<>();
        for (Diagnostic diagnostic : diagnostics) {
            try {
                alerts.addAll(diagnostic.evaluate(input, now));
            } catch (RuntimeException e) {
                alerts.add(new Alert("diagnostic-error:" + diagnostic.name(), "Health Check", Severity.INFO,
                        AlertKind.UNAVAILABLE, "Verificação não concluída: " + diagnostic.name(),
                        "Esta verificação não pôde ser executada (" + e.getClass().getSimpleName()
                                + "). É uma falha do diagnóstico, não do computador.",
                        null, null, now));
            }
        }
        // Ordenação estável: mais grave primeiro, preservando a ordem das verificações.
        alerts.sort(Comparator.comparing(Alert::severity).reversed());

        int info = 0;
        int warning = 0;
        int critical = 0;
        for (Alert a : alerts) {
            switch (a.severity()) {
                case INFO -> info++;
                case WARNING -> warning++;
                case CRITICAL -> critical++;
            }
        }
        Severity overall = critical > 0 ? Severity.CRITICAL : (warning > 0 ? Severity.WARNING : Severity.INFO);
        return new HealthReport(now, HealthReport.SOURCE, overall, info, warning, critical, thresholds,
                HealthThresholds.SOURCE, List.copyOf(alerts));
    }
}
