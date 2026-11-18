package com.pccheck.dto;

import java.time.Instant;
import java.util.List;

/**
 * Jogos instalados pela Steam, lidos dos arquivos locais da própria Steam (nada é consultado na internet).
 *
 * @param steamFound se a pasta da Steam foi encontrada
 * @param steamPath  pasta da Steam usada (null se não encontrada)
 * @param libraries  bibliotecas (pastas de jogos) declaradas em {@code libraryfolders.vdf}
 * @param games      jogos e ferramentas com {@code appmanifest_*.acf}, em ordem alfabética
 * @param problems   arquivos que não puderam ser lidos (o restante continua listado)
 * @param scannedAt  momento da leitura
 */
public record InstalledGamesDto(boolean steamFound, String steamPath, List<Library> libraries, List<Game> games,
                                List<String> problems, Instant scannedAt) {

    /**
     * Biblioteca da Steam e o volume onde está.
     *
     * @param mountPoint volume (ex.: "D:\"); null se nenhum volume do inventário contém a pasta
     * @param mediaType  "SSD"/"HDD" do disco físico desse volume, só quando o SO informa
     * @param available  se a pasta existe agora (ex.: disco externo desconectado = false)
     */
    public record Library(String path, String mountPoint, Long freeBytes, Long totalBytes, String mediaType,
                          boolean available, int gameCount) {
    }

    /**
     * Jogo (ou ferramenta da Steam) instalado.
     *
     * @param state           {@code INSTALLED}, {@code UPDATE_REQUIRED} (instalado, com atualização pendente) ou
     *                        {@code INCOMPLETE} (download/instalação não concluídos), pelo {@code StateFlags}
     * @param sizeOnDiskBytes tamanho em disco informado pela Steam ({@code SizeOnDisk})
     * @param lastUpdated     última atualização ({@code LastUpdated}); null se não informada
     */
    public record Game(long appId, String name, String installDir, String libraryPath, String mountPoint,
                       String mediaType, Long sizeOnDiskBytes, String state, Instant lastUpdated, String storeUrl) {
    }
}
