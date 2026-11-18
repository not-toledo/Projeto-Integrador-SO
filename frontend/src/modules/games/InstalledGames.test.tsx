import { cleanup, fireEvent, render, screen, within } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { fetchInstalledGames } from '../../services/gameService'
import { InstalledGames } from './InstalledGames'
import type { InstalledGames as InstalledGamesData } from './types'

vi.mock('../../services/gameService', async (importOriginal) => ({
  ...(await importOriginal<typeof import('../../services/gameService')>()),
  fetchInstalledGames: vi.fn(),
}))

const fetchMock = vi.mocked(fetchInstalledGames)

const DATA: InstalledGamesData = {
  steamFound: true,
  steamPath: 'C:\\Program Files (x86)\\Steam',
  libraries: [
    { path: 'C:\\Program Files (x86)\\Steam', mountPoint: 'C:\\', freeBytes: 100 * 1024 ** 3, totalBytes: 446 * 1024 ** 3, mediaType: 'SSD', available: true, gameCount: 1 },
    { path: 'F:\\Externo', mountPoint: null, freeBytes: null, totalBytes: null, mediaType: null, available: false, gameCount: 0 },
  ],
  games: [
    { appId: 900001, name: 'Aventura de Teste', installDir: 'Aventura', libraryPath: 'D:\\SteamLibrary', mountPoint: 'D:\\', mediaType: 'HDD', sizeOnDiskBytes: 45 * 1024 ** 3, state: 'UPDATE_REQUIRED', lastUpdated: null, storeUrl: 'https://store.steampowered.com/app/900001/' },
    { appId: 228980, name: 'Steamworks Common Redistributables', installDir: 'Steamworks Shared', libraryPath: 'C:\\Program Files (x86)\\Steam', mountPoint: 'C:\\', mediaType: 'SSD', sizeOnDiskBytes: null, state: 'INSTALLED', lastUpdated: null, storeUrl: 'https://store.steampowered.com/app/228980/' },
  ],
  problems: ['appmanifest_9.acf: VDF inválido'],
  scannedAt: '2026-11-18T12:00:00Z',
}

function open() {
  fireEvent.click(screen.getByText('Jogos instalados neste PC (Steam)'))
  const details = screen.getByText('Jogos instalados neste PC (Steam)').closest('details') as HTMLDetailsElement
  details.open = true
  fireEvent(details, new Event('toggle'))
}

beforeEach(() => fetchMock.mockReset())
afterEach(() => cleanup())

describe('InstalledGames', () => {
  it('só lê a Steam quando o bloco é aberto e lista jogos, disco e estado', async () => {
    fetchMock.mockResolvedValue(DATA)
    const onSelect = vi.fn()
    render(<InstalledGames onSelect={onSelect} />)
    expect(fetchMock).not.toHaveBeenCalled()

    open()
    const row = (await screen.findByText('Aventura de Teste')).closest('tr') as HTMLTableRowElement
    expect(fetchMock).toHaveBeenCalledTimes(1)
    expect(within(row).getByText('D:\\ · HDD')).toBeTruthy()
    expect(within(row).getByText('45,00 GiB')).toBeTruthy()
    expect(within(row).getByText('Atualização pendente')).toBeTruthy()
    // Tamanho não informado não vira 0.
    const tools = screen.getByText('Steamworks Common Redistributables').closest('tr') as HTMLTableRowElement
    expect(within(tools).getByText('Não disponível')).toBeTruthy()
    expect(screen.getByText(/F:\\Externo — pasta não encontrada/)).toBeTruthy()
    expect(screen.getByText('1 arquivo(s) não lido(s)')).toBeTruthy()

    fireEvent.click(within(row).getByRole('button', { name: 'Verificar compatibilidade' }))
    expect(onSelect).toHaveBeenCalledWith({ appId: 900001, name: 'Aventura de Teste', headerImage: null, storeUrl: 'https://store.steampowered.com/app/900001/' })
  })

  it('filtra pelo nome', async () => {
    fetchMock.mockResolvedValue(DATA)
    render(<InstalledGames onSelect={() => {}} />)
    open()
    await screen.findByText('Aventura de Teste')
    fireEvent.change(screen.getByLabelText('Filtrar'), { target: { value: 'steamworks' } })
    expect(screen.queryByText('Aventura de Teste')).toBeNull()
    expect(screen.getByText('Steamworks Common Redistributables')).toBeTruthy()
  })

  it('sem Steam mostra como configurar; erro da API aparece', async () => {
    fetchMock.mockResolvedValueOnce({ ...DATA, steamFound: false, steamPath: null, libraries: [], games: [],
      problems: ['Pasta da Steam não encontrada. Defina PCCHECK_STEAM_DIR.'] })
    render(<InstalledGames onSelect={() => {}} />)
    open()
    expect(await screen.findByText(/PCCHECK_STEAM_DIR/)).toBeTruthy()
    cleanup()

    fetchMock.mockRejectedValueOnce(new Error('Não foi possível conectar ao backend local.'))
    render(<InstalledGames onSelect={() => {}} />)
    open()
    expect(await screen.findByRole('alert')).toBeTruthy()
  })
})
