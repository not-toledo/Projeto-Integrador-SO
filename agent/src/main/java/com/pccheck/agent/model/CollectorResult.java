package com.pccheck.agent.model;

/**
 * Resultado de um coletor. Garante que a falha de um coletor seja representada
 * como dado, sem derrubar os demais.
 *
 * @param collector nome do coletor
 * @param status    estado da coleta
 * @param data      dados coletados (somente quando status = OK)
 * @param message   explicação quando status != OK
 */
public record CollectorResult<T>(String collector, CollectorStatus status, T data, String message) {

    public static <T> CollectorResult<T> ok(String collector, T data) {
        return new CollectorResult<>(collector, CollectorStatus.OK, data, null);
    }

    public static <T> CollectorResult<T> unavailable(String collector, String message) {
        return new CollectorResult<>(collector, CollectorStatus.UNAVAILABLE, null, message);
    }

    public static <T> CollectorResult<T> error(String collector, String message) {
        return new CollectorResult<>(collector, CollectorStatus.ERROR, null, message);
    }

    public boolean isOk() {
        return status == CollectorStatus.OK;
    }
}
