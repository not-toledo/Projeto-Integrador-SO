import { cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { attentionAlerts, criticalAlerts, makeReport, noSensorAlerts, normalAlerts } from '../modules/health/fixtures'
import type { HealthReport } from '../modules/health/types'
import { ApiError } from '../services/inventoryService'
import { fetchHealthReport } from '../services/healthService'
import { HealthCheckPage } from './HealthCheckPage'

vi.mock('../services/healthService', () => ({ fetchHealthReport: vi.fn() }))

const fetchMock = vi.mocked(fetchHealthReport)

beforeEach(() => {
  fetchMock.mockReset()
})

afterEach(() => {
  cleanup()
})

describe('HealthCheckPage', () => {
  it('mostra o estado de carregamento enquanto aguarda a API', () => {
    fetchMock.mockReturnValue(new Promise<HealthReport>(() => {}))
    render(<HealthCheckPage />)

    expect(screen.getByText(/Avaliando os indicadores/)).toBeTruthy()
    const button = screen.getByRole('button', { name: 'Avaliando…' }) as HTMLButtonElement
    expect(button.disabled).toBe(true)
  })

  it('mostra erro de comunicação e permite tentar novamente', async () => {
    fetchMock.mockRejectedValueOnce(new ApiError('Não foi possível conectar ao backend.'))
    render(<HealthCheckPage />)

    expect(await screen.findByText('Não foi possível conectar ao backend.')).toBeTruthy()
    expect(screen.queryByLabelText('Resumo de saúde')).toBeNull()

    fetchMock.mockResolvedValueOnce(makeReport(normalAlerts))
    fireEvent.click(screen.getByRole('button', { name: 'Reavaliar' }))

    expect(await screen.findByLabelText('Resumo de saúde')).toBeTruthy()
    expect(screen.queryByText('Não foi possível conectar ao backend.')).toBeNull()
  })

  it('cenário normal: resumo em Informação e nenhum alerta de Atenção/Crítico', async () => {
    fetchMock.mockResolvedValue(makeReport(normalAlerts))
    render(<HealthCheckPage />)

    const summary = await screen.findByLabelText('Resumo de saúde')
    expect(summary.getAttribute('data-overall')).toBe('INFO')
    expect(within(summary).getByText(/Nenhum indicador avaliado passou/)).toBeTruthy()
    const items = within(screen.getByLabelText('Alertas do Health Check')).getAllByRole('listitem')
    expect(items.every((li) => li.getAttribute('data-severity') === 'INFO')).toBe(true)
  })

  it('cenário atenção: destaca o alerta com valor, unidade e limite', async () => {
    fetchMock.mockResolvedValue(makeReport(attentionAlerts))
    render(<HealthCheckPage />)

    const summary = await screen.findByLabelText('Resumo de saúde')
    expect(summary.getAttribute('data-overall')).toBe('WARNING')
    expect(within(summary).getByText('Atenção', { selector: '.badge' })).toBeTruthy()
    expect(screen.getByText('Espaço livre baixo em C:')).toBeTruthy()
    expect(screen.getByText('10,0 GiB livres de 100,0 GiB (10,0% livre)')).toBeTruthy()
    expect(screen.getByText('Atenção abaixo de 15,0% livre')).toBeTruthy()
  })

  it('cenário crítico: o alerta crítico aparece primeiro e é preventivo', async () => {
    fetchMock.mockResolvedValue(makeReport(criticalAlerts))
    render(<HealthCheckPage />)

    const summary = await screen.findByLabelText('Resumo de saúde')
    expect(summary.getAttribute('data-overall')).toBe('CRITICAL')
    const items = within(screen.getByLabelText('Alertas do Health Check')).getAllByRole('listitem')
    expect(items[0].getAttribute('data-severity')).toBe('CRITICAL')
    expect(within(items[0]).getByText('97,0 °C')).toBeTruthy()
    expect(within(items[0]).getByText(/não comprova defeito/)).toBeTruthy()
  })

  it('cenário sensor ausente: informa indisponibilidade sem tratar como defeito', async () => {
    fetchMock.mockResolvedValue(makeReport(noSensorAlerts))
    render(<HealthCheckPage />)

    expect(await screen.findByText('Temperatura da CPU não disponível')).toBeTruthy()
    expect(screen.getByText('Sem leitura')).toBeTruthy()
    expect(screen.getByText(/não é falha física/)).toBeTruthy()
    expect(screen.getByLabelText('Resumo de saúde').getAttribute('data-overall')).toBe('INFO')
  })

  it('mantém o último relatório quando a reavaliação falha', async () => {
    fetchMock.mockResolvedValueOnce(makeReport(attentionAlerts))
    render(<HealthCheckPage />)
    expect(await screen.findByText('Espaço livre baixo em C:')).toBeTruthy()

    fetchMock.mockRejectedValueOnce(new ApiError('Backend indisponível.'))
    fireEvent.click(screen.getByRole('button', { name: 'Reavaliar' }))

    expect(await screen.findByText('Backend indisponível.')).toBeTruthy()
    expect(screen.getByText('Espaço livre baixo em C:')).toBeTruthy()
    expect(screen.getByText(/última avaliação bem-sucedida/)).toBeTruthy()
  })

  it('não dispara duas requisições ao mesmo tempo', async () => {
    let resolve: (r: HealthReport) => void = () => {}
    fetchMock.mockReturnValue(new Promise<HealthReport>((r) => (resolve = r)))
    render(<HealthCheckPage />)

    fireEvent.click(screen.getByRole('button', { name: 'Avaliando…' }))
    expect(fetchMock).toHaveBeenCalledTimes(1)

    resolve(makeReport(normalAlerts))
    await waitFor(() => expect(screen.getByRole('button', { name: 'Reavaliar' })).toBeTruthy())
  })

  it('informa a origem dos limites e que o módulo só informa', async () => {
    fetchMock.mockResolvedValue(makeReport(normalAlerts))
    render(<HealthCheckPage />)

    expect(await screen.findByText(/Limites genéricos de teste/)).toBeTruthy()
    expect(screen.getByText(/apenas informa/)).toBeTruthy()
  })
})
