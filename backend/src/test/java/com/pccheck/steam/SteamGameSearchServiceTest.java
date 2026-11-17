package com.pccheck.steam;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class SteamGameSearchServiceTest {
    @Test
    void mapsSteamSearchItemsAndCapsAtTen() throws Exception {
        SteamStoreClient client = mock(SteamStoreClient.class);
        StringBuilder json = new StringBuilder("{\"items\":[");
        for (int i = 1; i <= 12; i++) {
            if (i > 1) json.append(",");
            json.append("{\"id\":").append(i).append(",\"name\":\"Game ").append(i)
                    .append("\",\"tiny_image\":\"https://cdn.example/image.png\"}");
        }
        json.append("]}");
        when(client.search("Game")).thenReturn(new ObjectMapper().readTree(json.toString()));
        var service = new SteamGameSearchService(client, 300);
        var results = service.search("Game");
        assertEquals(10, results.size());
        assertEquals(1L, results.get(0).appId());
        assertTrue(results.get(0).storeUrl().contains("/app/1/"));
    }

    @Test
    void rejectsShortSearchTermsBeforeCallingSteam() {
        SteamStoreClient client = mock(SteamStoreClient.class);
        var service = new SteamGameSearchService(client, 300);
        assertThrows(IllegalArgumentException.class, () -> service.search("x"));
        verifyNoInteractions(client);
    }
}
