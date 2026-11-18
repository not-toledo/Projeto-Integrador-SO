import { cleanup, fireEvent, render, screen, within } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { fetchPrecheck } from '../../services/gameService'
import { PrecheckPanel } from './PrecheckPanel'
import type { Precheck } from './types'

vi.mock('../../services/gameService', async (importOriginal) => ({
  ...(await importOriginal<typeof import('../../services/gameService')>()),
  fetchPrecheck: vi.fn(),
}))

const fetchMock = vi.mocked(fetchPrecheck)
const GIB = 1024 ** 3

const RESULT: Precheck = {
  appId: 900001,
  gameName: 'Aventura de Teste',
  overall: 'ATTENTION',
  overallLabel: 'Atenção antes de jogar',
  items: [
    { id: 'ram', label: 'Memória disponível agora', status: 'OK', observed: '9,8 GiB disponíveis de 16,0 GiB', detail: 'O mínimo publicado é 8,0 GiB.' },
    { id: 'game-ssd', label: 'SSD exigido pelo jogo', status: 'ATTENTION', observed: 'Instalado num HDD (D:\\)', detail: 'Os requisitos pedem SSD.' },
    { id: 'power', label: 'Plano de energia', status: 'UNAVAILABLE', observed: null, detail: 'O powercfg não respondeu.' },
  ],
  topCpu: [{ name: 'brave', processCount: 12, cpuPercent: 3.2, memoryBytes: 2 * GIB }],
  topMemory: [{ name: 'brave', processCount: 12, cpuPercent: 3.2, memoryBytes: 2 * GIB }],
  processError: null,
  checkedAt: '2026-11-18T12:00:00Z',
}

beforeEach(() => fetchMock.mockReset())
afterEach(() => cleanup())

describe('PrecheckPanel', () => {
  it('só verifica quando pedido e mostra resultado, motivos e processos', async () => {
    fetchMock.mockResolvedValue(RESULT)
    render(<PrecheckPanel appId={900001} />)
    expect(fetchMock).not.toHaveBeenCalled()

    fireEvent.click(screen.getByRole('button', { name: 'Verificar agora' }))
    expect(await screen.findByText('Atenção antes de jogar')).toBeTruthy()
    expect(fetchMock).toHaveBeenCalledWith(900001)

    const ssd = screen.getByText('SSD exigido pelo jogo').closest('tr') as HTMLTableRowElement
    expect(within(ssd).getByText('Atenção')).toBeTruthy()
    // Dado não lido aparece como "Não verificado", nunca como ok.
    const power = screen.getByText('Plano de energia').closest('tr') as HTMLTableRowElement
    expect(within(power).getByText('Não verificado')).toBeTruthy()
    expect(within(power).getByText('—')).toBeTruthy()

    expect(screen.getByText('3,2%')).toBeTruthy()
    expect(screen.getByText('2,00 GiB')).toBeTruthy()
    expect(screen.getAllByText(/brave \(12 processos\)/)).toHaveLength(2)
    expect(screen.getByRole('button', { name: 'Verificar de novo' })).toBeTruthy()
  })

  it('resultado de outro jogo não é exibido e erro aparece', async () => {
    fetchMock.mockResolvedValueOnce(RESULT)
    const { rerender } = render(<PrecheckPanel appId={900001} />)
    fireEvent.click(screen.getByRole('button', { name: 'Verificar agora' }))
    await screen.findByText('Atenção antes de jogar')

    rerender(<PrecheckPanel appId={123} />)
    expect(screen.queryByText('Atenção antes de jogar')).toBeNull()

    fetchMock.mockRejectedValueOnce(new Error('A API respondeu com HTTP 500.'))
    fireEvent.click(screen.getByRole('button', { name: 'Verificar agora' }))
    expect(await screen.findByRole('alert')).toBeTruthy()
  })
})
