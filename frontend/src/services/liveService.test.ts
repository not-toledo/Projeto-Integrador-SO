import { afterEach, describe, expect, it, vi } from 'vitest'
import { makeHistory, makeLive, makeSample } from '../modules/live/fixtures'
import { fetchLive, fetchLiveHistory } from './liveService'
import { ApiError } from './inventoryService'

function respond(body: unknown, init: { ok?: boolean; status?: number } = {}) {
  const response = { ok: init.ok ?? true, status: init.status ?? 200, json: () => Promise.resolve(body) }
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue(response))
}

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('liveService', () => {
  it('fetchLive devolve a resposta válida (inclusive latest nulo)', async () => {
    respond(makeLive(makeSample()))
    expect((await fetchLive()).latest?.cpu.status).toBe('OK')
    respond(makeLive(null))
    expect((await fetchLive()).latest).toBeNull()
  })

  it('fetchLiveHistory pede o limite e devolve as amostras', async () => {
    respond(makeHistory([makeSample()]))
    const history = await fetchLiveHistory(300)
    expect(history.samples.length).toBe(1)
    expect(vi.mocked(fetch).mock.calls[0][0]).toBe('/api/live/history?limit=300')
  })

  it('falha de rede vira mensagem clara', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('failed')))
    await expect(fetchLive()).rejects.toThrow(/Não foi possível conectar ao backend/)
    await expect(fetchLive()).rejects.toBeInstanceOf(ApiError)
  })

  it('HTTP de erro vira mensagem com o código', async () => {
    respond({}, { ok: false, status: 500 })
    await expect(fetchLive()).rejects.toThrow('HTTP 500')
  })

  it('JSON inválido e formato inesperado são rejeitados', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: true, status: 200, json: () => Promise.reject(new SyntaxError('x')) }))
    await expect(fetchLive()).rejects.toThrow('não é um JSON válido')

    respond({ status: { running: true }, latest: null })
    await expect(fetchLive()).rejects.toThrow('formato inesperado')

    respond({ ...makeHistory([makeSample()]), samples: [{ collectedAt: 'x' }] })
    await expect(fetchLiveHistory(10)).rejects.toThrow('formato inesperado')
  })
})
