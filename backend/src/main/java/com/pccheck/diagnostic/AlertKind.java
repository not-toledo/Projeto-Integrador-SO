package com.pccheck.diagnostic;

/**
 * Natureza do alerta. Distingue indicador preventivo de falha comprovada: o Health Check
 * só observa indicadores; ausência de sensor ou uso alto de RAM não provam defeito.
 */
public enum AlertKind {
    /** Leitura dentro do esperado. */
    NORMAL,
    /** Indicador acima do limite configurado: recomenda atenção, mas não prova defeito. */
    PREVENTIVE,
    /** Dado indisponível (sem sensor, sem suporte, coleta incompleta): não é falha física. */
    UNAVAILABLE
}
