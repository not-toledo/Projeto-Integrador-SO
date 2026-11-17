package com.pccheck.steam;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;

class SteamStoreClientTest {
    private SteamStoreClient clientFor(HttpClient http, int status, String body) throws Exception {
        @SuppressWarnings("unchecked")
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(status);
        when(response.body()).thenReturn(body);
        doReturn(response).when(http).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
        return new SteamStoreClient(new ObjectMapper(), "br", "brazilian", 8000, http);
    }

    @Test
    void buildsFixedSteamSearchUrlAndParsesResponse() throws Exception {
        HttpClient http = mock(HttpClient.class);
        SteamStoreClient client = clientFor(http, 200, "{\"items\":[]}");
        assertTrue(client.search("Dota 2").path("items").isArray());
        verify(http).send(argThat(request -> request.uri().toString().contains(
                "https://store.steampowered.com/api/storesearch/?term=Dota+2&cc=br&l=brazilian")),
                any(HttpResponse.BodyHandler.class));
    }

    @Test
    void handlesRateLimitAndServerErrors() throws Exception {
        SteamStoreClient limited = clientFor(mock(HttpClient.class), 429, "rate limited");
        assertTrue(assertThrows(SteamUnavailableException.class, () -> limited.search("Dota")).getMessage().contains("429"));

        SteamStoreClient unavailable = clientFor(mock(HttpClient.class), 503, "unavailable");
        assertTrue(assertThrows(SteamUnavailableException.class, () -> unavailable.search("Dota")).getMessage().contains("503"));
    }

    @Test
    void handlesInvalidJsonAndRejectsInvalidAppId() throws Exception {
        SteamStoreClient invalid = clientFor(mock(HttpClient.class), 200, "{invalid");
        assertThrows(SteamUnavailableException.class, () -> invalid.search("Dota"));
        HttpClient http = mock(HttpClient.class);
        SteamStoreClient client = clientFor(http, 200, "{}");
        assertThrows(IllegalArgumentException.class, () -> client.details(0));
        verify(http, never()).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }
}
