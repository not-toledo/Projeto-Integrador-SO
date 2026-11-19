package com.pccheck.agent;

import java.util.Map;

/**
 * Variáveis de ambiente do NEXCORE. O produto se chamava PCCheck: uma variável {@code NEXCORE_X} ausente é procurada
 * também como {@code PCCHECK_X}, para quem já tinha configurado o nome antigo.
 */
public final class Env {

    static final String PREFIX = "NEXCORE_";
    static final String LEGACY_PREFIX = "PCCHECK_";

    private Env() {
    }

    /** Valor de {@code name} (ex.: NEXCORE_DATA_DIR); se ausente ou em branco, o da variável com o nome antigo. */
    public static String get(Map<String, String> env, String name) {
        String value = env.get(name);
        if ((value == null || value.isBlank()) && name.startsWith(PREFIX)) {
            value = env.get(LEGACY_PREFIX + name.substring(PREFIX.length()));
        }
        return value;
    }
}
