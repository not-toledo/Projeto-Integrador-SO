package com.pccheck.steam.local;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pccheck.agent.model.DiskInfo;
import com.pccheck.agent.model.StorageInfo;
import com.pccheck.agent.model.VolumeInfo;
import com.pccheck.dto.InstalledGamesDto;
import com.pccheck.dto.InstalledGamesDto.Game;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Formato real dos arquivos da Steam (conferido numa instalação real), com valores fictícios. */
class InstalledGamesScannerTest {

    static final Instant NOW = Instant.parse("2026-11-18T12:00:00Z");

    @TempDir
    Path temp;

    static String manifest(String appId, String name, String flags, String size) {
        return "\"AppState\"\n{\n\t\"appid\"\t\t\"" + appId + "\"\n\t\"universe\"\t\t\"1\"\n\t\"name\"\t\t\"" + name
                + "\"\n\t\"StateFlags\"\t\t\"" + flags + "\"\n\t\"installdir\"\t\t\"" + name
                + "\"\n\t\"LastUpdated\"\t\t\"1793684893\"\n\t\"SizeOnDisk\"\t\t\"" + size
                + "\"\n\t\"LastOwner\"\t\t\"76500000000000000\"\n\t\"InstalledDepots\"\n\t{\n\t\t\"1001\"\n\t\t{\n"
                + "\t\t\t\"manifest\"\t\t\"123\"\n\t\t\t\"size\"\t\t\"" + size + "\"\n\t\t}\n\t}\n}\n";
    }

    @Test
    void vdfLeBlocosEscapesEComentarios() {
        Map<String, Object> doc = Vdf.parse("// comentário\n\"libraryfolders\"\n{\n\t\"0\"\n\t{\n"
                + "\t\t\"path\"\t\t\"C:\\\\Program Files (x86)\\\\Steam\"\n\t\t\"label\"\t\t\"\"\n\t}\n"
                + "\t\"1\"\t\"D:\\\\Antigo\"\n}\n");
        Map<String, Object> root = Vdf.block(doc, "LibraryFolders");
        assertEquals("C:\\Program Files (x86)\\Steam", Vdf.string(Vdf.block(root, "0"), "path"));
        assertEquals("", Vdf.string(Vdf.block(root, "0"), "label"));
        assertEquals("D:\\Antigo", Vdf.string(root, "1"));
        assertNull(Vdf.string(root, "0"), "bloco não é texto");
    }

    @Test
    void vdfInvalidoERejeitado() {
        assertThrows(IllegalArgumentException.class, () -> Vdf.parse("\"a\" { \"b\" \"c\""));
        assertThrows(IllegalArgumentException.class, () -> Vdf.parse("\"a\" \"sem fim"));
        assertThrows(IllegalArgumentException.class, () -> Vdf.parse("}"));
        assertThrows(IllegalArgumentException.class, () -> Vdf.parse("{".repeat(40)));
    }

    @Test
    void estadoPeloStateFlags() {
        assertEquals("INSTALLED", InstalledGamesScanner.state(4L));
        assertEquals("UPDATE_REQUIRED", InstalledGamesScanner.state(6L));
        assertEquals("INCOMPLETE", InstalledGamesScanner.state(1026L)); // baixando, ainda não instalado
        assertEquals("INCOMPLETE", InstalledGamesScanner.state(null));
    }

    @Test
    void leBibliotecasJogosVolumesETipoDeDisco() throws Exception {
        Path steam = Files.createDirectories(temp.resolve("Steam"));
        Path extra = Files.createDirectories(temp.resolve("Biblioteca2"));
        Path missing = temp.resolve("DiscoExternoDesconectado");
        Files.createDirectories(steam.resolve("steamapps"));
        Files.createDirectories(extra.resolve("steamapps"));
        Files.writeString(steam.resolve("steamapps/libraryfolders.vdf"), "\"libraryfolders\"\n{\n"
                + "\t\"0\"\n\t{\n\t\t\"path\"\t\t\"" + esc(steam) + "\"\n\t}\n"
                + "\t\"1\"\n\t{\n\t\t\"path\"\t\t\"" + esc(extra) + "\"\n\t}\n"
                + "\t\"2\"\n\t{\n\t\t\"path\"\t\t\"" + esc(missing) + "\"\n\t}\n}\n");
        Files.writeString(steam.resolve("steamapps/appmanifest_228980.acf"),
                manifest("228980", "Steamworks Common Redistributables", "4", "245987471"));
        Files.writeString(extra.resolve("steamapps/appmanifest_900001.acf"),
                manifest("900001", "Aventura de Teste", "6", "48729439645"));
        Files.writeString(extra.resolve("steamapps/appmanifest_900002.acf"), "\"AppState\" { \"appid\" ");

        // Volumes: a pasta temporária num volume HDD; a Steam numa subpasta montada num SSD (montagem mais longa).
        String tempMount = temp.toString();
        String steamMount = steam.toString();
        StorageInfo storage = new StorageInfo(
                List.of(new DiskInfo("disk0", "SSD X", 1L, "SSD", "NVMe", List.of(steamMount)),
                        new DiskInfo("disk1", "HDD Y", 2L, "HDD", "SATA", List.of(tempMount))),
                List.of(new VolumeInfo(tempMount, null, "NTFS", 1000L, 400L, 600L, 60.0),
                        new VolumeInfo(steamMount, null, "NTFS", 500L, 100L, 400L, 80.0)));

        InstalledGamesDto dto = new InstalledGamesScanner(isWindows()).scan(steam, storage, NOW);

        assertTrue(dto.steamFound());
        assertEquals(3, dto.libraries().size());
        assertEquals("SSD", dto.libraries().get(0).mediaType());
        assertEquals(100L, dto.libraries().get(0).freeBytes());
        assertEquals("HDD", dto.libraries().get(1).mediaType());
        assertEquals(1, dto.libraries().get(1).gameCount());
        assertFalse(dto.libraries().get(2).available(), "biblioteca em disco desconectado continua listada");

        assertEquals(List.of("Aventura de Teste", "Steamworks Common Redistributables"),
                dto.games().stream().map(Game::name).toList());
        Game game = dto.games().get(0);
        assertEquals(900001L, game.appId());
        assertEquals("UPDATE_REQUIRED", game.state());
        assertEquals(48729439645L, game.sizeOnDiskBytes());
        assertEquals("HDD", game.mediaType());
        assertEquals(Instant.ofEpochSecond(1793684893L), game.lastUpdated());
        assertEquals("https://store.steampowered.com/app/900001/", game.storeUrl());

        // O manifesto quebrado vira um problema, sem esconder os demais.
        assertEquals(1, dto.problems().size());
        assertTrue(dto.problems().get(0).startsWith("appmanifest_900002.acf"));
    }

