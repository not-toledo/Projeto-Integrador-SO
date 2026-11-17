import { cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { makeInventory } from '../modules/hardware/fixtures'
import type { Inventory } from '../modules/hardware/types'
import { ApiError, fetchInventory } from '../services/inventoryService'
import { HardwareOverviewPage } from './HardwareOverviewPage'

vi.mock('../services/inventoryService', async (importOriginal) => ({
  ...(await importOriginal<typeof import('../services/inventoryService')>()),
  fetchInventory: vi.fn(),
}))

const fetchMock = vi.mocked(fetchInventory)

beforeEach(() => {
  fetchMock.mockReset()
})

afterEach(() => {
  cleanup()
})

describe('HardwareOverviewPage', () => {
  it('mostra o estado de carregamento enquanto aguarda a API', () => {
    fetchMock.mockReturnValue(new Promise<Inventory>(() => {}))
    render(<HardwareOverviewPage />)

    expect(screen.getByText(/Carregando inventário/)).toBeTruthy()
    const button = screen.getByRole('button', { name: 'Atualizando…' }) as HTMLButtonElement
    expect(button.disabled).toBe(true)
  })

  it('mostra erro quando o backend não responde e permite tentar novamente', async () => {
    fetchMock.mockRejectedValueOnce(new ApiError('Não foi possível conectar ao backend.'))
    render(<HardwareOverviewPage />)

    expect(await screen.findByText('Não foi possível conectar ao backend.')).toBeTruthy()
    expect(screen.queryByText('Sistema operacional')).toBeNull()

    fetchMock.mockResolvedValueOnce(makeInventory())
    fireEvent.click(screen.getByRole('button', { name: 'Atualizar' }))

    expect(await screen.findByText('SO de teste')).toBeTruthy()
    expect(screen.queryByText('Não foi possível conectar ao backend.')).toBeNull()
    expect(fetchMock).toHaveBeenCalledTimes(2)
  })

  it('mostra os seis cartões com os dados e "Não disponível" para campos nulos', async () => {
    fetchMock.mockResolvedValue(makeInventory())
    render(<HardwareOverviewPage />)

    expect(await screen.findByText('SO de teste')).toBeTruthy()
    for (const title of ['Sistema operacional', 'CPU', 'Memória RAM', 'Placa de vídeo', 'Placa-mãe', 'Armazenamento']) {
      expect(screen.getByRole('heading', { name: title })).toBeTruthy()
    }
    expect(screen.getByText('CPU de teste')).toBeTruthy()
    expect(screen.getByText('GPU de teste')).toBeTruthy()
    expect(screen.getByText('8,00 GiB')).toBeTruthy()
    // Campos nulos nunca são preenchidos: aparecem como "Não disponível".
    expect(screen.getAllByText('Não disponível').length).toBeGreaterThan(5)
    expect(screen.getByText(/Origem: Origem de teste/)).toBeTruthy()
    expect(screen.getByText(/Última coleta:/)).toBeTruthy()
  })

  it('placa-mãe diferencia "não informado pelo fabricante" de "não disponível"', async () => {
    fetchMock.mockResolvedValue(
      makeInventory({
        motherboard: {
          status: 'OK',
          message: null,
          data: {
            manufacturer: 'Gigabyte Technology Co., Ltd.',
            model: 'H81M-S1',
            version: null,
            serialNumber: null,
            systemManufacturer: null,
            systemModel: null,
            notReportedFields: ['version', 'serialNumber'],
          },
        },
      }),
    )
    render(<HardwareOverviewPage />)

    expect(await screen.findByText('H81M-S1')).toBeTruthy()
    const values = within(screen.getByRole('region', { name: 'Placa-mãe' }))
    expect(values.getAllByText('Não informado pelo fabricante')).toHaveLength(2)
    // Campos sem texto do fabricante continuam como "Não disponível".
    expect(values.getAllByText('Não disponível')).toHaveLength(2)
  })

  it('falha ou indisponibilidade de um componente não afeta os demais', async () => {
    fetchMock.mockResolvedValue(
      makeInventory({
        cpu: { status: 'ERROR', message: 'Falha de teste na CPU', data: null },
        gpu: { status: 'UNAVAILABLE', message: 'Sem GPU identificada', data: null },
      }),
    )
    render(<HardwareOverviewPage />)

    expect(await screen.findByText('Falha de teste na CPU')).toBeTruthy()
    expect(screen.getByText('Falha na coleta')).toBeTruthy()
    expect(screen.getByText('Sem GPU identificada')).toBeTruthy()
    // Os demais continuam exibindo dados.
    expect(screen.getByText('SO de teste')).toBeTruthy()
    expect(screen.getByText('8,00 GiB')).toBeTruthy()
    expect(screen.getByText('Placa de teste')).toBeTruthy()
  })

  it('mantém a última coleta e avisa quando a atualização falha', async () => {
    fetchMock.mockResolvedValueOnce(makeInventory())
    render(<HardwareOverviewPage />)
    expect(await screen.findByText('SO de teste')).toBeTruthy()

    fetchMock.mockRejectedValueOnce(new ApiError('Backend fora do ar.'))
    fireEvent.click(screen.getByRole('button', { name: 'Atualizar' }))

    expect(await screen.findByText('Backend fora do ar.')).toBeTruthy()
    expect(screen.getByText(/Exibindo abaixo a última coleta bem-sucedida/)).toBeTruthy()
    expect(screen.getByText('SO de teste')).toBeTruthy()
  })

  it('não inicia nova coleta enquanto a anterior estiver em andamento', async () => {
    let resolve: (value: Inventory) => void = () => {}
    fetchMock.mockReturnValueOnce(new Promise<Inventory>((r) => (resolve = r)))
    render(<HardwareOverviewPage />)

    const button = screen.getByRole('button', { name: 'Atualizando…' })
    fireEvent.click(button)
    fireEvent.click(button)
    expect(fetchMock).toHaveBeenCalledTimes(1)

    resolve(makeInventory())
    await waitFor(() => expect(screen.getByRole('button', { name: 'Atualizar' })).toBeTruthy())
    expect(fetchMock).toHaveBeenCalledTimes(1)
  })
})
