package com.pccheck.diagnostic;

/** Severidades padronizadas dos alertas (ordem crescente de gravidade). */
public enum Severity {
    INFO("Informação"),
    WARNING("Atenção"),
    CRITICAL("Crítico");

    private final String label;

    Severity(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
