package com.pccheck.agent.lhm;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Set;

/**
 * Lê o {@code data.json} do servidor web do LibreHardwareMonitor (Options → Remote Web Server → Run).
 *
 * <p>Segurança: só aceita endereços locais (localhost, 127.0.0.1 ou ::1); o PCCheck não consulta
 * máquinas remotas. Uma leitura recente (menos de 1 s) é reaproveitada, para que Live Monitor e
 * Health Check não consultem o programa duas vezes seguidas.
 */
public final class LhmClient implements LhmSource {

    public static final String DEFAULT_URL = "http://127.0.0.1:8085/data.json";

    private static final Set<String> LOCAL_HOSTS = Set.of("localhost", "127.0.0.1", "[::1]", "::1");
    private static final long CACHE_MS = 1000;

    private final URI uri;
    private final HttpClient http;
    private LhmReading cached;
    private long cachedAtNanos;

    /** @throws IllegalArgumentException se a URL não for http e local */
    public LhmClient(String url) {
        this.uri = validate(url);
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(800)).build();
    }

    /**
     * Cria a fonte para a URL configurada. URL em branco desativa a integração (a fonte informa
     * "indisponível" em toda leitura).
     *
     * @throws IllegalArgumentException se a URL não for http e local
     */
    public static LhmSource create(String url) {
        if (url == null || url.isBlank()) {
            return () -> {
                throw new LhmUnavailableException(
                        "A integração com o LibreHardwareMonitor está desativada (pccheck.lhm.url vazio).");
            };
        }
        return new LhmClient(url);
    }

    static URI validate(String url) {
        URI parsed;
        try {
            parsed = URI.create(url == null ? "" : url.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Endereço do LibreHardwareMonitor inválido: " + url, e);
        }
        String host = parsed.getHost();
        if (!"http".equalsIgnoreCase(parsed.getScheme()) || host == null
                || !LOCAL_HOSTS.contains(host.toLowerCase())) {
            throw new IllegalArgumentException(
                    "O endereço do LibreHardwareMonitor deve ser http e local (localhost ou 127.0.0.1): " + url);
        }
        return parsed;
    }

    @Override
    public synchronized LhmReading read() throws LhmUnavailableException {
        long now = System.nanoTime();
        if (cached != null && (now - cachedAtNanos) / 1_000_000L < CACHE_MS) {
            return cached;
        }
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofMillis(1500))
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> response;
        try {
            response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (ConnectException | HttpConnectTimeoutException e) {
            throw notRunning(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LhmUnavailableException("Leitura do LibreHardwareMonitor interrompida.", e);
        } catch (IOException e) {
            throw notRunning(e);
        }
        if (response.statusCode() != 200) {
            throw new LhmUnavailableException("O LibreHardwareMonitor respondeu com o código HTTP "
                    + response.statusCode() + " em " + uri + " (verifique se a autenticação do servidor está desativada).");
        }
        LhmReading reading;
        try {
            reading = LhmParser.parse(response.body());
        } catch (IllegalArgumentException e) {
            throw new LhmUnavailableException("Resposta inesperada do LibreHardwareMonitor em " + uri + ": "
                    + e.getMessage(), e);
        }
        cached = reading;
        cachedAtNanos = now;
        return reading;
    }

    private LhmUnavailableException notRunning(Exception cause) {
        return new LhmUnavailableException("O LibreHardwareMonitor não respondeu em " + uri
                + ". Abra-o como administrador e ative Options > Remote Web Server > Run.", cause);
    }
}
