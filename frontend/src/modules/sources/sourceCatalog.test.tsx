import { cleanup, render, screen, within } from '@testing-library/react'
import { afterEach, describe, expect, it } from 'vitest'
import { CpuCard, GpuCard, MemoryCard, MotherboardCard, OsCard, StorageCard } from '../hardware/cards'
import { makeInventory } from '../hardware/fixtures'
import { CpuPanel, DisksPanel, GpuPanel, MemoryPanel, SensorsPanel } from '../live/panels'
import { GPU_OK, makeSample } from '../live/fixtures'
import { DataSources } from './DataSources'
import { DATA_SOURCES, platformOf, type SourceSection } from './sourceCatalog'

afterEach(() => {
  cleanup()
})

/** Rótulos dos campos exibidos (<dt>) dentro do card/painel com o título dado. */
function fieldLabels(title: string): string[] {
  const region = screen.getByRole('region', { name: title })
  return Array.from(region.querySelectorAll('dl.fields dt')).map((dt) => dt.textContent ?? '')
}

function expectCovered(title: string, section: SourceSection) {
  const known = DATA_SOURCES[section].map((s) => s.field)
  const labels = fieldLabels(title)
  expect(labels.length).toBeGreaterThan(0)
  for (const label of labels) {
    expect(known, `"${label}" (${title}) sem fonte em ${section}`).toContain(label)
  }
}

describe('catálogo de fontes', () => {
  it('toda entrada tem fonte no Windows e no Linux, sem campos repetidos', () => {
    for (const [section, rows] of Object.entries(DATA_SOURCES)) {
      const fields = rows.map((r) => r.field)
      expect(new Set(fields).size, section).toBe(fields.length)
      for (const row of rows) {
        expect(row.windows.trim(), `${section}/${row.field}`).not.toBe('')
        expect(row.linux.trim(), `${section}/${row.field}`).not.toBe('')
      }
    }
  })

  it('todo campo dos cards da visão geral tem fonte registrada', () => {
    const inv = makeInventory({
      memory: { status: 'OK', message: null, data: {
        totalBytes: 1, availableBytes: 1, usedBytes: 0, usedPercent: 0,
        virtualMemory: { swapTotalBytes: 1, swapUsedBytes: 0, swapUsedPercent: 0,
          commitLimitBytes: 2, commitUsedBytes: 1, commitPercent: 50 } } },
    })
    render(
      <>
        <OsCard result={inv.os} />
        <CpuCard result={inv.cpu} />
        <MemoryCard result={inv.memory} />
        <GpuCard result={inv.gpu} />
        <MotherboardCard result={inv.motherboard} />
        <StorageCard result={inv.storage} />
      </>,
    )
    expectCovered('Sistema operacional', 'os')
    expectCovered('CPU', 'cpu')
    expectCovered('Memória RAM', 'memory')
    expectCovered('Placa de vídeo', 'gpu')
    expectCovered('Placa-mãe', 'motherboard')
    expectCovered('Armazenamento', 'storage')
  })

  it('todo campo dos painéis do Live Monitor tem fonte registrada', () => {
    const samples = [makeSample({ gpu: GPU_OK }), makeSample({ at: '2026-11-14T12:00:02Z', gpu: GPU_OK })]
    render(
      <>
        <MemoryPanel samples={samples} maxGapMs={5000} />
        <GpuPanel samples={samples} maxGapMs={5000} />
        <SensorsPanel samples={samples} maxGapMs={5000} />
      </>,
    )
    expectCovered('Memória RAM', 'live-memory')
    expectCovered('GPU', 'live-gpu')
    expectCovered('Sensores', 'live-sensors')
  })

  it('painéis sem lista de campos também mostram as fontes', () => {
    const samples = [makeSample()]
    render(
      <>
        <CpuPanel samples={samples} maxGapMs={5000} />
        <DisksPanel samples={samples} maxGapMs={5000} />
      </>,
    )
    expect(within(screen.getByRole('region', { name: 'CPU' })).getByText('Uso total e por núcleo')).toBeTruthy()
    expect(within(screen.getByRole('region', { name: 'Discos' })).getByText('Leitura, escrita e tempo ocupado')).toBeTruthy()
  })

  it('identifica a plataforma pelo nome do SO', () => {
    expect(platformOf('Windows')).toBe('windows')
    expect(platformOf('Ubuntu Linux')).toBe('linux')
    expect(platformOf('macOS')).toBeNull()
    expect(platformOf(null)).toBeNull()
  })
})

describe('DataSources', () => {
  it('destaca a coluna do SO deste computador', () => {
    render(<DataSources section="motherboard" platform="linux" />)
    expect(screen.getByText(/Este computador usa Linux/)).toBeTruthy()
    const cell = screen.getByText('/sys/devices/virtual/dmi/id/board_name')
    expect(cell.className).toBe('data-sources-current')
    expect(screen.getByText(/WMI Win32_BaseBoard: Product/).className).toBe('')
  })

  it('sem SO conhecido mostra as duas colunas sem destaque', () => {
    render(<DataSources section="os" />)
    expect(screen.queryByText(/Este computador usa/)).toBeNull()
    expect(screen.getByRole('columnheader', { name: 'Windows' })).toBeTruthy()
    expect(screen.getByRole('columnheader', { name: 'Linux' })).toBeTruthy()
  })

  it('aparece mesmo quando a coleta do componente falhou', () => {
    render(<GpuCard result={{ status: 'ERROR', message: 'Falha de teste', data: null }} platform="windows" />)
    const card = screen.getByRole('region', { name: 'Placa de vídeo' })
    expect(within(card).getByText('Falha de teste')).toBeTruthy()
    expect(within(card).getByText('De onde vêm estes dados?')).toBeTruthy()
  })
})
