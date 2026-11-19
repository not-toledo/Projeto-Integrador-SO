package com.pccheck.steam.local;

import com.pccheck.agent.Env;
import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinReg;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Encontra a pasta da Steam. Windows: registro ({@code HKCU\Software\Valve\Steam\SteamPath}, depois
 * {@code HKLM\SOFTWARE\WOW6432Node\Valve\Steam\InstallPath}) e a pasta padrão. Linux: as pastas usadas pelo
 * pacote nativo e pelo Flatpak. A variável {@code NEXCORE_STEAM_DIR} tem prioridade (Steam em local não padrão).
 */
public final class SteamLocator {

    private SteamLocator() {
    }

    /** Candidatas em ordem de prioridade (podem não existir). */
    public static List<Path> candidates(boolean windows, String override, String home, List<String> registryPaths) {
        List<Path> result = new ArrayList<>();
        if (override != null && !override.isBlank()) {
            result.add(Path.of(override.strip()));
        }
        if (windows) {
            for (String p : registryPaths) {
                if (p != null && !p.isBlank()) {
                    result.add(Path.of(p.strip()));
                }
            }
            result.add(Path.of("C:\\Program Files (x86)\\Steam"));
        } else if (home != null && !home.isBlank()) {
            result.add(Path.of(home, ".steam", "steam"));
            result.add(Path.of(home, ".local", "share", "Steam"));
            result.add(Path.of(home, ".var", "app", "com.valvesoftware.Steam", ".local", "share", "Steam"));
        }
        return result;
    }

    /** Primeira candidata que tem a pasta {@code steamapps}; null se nenhuma. */
    public static Path find(List<Path> candidates, Predicate<Path> isDirectory) {
        for (Path p : candidates) {
            if (isDirectory.test(p.resolve("steamapps"))) {
                return p;
            }
        }
        return null;
    }

    /** Pasta da Steam deste computador; null se não encontrada. */
    public static Path locate() {
        boolean windows = System.getProperty("os.name", "").toLowerCase().startsWith("windows");
        List<String> registry = windows ? List.of(
                registry(WinReg.HKEY_CURRENT_USER, "Software\\Valve\\Steam", "SteamPath"),
                registry(WinReg.HKEY_LOCAL_MACHINE, "SOFTWARE\\WOW6432Node\\Valve\\Steam", "InstallPath")) : List.of();
        return find(candidates(windows, Env.get(System.getenv(), "NEXCORE_STEAM_DIR"), System.getProperty("user.home"),
                registry), Files::isDirectory);
    }

    private static String registry(WinReg.HKEY root, String key, String value) {
        try {
            return Advapi32Util.registryValueExists(root, key, value)
                    ? Advapi32Util.registryGetStringValue(root, key, value) : null;
        } catch (RuntimeException | LinkageError e) {
            return null; // sem acesso ao registro: tenta a próxima candidata
        }
    }
}
