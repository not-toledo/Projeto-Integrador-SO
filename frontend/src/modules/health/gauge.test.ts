import { describe, expect, it } from 'vitest'
import { factsOf, gaugeOf, parseNumber } from './gauge'
import type { HealthAlert, HealthThresholds } from './types'

const T: HealthThresholds = {
  cpuTempWarningC: 80, cpuTempCriticalC: 95, diskFreeWarningPercent: 15, diskFreeCriticalPercent: 5,
  ramUsedWarningPercent: 85,
}

function alert(id: string, observedValue: string | null, threshold: string | null = null): HealthAlert {
  return {
    id, component: 'x', severity: 'INFO', kind: 'NORMAL', title: 't', description: 'd', observedValue, threshold,
    timestamp: '2026-01-01T00:00:00Z',
  }
}

describe('gaugeOf', () => {
  it('lê números em pt-BR', () => {
    expect(parseNumber('18,5')).toBe(18.5)
    expect(parseNumber('1.234,5')).toBe(1234.5)
    expect(parseNumber('abc')).toBeNull()
  })

  it('temperatura da CPU: régua até o limite crítico, com a marca de atenção', () => {
    const g = gaugeOf(alert('temperature:cpu', '57,0 °C'), T)!
    expect(g.fill).toBeCloseTo(60)
    expect(g.headline).toBe('57,0 °C')
    expect(g.marks.map((m) => Math.round(m.at))).toEqual([84, 100])
  })

  it('espaço em disco: régua do espaço usado e destaque do espaço livre', () => {
    const g = gaugeOf(alert('disk-space:C:\\', '82,5 GiB livres de 446,5 GiB (18,5% livre)'), T)!
    expect(g.fill).toBeCloseTo(81.5)
    expect(g.headline).toBe('18,5% livre')
    expect(g.marks.map((m) => m.at)).toEqual([85, 95])
  })

  it('memória: uso e comprometida (limite lido do texto do limite)', () => {
    expect(gaugeOf(alert('memory:usage', '73,6% (11,0 GiB de 14,9 GiB)'), T)!.fill).toBeCloseTo(73.6)
    const commit = gaugeOf(alert('memory:commit', '17,0 GiB de 20,4 GiB (83,3%)', 'Aviso a partir de 90,0% (Windows)'), T)!
    expect(commit.fill).toBeCloseTo(83.3)
    expect(commit.marks[0].at).toBe(90)
  })

  it('sem leitura ou formato inesperado: sem régua (nada é estimado)', () => {
    expect(gaugeOf(alert('temperature:cpu', null), T)).toBeNull()
    expect(gaugeOf(alert('disk-space:C:', 'formato novo'), T)).toBeNull()
    expect(gaugeOf(alert('smart:0', 'Vida útil restante 70,0% · 48,0 °C'), T)).toBeNull()
  })

  it('indicadores separados por " · " viram lista', () => {
    expect(factsOf(alert('smart:0', 'Vida útil restante 70,0% · 48,0 °C'))).toEqual(['Vida útil restante 70,0%', '48,0 °C'])
    expect(factsOf(alert('smart:1', '37,0 °C'))).toEqual([])
  })
})
