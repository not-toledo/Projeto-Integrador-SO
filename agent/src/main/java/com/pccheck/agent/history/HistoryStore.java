package com.pccheck.agent.history;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Arquivos do histórico de coletas agendadas.
 *
 * <ul>
 *   <li><b>Local:</b> {@code <dados>/coletas/}, onde {@code <dados>} vem da variável de ambiente
 *       {@value #ENV_DATA_DIR} ou do diretório de dados do usuário no SO.</li>
 *   <li><b>Rotação:</b> um arquivo por dia ({@code coletas-AAAA-MM-DD.jsonl}), uma coleta por linha.</li>
 *   <li><b>Retenção:</b> arquivos mais antigos que N dias são apagados; só arquivos com esse padrão de nome.</li>
 *   <li><b>Concorrência:</b> cada linha é gravada sob trava exclusiva do arquivo (o agendador grava enquanto o
 *       backend lê); o leitor ignora linhas incompletas.</li>
 *   <li><b>Permissões:</b> em sistemas POSIX (Linux), pasta 700 e arquivos 600: as coletas descrevem o computador
 *       e só interessam ao próprio usuário.</li>
 * </ul>
 */
public final class HistoryStore {

    public static final String ENV_DATA_DIR = "PCCHECK_DATA_DIR";
    public static final int DEFAULT_RETENTION_DAYS = 7;
    private static final Pattern FILE_NAME = Pattern.compile("coletas-(\\d{4}-\\d{2}-\\d{2})\\.jsonl");

    private final Path directory;
    private final ZoneId zone;

    public HistoryStore(Path dataDir, ZoneId zone) {
        this.directory = dataDir.resolve("coletas");
        this.zone = zone;
    }

    /** Diretório de dados padrão: {@value #ENV_DATA_DIR}; senão %LOCALAPPDATA%\PCCheck (Windows) ou XDG (Linux). */
    public static Path defaultDataDir(Map<String, String> env, String osName, String userHome) {
        String explicit = env.get(ENV_DATA_DIR);
        if (explicit != null && !explicit.isBlank()) {
            return Path.of(explicit.trim());
        }
        if (osName != null && osName.toLowerCase(Locale.ROOT).startsWith("windows")) {
            String local = env.get("LOCALAPPDATA");
            return (local == null || local.isBlank() ? Path.of(userHome, "AppData", "Local") : Path.of(local))
                    .resolve("PCCheck");
        }
        String state = env.get("XDG_STATE_HOME");
        return (state == null || state.isBlank() ? Path.of(userHome, ".local", "state") : Path.of(state))
                .resolve("pccheck");
    }

    public Path directory() {
        return directory;
    }

    /** Acrescenta a coleta ao arquivo do dia em que foi feita. Retorna o arquivo usado. */
    public Path append(HistoryRecord record) throws IOException {
        createPrivateDirectories(directory);
        Path file = fileFor(record.collectedAt().atZone(zone).toLocalDate());
        boolean isNew = !Files.exists(file);
        byte[] line = (HistoryJson.toLine(record) + "\n").getBytes(StandardCharsets.UTF_8);
        try (FileChannel channel = FileChannel.open(file, StandardOpenOption.CREATE, StandardOpenOption.WRITE,
                StandardOpenOption.APPEND);
             FileLock ignored = channel.lock()) {
            ByteBuffer buffer = ByteBuffer.wrap(line);
            while (buffer.hasRemaining()) {
                channel.write(buffer);
            }
        }
        if (isNew) {
            restrictToOwner(file, "rw-------");
        }
        return file;
    }

    /** Apaga os arquivos do histórico com mais de {@code days} dias. Retorna os apagados. */
    public List<Path> deleteOlderThan(int days, LocalDate today) throws IOException {
        if (days < 1) throw new IllegalArgumentException("A retenção deve ser de pelo menos 1 dia.");
        List<Path> deleted = new ArrayList<>();
        if (!Files.isDirectory(directory)) return deleted;
        LocalDate oldestKept = today.minusDays(days - 1L);
        for (Path file : historyFiles()) {
            LocalDate date = dateOf(file);
            if (date != null && date.isBefore(oldestKept)) {
                Files.deleteIfExists(file);
                deleted.add(file);
            }
        }
        return deleted;
    }

    /** Resultado da leitura: coletas em ordem cronológica e arquivos que não puderam ser lidos. */
    public record ReadResult(List<HistoryRecord> records, List<String> problems) {
    }

    /** Coletas feitas a partir de {@code from}, em ordem cronológica. */
    public ReadResult read(Instant from) {
        List<HistoryRecord> records = new ArrayList<>();
        List<String> problems = new ArrayList<>();
        if (!Files.isDirectory(directory)) return new ReadResult(records, problems);
        LocalDate firstDay = from.atZone(zone).toLocalDate().minusDays(1);
        List<Path> files;
        try {
            files = historyFiles();
        } catch (IOException e) {
            problems.add("Não foi possível listar " + directory + ": " + e.getMessage());
            return new ReadResult(records, problems);
        }
        for (Path file : files) {
            LocalDate date = dateOf(file);
            if (date == null || date.isBefore(firstDay)) continue;
            List<String> lines = readLines(file, problems);
            for (String line : lines) {
                HistoryRecord r = HistoryJson.fromLine(line);
                if (r != null && !r.collectedAt().isBefore(from)) records.add(r);
            }
        }
        records.sort(Comparator.comparing(HistoryRecord::collectedAt));
        return new ReadResult(records, problems);
    }

    Path fileFor(LocalDate date) {
        return directory.resolve("coletas-" + date + ".jsonl");
    }

    private List<Path> historyFiles() throws IOException {
        List<Path> files = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, "coletas-*.jsonl")) {
            for (Path p : stream) {
                if (dateOf(p) != null) files.add(p);
            }
        }
        files.sort(Comparator.naturalOrder());
        return files;
    }

    static LocalDate dateOf(Path file) {
        Matcher m = FILE_NAME.matcher(file.getFileName().toString());
        if (!m.matches()) return null;
        try {
            return LocalDate.parse(m.group(1));
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** No Windows, a trava do gravador bloqueia a leitura por instantes: uma nova tentativa resolve. */
    private static List<String> readLines(Path file, List<String> problems) {
        for (int attempt = 1; ; attempt++) {
            try {
                return Files.readAllLines(file, StandardCharsets.UTF_8);
            } catch (IOException e) {
                if (attempt >= 3) {
                    problems.add("Não foi possível ler " + file.getFileName() + ": " + e.getMessage());
                    return List.of();
                }
                try {
                    Thread.sleep(50L * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return List.of();
                }
            }
        }
    }

    private static boolean posix() {
        return FileSystems.getDefault().supportedFileAttributeViews().contains("posix");
    }

    /** Cria as pastas que faltam e restringe ao dono SOMENTE as criadas aqui (nunca uma pasta já existente). */
    private static void createPrivateDirectories(Path dir) throws IOException {
        if (Files.isDirectory(dir)) return;
        List<Path> missing = new ArrayList<>();
        for (Path p = dir.toAbsolutePath(); p != null && !Files.exists(p); p = p.getParent()) {
            missing.add(p);
        }
        Files.createDirectories(dir);
        for (Path created : missing) {
            restrictToOwner(created, "rwx------");
        }
    }

    private static void restrictToOwner(Path path, String permissions) throws IOException {
        if (path != null && posix()) {
            Files.setPosixFilePermissions(path, PosixFilePermissions.fromString(permissions));
        }
    }
}
