package com.pccheck.steam;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class SteamRequirementsServiceTest {
    @Test
    void keepsRawSteamRequirementsAndParsesThem() throws Exception {
        SteamStoreClient client = mock(SteamStoreClient.class);
        var json = new ObjectMapper().readTree("""
            {"730":{"success":true,"data":{"type":"game","name":"Counter-Strike 2",
            "header_image":"https://cdn.example/header.jpg",
            "pc_requirements":{"minimum":"<li>Memory: 8 GB RAM</li>","recommended":"<li>Memory: 16 GB RAM</li>"}}}}
            """);
        when(client.details(730)).thenReturn(json);
        var service = new SteamRequirementsService(client, new SteamRequirementsParser());
        var result = service.get(730, false);
        assertEquals("Counter-Strike 2", result.name());
        assertTrue(result.minimumRawText().contains("<li>"));
        assertEquals(8_000_000_000L, result.minimum().get(0).parsedBytes());
        assertEquals(16_000_000_000L, result.recommended().get(0).parsedBytes());
    }

    @Test
    void separaRecomendadosPublicadosDentroDosMinimos() throws Exception {
        // HTML real do Portal (AppID 400) na Steam em português: mínimos e recomendados no mesmo campo.
        SteamStoreClient client = mock(SteamStoreClient.class);
        String minimum = "<p><strong>Mínimo: </strong>Processador de 1.7 GHz, 512 MB de RAM, Placa de Vídeo com "
                + "DirectX&reg; 8.1 (Requer Suporte para SSE), Windows&reg; Vista/XP, Mouse, Teclado, Conexão com a "
                + "Internet</p>\n\n<p><strong>Recomendado: </strong>Processador Pentium 4 (3.0 GHz, ou melhor), "
                + "1 GB de RAM, Placa de Vídeo com DirectX&reg; 9, Windows&reg; Vista/XP, Mouse, Teclado, Conexão "
                + "com a Internet</p>";
        var root = new ObjectMapper().createObjectNode();
        var data = root.putObject("400").put("success", true).putObject("data");
        data.put("type", "game").put("name", "Portal");
        data.putObject("pc_requirements").put("minimum", minimum);
        when(client.details(400)).thenReturn(root);

        var result = new SteamRequirementsService(client, new SteamRequirementsParser()).get(400, false);

        assertEquals(minimum, result.minimumRawText());
        assertEquals(512_000_000L, only(result.minimum(), "RAM").parsedBytes());
        assertEquals(1_000_000_000L, only(result.recommended(), "RAM").parsedBytes());
        assertEquals("Processador Pentium 4 (3.0 GHz, ou melhor)", only(result.recommended(), "CPU").rawText());
        assertEquals("Placa de Vídeo com DirectX® 8.1 (Requer Suporte para SSE)",
                only(result.minimum(), "GPU").rawText());
        assertFalse(result.warnings().contains("A Steam não publicou requisitos recomendados para Windows."));
    }

    private static com.pccheck.dto.RequirementFieldDto only(java.util.List<com.pccheck.dto.RequirementFieldDto> fields,
                                                            String component) {
        var matches = fields.stream().filter(f -> component.equals(f.component())).toList();
        assertEquals(1, matches.size(), component + " em " + fields);
        return matches.get(0);
    }

    @Test
    void rejectsNonGameAppIds() throws Exception {
        SteamStoreClient client = mock(SteamStoreClient.class);
        when(client.details(10)).thenReturn(new ObjectMapper().readTree("""
            {"10":{"success":true,"data":{"type":"dlc","name":"Some DLC"}}}
            """));
        var service = new SteamRequirementsService(client, new SteamRequirementsParser());
        assertThrows(IllegalArgumentException.class, () -> service.get(10, false));
    }
}
