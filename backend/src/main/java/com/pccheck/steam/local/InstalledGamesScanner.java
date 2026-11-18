package com.pccheck.steam.local;

import com.pccheck.agent.model.DiskInfo;
import com.pccheck.agent.model.StorageInfo;
import com.pccheck.agent.model.VolumeInfo;
import com.pccheck.dto.InstalledGamesDto;
import com.pccheck.dto.InstalledGamesDto.Game;
import com.pccheck.dto.InstalledGamesDto.Library;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Collator;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Lê as bibliotecas ({@code steamapps/libraryfolders.vdf}) e os manifestos ({@code appmanifest_<id>.acf}) da Steam.
 * Só leitura: nada é alterado nas pastas da Steam. Campos pessoais do manifesto (ex.: {@code LastOwner}, o ID da
 * conta) não são lidos.
 */
public final class InstalledGamesScanner {

    /** {@code StateFlags}: bits do enum EAppState da Steam. */
    static final long STATE_UPDATE_REQUIRED = 2;
    static final long STATE_FULLY_INSTALLED = 4;

    /** Arquivos reais têm poucos KB; um limite evita ler arquivos estranhos inteiros na memória. */
    static final long MAX_FILE_BYTES = 4L * 1024 * 1024;
    static final int MAX_MANIFESTS_PER_LIBRARY = 5000;

    private final boolean windows;

    public InstalledGamesScanner(boolean windows) {
        this.windows = windows;
    }

    /** Lê a Steam em {@code steamRoot}; {@code storage} (pode ser null) liga cada biblioteca ao volume e ao disco. */
    public InstalledGamesDto scan(Path steamRoot, StorageInfo storage, Instant now) {
        List<String> problems = new ArrayList<>();
        List<Path> libraryPaths = libraries(steamRoot, problems);
        List<Library> libraries = new ArrayList<>();
        List<Game> games = new ArrayList<>();
        for (Path libraryPath : libraryPaths) {
            String mount = mountPointOf(libraryPath.toString(), storage);
            VolumeInfo volume = volume(mount, storage);
            String media = mediaTypeOf(mount, storage);
            boolean available = Files.isDirectory(libraryPath);
            List<Game> found = available ? games(libraryPath, mount, media, problems) : List.of();
            games.addAll(found);
            libraries.add(new Library(libraryPath.toString(), mount, volume == null ? null : volume.freeBytes(),
                    volume == null ? null : volume.totalBytes(), media, available, found.size()));
        }
        Collator collator = Collator.getInstance(Locale.of("pt", "BR"));
        collator.setStrength(Collator.PRIMARY);
        games.sort(Comparator.comparing((Game g) -> g.name() == null ? "" : g.name(), collator)
                .thenComparingLong(Game::appId));
        return new InstalledGamesDto(true, steamRoot.toString(), List.copyOf(libraries), List.copyOf(games),
                List.copyOf(problems), now);
    }

    /** Pastas de bibliotecas: a própria pasta da Steam e as de {@code libraryfolders.vdf}, sem repetição. */
    List<Path> libraries(Path steamRoot, List<String> problems) {
        Map<String, Path> result = new LinkedHashMap<>();
        result.put(key(steamRoot), steamRoot);
        Path file = steamRoot.resolve("steamapps").resolve("libraryfolders.vdf");
        if (!Files.isRegularFile(file)) {
            return List.copyOf(result.values());
        }
        try {
            Map<String, Object> root = Vdf.block(Vdf.parse(read(file)), "libraryfolders");
            if (root == null) {
                problems.add("libraryfolders.vdf sem o bloco \"libraryfolders\"; usada só a pasta principal da Steam.");
                return List.copyOf(result.values());
            }
            for (Map.Entry<String, Object> entry : root.entrySet()) {
                if (!entry.getKey().chars().allMatch(Character::isDigit)) {
                    continue; // ex.: "contentstatsid"
                }
                // Formato atual: bloco com "path"; formato antigo: o valor é o próprio caminho.
                String path = entry.getValue() instanceof String s ? s
                        : Vdf.string(Vdf.block(root, entry.getKey()), "path");
                if (path != null && !path.isBlank()) {
                    Path p = Path.of(path);
                    result.putIfAbsent(key(p), p);
                }
            }
        } catch (IOException | IllegalArgumentException e) {
            problems.add("Não foi possível ler libraryfolders.vdf: " + e.getMessage()
                    + ". Usada só a pasta principal da Steam.");
        }
        return List.copyOf(result.values());
    }

