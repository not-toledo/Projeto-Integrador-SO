import type { GameCompatibility, GameSearchResult, InstalledGames, Precheck } from '../modules/games/types'

const API_BASE: string = import.meta.env.VITE_API_BASE ?? ''

export class GameApiError extends Error {}

async function request<T>(url: string): Promise<T> {
  let response: Response
  try {
    response = await fetch(`${API_BASE}${url}`)
  } catch {
    throw new GameApiError('Não foi possível conectar ao backend local.')
  }
  if (!response.ok) {
    let message = `A API respondeu com HTTP ${response.status}.`
    try {
      const body = await response.json() as { error?: string }
      if (body.error) message = body.error
    } catch { /* mantém a mensagem HTTP */ }
    throw new GameApiError(message)
  }
  return response.json() as Promise<T>
}

export function searchGames(query: string): Promise<GameSearchResult[]> {
  return request(`/api/games/search?query=${encodeURIComponent(query)}`)
}
export function fetchGameCompatibility(appId: number, refresh = false): Promise<GameCompatibility> {
  return request(`/api/games/${appId}/compatibility${refresh ? '?refresh=true' : ''}`)
}
/** Jogos instalados pela Steam, lidos pelo backend nos arquivos locais da Steam. */
export function fetchInstalledGames(): Promise<InstalledGames> {
  return request('/api/games/installed')
}
/** Pré-checagem antes de jogar: estado do PC agora (leva cerca de 1 s no backend). */
export function fetchPrecheck(appId: number): Promise<Precheck> {
  return request(`/api/games/${appId}/precheck`)
}
