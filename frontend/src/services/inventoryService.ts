import type { ComponentResult, Inventory } from '../modules/hardware/types'

/** Erro com mensagem já adequada para exibir ao usuário. */
export class ApiError extends Error {}

// Em desenvolvimento, o Vite encaminha /api para o backend local (veja vite.config.ts).
const API_BASE: string = import.meta.env.VITE_API_BASE ?? ''

const COMPONENTS = ['os', 'cpu', 'memory', 'gpu', 'motherboard', 'storage'] as const
const STATUSES = ['OK', 'UNAVAILABLE', 'ERROR']

function isComponentResult(value: unknown): value is ComponentResult<unknown> {
  if (typeof value !== 'object' || value === null) {
    return false
  }
  const status = (value as { status?: unknown }).status
  return typeof status === 'string' && STATUSES.includes(status)
}

function isInventory(value: unknown): value is Inventory {
  if (typeof value !== 'object' || value === null) {
    return false
  }
  const body = value as Record<string, unknown>
  return typeof body.collectedAt === 'string' && COMPONENTS.every((key) => isComponentResult(body[key]))
}

export async function fetchInventory(signal?: AbortSignal): Promise<Inventory> {
  let response: Response
  try {
    response = await fetch(`${API_BASE}/api/inventory`, { signal })
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

  if (!isInventory(body)) {
    throw new ApiError('A resposta do backend tem um formato inesperado.')
  }
  return body
}
