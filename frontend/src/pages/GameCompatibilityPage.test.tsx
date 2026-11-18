import { cleanup, fireEvent, render, screen, within } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { GameCompatibility } from '../modules/games/types'
import { fetchGameCompatibility, searchGames } from '../services/gameService'
import { GameCompatibilityPage } from './GameCompatibilityPage'

vi.mock('../services/gameService', () => ({
  searchGames: vi.fn(),
  fetchGameCompatibility: vi.fn(),
}))

const searchMock = vi.mocked(searchGames)
const compatibilityMock = vi.mocked(fetchGameCompatibility)

function comparison(overrides: Partial<GameCompatibility> = {}): GameCompatibility {
  return {
    appId: 10,
    gameName: 'Jogo de teste',
    overallMinimum: 'Abaixo dos mínimos',
    overallRecommended: 'Análise incompleta',
    minimumResults: [
      { component: 'RAM', requirement: 'Memória: 16 GB', detected: '8,0 GiB', status: 'BELOW_MINIMUM',
        explanation: 'A memória detectada está abaixo do requisito.' },
      { component: 'CPU', requirement: 'Processador: i5-8400', detected: 'i7-4790K', status: 'UNKNOWN',
        explanation: 'Sem comparação.' },
    ],
    recommendedResults: [],
    minimumRawText: '',
    recommendedRawText: '',
    evaluatedAt: '2026-01-01T12:00:00Z',
    source: 'Steam',
    warnings: [],
    verdict: {
      code: 'NOT_RECOMMENDED',
      label: 'Não recomendado',
      reasons: ['Não atende ao mínimo de RAM: A memória detectada está abaixo do requisito.'],
      belowMinimum: ['RAM'],
      belowRecommended: [],
      pendingReview: ['CPU'],
    },
    ...overrides,
  }
}

async function selectGame() {
  searchMock.mockResolvedValue([{ appId: 10, name: 'Jogo de teste', headerImage: null, storeUrl: 'url' }])
  render(<GameCompatibilityPage />)
  fireEvent.change(screen.getByLabelText('Nome do jogo'), { target: { value: 'Jogo' } })
  fireEvent.click(await screen.findByRole('button', { name: /Jogo de teste/ }))
}

beforeEach(() => {
  searchMock.mockReset()
  compatibilityMock.mockReset()
})

afterEach(() => {
  cleanup()
})

describe('GameCompatibilityPage', () => {
  it('mostra a classificação geral com os motivos', async () => {
    compatibilityMock.mockResolvedValue(comparison())
    await selectGame()

    const verdict = await screen.findByRole('region', { name: 'Classificação geral' })
    expect(within(verdict).getByText('Não recomendado')).toBeTruthy()
    expect(within(verdict).getByText(/Não atende ao mínimo de RAM/)).toBeTruthy()
    expect(verdict.className).toContain('game-verdict-bad')
  })

  it('item sem dado aparece como "Dados insuficientes" e o aviso de FPS fica só no rodapé', async () => {
    compatibilityMock.mockResolvedValue(comparison({
      verdict: { code: 'INSUFFICIENT_DATA', label: 'Dados insuficientes', reasons: [], belowMinimum: [],
        belowRecommended: [], pendingReview: ['CPU'] },
    }))
    await selectGame()

    await screen.findByRole('region', { name: 'Classificação geral' })
    // Rótulo do veredito e do item da tabela.
    expect(screen.getAllByText('Dados insuficientes')).toHaveLength(2)
    expect(screen.queryByText('Desconhecido')).toBeNull()
    expect(screen.queryByText('Limitações da análise')).toBeNull()
    expect(screen.getAllByText(/Não garante FPS/)).toHaveLength(1)
  })

  it('CPU comparada por benchmark mostra a confiança e a fonte', async () => {
    compatibilityMock.mockResolvedValue(comparison({
      minimumResults: [{
        component: 'CPU', requirement: 'Processador: Core i7-7700K / Ryzen 5 1600', detected: 'i7-4790K',
        status: 'BELOW_MINIMUM', explanation: 'Benchmark do Blender 4.5.0 (CPU): 83% do exigido.',
        benchmark: {
          localModel: 'Intel Core i7-4790K', requiredModel: 'Intel Core i7-7700K', localScore: 73.67,
          requiredScore: 88.89, localSamples: 29, requiredSamples: 39, backend: 'CPU', blenderVersion: '4.5.0',
          ratio: 0.83, confidence: 'HIGH', source: 'Blender Open Data (opendata.blender.org, CC0), snapshot de 2026-11-18',
        },
      }],
    }))
    await selectGame()

    expect(await screen.findByText(/83% do exigido/)).toBeTruthy()
    expect(screen.getByText(/Comparado por benchmark \(confiança alta\) · Fonte: Blender Open Data/)).toBeTruthy()
  })
})
