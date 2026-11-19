package com.pccheck.launcher;

import com.pccheck.PcCheckBackendApplication;
import java.awt.AWTException;
import java.awt.Desktop;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.swing.JOptionPane;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Ponto de entrada da versão instalada do NEXCORE ({@code NEXCORE.exe}). Não é usado em desenvolvimento
 * ({@code run-backend} continua iniciando {@link PcCheckBackendApplication} diretamente).
 *
 * <ol>
 *   <li>Uma instância por usuário: se o NEXCORE já está aberto, só abre o navegador.</li>
 *   <li>Inicia a API e a interface (arquivos estáticos dentro do próprio jar) em {@code 127.0.0.1:<porta>}.</li>
 *   <li>Abre o navegador padrão e mostra um ícone na bandeja com "Abrir NEXCORE" e "Sair".</li>
 *   <li>Qualquer falha ao iniciar aparece numa janela e fica registrada em {@code %LOCALAPPDATA%\NEXCORE\logs}.</li>
 * </ol>
 */
public final class NexcoreLauncher {

    /** Porta padrão da versão instalada (a de desenvolvimento é 8080). Pode ser trocada em config\nexcore.properties. */
    static final int DEFAULT_PORT = 47820;
    static final String PROFILE = "app";

    private static FileLock instanceLock;
    private static ConfigurableApplicationContext context;

    private NexcoreLauncher() {
    }

    public static void main(String[] args) {
        Path home = LauncherPaths.dataHome(System.getenv(), System.getProperty("user.home"));
        Path config = home.resolve("config").resolve("nexcore.properties");
        try {
            Files.createDirectories(home.resolve("logs"));
            LauncherConfig.writeTemplateIfMissing(config, DEFAULT_PORT);
        } catch (IOException e) {
            fail("Não foi possível criar a pasta de dados do NEXCORE em " + home + ".", e);
            return;
        }
        int port = LauncherConfig.port(config, DEFAULT_PORT);
        String url = "http://127.0.0.1:" + port + "/";

        if (!acquireInstanceLock(home.resolve("nexcore.lock")) || isNexcoreRunning(port)) {
            // Já está aberto (outra janela, atalho clicado duas vezes): só mostra a interface.
            openBrowser(url);
            return;
        }
        if (!isPortFree(port)) {
            fail("A porta " + port + " já está em uso por outro programa.\n\nAltere server.port em:\n" + config
                    + "\ne abra o NEXCORE de novo.", null);
            return;
        }

        SpringApplication app = new SpringApplication(PcCheckBackendApplication.class);
        app.setHeadless(false); // necessário para a bandeja e as janelas de erro
        app.setAdditionalProfiles(PROFILE);
        System.setProperty("nexcore.app.sensor-host", LauncherPaths.sensorHost(System.getProperty("jpackage.app-path"))
                .map(Path::toString).orElse(""));
        System.setProperty("nexcore.app.config", config.toString());
        System.setProperty("nexcore.app.home", home.toString());
        try {
            context = app.run(args);
        } catch (RuntimeException e) {
            fail("O NEXCORE não conseguiu iniciar.\n\nDetalhes em " + home.resolve("logs"), e);
            return;
        }
        installTray(url, home);
        openBrowser(url);
    }

