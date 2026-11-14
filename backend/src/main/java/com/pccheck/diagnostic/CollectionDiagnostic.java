package com.pccheck.diagnostic;

import com.pccheck.agent.model.CollectorResult;
import com.pccheck.agent.model.SystemSnapshot;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Detecta coleta incompleta (componente sem leitura ou não identificado). Informa o dado ausente;
 * jamais o trata como falha física. RAM e armazenamento têm tratamento próprio nos respectivos diagnósticos.
 */
public class CollectionDiagnostic implements Diagnostic {

    @Override
    public String name() {
        return "CollectionDiagnostic";
    }

    @Override
    public List<Alert> evaluate(HealthInput input, Instant now) {
        SystemSnapshot s = input.snapshot();
        List<Alert> alerts = new ArrayList<>();
        check(alerts, "collection:os", "Sistema operacional", s.os(), now);
        check(alerts, "collection:cpu", "CPU", s.cpu(), now);
        check(alerts, "collection:gpu", "GPU", s.gpu(), now);
        check(alerts, "collection:motherboard", "Placa-mãe", s.motherboard(), now);
        return alerts;
    }

    private static void check(List<Alert> alerts, String id, String component, CollectorResult<?> result,
                              Instant now) {
        if (result == null || result.isOk()) {
            return;
        }
        String detail = result.message() != null ? " Detalhe: " + result.message() : "";
        alerts.add(new Alert(id, component, Severity.INFO, AlertKind.UNAVAILABLE,
                "Coleta incompleta: " + component,
                "Os dados deste componente não puderam ser lidos ou identificados. É uma limitação da coleta "
                        + "(sistema, driver ou permissões), não uma falha física." + detail,
                null, null, now));
    }
}
