package com.pccheck.agent.sensors;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.Ole32;
import com.sun.jna.platform.win32.Shell32;
import com.sun.jna.platform.win32.ShellAPI;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.IntByReference;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.lang.ProcessBuilder.Redirect;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Parte nativa do Windows (via JNA, que já vem com o OSHI): inicia o leitor, com ou sem elevação, e lê o
 * pipe nomeado com tempo limite. Sem lógica de negócio, para que {@link SensorHostClient} seja testável.
 */
final class WindowsSensorHost implements SensorHostClient.HostLauncher, SensorHostClient.PipeReader {

    private static final int SEE_MASK_NOCLOSEPROCESS = 0x00000040;
    private static final int SEE_MASK_NOASYNC = 0x00000100;
    private static final int SW_HIDE = 0;
    private static final int COINIT_APARTMENTTHREADED_DISABLE_OLE1DDE = 0x2 | 0x4;
    private static final int ERROR_FILE_NOT_FOUND = 2;
    private static final int ERROR_BROKEN_PIPE = 109;
    private static final int ERROR_PIPE_BUSY = 231;
    private static final int ERROR_CANCELLED = 1223;
    private static final int WAIT_TIMEOUT = 0x102;
    private static final int MAX_LINE_BYTES = 4 * 1024 * 1024;

    @Override
    public SensorHostClient.HostProcess launch(Path exe, List<String> args, boolean elevate) throws IOException {
        if (!elevate) {
            List<String> command = new ArrayList<>();
            command.add(exe.toString());
            command.addAll(args);
            Process process = new ProcessBuilder(command)
                    .directory(exe.getParent().toFile())
                    .redirectOutput(Redirect.DISCARD)
                    .redirectError(Redirect.DISCARD)
                    .start();
            return new JavaProcess(process);
        }
        // "runas" mostra o aviso de administrador do Windows (UAC). Se o backend já estiver elevado, não pergunta.
        Ole32.INSTANCE.CoInitializeEx(null, COINIT_APARTMENTTHREADED_DISABLE_OLE1DDE);
        try {
            ShellAPI.SHELLEXECUTEINFO info = new ShellAPI.SHELLEXECUTEINFO();
            info.cbSize = info.size();
            info.fMask = SEE_MASK_NOCLOSEPROCESS | SEE_MASK_NOASYNC;
            info.lpVerb = "runas";
            info.lpFile = exe.toString();
            info.lpParameters = String.join(" ", args); // valores sem espaços (nome do pipe e PID)
            info.lpDirectory = exe.getParent().toString();
            info.nShow = SW_HIDE;
            if (!Shell32.INSTANCE.ShellExecuteEx(info)) {
                int error = Native.getLastError();
                if (error == ERROR_CANCELLED) {
                    throw new SensorHostClient.ElevationDeclinedException();
                }
                throw new IOException("o Windows recusou a execução (código " + error + ")");
            }
            if (info.hProcess == null) {
                throw new IOException("o Windows não devolveu o processo iniciado");
            }
            return new NativeProcess(info.hProcess);
        } finally {
            Ole32.INSTANCE.CoUninitialize();
        }
    }

    @Override
    public String readLine(String pipeName, long timeoutMs) throws IOException {
        String path = "\\\\.\\pipe\\" + pipeName;
        long deadline = System.nanoTime() + timeoutMs * 1_000_000L;
        Kernel32 k32 = Kernel32.INSTANCE;
        WinNT.HANDLE handle;
        while (true) {
            handle = k32.CreateFile(path, WinNT.GENERIC_READ, 0, null, WinNT.OPEN_EXISTING, 0, null);
            if (handle != null && !WinBase.INVALID_HANDLE_VALUE.equals(handle)) {
                break;
            }
            int error = Native.getLastError();
            if (error == ERROR_FILE_NOT_FOUND) {
                throw new SensorHostClient.PipeNotFoundException();
            }
            long remainingMs = (deadline - System.nanoTime()) / 1_000_000L;
            if (error != ERROR_PIPE_BUSY || remainingMs <= 0) {
                throw new IOException(error == ERROR_PIPE_BUSY ? "tempo limite esgotado"
                        : "falha ao abrir o canal (código " + error + ")");
            }
            k32.WaitNamedPipe(path, (int) Math.min(remainingMs, 500));
        }
        try {
            return readUntilNewline(k32, handle, deadline);
        } finally {
            k32.CloseHandle(handle);
        }
    }

    /** Lê sem bloquear indefinidamente: só chama ReadFile quando há bytes disponíveis. */
    private static String readUntilNewline(Kernel32 k32, WinNT.HANDLE handle, long deadline) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(16 * 1024);
        byte[] buffer = new byte[64 * 1024];
        IntByReference available = new IntByReference();
        IntByReference read = new IntByReference();
        while (true) {
            if (!k32.PeekNamedPipe(handle, null, 0, null, available, null)) {
                int error = Native.getLastError();
                if (error == ERROR_BROKEN_PIPE) {
                    break; // o leitor terminou de escrever e desconectou
                }
                throw new IOException("falha ao ler o canal (código " + error + ")");
            }
            int count = available.getValue();
            if (count > 0) {
                if (!k32.ReadFile(handle, buffer, Math.min(count, buffer.length), read, null)) {
                    throw new IOException("falha ao ler o canal (código " + Native.getLastError() + ")");
                }
                out.write(buffer, 0, read.getValue());
                if (indexOfNewline(buffer, read.getValue()) >= 0) {
                    break;
                }
                if (out.size() > MAX_LINE_BYTES) {
                    throw new IOException("resposta grande demais");
                }
                continue;
            }
            if (System.nanoTime() > deadline) {
                throw new IOException("tempo limite esgotado");
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new InterruptedIOException("leitura interrompida");
            }
        }
        String text = out.toString(StandardCharsets.UTF_8);
        int newline = text.indexOf('\n');
        return newline >= 0 ? text.substring(0, newline) : text;
    }

    private static int indexOfNewline(byte[] data, int length) {
        for (int i = 0; i < length; i++) {
            if (data[i] == '\n') {
                return i;
            }
        }
        return -1;
    }

    private record JavaProcess(Process process) implements SensorHostClient.HostProcess {
        @Override
        public boolean isAlive() {
            return process.isAlive();
        }

        @Override
        public void stop() {
            process.destroy();
        }
    }

    private static final class NativeProcess implements SensorHostClient.HostProcess {
        private final WinNT.HANDLE handle;
        private boolean closed;

        NativeProcess(WinNT.HANDLE handle) {
            this.handle = handle;
        }

        @Override
        public synchronized boolean isAlive() {
            // Em caso de falha da consulta, considera vivo (o pipe dirá se responde).
            return !closed && Kernel32.INSTANCE.WaitForSingleObject(handle, 0) != WinBase.WAIT_OBJECT_0;
        }

        @Override
        public synchronized void stop() {
            if (closed) {
                return;
            }
            closed = true;
            // Pode falhar sem permissão; de qualquer forma o leitor termina quando o backend termina.
            if (Kernel32.INSTANCE.WaitForSingleObject(handle, 0) == WAIT_TIMEOUT) {
                Kernel32.INSTANCE.TerminateProcess(handle, 0);
            }
            Kernel32.INSTANCE.CloseHandle(handle);
        }
    }
}
