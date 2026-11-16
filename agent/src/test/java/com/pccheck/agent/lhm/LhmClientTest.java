package com.pccheck.agent.lhm;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LhmClientTest {

    @Test
    void aceitaSomenteEnderecosLocais() {
        LhmClient.validate("http://127.0.0.1:8085/data.json");
        LhmClient.validate("http://localhost:8085/data.json");
        assertThrows(IllegalArgumentException.class, () -> LhmClient.validate("http://192.168.0.10:8085/data.json"));
        assertThrows(IllegalArgumentException.class, () -> LhmClient.validate("http://exemplo.com/data.json"));
        assertThrows(IllegalArgumentException.class, () -> LhmClient.validate("https://127.0.0.1:8085/data.json"));
        assertThrows(IllegalArgumentException.class, () -> LhmClient.validate("nao-e-url"));
    }

    @Test
    void urlEmBrancoDesativaAIntegracaoSemQuebrar() {
        LhmUnavailableException e = assertThrows(LhmUnavailableException.class, () -> LhmClient.create("  ").read());
        assertTrue(e.getMessage().contains("desativada"));
    }
}
