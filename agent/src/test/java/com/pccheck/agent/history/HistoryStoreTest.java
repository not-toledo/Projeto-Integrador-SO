package com.pccheck.agent.history;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pccheck.agent.history.HistoryRecord.VolumeRecord;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class HistoryStoreTest {

    private static final ZoneId UTC = ZoneOffset.UTC;

    @TempDir
    Path temp;

    private static HistoryRecord record(String at, Double cpu) {
        return new HistoryRecord(Instant.parse(at), cpu, 70.5, 82.4, 10.0, 3600L,
                List.of(new VolumeRecord("C:\\", 500_000_000_000L, 120_000_000_000L)));
    }

    @Test
    void jsonIdaEVoltaPreservaOsCampos() {
        HistoryRecord r = new HistoryRecord(Instant.parse("2026-11-17T12:00:00Z"), 12.5, null, 82.4, null, 99L,
                List.of(new VolumeRecord("/mnt/\"dados\"\\x", 1_000_000_000_000L, 5L)));
        HistoryRecord back = HistoryJson.fromLine(HistoryJson.toLine(r));
        assertEquals(r, back);
    }

    @Test
    void linhaIncompletaOuInvalidaEIgnorada() {
        assertNull(HistoryJson.fromLine("{\"collectedAt\":\"2026-11-17T12:00:00Z\",\"cpuPer"));
        assertNull(HistoryJson.fromLine("{\"collectedAt\":\"ontem\"}"));
        assertNull(HistoryJson.fromLine(""));
    }

    @Test
    void gravaUmArquivoPorDiaEUmaColetaPorLinha() throws Exception {
        HistoryStore store = new HistoryStore(temp, UTC);
        Path day1 = store.append(record("2026-11-16T23:59:00Z", 10.0));
        store.append(record("2026-11-17T00:01:00Z", 20.0));
        Path day2 = store.append(record("2026-11-17T10:00:00Z", 30.0));

        assertEquals("coletas-2026-11-16.jsonl", day1.getFileName().toString());
        assertEquals("coletas-2026-11-17.jsonl", day2.getFileName().toString());
        assertEquals(2, Files.readAllLines(day2).size());
    }

    @Test
    void leEmOrdemCronologicaAPartirDoInicioPedido() throws Exception {
        HistoryStore store = new HistoryStore(temp, UTC);
        store.append(record("2026-11-17T10:00:00Z", 30.0));
        store.append(record("2026-11-15T10:00:00Z", 10.0));
        store.append(record("2026-11-16T10:00:00Z", 20.0));

        var result = store.read(Instant.parse("2026-11-16T00:00:00Z"));
        assertEquals(List.of(20.0, 30.0), result.records().stream().map(HistoryRecord::cpuPercent).toList());
        assertTrue(result.problems().isEmpty());
    }

    @Test
    void leituraIgnoraLinhaCortadaPorGravacaoInterrompida() throws Exception {
        HistoryStore store = new HistoryStore(temp, UTC);
        Path file = store.append(record("2026-11-17T10:00:00Z", 30.0));
        Files.writeString(file, "{\"collectedAt\":\"2026-11-17T11:00", StandardCharsets.UTF_8,
                java.nio.file.StandardOpenOption.APPEND);

        assertEquals(1, store.read(Instant.parse("2026-11-17T00:00:00Z")).records().size());
    }

    @Test
    void semPastaAindaNaoHaColetasNemErro() {
        var result = new HistoryStore(temp.resolve("nao-existe"), UTC).read(Instant.EPOCH);
        assertTrue(result.records().isEmpty());
        assertTrue(result.problems().isEmpty());
    }

    @Test
    void retencaoApagaSoArquivosAntigosDoHistorico() throws Exception {
        HistoryStore store = new HistoryStore(temp, UTC);
        for (String day : List.of("2026-11-08", "2026-11-10", "2026-11-11", "2026-11-17")) {
            store.append(record(day + "T12:00:00Z", 1.0));
        }
        Path alheio = store.directory().resolve("anotacoes.txt");
        Files.writeString(alheio, "não é do PCCheck");
        Path parecido = store.directory().resolve("coletas-backup.jsonl");
        Files.writeString(parecido, "{}");

        // 7 dias mantidos: de 11/11 a 17/11.
        List<Path> deleted = store.deleteOlderThan(7, LocalDate.parse("2026-11-17"));

        assertEquals(List.of("coletas-2026-11-08.jsonl", "coletas-2026-11-10.jsonl"),
                deleted.stream().map(p -> p.getFileName().toString()).sorted().toList());
        assertTrue(Files.exists(store.directory().resolve("coletas-2026-11-11.jsonl")));
        assertTrue(Files.exists(alheio));
        assertTrue(Files.exists(parecido));
        assertThrows(IllegalArgumentException.class, () -> store.deleteOlderThan(0, LocalDate.now()));
    }

    @Test
    void pastaPadraoSegueVariavelDeAmbienteESistema() {
        assertEquals(Path.of("D:/dados"), HistoryStore.defaultDataDir(
                Map.of(HistoryStore.ENV_DATA_DIR, "D:/dados", "LOCALAPPDATA", "C:/x"), "Windows 10", "C:/Users/u"));
        assertEquals(Path.of("C:/Users/u/AppData/Local/PCCheck"),
                HistoryStore.defaultDataDir(Map.of("LOCALAPPDATA", "C:/Users/u/AppData/Local"), "Windows 11", "C:/Users/u"));
        assertEquals(Path.of("/home/u/.local/state/pccheck"),
                HistoryStore.defaultDataDir(Map.of(), "Linux", "/home/u"));
        assertEquals(Path.of("/xdg/state/pccheck"),
                HistoryStore.defaultDataDir(Map.of("XDG_STATE_HOME", "/xdg/state"), "Linux", "/home/u"));
    }

    @Test
    void retencaoInvalidaUsaOPadrao() {
        assertEquals(7, ScheduledCollection.retentionDays(Map.of()));
        assertEquals(30, ScheduledCollection.retentionDays(Map.of(ScheduledCollection.ENV_RETENTION_DAYS, "30")));
        assertEquals(7, ScheduledCollection.retentionDays(Map.of(ScheduledCollection.ENV_RETENTION_DAYS, "0")));
        assertEquals(7, ScheduledCollection.retentionDays(Map.of(ScheduledCollection.ENV_RETENTION_DAYS, "abc")));
    }

    @Test
    void noLinuxPastaEArquivoFicamSoParaODono() throws Exception {
        if (!FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
            return; // Windows: a pasta do usuário já é protegida pelas permissões (ACL) do perfil.
        }
        Path existente = Files.createDirectory(temp.resolve("compartilhada"));
        Files.setPosixFilePermissions(existente, PosixFilePermissions.fromString("rwxr-xr-x"));
        HistoryStore store = new HistoryStore(existente.resolve("pccheck"), UTC);
        Path file = store.append(record("2026-11-17T10:00:00Z", 1.0));

        assertEquals("rwx------", PosixFilePermissions.toString(Files.getPosixFilePermissions(store.directory())));
        assertEquals("rw-------", PosixFilePermissions.toString(Files.getPosixFilePermissions(file)));
        // Pasta que já existia não é alterada.
        assertEquals("rwxr-xr-x", PosixFilePermissions.toString(Files.getPosixFilePermissions(existente)));
    }
}
