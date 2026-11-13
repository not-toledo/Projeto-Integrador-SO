package com.pccheck.agent;

/**
 * Ponto de entrada do agente local do PCCheck.
 *
 * V0.1: apenas inicia e informa a versão e o ambiente Java em uso.
 * Nenhuma coleta de hardware é feita nesta versão (começa na V0.2).
 */
public final class PcCheckAgent {

    public static final String VERSION = "0.1.0";

    private PcCheckAgent() {
    }

    public static void main(String[] args) {
        System.out.println("PCCheck Agent v" + VERSION);
        System.out.println("Java: " + System.getProperty("java.version")
                + " (" + System.getProperty("java.vendor") + ")");
        System.out.println("Agente iniciado. Coleta de hardware ainda nao implementada (V0.2).");
    }
}
