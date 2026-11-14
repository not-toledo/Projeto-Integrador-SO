import { afterEach, describe, expect, it, vi } from 'vitest'
import { makeInventory } from '../modules/hardware/fixtures'
import { ApiError, fetchInventory } from './inventoryService'

function mockFetch(impl: () => Promise<Response>) {
  vi.stubGlobal('fetch', vi.fn(impl))
}

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('fetchInventory', () => {
  it('retorna o inventário quando a resposta é válida', async () => {
    const inventory = makeInventory()
    mockFetch(() => Promise.resolve(new Response(JSON.stringify(inventory), { status: 200 })))
    await expect(fetchInventory()).resolves.toEqual(inventory)
  })

  it('converte falha de conexão em ApiError com mensagem útil', async () => {
    mockFetch(() => Promise.reject(new TypeError('Failed to fetch')))
    await expect(fetchInventory()).rejects.toThrow(ApiError)
    await expect(fetchInventory()).rejects.toThrow(/backend/)
  })

  it('converte HTTP de erro em ApiError', async () => {
    mockFetch(() => Promise.resolve(new Response('erro', { status: 500 })))
    await expect(fetchInventory()).rejects.toThrow(/HTTP 500/)
  })

  it('rejeita JSON inválido', async () => {
    mockFetch(() => Promise.resolve(new Response('não é json', { status: 200 })))
    await expect(fetchInventory()).rejects.toThrow(/JSON/)
  })

  it('rejeita resposta com formato inesperado', async () => {
    mockFetch(() => Promise.resolve(new Response(JSON.stringify({ collectedAt: 'x', os: {} }), { status: 200 })))
    await expect(fetchInventory()).rejects.toThrow(/formato inesperado/)
  })

  it('não mascara o cancelamento da requisição', async () => {
    const controller = new AbortController()
    controller.abort()
    mockFetch(() => Promise.reject(new DOMException('abortado', 'AbortError')))
    const error = await fetchInventory(controller.signal).catch((e: unknown) => e)
    expect(error).not.toBeInstanceOf(ApiError)
  })
})
