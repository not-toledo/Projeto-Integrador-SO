import { act, cleanup, fireEvent, render, screen, within } from '@testing-library/react'
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

function navLink(name: string) {
  return within(screen.getByRole('navigation', { name: 'Telas do PCCheck' })).getByRole('link', { name })
}

/** Segue um link da navegação: o endereço muda (#/...) e a tela acompanha. */
async function go(name: string) {
  const href = navLink(name).getAttribute('href') ?? ''
  fireEvent.click(navLink(name))
  // O jsdom nem sempre navega pelo clique em âncora; o hashchange é o que a aplicação escuta.
  await act(async () => {
    if (window.location.hash !== href) window.location.hash = href
    window.dispatchEvent(new HashChangeEvent('hashchange'))
  })
}

beforeEach(() => {
  window.location.hash = ''
  window.scrollTo = vi.fn() as typeof window.scrollTo
  vi.mocked(fetchInventory).mockResolvedValue(makeInventory())
  vi.mocked(fetchHealthReport).mockResolvedValue(makeReport(normalAlerts))
  vi.mocked(fetchLiveHistory).mockResolvedValue(makeHistory([makeSample({ at: new Date().toISOString() })]))
})

afterEach(() => {
  cleanup()
  vi.clearAllMocks()
})

describe('App', () => {
  it('abre no Início, com um link para cada módulo', () => {
    render(<App />)
    expect(screen.getByRole('heading', { level: 1, name: /Conheça/ })).toBeTruthy()
    expect(navLink('Início').getAttribute('aria-current')).toBe('page')
    for (const name of ['Hardware', 'Saúde', 'Monitor', 'Histórico', 'Jogos']) {
      expect(screen.getByRole('link', { name: `Abrir ${name}` })).toBeTruthy()
    }
    expect(screen.getByRole('link', { name: /Abrir monitoramento/ }).getAttribute('href')).toBe('#/live')
    expect(fetchHealthReport).not.toHaveBeenCalled()
  })

  it('alterna entre Hardware e Saúde pela navegação', async () => {
    render(<App />)
    await go('Hardware')
    expect(await screen.findByText('SO de teste')).toBeTruthy()
    expect(fetchHealthReport).not.toHaveBeenCalled()

    await go('Saúde')
    expect(await screen.findByLabelText('Resumo de saúde')).toBeTruthy()
    expect(navLink('Saúde').getAttribute('aria-current')).toBe('page')

    await go('Hardware')
    expect(await screen.findByText('SO de teste')).toBeTruthy()
  })

  it('abre a tela direto pelo endereço', async () => {
    window.location.hash = '#/games'
    render(<App />)
    expect(await screen.findByRole('heading', { level: 1, name: 'Compatibilidade com jogos' })).toBeTruthy()
    expect(screen.getByLabelText('Nome do jogo')).toBeTruthy()
  })

  it('abre o Live Monitor pela navegação e interrompe a consulta ao sair da tela', async () => {
    render(<App />)
    // O Início tem o painel "Agora", que também consulta o Live Monitor.
    expect(await screen.findByText('Uso da CPU')).toBeTruthy()
    const before = vi.mocked(fetchLiveHistory).mock.calls.length
    await go('Monitor')
    expect(await screen.findByLabelText('CPU')).toBeTruthy()
    expect(navLink('Monitor').getAttribute('aria-current')).toBe('page')
    expect(fetchLiveHistory).toHaveBeenCalledTimes(before + 1)

    await go('Hardware')
    expect(await screen.findByText('SO de teste')).toBeTruthy()
    expect(screen.queryByRole('heading', { name: 'Por processador lógico' })).toBeNull()
  })

  it('o botão Menu abre e fecha a navegação em telas estreitas', () => {
    render(<App />)
    const toggle = screen.getByRole('button', { name: 'Menu' })
    expect(toggle.getAttribute('aria-expanded')).toBe('false')
    fireEvent.click(toggle)
    expect(toggle.getAttribute('aria-expanded')).toBe('true')
    fireEvent.keyDown(window, { key: 'Escape' })
    expect(toggle.getAttribute('aria-expanded')).toBe('false')
  })
})
