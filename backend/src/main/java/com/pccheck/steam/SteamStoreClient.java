
package com.pccheck.steam;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Cliente isolado dos endpoints públicos da Steam Store. */
@Component
public class SteamStoreClient {

    private final HttpClient http;
    private final ObjectMapper mapper;
    private final String country;
    private final String language;
    private final Duration readTimeout;

    @Autowired
    public SteamStoreClient(
            ObjectMapper mapper,
            @Value("${nexcore.steam.country:br}") String country,
            @Value("${nexcore.steam.language:brazilian}") String language,
            @Value("${nexcore.steam.connect-timeout-ms:5000}") long connectTimeout,
            @Value("${nexcore.steam.read-timeout-ms:8000}") long readTimeout) {

        this(
                mapper,
                country,
                language,
                readTimeout,
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofMillis(Math.max(1000, connectTimeout)))
                        .build()
        );
    }

    /** Construtor package-private para testes isolados da camada HTTP. */
    SteamStoreClient(
            ObjectMapper mapper,
            String country,
            String language,
            long readTimeout,
            HttpClient http) {

        this.mapper = mapper;
        this.country = country;
        this.language = language;
        this.readTimeout = Duration.ofMillis(Math.max(1000, readTimeout));
        this.http = http;
    }

    public JsonNode search(String term) {

        String encoded = URLEncoder.encode(term, StandardCharsets.UTF_8);

        return get(
                "https://store.steampowered.com/api/storesearch/?term="
                        + encoded
                        + "&cc=" + country
                        + "&l=" + language
        );
    }

    public JsonNode details(long appId) {

        if (appId <= 0) {
            throw new IllegalArgumentException("AppID deve ser positivo.");
        }

        return get(
                "https://store.steampowered.com/api/appdetails?appids="
                        + appId
                        + "&cc=" + country
                        + "&l=" + language
        );
    }

    private JsonNode get(String url) {

        try {

            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(readTimeout)
                    .header("Accept", "application/json")
                    .header("User-Agent", "NEXCORE/1.1")
                    .GET()
                    .build();

            HttpResponse<String> response = http.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() == 429) {
                throw new SteamUnavailableException(
                        "A Steam limitou as consultas (HTTP 429). Tente novamente mais tarde."
                );
            }

            if (response.statusCode() >= 500) {
                throw new SteamUnavailableException(
                        "A Steam está temporariamente indisponível (HTTP "
                                + response.statusCode() + ")."
                );
            }

            if (response.statusCode() != 200) {
                throw new SteamUnavailableException(
                        "A Steam respondeu com HTTP "
                                + response.statusCode() + "."
                );
            }

            JsonNode body = mapper.readTree(response.body());

            if (body == null) {
                throw new SteamUnavailableException(
                        "A Steam retornou uma resposta vazia."
                );
            }

            return body;

        } catch (SteamUnavailableException e) {

            throw e;

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            throw new SteamUnavailableException(
                    "Consulta à Steam interrompida.",
                    e
            );

        } catch (Exception e) {

            throw new SteamUnavailableException(
                    "Não foi possível consultar a Steam: " + e.getMessage(),
                    e
            );
        }
    }
}
