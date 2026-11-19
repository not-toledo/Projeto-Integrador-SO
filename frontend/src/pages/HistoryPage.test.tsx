import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { historyMaxGapMs, freePercentSeries, volumeMounts, commitSeries } from '../modules/history/series'
import type { HistoryRecord, HistoryResponse } from '../modules/history/types'
import { fetchHistory } from '../services/historyService'
import { HistoryPage } from './HistoryPage'

vi.mock('../services/historyService', () => ({ fetchHistory: vi.fn() }))
const fetchMock = vi.mocked(fetchHistory)

function record(at: string, overrides: Partial<HistoryRecord> = {}): HistoryRecord {
  return {
    collectedAt: at, cpuPercent: 20, ramUsedPercent: 70, commitPercent: 80, swapUsedPercent: 10, uptimeSeconds: 100,
    volumes: [{ mountPoint: 'C:\\', totalBytes: 1000, freeBytes: 250 }],
    ...overrides,
  }
}

function response(overrides: Partial<HistoryResponse> = {}): HistoryResponse {
  return {
    directory: 'C:\\Users\\u\\AppData\\Local\\NEXCORE\\coletas', days: 7, count: 2,
    lastCollectedAt: '2026-11-17T12:15:00Z',
    scheduler: { state: 'INSTALLED', details: 'Próxima execução: 17/11/2026 12:30:00 · Estado: Pronto' },
    records: [record('2026-11-17T12:00:00Z'), record('2026-11-17T12:15:00Z')],
    problems: [],
    ...overrides,
  }
}

beforeEach(() => {
  fetchMock.mockReset()
})

afterEach(() => {
  cleanup()
})

describe('HistoryPage', () => {
  it('mostra o agendador ativo, o resumo e os gráficos', async () => {
    fetchMock.mockResolvedValue(response())
    render(<HistoryPage />)

    expect(await screen.findByText('Coleta agendada ativa.')).toBeTruthy()
    expect(screen.getByText(/Próxima execução: 17\/11\/2026 12:30:00/)).toBeTruthy()
    expect(screen.getByText(/2 coleta\(s\) no período/)).toBeTruthy()
    expect(screen.getByRole('img', { name: /Uso da CPU em %/ })).toBeTruthy()
    expect(screen.getByRole('img', { name: /Espaço livre — C:\\ em %/ })).toBeTruthy()
    expect(fetchMock).toHaveBeenCalledWith(7, expect.anything())
  })

  it('sem tarefa instalada orienta a usar o script de agendamento', async () => {
    fetchMock.mockResolvedValue(response({ scheduler: { state: 'NOT_INSTALLED', details: null }, records: [], count: 0, lastCollectedAt: null }))
    render(<HistoryPage />)

    expect(await screen.findByText('Coleta agendada não instalada.')).toBeTruthy()
    expect(screen.getByText('scripts\\agendar-coleta.bat instalar')).toBeTruthy()
    expect(screen.getByText('Nenhuma coleta neste período.')).toBeTruthy()
  })

  it('troca o período consultado', async () => {
    fetchMock.mockResolvedValue(response())
    render(<HistoryPage />)
    await screen.findByText('Coleta agendada ativa.')

    fireEvent.change(screen.getByLabelText('Período'), { target: { value: '30' } })
    expect(fetchMock).toHaveBeenLastCalledWith(30, expect.anything())
  })

  it('mostra arquivos que não puderam ser lidos sem esconder o restante', async () => {
    fetchMock.mockResolvedValue(response({ problems: ['Não foi possível ler coletas-2026-11-16.jsonl: bloqueado'] }))
    render(<HistoryPage />)

    expect(await screen.findByText(/coletas-2026-11-16.jsonl: bloqueado/)).toBeTruthy()
    expect(screen.getByRole('img', { name: /Uso da CPU em %/ })).toBeTruthy()
  })

  it('erro do backend aparece como alerta', async () => {
    fetchMock.mockRejectedValue(new Error('Não foi possível conectar ao backend.'))
    render(<HistoryPage />)
    expect(await screen.findByText('Não foi possível conectar ao backend.')).toBeTruthy()
  })
})

describe('séries do histórico', () => {
  it('espaço livre por volume, com lacuna quando o volume não aparece', () => {
    const records = [record('2026-11-17T12:00:00Z'), record('2026-11-17T12:15:00Z', { volumes: [] })]
    expect(volumeMounts(records)).toEqual(['C:\\'])
    expect(freePercentSeries(records, 'C:\\')).toEqual([25, null])
  })

  it('memória comprometida acima de 100% (Linux) é mantida; valores inválidos viram lacuna', () => {
    expect(commitSeries([record('t', { commitPercent: 140 }), record('t', { commitPercent: -1 })])).toEqual([140, null])
  })

  it('lacuna máxima é 3 vezes o intervalo mais comum entre coletas', () => {
    const min = 60_000
    expect(historyMaxGapMs([0, 15 * min, 30 * min, 45 * min, 600 * min])).toBe(45 * min)
    expect(historyMaxGapMs([0])).toBe(60 * min)
  })
})
