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
    void rejectsNonGameAppIds() throws Exception {
        SteamStoreClient client = mock(SteamStoreClient.class);
        when(client.details(10)).thenReturn(new ObjectMapper().readTree("""
            {"10":{"success":true,"data":{"type":"dlc","name":"Some DLC"}}}
            """));
        var service = new SteamRequirementsService(client, new SteamRequirementsParser());
        assertThrows(IllegalArgumentException.class, () -> service.get(10, false));
    }
}
