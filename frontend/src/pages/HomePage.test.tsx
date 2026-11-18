import { cleanup, render, screen, within } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { makeInventory } from '../modules/hardware/fixtures'
import { makeHistory, makeSample, makeSampleWithoutSensors } from '../modules/live/fixtures'
import { fetchInventory } from '../services/inventoryService'
import { fetchLiveHistory } from '../services/liveService'
import { HomePage } from './HomePage'

vi.mock('../services/inventoryService', async (importOriginal) => ({
  ...(await importOriginal<typeof import('../services/inventoryService')>()),
  fetchInventory: vi.fn(),
}))
vi.mock('../services/liveService', () => ({ fetchLive: vi.fn(), fetchLiveHistory: vi.fn() }))

beforeEach(() => {
  vi.mocked(fetchInventory).mockResolvedValue(makeInventory())
  vi.mocked(fetchLiveHistory).mockResolvedValue(makeHistory([makeSample({ at: new Date().toISOString() })]))
})

afterEach(() => {
  cleanup()
  vi.clearAllMocks()
})

describe('HomePage', () => {
  it('resume o computador com os dados do inventário', async () => {
    render(<HomePage />)
    const stats = await screen.findByLabelText('Números deste computador')
    expect(within(stats).getByText('4')).toBeTruthy()
    expect(within(stats).getByText('8')).toBeTruthy()
    expect(within(stats).getByText('8,00 GiB')).toBeTruthy()
    expect(screen.getByRole('heading', { level: 3, name: 'CPU de teste' })).toBeTruthy()
    expect(screen.getByText('GPU de teste')).toBeTruthy()
  })

  it('distingue componente não informado de falha de coleta', async () => {
    vi.mocked(fetchInventory).mockResolvedValue(makeInventory({
      gpu: { status: 'UNAVAILABLE', message: null, data: null },
      memory: { status: 'ERROR', message: 'falhou', data: null },
    }))
    render(<HomePage />)
    expect(await screen.findByText('Placa de vídeo:')).toBeTruthy()
    const gpu = screen.getByText('Placa de vídeo:').parentElement!
    expect(within(gpu).getByText('Não disponível')).toBeTruthy()
    const memory = screen.getByText('Memória:').parentElement!
    expect(within(memory).getByText('Falha na coleta')).toBeTruthy()
  })

  it('sem backend, explica o erro e permite tentar de novo', async () => {
    vi.mocked(fetchInventory).mockRejectedValue(new Error('Sem conexão com o backend'))
    render(<HomePage />)
    const alert = await screen.findByText('Não foi possível obter o inventário.')
    expect(alert.closest('[role="alert"]')?.textContent).toContain('Sem conexão com o backend')
    expect(screen.getByRole('button', { name: 'Tentar de novo' })).toBeTruthy()
  })

  it('mostra leituras reais no painel "Agora" e "Sem leitura" quando falta sensor', async () => {
    vi.mocked(fetchLiveHistory).mockResolvedValue(makeHistory([makeSampleWithoutSensors(new Date().toISOString())]))
    render(<HomePage />)
    const cpu = (await screen.findByText('Uso da CPU')).parentElement!
    expect(within(cpu).getByText('25,0%')).toBeTruthy()
    const temp = screen.getByText('Temperatura da CPU').parentElement!
    expect(within(temp).getByText('Sem leitura')).toBeTruthy()
  })
})
