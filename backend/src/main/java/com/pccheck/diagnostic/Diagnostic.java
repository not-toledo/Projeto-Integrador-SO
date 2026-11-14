package com.pccheck.diagnostic;

import java.time.Instant;
import java.util.List;

/** Regra de diagnóstico independente. Somente observa e informa; nunca altera o sistema. */
public interface Diagnostic {

    /** Nome da verificação (usado quando a regra falha). */
    String name();

    List<Alert> evaluate(HealthInput input, Instant now);
}
