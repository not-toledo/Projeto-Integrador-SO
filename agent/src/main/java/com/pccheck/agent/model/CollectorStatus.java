package com.pccheck.agent.model;

/** Estado do resultado de um coletor. */
public enum CollectorStatus {
    /** Coleta concluída com dados. */
    OK,
    /** O sistema/driver não forneceu o dado (não é falha física). */
    UNAVAILABLE,
    /** A coleta lançou uma exceção. */
    ERROR
}
