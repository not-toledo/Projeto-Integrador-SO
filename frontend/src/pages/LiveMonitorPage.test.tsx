import { act, cleanup, render, screen, within } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { GPU_OK, makeHistory, makeLive, makeSample, makeSampleWithoutSensors, makeStatus } from '../modules/live/fixtures'
import type { LiveHistoryResponse, LiveResponse } from '../modules/live/types'
import { ApiError } from '../services/inventoryService'
import { fetchLive, fetchLiveHistory } from '../services/liveService'
import { LiveMonitorPage } from './LiveMonitorPage'

vi.mock('../services/liveService', () => ({ fetchLive: vi.fn(), fetchLiveHistory: vi.fn() }))

const historyMock = vi.mocked(fetchLiveHistory)
const liveMock = vi.mocked(fetchLive)

/** Amostra com horário atual, para que o estado de conexão seja "Conectado". */
const fresh = (offsetMs = 0) => new Date(Date.now() + offsetMs).toISOString()

beforeEach(() => {
  vi.useFakeTimers({ shouldAdvanceTime: true })
  historyMock.mockReset()
  liveMock.mockReset()
})

afterEach(() => {
  cleanup()
  vi.useRealTimers()
})

async function advance(ms: number) {
  await act(async () => {
    await vi.advanceTimersByTimeAsync(ms)
  })
}

