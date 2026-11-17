package com.pccheck.agent.sensors;

/** Fonte das leituras de sensores e GPU (abstração para permitir testes sem o leitor de sensores). */
@FunctionalInterface
public interface SensorSource {

    /** @throws SensorsUnavailableException se não houver leitura agora; a mensagem é própria para o usuário */
    SensorReading read() throws SensorsUnavailableException;

    /** Fonte que nunca tem leitura (integração desativada ou sistema não suportado). */
    static SensorSource unavailable(String message) {
        return () -> {
            throw new SensorsUnavailableException(message);
        };
    }
}
