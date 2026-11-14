package com.pccheck.diagnostic;

import java.time.Instant;
import java.util.List;

/**
 * SMART. A coleta atual (OSHI) não expõe atributos SMART, e lê-los exigiria ferramenta externa
 * e privilégios elevados. Por isso a V0.5 informa claramente que não é suportado, em vez de
 * inventar um estado de saúde para a unidade.
 */
public class SmartDiagnostic implements Diagnostic {

    @Override
    public String name() {
        return "SmartDiagnostic";
    }

    @Override
    public List<Alert> evaluate(HealthInput input, Instant now) {
        return List.of(new Alert("smart", "SMART", Severity.INFO, AlertKind.UNAVAILABLE,
                "SMART não suportado nesta versão",
                "Os indicadores SMART dos discos não são lidos por esta versão do PCCheck (a biblioteca de coleta "
                        + "não os fornece e a leitura depende de driver e permissões). A saúde física dos discos "
                        + "NÃO foi avaliada; isso não significa que estejam saudáveis nem defeituosos.",
                null, null, now));
    }
}
