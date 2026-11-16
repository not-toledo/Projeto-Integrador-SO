package com.pccheck.agent.lhm;

/** O LibreHardwareMonitor não está acessível. A mensagem é própria para exibir ao usuário. */
public class LhmUnavailableException extends Exception {

    public LhmUnavailableException(String message) {
        super(message);
    }

    public LhmUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