describe('LiveMonitorPage', () => {
  it('mostra o estado de conexão enquanto aguarda o backend', () => {
    historyMock.mockReturnValue(new Promise<LiveHistoryResponse>(() => {}))
    render(<LiveMonitorPage />)

    expect(screen.getByText('Conectando ao backend…')).toBeTruthy()
    expect(document.querySelector('[data-state="connecting"]')).toBeTruthy()
  })

  it('exibe indicadores atuais, gráficos com unidades e horário da leitura', async () => {
    historyMock.mockResolvedValue(makeHistory([makeSample({ at: fresh(-2000) }), makeSample({ at: fresh() })]))
    render(<LiveMonitorPage />)

    expect(await screen.findByLabelText('CPU')).toBeTruthy()
    expect(document.querySelector('[data-state="connected"]')).toBeTruthy()
    expect(screen.getByText('Conectado')).toBeTruthy()
    expect(screen.getByText(/Amostras: 2\/300/)).toBeTruthy()

    const cpu = screen.getByLabelText('CPU')
    expect(within(cpu).getAllByText('25,0%').length).toBeGreaterThan(0)
    expect(within(cpu).getByRole('img', { name: /Uso da CPU em %/ })).toBeTruthy()
    // Núcleo sem leitura aparece como tal, sem valor inventado.
    expect(within(cpu).getByText('CPU 2').closest('.usage')?.textContent).toContain('Não disponível')

    const memory = screen.getByLabelText('Memória RAM')
    expect(within(memory).getByText('62,5%')).toBeTruthy()
    expect(within(memory).getByRole('img', { name: /Uso da RAM em %/ })).toBeTruthy()

    const disks = screen.getByLabelText('Discos')
    expect(within(disks).getByRole('img', { name: /Leitura e escrita — Disco de teste em MiB\/s/ })).toBeTruthy()

    const sensors = screen.getByLabelText('Sensores')
    expect(within(sensors).getByText('55,0 °C')).toBeTruthy()
    expect(within(sensors).getByText('1.200 RPM, 1.300 RPM')).toBeTruthy()
  })

  it('GPU sem o leitor de sensores aparece como "Sem leitura", com a orientação e sem valores', async () => {
    historyMock.mockResolvedValue(makeHistory([makeSample({ at: fresh() })]))
    render(<LiveMonitorPage />)

    const gpu = await screen.findByLabelText('GPU')
    expect(within(gpu).getByText('Sem leitura')).toBeTruthy()
    expect(within(gpu).getByText(/leitor de sensores/)).toBeTruthy()
    expect(within(gpu).queryByText(/%/)).toBeNull()
  })

  it('GPU com leituras mostra placa, utilização, memória de vídeo e temperatura', async () => {
    historyMock.mockResolvedValue(makeHistory([makeSample({ at: fresh(), gpu: GPU_OK })]))
    render(<LiveMonitorPage />)

    const gpu = await screen.findByLabelText('GPU')
    expect(within(gpu).getByText('GPU de teste')).toBeTruthy()
    expect(within(gpu).getByText('42,0%')).toBeTruthy()
    expect(within(gpu).getByText('61,0 °C')).toBeTruthy()
    expect(within(gpu).getByRole('img', { name: /Uso da GPU e da memória de vídeo em %/ })).toBeTruthy()
  })

  it('sensores e taxas de disco ausentes viram "Sem leitura", sem quebrar o restante', async () => {
    historyMock.mockResolvedValue(makeHistory([makeSampleWithoutSensors(fresh())]))
    render(<LiveMonitorPage />)

    const sensors = await screen.findByLabelText('Sensores')
    expect(within(sensors).getByText('Sem leitura')).toBeTruthy()

    const disks = screen.getByLabelText('Discos')
    const chart = within(disks).getByRole('img', { name: /Leitura e escrita — Disco de teste: Sem leitura/ })
    expect(chart).toBeTruthy()
    expect(within(screen.getByLabelText('CPU')).getAllByText('25,0%').length).toBeGreaterThan(0)
  })

  it('falha de coleta de um grupo não afeta os demais', async () => {
    const sample = makeSample({ at: fresh(), sensors: { status: 'ERROR', message: 'leitura falhou', data: null } })
    historyMock.mockResolvedValue(makeHistory([sample]))
    render(<LiveMonitorPage />)

    const sensors = await screen.findByLabelText('Sensores')
    expect(within(sensors).getByText('Falha na coleta')).toBeTruthy()
    expect(within(screen.getByLabelText('Memória RAM')).getByText('62,5%')).toBeTruthy()
  })

  it('mostra erro quando o backend está inacessível e tenta de novo sozinho', async () => {
    historyMock.mockRejectedValueOnce(new ApiError('Não foi possível conectar ao backend.'))
    render(<LiveMonitorPage />)

    expect(await screen.findByText('Não foi possível conectar ao backend.')).toBeTruthy()
    expect(document.querySelector('[data-state="disconnected"]')).toBeTruthy()
    expect(screen.queryByLabelText('CPU')).toBeNull()

    historyMock.mockResolvedValueOnce(makeHistory([makeSample({ at: fresh(2000) })]))
    await advance(2100)
    expect(await screen.findByLabelText('CPU')).toBeTruthy()
    expect(document.querySelector('[data-state="connected"]')).toBeTruthy()
  })

  it('atualiza com novas amostras e mantém a última ao perder a conexão', async () => {
    historyMock.mockResolvedValue(makeHistory([makeSample({ at: fresh() })]))
    render(<LiveMonitorPage />)
    expect(await screen.findByText(/Amostras: 1\/300/)).toBeTruthy()

    liveMock.mockResolvedValueOnce(makeLive(makeSample({ at: fresh(2000) })) as LiveResponse)
    await advance(2100)
    expect(screen.getByText(/Amostras: 2\/300/)).toBeTruthy()

    liveMock.mockRejectedValueOnce(new ApiError('Não foi possível conectar ao backend.'))
    await advance(2100)
    expect(document.querySelector('[data-state="disconnected"]')).toBeTruthy()
    // A última amostra continua visível, com aviso.
    expect(screen.getByLabelText('CPU')).toBeTruthy()
    expect(screen.getByText(/Exibindo a última amostra recebida/)).toBeTruthy()
  })

  it('não empilha consultas: aguarda a anterior terminar antes de agendar a próxima', async () => {
    historyMock.mockResolvedValue(makeHistory([makeSample({ at: fresh() })]))
    render(<LiveMonitorPage />)
    await screen.findByLabelText('CPU')

    let release: (value: LiveResponse) => void = () => {}
    liveMock.mockReturnValue(new Promise<LiveResponse>((resolve) => (release = resolve)))
    await advance(2100)
    expect(liveMock).toHaveBeenCalledTimes(1)

    // Mesmo passando muito tempo, enquanto a consulta não termina nenhuma outra é feita.
    await advance(30_000)
    expect(liveMock).toHaveBeenCalledTimes(1)

    liveMock.mockResolvedValue(makeLive(makeSample({ at: fresh() })))
    await act(async () => {
      release(makeLive(makeSample({ at: fresh() })))
    })
    await advance(2100)
    expect(liveMock).toHaveBeenCalledTimes(2)
  })

  it('aguarda a primeira coleta quando ainda não há amostra', async () => {
    historyMock.mockResolvedValue(makeHistory([]))
    render(<LiveMonitorPage />)

    expect(await screen.findByText('Aguardando a primeira coleta terminar…')).toBeTruthy()
    expect(screen.queryByLabelText('CPU')).toBeNull()
  })

  it('indica coleta com problemas sem afirmar defeito de hardware', async () => {
    historyMock.mockResolvedValue(
      makeHistory([makeSample({ at: fresh() })], makeStatus({ consecutiveFailures: 3, lastError: 'falha de teste' })),
    )
    render(<LiveMonitorPage />)

    expect(await screen.findByText('A coleta está com problemas.')).toBeTruthy()
    expect(screen.getByText(/falha de teste/)).toBeTruthy()
    expect(screen.getByText(/não indica defeito no hardware/i)).toBeTruthy()
  })
})
