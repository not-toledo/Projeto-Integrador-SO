package com.pccheck.dto;

import com.pccheck.agent.model.CollectorResult;

/**
 * Resultado de um componente na API.
 *
 * @param status  "OK", "UNAVAILABLE" (o sistema não informou o dado) ou "ERROR" (a coleta falhou)
 * @param message explicação quando status não é OK
 * @param data    dados coletados (nulo quando status não é OK); campos nulos = "não disponível"
 */
public record ComponentDto<T>(String status, String message, T data) {

    public static <T> ComponentDto<T> from(CollectorResult<T> result) {
        return new ComponentDto<>(result.status().name(), result.message(), result.data());
    }
}
