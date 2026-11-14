import { ApiError } from './inventoryService'
import type { LiveHistoryResponse, LiveResponse, LiveSample, MonitorStatus } from '../modules/live/types'

// Em desenvolvimento, o Vite encaminha /api para o backend local (veja vite.config.ts).
const API_BASE: string = import.meta.env.VITE_API_BASE ?? ''

const STATUSES = ['OK', 'UNAVAILABLE', 'ERROR']
const GROUPS = ['cpu', 'memory', 'gpu', 'disks', 'sensors'] as const

function isObject(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null
}

function isGroup(value: unknown): boolean {
  return isObject(value) && typeof value.status === 'string' && STATUSES.includes(value.status)
}

function isSample(value: unknown): value is LiveSample {
  return (
    isObject(value) &&
    typeof value.collectedAt === 'string' &&
    Number.isFinite(Date.parse(value.collectedAt)) &&
    GROUPS.every((key) => isGroup(value[key]))
  )
}

function isStatus(value: unknown): value is MonitorStatus {
  return (
    isObject(value) &&
    typeof value.running === 'boolean' &&
    typeof value.collecting === 'boolean' &&
    typeof value.stalled === 'boolean' &&
    typeof value.intervalMs === 'number' &&
    typeof value.historyCapacity === 'number' &&
    typeof value.sampleCount === 'number' &&
    typeof value.consecutiveFailures === 'number'
  )
}

function isLive(value: unknown): value is LiveResponse {
  return isObject(value) && isStatus(value.status) && (value.latest === null || isSample(value.latest))
}

function isHistory(value: unknown): value is LiveHistoryResponse {
  return isObject(value) && isStatus(value.status) && Array.isArray(value.samples) && value.samples.every(isSample)
}

async function getJson(path: string, signal?: AbortSignal): Promise<unknown> {
  let response: Response
  try {
    response = await fetch(`${API_BASE}${path}`, { signal })
  } catch (error) {
    if (signal?.aborted) {
      throw error
    }
    throw new ApiError('Não foi possível conectar ao backend. Verifique se ele está em execução em 127.0.0.1:8080.')
  }

  if (!response.ok) {
    throw new ApiError(`O backend respondeu com erro (HTTP ${response.status}).`)
  }

  try {
    return await response.json()
  } catch {
    throw new ApiError('A resposta do backend não é um JSON válido.')
  }
}

/** Estado da coleta e amostra mais recente (GET /api/live). Apenas lê a memória do backend. */
export async function fetchLive(signal?: AbortSignal): Promise<LiveResponse> {
  const body = await getJson('/api/live', signal)
  if (!isLive(body)) {
    throw new ApiError('A resposta do backend tem um formato inesperado.')
  }
  return body
}

/** Histórico em memória, da amostra mais antiga para a mais recente (GET /api/live/history). */
export async function fetchLiveHistory(limit: number, signal?: AbortSignal): Promise<LiveHistoryResponse> {
  const body = await getJson(`/api/live/history?limit=${encodeURIComponent(String(limit))}`, signal)
  if (!isHistory(body)) {
    throw new ApiError('A resposta do backend tem um formato inesperado.')
  }
  return body
}
