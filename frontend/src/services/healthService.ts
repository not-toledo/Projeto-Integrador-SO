import { ApiError } from './inventoryService'
import type { HealthReport } from '../modules/health/types'

// Em desenvolvimento, o Vite encaminha /api para o backend local (veja vite.config.ts).
const API_BASE: string = import.meta.env.VITE_API_BASE ?? ''

const SEVERITIES = ['INFO', 'WARNING', 'CRITICAL']
const KINDS = ['NORMAL', 'PREVENTIVE', 'UNAVAILABLE']

function isAlert(value: unknown): boolean {
  if (typeof value !== 'object' || value === null) {
    return false
  }
  const a = value as Record<string, unknown>
  return (
    typeof a.id === 'string' &&
    typeof a.component === 'string' &&
    typeof a.title === 'string' &&
    typeof a.description === 'string' &&
    typeof a.severity === 'string' &&
    SEVERITIES.includes(a.severity) &&
    typeof a.kind === 'string' &&
    KINDS.includes(a.kind)
  )
}

function isHealthReport(value: unknown): value is HealthReport {
  if (typeof value !== 'object' || value === null) {
    return false
  }
  const body = value as Record<string, unknown>
  return (
    typeof body.collectedAt === 'string' &&
    typeof body.overall === 'string' &&
    SEVERITIES.includes(body.overall) &&
    typeof body.infoCount === 'number' &&
    typeof body.warningCount === 'number' &&
    typeof body.criticalCount === 'number' &&
    Array.isArray(body.alerts) &&
    body.alerts.every(isAlert)
  )
}

export async function fetchHealthReport(signal?: AbortSignal): Promise<HealthReport> {
  let response: Response
  try {
    response = await fetch(`${API_BASE}/api/health-check`, { signal })
  } catch (error) {
    if (signal?.aborted) {
      throw error
    }
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

  if (!isHealthReport(body)) {
    throw new ApiError('A resposta do backend tem um formato inesperado.')
  }
  return body
}
