package com.pccheck.agent.sensors;

/** Não há leitura de sensores agora. A mensagem é própria para exibir ao usuário. */
public class SensorsUnavailableException extends Exception {

    public SensorsUnavailableException(String message) {
        super(message);
    }

    public SensorsUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