    @Test
    void semLibraryfoldersUsaAPastaPrincipalESemInventarioFicaSemVolume() throws Exception {
        Path steam = Files.createDirectories(temp.resolve("Steam/steamapps")).getParent();
        Files.writeString(steam.resolve("steamapps/appmanifest_10.acf"), manifest("10", "Jogo", "4", "0"));
        InstalledGamesDto dto = new InstalledGamesScanner(isWindows()).scan(steam, null, NOW);
        assertEquals(1, dto.libraries().size());
        assertNull(dto.libraries().get(0).mountPoint());
        Game g = dto.games().get(0);
        assertNull(g.sizeOnDiskBytes(), "tamanho 0 não é informado como 0 bytes");
        assertNull(g.mediaType());
    }

    @Test
    void volumeDoWindowsSemDiferenciarMaiusculas() {
        StorageInfo storage = new StorageInfo(
                List.of(new DiskInfo("\\\\.\\PHYSICALDRIVE1", "HDD", 1L, "HDD", "SATA", List.of("D:\\"))),
                List.of(new VolumeInfo("C:\\", null, "NTFS", 1L, 1L, 0L, 0.0),
                        new VolumeInfo("D:\\", null, "NTFS", 1L, 1L, 0L, 0.0)));
        InstalledGamesScanner win = new InstalledGamesScanner(true);
        assertEquals("D:\\", win.mountPointOf("d:\\SteamLibrary", storage));
        assertEquals("HDD", win.mediaTypeOf("D:\\", storage));
        assertEquals("C:\\", win.mountPointOf("C:\\Program Files (x86)\\Steam", storage));
        assertNull(win.mountPointOf("E:\\Jogos", storage));
    }

    @Test
    void volumeDoLinuxRespeitaLimiteDePasta() {
        StorageInfo storage = new StorageInfo(List.of(),
                List.of(new VolumeInfo("/", null, "ext4", 1L, 1L, 0L, 0.0),
                        new VolumeInfo("/home", null, "ext4", 1L, 1L, 0L, 0.0)));
        InstalledGamesScanner linux = new InstalledGamesScanner(false);
        assertEquals("/home", linux.mountPointOf("/home/ana/.local/share/Steam", storage));
        assertEquals("/", linux.mountPointOf("/homework/Steam", storage), "/homework não está em /home");
    }

    @Test
    void localizadorRespeitaPrioridade() {
        List<Path> win = SteamLocator.candidates(true, null, null, List.of("c:/program files (x86)/steam"));
        assertEquals(2, win.size());
        List<Path> linux = SteamLocator.candidates(false, "/opt/steam", "/home/ana", List.of());
        assertEquals(Path.of("/opt/steam"), linux.get(0));
        assertEquals(Path.of("/home/ana", ".local", "share", "Steam"), linux.get(2));
        Path found = SteamLocator.find(linux, p -> p.startsWith(Path.of("/home/ana", ".local")));
        assertEquals(Path.of("/home/ana", ".local", "share", "Steam"), found);
        assertNull(SteamLocator.find(linux, p -> false));
    }

    @Test
    void servicoSemSteamInformaComoConfigurar() {
        InstalledGamesDto dto = new InstalledGamesService(() -> null, () -> null, true).installed();
        assertFalse(dto.steamFound());
        assertTrue(dto.games().isEmpty());
        assertTrue(dto.problems().get(0).contains("PCCHECK_STEAM_DIR"));
    }

    private static String esc(Path p) {
        return p.toString().replace("\\", "\\\\");
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().startsWith("windows");
    }
}
