package com.pccheck.diagnostic;

import com.pccheck.agent.model.CollectorResult;
import com.pccheck.agent.model.MemoryInfo;
import java.time.Instant;
import java.util.List;

/** Avalia o uso de RAM. Uso alto gera apenas aviso: nunca é classificado como defeito. */
public class MemoryDiagnostic implements Diagnostic {

    static final String COMPONENT = "Memória RAM";
    static final String ID = "memory:usage";

    private final HealthThresholds thresholds;

    public MemoryDiagnostic(HealthThresholds thresholds) {
        this.thresholds = thresholds;
    }

    @Override
    public String name() {
        return "MemoryDiagnostic";
    }

    @Override
    public List<Alert> evaluate(HealthInput input, Instant now) {
        CollectorResult<MemoryInfo> memory = input.snapshot().memory();
        Double used = (memory.isOk() && memory.data() != null) ? memory.data().usedPercent() : null;

        if (used == null) {
            return List.of(new Alert(ID, COMPONENT, Severity.INFO, AlertKind.UNAVAILABLE,
                    "Uso de RAM não disponível",
                    "O sistema não informou memória total e disponível válidas; o uso não pôde ser avaliado. "
                            + "Isso não indica falha física.",
                    null, null, now));
        }

        MemoryInfo m = memory.data();
        String observed = Formatting.percent(used)
                + (m.usedBytes() != null && m.totalBytes() != null
                ? " (" + Formatting.bytes(m.usedBytes()) + " de " + Formatting.bytes(m.totalBytes()) + ")" : "");
        String limit = "Aviso a partir de " + Formatting.percent(thresholds.ramUsedWarningPercent());

        if (used >= thresholds.ramUsedWarningPercent()) {
            return List.of(new Alert(ID, COMPONENT, Severity.WARNING, AlertKind.PREVENTIVE,
                    "Uso de RAM elevado",
                    "Muitos programas podem estar abertos no momento. Uso alto de memória é normal em algumas "
                            + "situações e não indica defeito do módulo de RAM.",
                    observed, limit, now));
        }
        return List.of(new Alert(ID, COMPONENT, Severity.INFO, AlertKind.NORMAL,
                "Uso de RAM normal",
                "O uso de memória está abaixo do limite de aviso configurado.",
                observed, limit, now));
    }
}
