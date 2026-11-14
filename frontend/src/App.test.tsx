import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import App from './App'
import { makeInventory } from './modules/hardware/fixtures'
import { makeReport, normalAlerts } from './modules/health/fixtures'
import { fetchHealthReport } from './services/healthService'
import { fetchInventory } from './services/inventoryService'

vi.mock('./services/inventoryService', async (importOriginal) => ({
  ...(await importOriginal<typeof import('./services/inventoryService')>()),
  fetchInventory: vi.fn(),
}))
vi.mock('./services/healthService', () => ({ fetchHealthReport: vi.fn() }))

beforeEach(() => {
  vi.mocked(fetchInventory).mockResolvedValue(makeInventory())
  vi.mocked(fetchHealthReport).mockResolvedValue(makeReport(normalAlerts))
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
})
