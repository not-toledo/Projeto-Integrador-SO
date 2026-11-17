package com.pccheck.agent.lhm;

/** Fonte das leituras do LibreHardwareMonitor (abstração para permitir testes sem o programa). */
@FunctionalInterface
public interface LhmSource {

    /** @throws LhmUnavailableException se o LibreHardwareMonitor não estiver acessível ou responder algo inválido */
    LhmReading read() throws LhmUnavailableException;
}