    /** Trava de arquivo mantida enquanto o processo vive: impede uma segunda instância. */
    private static boolean acquireInstanceLock(Path lockFile) {
        try {
            FileChannel channel = FileChannel.open(lockFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            instanceLock = channel.tryLock();
            return instanceLock != null;
        } catch (IOException e) {
            return true; // sem trava não bloqueia: a verificação da porta ainda evita duas instâncias
        }
    }

    /** O NEXCORE já responde nesta porta (resposta de /api/ping com o nome do produto). */
    static boolean isNexcoreRunning(int port) {
        try {
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build();
            HttpResponse<String> r = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/ping"))
                    .timeout(Duration.ofSeconds(2)).GET().build(), HttpResponse.BodyHandlers.ofString());
            return r.statusCode() == 200 && r.body().contains("\"app\":\"NEXCORE\"");
        } catch (IOException e) {
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    static boolean isPortFree(int port) {
        try (ServerSocket socket = new ServerSocket(port, 1, InetAddress.getLoopbackAddress())) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static void installTray(String url, Path home) {
        if (GraphicsEnvironment.isHeadless() || !SystemTray.isSupported()) {
            return;
        }
        PopupMenu menu = new PopupMenu();
        MenuItem open = new MenuItem("Abrir NEXCORE");
        open.addActionListener(e -> openBrowser(url));
        MenuItem logs = new MenuItem("Abrir pasta de dados");
        logs.addActionListener(e -> openFolder(home));
        MenuItem exit = new MenuItem("Sair");
        exit.addActionListener(e -> shutdown());
        menu.add(open);
        menu.add(logs);
        menu.addSeparator();
        menu.add(exit);
        TrayIcon icon = new TrayIcon(trayImage(), "NEXCORE", menu);
        icon.setImageAutoSize(true);
        icon.addActionListener(e -> openBrowser(url)); // clique duplo
        try {
            SystemTray.getSystemTray().add(icon);
        } catch (AWTException e) {
            // Sem bandeja o NEXCORE continua funcionando; fecha pelo Gerenciador de Tarefas ou ao desligar.
        }
    }

    private static Image trayImage() {
        for (String name : List.of("/static/brand/nexcore-32.png", "/static/brand/nexcore-64.png")) {
            try (InputStream in = NexcoreLauncher.class.getResourceAsStream(name)) {
                if (in != null) {
                    return Toolkit.getDefaultToolkit().createImage(in.readAllBytes());
                }
            } catch (IOException e) {
                // tenta o próximo
            }
        }
        return Toolkit.getDefaultToolkit().createImage(new byte[0]);
    }

    private static void shutdown() {
        if (context != null) {
            SpringApplication.exit(context);
        }
        System.exit(0);
    }

    static void openBrowser(String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(url));
                return;
            }
        } catch (IOException | UnsupportedOperationException e) {
            // segue para o comando do Windows
        }
        try {
            new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", url).start();
        } catch (IOException e) {
            showMessage("Abra no navegador: " + url, JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private static void openFolder(Path folder) {
        try {
            Desktop.getDesktop().open(folder.toFile());
        } catch (IOException | UnsupportedOperationException e) {
            showMessage("Pasta de dados: " + folder, JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private static void fail(String message, Throwable cause) {
        if (cause != null) {
            LauncherPaths.appendStartupError(cause);
        }
        showMessage(message, JOptionPane.ERROR_MESSAGE);
        System.exit(1);
    }

    private static void showMessage(String message, int type) {
        if (!GraphicsEnvironment.isHeadless()) {
            JOptionPane.showMessageDialog(null, message, "NEXCORE", type);
        }
    }

    /** Lê só server.port do arquivo do usuário (o resto é lido pelo Spring). */
    static final class LauncherConfig {
        private LauncherConfig() {
        }

        static int port(Path config, int fallback) {
            Properties p = new Properties();
            try (InputStream in = Files.newInputStream(config)) {
                p.load(in);
                int port = Integer.parseInt(p.getProperty("server.port", String.valueOf(fallback)).trim());
                return port > 0 && port < 65536 ? port : fallback;
            } catch (IOException | NumberFormatException e) {
                return fallback;
            }
        }

        /** Cria o arquivo de configuração comentado na primeira execução. Nunca sobrescreve o do usuário. */
        static void writeTemplateIfMissing(Path config, int port) throws IOException {
            if (Files.exists(config)) {
                return;
            }
            Files.createDirectories(config.getParent());
            String template = String.join(System.lineSeparator(), List.of(
                    "# Configuracao do NEXCORE (deste usuario). Este arquivo nao e alterado por atualizacoes.",
                    "# Linhas com # sao comentarios. Feche e abra o NEXCORE depois de alterar.",
                    "",
                    "# Porta local da interface e da API (somente neste computador, 127.0.0.1).",
                    "server.port=" + port,
                    "",
                    "# Pedir permissao de administrador ao abrir, para ler temperaturas, ventoinhas e saude dos discos.",
                    "# false: o NEXCORE abre sem o aviso do Windows, mas sem essas leituras.",
                    "#nexcore.sensors.elevate=true",
                    "",
                    "# Limites do Health Check (padroes do projeto). Exemplos:",
                    "#nexcore.health.cpu-temp-warning-c=80",
                    "#nexcore.health.disk-free-warning-percent=15",
                    ""));
            Files.writeString(config, template, StandardCharsets.UTF_8);
        }
    }

    /** Pastas da versão instalada. */
    static final class LauncherPaths {
        private LauncherPaths() {
        }

        /** %LOCALAPPDATA%\NEXCORE (Windows) ou ~/.local/state/nexcore. */
        static Path dataHome(Map<String, String> env, String userHome) {
            String local = env.get("LOCALAPPDATA");
            if (local != null && !local.isBlank()) {
                return Path.of(local, "NEXCORE");
            }
            return Path.of(userHome, ".local", "state", "nexcore");
        }

        /**
         * Leitor de sensores ao lado do executável: {@code <instalação>\app\sensor-host\NEXCORE.SensorHost.exe}
         * (jpackage informa o caminho do executável em {@code jpackage.app-path}).
         */
        static java.util.Optional<Path> sensorHost(String appPath) {
            if (appPath == null || appPath.isBlank()) {
                return java.util.Optional.empty();
            }
            Path base = Path.of(appPath).toAbsolutePath().getParent();
            for (Path candidate : List.of(base.resolve("app").resolve("sensor-host"), base.resolve("sensor-host"))) {
                Path exe = candidate.resolve("NEXCORE.SensorHost.exe");
                if (Files.isRegularFile(exe)) {
                    return java.util.Optional.of(exe);
                }
            }
            return java.util.Optional.empty();
        }

        /** Falha antes do log do Spring existir: grava em logs\inicializacao.log. */
        static void appendStartupError(Throwable cause) {
            try {
                Path log = dataHome(System.getenv(), System.getProperty("user.home")).resolve("logs")
                        .resolve("inicializacao.log");
                Files.createDirectories(log.getParent());
                java.io.StringWriter text = new java.io.StringWriter();
                cause.printStackTrace(new java.io.PrintWriter(text));
                Files.writeString(log, java.time.Instant.now() + System.lineSeparator() + text + System.lineSeparator(),
                        StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {
                // sem log: a janela de erro já informou o usuário
            }
        }
    }
}
