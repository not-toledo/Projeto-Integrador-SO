import { ApiError } from './inventoryService'
import type { HistoryResponse } from '../modules/history/types'

// Em desenvolvimento, o Vite encaminha /api para o backend local (veja vite.config.ts).
const API_BASE: string = import.meta.env.VITE_API_BASE ?? ''

const STATES = ['INSTALLED', 'NOT_INSTALLED', 'UNKNOWN']

function isHistory(value: unknown): value is HistoryResponse {
  if (typeof value !== 'object' || value === null) return false
  const b = value as Record<string, unknown>
  const scheduler = b.scheduler as Record<string, unknown> | null
  return (
    typeof b.directory === 'string' &&
    typeof b.days === 'number' &&
    typeof b.count === 'number' &&
    Array.isArray(b.records) &&
    b.records.every((r) => typeof r === 'object' && r !== null && typeof (r as Record<string, unknown>).collectedAt === 'string') &&
    Array.isArray(b.problems) &&
    typeof scheduler === 'object' && scheduler !== null &&
    typeof scheduler.state === 'string' && STATES.includes(scheduler.state)
  )
}

export async function fetchHistory(days: number, signal?: AbortSignal): Promise<HistoryResponse> {
  let response: Response
  try {
    response = await fetch(`${API_BASE}/api/history?days=${days}`, { signal })
  } catch (error) {
    if (signal?.aborted) throw error
    throw new ApiError('Não foi possível conectar ao backend. Verifique se ele está em execução em 127.0.0.1:8080.')
  }
  if (!response.ok) {
    throw new ApiError(`O backend respondeu com erro (HTTP ${response.status}).`)
  }
  let body: unknown
  try {
    body = await response.json()
  } catch {
    throw new ApiError('A resposta do backend não é um JSON válido.')
  }
  if (!isHistory(body)) {
    throw new ApiError('A resposta do backend tem um formato inesperado.')
  }
  return body
}