    private List<Game> games(Path library, String mount, String media, List<String> problems) {
        List<Game> result = new ArrayList<>();
        Path steamapps = library.resolve("steamapps");
        if (!Files.isDirectory(steamapps)) {
            return result;
        }
        int count = 0;
        try (DirectoryStream<Path> files = Files.newDirectoryStream(steamapps, "appmanifest_*.acf")) {
            for (Path file : files) {
                if (++count > MAX_MANIFESTS_PER_LIBRARY) {
                    problems.add("Mais de " + MAX_MANIFESTS_PER_LIBRARY + " manifestos em " + steamapps
                            + "; os demais foram ignorados.");
                    break;
                }
                try {
                    Game game = toGame(Vdf.parse(read(file)), library, mount, media);
                    if (game == null) {
                        problems.add(file.getFileName() + ": manifesto sem AppID válido.");
                    } else {
                        result.add(game);
                    }
                } catch (IOException | IllegalArgumentException e) {
                    problems.add(file.getFileName() + ": " + e.getMessage());
                }
            }
        } catch (IOException e) {
            problems.add("Não foi possível listar " + steamapps + ": " + e.getMessage());
        }
        return result;
    }

    /** Conversão pura do manifesto; null sem AppID numérico positivo. */
    static Game toGame(Map<String, Object> manifest, Path library, String mount, String media) {
        Map<String, Object> app = Vdf.block(manifest, "AppState");
        Long appId = positive(Vdf.string(app, "appid"));
        if (appId == null) {
            return null;
        }
        String name = Vdf.string(app, "name");
        Long flags = positive(Vdf.string(app, "StateFlags"));
        Long updated = positive(Vdf.string(app, "LastUpdated"));
        return new Game(appId, name == null || name.isBlank() ? null : name.strip(), Vdf.string(app, "installdir"),
                library.toString(), mount, media, positive(Vdf.string(app, "SizeOnDisk")), state(flags),
                updated == null ? null : Instant.ofEpochSecond(updated),
                "https://store.steampowered.com/app/" + appId + "/");
    }

    static String state(Long flags) {
        if (flags == null || (flags & STATE_FULLY_INSTALLED) == 0) {
            return "INCOMPLETE";
        }
        return (flags & STATE_UPDATE_REQUIRED) != 0 ? "UPDATE_REQUIRED" : "INSTALLED";
    }

    /** Volume do inventário que contém o caminho (o ponto de montagem mais longo que é prefixo dele). */
    String mountPointOf(String path, StorageInfo storage) {
        if (storage == null || storage.volumes() == null) {
            return null;
        }
        String target = normalize(path);
        String best = null;
        for (VolumeInfo v : storage.volumes()) {
            if (v.mountPoint() == null) {
                continue;
            }
            String mount = normalize(v.mountPoint());
            boolean contains = mount.equals("/") ? target.startsWith("/")
                    : target.equals(mount) || target.startsWith(mount.endsWith("/") ? mount : mount + "/");
            if (contains && (best == null || mount.length() > normalize(best).length())) {
                best = v.mountPoint();
            }
        }
        return best;
    }

    private static VolumeInfo volume(String mount, StorageInfo storage) {
        if (mount == null || storage.volumes() == null) {
            return null;
        }
        return storage.volumes().stream().filter(v -> mount.equals(v.mountPoint())).findFirst().orElse(null);
    }

    /** Tipo (SSD/HDD) do disco físico que tem esse volume, pelos pontos de montagem das partições. */
    String mediaTypeOf(String mount, StorageInfo storage) {
        if (mount == null || storage == null || storage.drives() == null) {
            return null;
        }
        String m = normalize(mount);
        for (DiskInfo d : storage.drives()) {
            for (String p : d.mountPoints()) {
                if (normalize(p).equals(m)) {
                    return d.mediaType();
                }
            }
        }
        return null;
    }

    /** Barras unificadas, sem barra final (exceto a raiz) e, no Windows, sem diferenciar maiúsculas. */
    private String normalize(String path) {
        String p = path.replace('\\', '/');
        while (p.length() > 1 && p.endsWith("/") && !(windows && p.length() == 3 && p.charAt(1) == ':')) {
            p = p.substring(0, p.length() - 1);
        }
        return windows ? p.toLowerCase(Locale.ROOT) : p;
    }

    private String key(Path p) {
        return normalize(p.toString());
    }

    private static String read(Path file) throws IOException {
        if (Files.size(file) > MAX_FILE_BYTES) {
            throw new IOException("arquivo maior que " + MAX_FILE_BYTES / (1024 * 1024) + " MiB");
        }
        return Files.readString(file, StandardCharsets.UTF_8);
    }

    private static Long positive(String value) {
        if (value == null) {
            return null;
        }
        try {
            long v = Long.parseLong(value.strip());
            return v > 0 ? v : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
