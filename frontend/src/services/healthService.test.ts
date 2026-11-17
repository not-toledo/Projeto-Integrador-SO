import { afterEach, describe, expect, it, vi } from 'vitest'
import { attentionAlerts, makeReport } from '../modules/health/fixtures'
import { ApiError } from './inventoryService'
import { fetchHealthReport } from './healthService'

function mockFetch(impl: () => Promise<Response>) {
  vi.stubGlobal('fetch', vi.fn(impl))
}

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('fetchHealthReport', () => {
  it('retorna o relatório quando a resposta é válida', async () => {
    const report = makeReport(attentionAlerts)
    mockFetch(() => Promise.resolve(new Response(JSON.stringify(report), { status: 200 })))
    await expect(fetchHealthReport()).resolves.toEqual(report)
  })

  it('converte falha de conexão em ApiError com mensagem útil', async () => {
    mockFetch(() => Promise.reject(new TypeError('Failed to fetch')))
    await expect(fetchHealthReport()).rejects.toThrow(ApiError)
    await expect(fetchHealthReport()).rejects.toThrow(/backend/)
  })

  it('converte HTTP de erro em ApiError', async () => {
    mockFetch(() => Promise.resolve(new Response('erro', { status: 500 })))
    await expect(fetchHealthReport()).rejects.toThrow(/HTTP 500/)
  })

  it('rejeita JSON inválido', async () => {
    mockFetch(() => Promise.resolve(new Response('não é json', { status: 200 })))
    await expect(fetchHealthReport()).rejects.toThrow(/JSON/)
  })

  it('rejeita formato inesperado e severidade desconhecida', async () => {
    mockFetch(() => Promise.resolve(new Response(JSON.stringify({ collectedAt: 'x' }), { status: 200 })))
    await expect(fetchHealthReport()).rejects.toThrow(/formato inesperado/)

    const invalid = makeReport(attentionAlerts)
    ;(invalid.alerts[0] as { severity: string }).severity = 'GRAVE'
    mockFetch(() => Promise.resolve(new Response(JSON.stringify(invalid), { status: 200 })))
    await expect(fetchHealthReport()).rejects.toThrow(/formato inesperado/)
  })
})
