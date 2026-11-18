import type { ProcessesResponse } from '../modules/processes/types'
import { ApiError } from './inventoryService'

// Em desenvolvimento, o Vite encaminha /api para o backend local (veja vite.config.ts).
const API_BASE: string = import.meta.env.VITE_API_BASE ?? ''

function isProcesses(value: unknown): value is ProcessesResponse {
  if (typeof value !== 'object' || value === null) return false
  const b = value as Record<string, unknown>
  return (
    typeof b.collectedAt === 'string' &&
    typeof b.sampleMillis === 'number' &&
    Array.isArray(b.processes) &&
    b.processes.every((p) => typeof p === 'object' && p !== null && typeof (p as Record<string, unknown>).pid === 'number') &&
    (b.osFamily === 'windows' || b.osFamily === 'linux')
  )
}

export async function fetchProcesses(signal?: AbortSignal): Promise<ProcessesResponse> {
  let response: Response
  try {
    response = await fetch(`${API_BASE}/api/processes`, { signal })
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
  if (!isProcesses(body)) {
    throw new ApiError('A resposta do backend tem um formato inesperado.')
  }
  return body
}
