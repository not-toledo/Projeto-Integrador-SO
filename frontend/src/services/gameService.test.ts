import { afterEach, describe, expect, it, vi } from 'vitest'
import { fetchGameCompatibility, searchGames } from './gameService'

afterEach(() => vi.unstubAllGlobals())

describe('gameService', () => {
  it('codifica o termo de pesquisa e retorna resultados Steam', async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify([
      { appId: 570, name: 'Dota 2', headerImage: null, storeUrl: 'https://store.steampowered.com/app/570/' },
    ]), { status: 200 }))
    vi.stubGlobal('fetch', fetchMock)
    const result = await searchGames('Dota 2')
    expect(fetchMock.mock.calls[0][0]).toContain('query=Dota%202')
    expect(result[0].appId).toBe(570)
  })

  it('mostra mensagem retornada pela API em caso de erro', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(JSON.stringify({ error: 'Steam indisponível' }), { status: 503 })))
    await expect(fetchGameCompatibility(570)).rejects.toThrow('Steam indisponível')
  })
})
