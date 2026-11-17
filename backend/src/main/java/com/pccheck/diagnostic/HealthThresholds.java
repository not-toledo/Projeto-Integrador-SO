package com.pccheck.diagnostic;

/**
 * Limites usados pelo Health Check. São configuráveis (application.properties, prefixo
 * {@code pccheck.health.}).
 *
 * <p>Origem dos valores padrão: limites genéricos e conservadores definidos pelo projeto, usados na
 * ausência de um limite do fabricante. NÃO são limites oficiais do seu modelo de processador ou disco:
 * ajuste-os conforme a documentação do fabricante do seu equipamento.
 *
 * @param cpuTempWarningC          temperatura (°C) a partir da qual há Atenção
 * @param cpuTempCriticalC         temperatura (°C) a partir da qual é Crítico
 * @param diskFreeWarningPercent   espaço livre (%) abaixo do qual há Atenção
 * @param diskFreeCriticalPercent  espaço livre (%) abaixo do qual é Crítico
 * @param ramUsedWarningPercent    uso de RAM (%) a partir do qual há aviso de uso elevado
 */
public record HealthThresholds(double cpuTempWarningC, double cpuTempCriticalC,
                               double diskFreeWarningPercent, double diskFreeCriticalPercent,
                               double ramUsedWarningPercent) {

    public static final String SOURCE =
            "Valores padrão do projeto PCCheck (genéricos e conservadores; não são limites oficiais do fabricante). "
                    + "Configuráveis em application.properties (pccheck.health.*).";

    public HealthThresholds {
        if (!(cpuTempWarningC > 0 && cpuTempCriticalC > cpuTempWarningC)) {
            throw new IllegalArgumentException("Temperatura: é preciso 0 < atenção < crítico.");
        }
        if (!(diskFreeCriticalPercent >= 0 && diskFreeWarningPercent > diskFreeCriticalPercent
                && diskFreeWarningPercent <= 100)) {
            throw new IllegalArgumentException("Disco: é preciso 0 <= crítico < atenção <= 100 (espaço livre em %).");
        }
        if (!(ramUsedWarningPercent > 0 && ramUsedWarningPercent <= 100)) {
            throw new IllegalArgumentException("RAM: o limite de aviso deve estar entre 0 e 100.");
        }
    }

    /** Valores padrão. */
    public static HealthThresholds defaults() {
        return new HealthThresholds(80.0, 95.0, 15.0, 5.0, 85.0);
    }
}
