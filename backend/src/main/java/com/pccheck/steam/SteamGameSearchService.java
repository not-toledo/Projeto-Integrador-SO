package com.pccheck.steam;

import com.fasterxml.jackson.databind.JsonNode;
import com.pccheck.dto.GameSearchResultDto;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SteamGameSearchService {
    private record Cached(List<GameSearchResultDto> value, Instant expiresAt) {}
    private final SteamStoreClient client;
    private final long cacheSeconds;
    private final ConcurrentHashMap<String, Cached> cache = new ConcurrentHashMap<>();

    public SteamGameSearchService(SteamStoreClient client,
            @Value("${pccheck.steam.search-cache-seconds:300}") long cacheSeconds) {
        this.client = client;
        this.cacheSeconds = Math.max(0, cacheSeconds);
    }

    public List<GameSearchResultDto> search(String query) {
        if (query == null || query.trim().length() < 2 || query.trim().length() > 100)
            throw new IllegalArgumentException("Informe um nome de jogo entre 2 e 100 caracteres.");
        String term = query.trim();
        Cached cached = cache.get(term.toLowerCase());
        if (cached != null && cached.expiresAt().isAfter(Instant.now())) return cached.value();
        JsonNode root = client.search(term);
        JsonNode items = root.path("items");
        List<GameSearchResultDto> results = new ArrayList<>();
        if (items.isArray()) {
            for (JsonNode item : items) {
                long id = item.path("id").asLong(0);
                String name = item.path("name").asText("");
                if (id > 0 && !name.isBlank() && results.size() < 10) {
                    String image = item.path("tiny_image").asText(null);
                    results.add(new GameSearchResultDto(id, name, image,
                            "https://store.steampowered.com/app/" + id + "/"));
                }
            }
        }
        List<GameSearchResultDto> immutable = List.copyOf(results);
        cache.put(term.toLowerCase(), new Cached(immutable, Instant.now().plusSeconds(cacheSeconds)));
        return immutable;
    }
}
