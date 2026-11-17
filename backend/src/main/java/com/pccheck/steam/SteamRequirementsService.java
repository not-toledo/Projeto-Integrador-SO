package com.pccheck.steam;

import com.fasterxml.jackson.databind.JsonNode;
import com.pccheck.dto.GameRequirementsDto;
import com.pccheck.dto.RequirementFieldDto;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class SteamRequirementsService {
    private record Cached(GameRequirementsDto value, Instant expiresAt) {}
    private final SteamStoreClient client;
    private final SteamRequirementsParser parser;
    private final ConcurrentHashMap<Long, Cached> cache = new ConcurrentHashMap<>();

    public SteamRequirementsService(SteamStoreClient client, SteamRequirementsParser parser) {
        this.client = client;
        this.parser = parser;
    }

    public GameRequirementsDto get(long appId, boolean refresh) {
        if (appId <= 0) throw new IllegalArgumentException("AppID deve ser positivo.");
        Cached cached = cache.get(appId);
        if (!refresh && cached != null && cached.expiresAt().isAfter(Instant.now())) return cached.value();

        JsonNode root = client.details(appId).path(Long.toString(appId));
        if (!root.path("success").asBoolean(false)) throw new SteamUnavailableException("A Steam não encontrou detalhes para o AppID " + appId + ".");
        JsonNode data = root.path("data");
        if (!"game".equals(data.path("type").asText())) throw new IllegalArgumentException("O AppID selecionado não corresponde a um jogo.");
        JsonNode requirements = data.path("pc_requirements");
        String minimum = html(requirements.path("minimum"));
        String recommended = html(requirements.path("recommended"));
        List<String> warnings = new ArrayList<>();
        if (minimum.isBlank()) warnings.add("A Steam não publicou requisitos mínimos para Windows.");
        if (recommended.isBlank()) warnings.add("A Steam não publicou requisitos recomendados para Windows.");
        if (minimum.isBlank() && recommended.isBlank()) warnings.add("Não há requisitos de PC interpretáveis para este jogo.");
        String name = data.path("name").asText("Jogo sem nome");
        String image = data.path("header_image").asText(null);
        GameRequirementsDto result = new GameRequirementsDto(appId, name, image,
                "https://store.steampowered.com/app/" + appId + "/", minimum, recommended,
                parser.parse(minimum), parser.parse(recommended), Instant.now(),
                "Steam Store — https://store.steampowered.com/api/appdetails", List.copyOf(warnings));
        cache.put(appId, new Cached(result, Instant.now().plusSeconds(43200)));
        return result;
    }

    private String html(JsonNode node) {
        if (node.isTextual()) return node.asText();
        if (node.isObject()) return node.path("html").asText("");
        return "";
    }
}
