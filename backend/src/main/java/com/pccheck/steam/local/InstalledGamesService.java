package com.pccheck.steam.local;

import com.pccheck.agent.collector.CollectorRunner;
import com.pccheck.agent.collector.DiskCollector;
import com.pccheck.agent.model.CollectorResult;
import com.pccheck.agent.model.StorageInfo;
import com.pccheck.dto.InstalledGamesDto;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;
import oshi.SystemInfo;

/** Jogos instalados pela Steam neste computador, com o volume e o tipo de disco de cada biblioteca. */
@Service
public class InstalledGamesService {

    static final String NOT_FOUND = "Pasta da Steam não encontrada. Se a Steam está instalada em outro local, "
            + "defina a variável de ambiente PCCHECK_STEAM_DIR com a pasta dela e reinicie o backend.";

    private final Supplier<Path> locator;
    private final Supplier<StorageInfo> storage;
    private final boolean windows;

    public InstalledGamesService() {
        this(SteamLocator::locate, new LazyStorage(),
                System.getProperty("os.name", "").toLowerCase().startsWith("windows"));
    }

    InstalledGamesService(Supplier<Path> locator, Supplier<StorageInfo> storage, boolean windows) {
        this.locator = locator;
        this.storage = storage;
        this.windows = windows;
    }

    public InstalledGamesDto installed() {
        Instant now = Instant.now();
        Path root = locator.get();
        if (root == null) {
            return new InstalledGamesDto(false, null, List.of(), List.of(), List.of(NOT_FOUND), now);
        }
        try {
            root = root.toRealPath(); // grafia real da pasta (o registro guarda em minúsculas)
        } catch (IOException e) {
            // mantém o caminho como veio
        }
        return new InstalledGamesScanner(windows).scan(root, storage.get(), now);
    }

    /** Discos e volumes pelo coletor do agente; null se a coleta falhar (as bibliotecas ficam sem volume). */
    private static final class LazyStorage implements Supplier<StorageInfo> {
        private DiskCollector collector;

        @Override
        public synchronized StorageInfo get() {
            try {
                if (collector == null) {
                    collector = new DiskCollector(new SystemInfo());
                }
            } catch (RuntimeException | LinkageError e) {
                return null;
            }
            CollectorResult<StorageInfo> result = CollectorRunner.run(collector);
            return result.isOk() ? result.data() : null;
        }
    }
}
