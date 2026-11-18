import { useState } from 'react'
import { fetchInstalledGames } from '../../services/gameService'
import { bytes, dateTime } from '../hardware/format'
import type { GameSearchResult, InstalledGame, InstalledGames as InstalledGamesData, SteamLibrary } from './types'

const STATE_LABEL: Record<InstalledGame['state'], string> = {
  INSTALLED: 'Instalado',
  UPDATE_REQUIRED: 'Atualização pendente',
  INCOMPLETE: 'Instalação incompleta',
}

/** Volume e tipo de disco ("D:\ · HDD"); o tipo só aparece quando o SO o informa. */
function where(mountPoint: string | null, mediaType: string | null): string {
  if (!mountPoint) return 'Volume não identificado'
  return mediaType ? `${mountPoint} · ${mediaType}` : mountPoint
}

function libraryLine(lib: SteamLibrary): string {
  if (!lib.available) return `${lib.path} — pasta não encontrada (disco desconectado?)`
  const free = lib.freeBytes != null ? ` · ${bytes(lib.freeBytes)} livres` : ''
  return `${lib.path} — ${where(lib.mountPoint, lib.mediaType)}${free} · ${lib.gameCount} ${lib.gameCount === 1 ? 'item' : 'itens'}`
}

/**
 * Jogos instalados pela Steam neste PC. A leitura só acontece quando o bloco é aberto (e em "Ler novamente").
 * "Verificar compatibilidade" usa o mesmo fluxo da pesquisa, pelo AppID.
 */
export function InstalledGames({ onSelect }: { onSelect: (game: GameSearchResult) => void }) {
  const [data, setData] = useState<InstalledGamesData | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [filter, setFilter] = useState('')

  async function load() {
    setLoading(true)
    setError('')
    try {
      setData(await fetchInstalledGames())
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Não foi possível ler os jogos instalados.')
    } finally {
      setLoading(false)
    }
  }

  const term = filter.trim().toLocaleLowerCase('pt-BR')
  const games = (data?.games ?? []).filter((g) => !term || (g.name ?? '').toLocaleLowerCase('pt-BR').includes(term))

  return (
    <details className="card installed-games" onToggle={(e) => { if (e.currentTarget.open && !data && !loading) void load() }}>
      <summary>Jogos instalados neste PC (Steam)</summary>
      {loading && <p className="loading" role="status">Lendo as bibliotecas da Steam…</p>}
      {error && <div className="banner banner-error" role="alert">{error}</div>}
      {data && !data.steamFound && <p className="muted">{data.problems[0]}</p>}
      {data?.steamFound && (
        <>
          <ul className="installed-libraries" aria-label="Bibliotecas da Steam">
            {data.libraries.map((lib) => <li key={lib.path}>{libraryLine(lib)}</li>)}
          </ul>
          <div className="installed-toolbar">
            <label htmlFor="installed-filter">Filtrar</label>
            <input id="installed-filter" value={filter} onChange={(e) => setFilter(e.target.value)} autoComplete="off" />
            <button type="button" onClick={() => void load()} disabled={loading}>Ler novamente</button>
          </div>
          {data.games.length === 0 ? <p className="muted">Nenhum jogo instalado encontrado nas bibliotecas.</p> : (
            <div className="table-scroll"><table className="game-table">
              <thead><tr><th>Jogo</th><th>Tamanho</th><th>Disco</th><th>Estado</th><th><span className="sr-only">Ação</span></th></tr></thead>
              <tbody>{games.map((g) => (
                <tr key={g.appId}>
                  <td>{g.name ?? `AppID ${g.appId}`}<small className="game-explanation">AppID {g.appId}</small></td>
                  <td>{bytes(g.sizeOnDiskBytes)}</td>
                  <td>{where(g.mountPoint, g.mediaType)}</td>
                  <td>{STATE_LABEL[g.state]}</td>
                  <td>
                    <button type="button" onClick={() => onSelect({
                      appId: g.appId, name: g.name ?? `AppID ${g.appId}`, headerImage: null, storeUrl: g.storeUrl,
                    })}>Verificar compatibilidade</button>
                  </td>
                </tr>
              ))}</tbody>
            </table></div>
          )}
          <p className="muted">
            Lido dos arquivos da própria Steam em {data.steamPath} ({dateTime(data.scannedAt)}). A lista inclui
            ferramentas da Steam (ex.: redistribuíveis), que não têm requisitos de jogo.
          </p>
          {data.problems.length > 0 && (
            <details><summary>{data.problems.length} arquivo(s) não lido(s)</summary>
              <ul>{data.problems.map((p) => <li key={p}>{p}</li>)}</ul>
            </details>
          )}
        </>
      )}
    </details>
  )
}
