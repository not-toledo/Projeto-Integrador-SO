package com.pccheck.agent.collector;

/**
 * Lançada por um coletor quando o dado não está disponível por um motivo conhecido. O
 * {@link CollectorRunner} a converte em UNAVAILABLE com esta mensagem (em vez da mensagem genérica).
 */
public class CollectorUnavailableException extends Exception {

    public CollectorUnavailableException(String message) {
        super(message);
    }
}
