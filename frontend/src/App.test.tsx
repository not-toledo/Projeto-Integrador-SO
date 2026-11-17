import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import App from './App'
import { makeInventory } from './modules/hardware/fixtures'
import { makeReport, normalAlerts } from './modules/health/fixtures'
import { makeHistory, makeSample } from './modules/live/fixtures'
import { fetchHealthReport } from './services/healthService'
import { fetchLiveHistory } from './services/liveService'
import { fetchInventory } from './services/inventoryService'

vi.mock('./services/inventoryService', async (importOriginal) => ({
  ...(await importOriginal<typeof import('./services/inventoryService')>()),
  fetchInventory: vi.fn(),
}))
vi.mock('./services/healthService', () => ({ fetchHealthReport: vi.fn() }))
vi.mock('./services/liveService', () => ({ fetchLive: vi.fn(), fetchLiveHistory: vi.fn() }))

beforeEach(() => {
  vi.mocked(fetchInventory).mockResolvedValue(makeInventory())
  vi.mocked(fetchHealthReport).mockResolvedValue(makeReport(normalAlerts))
  vi.mocked(fetchLiveHistory).mockResolvedValue(makeHistory([makeSample({ at: new Date().toISOString() })]))
})

afterEach(() => {
  cleanup()
  vi.clearAllMocks()
})

describe('App', () => {
  it('abre no Hardware Overview e alterna para o Health Check', async () => {
    render(<App />)
    expect(await screen.findByText('SO de teste')).toBeTruthy()
    expect(fetchHealthReport).not.toHaveBeenCalled()

    fireEvent.click(screen.getByRole('button', { name: 'Health Check' }))
    expect(await screen.findByLabelText('Resumo de saúde')).toBeTruthy()
    expect(screen.getByRole('button', { name: 'Health Check' }).getAttribute('aria-current')).toBe('page')

    fireEvent.click(screen.getByRole('button', { name: 'Hardware Overview' }))
    expect(await screen.findByText('SO de teste')).toBeTruthy()
  })

  it('abre a página de compatibilidade com jogos', async () => {
    render(<App />)
    fireEvent.click(screen.getByRole('button', { name: 'Compatibilidade com jogos' }))
    expect(await screen.findByRole('heading', { name: 'PCCheck — Compatibilidade com jogos' })).toBeTruthy()
    expect(screen.getByLabelText('Nome do jogo')).toBeTruthy()
  })

  it('abre o Live Monitor pela navegação e interrompe a consulta ao sair da aba', async () => {
    render(<App />)
    expect(await screen.findByText('SO de teste')).toBeTruthy()
    expect(fetchLiveHistory).not.toHaveBeenCalled()

    fireEvent.click(screen.getByRole('button', { name: 'Live Monitor' }))
    expect(await screen.findByLabelText('CPU')).toBeTruthy()
    expect(screen.getByRole('button', { name: 'Live Monitor' }).getAttribute('aria-current')).toBe('page')
    expect(fetchLiveHistory).toHaveBeenCalledTimes(1)

    fireEvent.click(screen.getByRole('button', { name: 'Hardware Overview' }))
    expect(await screen.findByText('SO de teste')).toBeTruthy()
    expect(screen.queryByRole('heading', { name: 'Por processador lógico' })).toBeNull()
  })
})
