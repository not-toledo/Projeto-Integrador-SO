import { cleanup, fireEvent, render, screen, within } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { ProcessInfo, ProcessesResponse } from '../modules/processes/types'
import { ApiError } from '../services/inventoryService'
import { fetchProcesses } from '../services/processService'
import { ProcessesPage } from './ProcessesPage'

vi.mock('../services/processService', () => ({ fetchProcesses: vi.fn() }))
const fetchMock = vi.mocked(fetchProcesses)

function proc(pid: number, name: string, overrides: Partial<ProcessInfo> = {}): ProcessInfo {
  return {
    pid, parentPid: 1, name, user: 'aluno', state: 'RUNNING', priority: 8, threads: 4, cpuPercent: 1,
    memoryBytes: 100 * 1024 * 1024, virtualBytes: 0, startTime: '2026-01-01T10:00:00Z', self: false, ...overrides,
  }
}

function response(overrides: Partial<ProcessesResponse> = {}): ProcessesResponse {
  return {
    collectedAt: '2026-01-01T12:00:00Z', sampleMillis: 1000, logicalProcessors: 8, totalMemoryBytes: 16 * 1024 ** 3,
    processCount: 4, threadCount: 120, error: null, osFamily: 'windows',
    processes: [
      proc(10, 'navegador', { cpuPercent: 12 }),
      proc(11, 'navegador', { cpuPercent: 3, priority: 10 }),
      proc(20, 'java', { cpuPercent: 2, memoryBytes: 900 * 1024 * 1024, self: true }),
      proc(30, 'svchost', { cpuPercent: null, user: null }),
    ],
    ...overrides,
  }
}

beforeEach(() => {
  fetchMock.mockReset()
})

afterEach(() => {
  cleanup()
})

describe('ProcessesPage', () => {
  it('resume os processos com os números do sistema', async () => {
    fetchMock.mockResolvedValue(response())
    render(<ProcessesPage />)
    const tiles = await screen.findByLabelText('Resumo dos processos')
    expect(within(tiles).getByText('4')).toBeTruthy()
    expect(within(tiles).getByText('120')).toBeTruthy()
    // Os dois processos "navegador" somados: 15%.
    expect(within(tiles).getByText('navegador')).toBeTruthy()
    expect(within(tiles).getByText('15,0%')).toBeTruthy()
  })

  it('agrupa por nome e permite ver cada processo', async () => {
    fetchMock.mockResolvedValue(response())
    render(<ProcessesPage />)
    const table = await screen.findByLabelText('Lista de processos')
    expect(within(table).getByText('2 processos')).toBeTruthy()
    expect(within(table).getByText('PCCheck')).toBeTruthy()

    fireEvent.click(within(table).getByLabelText('Agrupar por nome'))
    expect(within(table).queryByText('2 processos')).toBeNull()
    expect(within(table).getAllByText('Normal (8)')).toHaveLength(3)
    expect(within(table).getByText('Acima do normal (10)')).toBeTruthy()
  })

  it('sem medição de CPU ou sem dono, informa em vez de inventar', async () => {
    fetchMock.mockResolvedValue(response())
    render(<ProcessesPage />)
    const table = await screen.findByLabelText('Lista de processos')
    const row = within(table).getByText('svchost').closest('tr')!
    expect(within(row).getByText('Medindo…')).toBeTruthy()
    expect(within(row).getByText('Não informado')).toBeTruthy()
  })

  it('filtra por nome', async () => {
    fetchMock.mockResolvedValue(response())
    render(<ProcessesPage />)
    const table = await screen.findByLabelText('Lista de processos')
    fireEvent.change(within(table).getByLabelText('Filtrar processos'), { target: { value: 'jav' } })
    expect(within(table).getByText('java')).toBeTruthy()
    expect(within(table).queryByText('svchost')).toBeNull()
  })

  it('no Linux mostra a coluna Estado e a prioridade como nice', async () => {
    fetchMock.mockResolvedValue(response({
      osFamily: 'linux',
      processes: [proc(5, 'bash', { state: 'SLEEPING', priority: 20 })],
    }))
    render(<ProcessesPage />)
    const table = await screen.findByLabelText('Lista de processos')
    expect(within(table).getByText('Dormindo')).toBeTruthy()
    expect(within(table).getByText('nice 0')).toBeTruthy()
  })

  it('mostra erro de comunicação', async () => {
    fetchMock.mockRejectedValue(new ApiError('Não foi possível conectar ao backend.'))
    render(<ProcessesPage />)
    expect(await screen.findByText('Não foi possível conectar ao backend.')).toBeTruthy()
    expect(screen.queryByLabelText('Lista de processos')).toBeNull()
  })
})
