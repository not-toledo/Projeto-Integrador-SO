import type { HealthAlert, HealthThresholds } from './types'

/** Marca de limite na régua (posição de 0 a 100). */
export interface GaugeMark {
  at: number
  label: string
}

/**
 * Régua de um alerta: quanto do limite já foi usado. Calculada só a partir do valor observado e dos limites
 * enviados pela API; se o texto não tiver o número esperado, não há régua (nada é estimado).
 */
export interface Gauge {
  /** Preenchimento, de 0 a 100. */
  fill: number
  marks: GaugeMark[]
  /** Valor curto em destaque (ex.: "18,5% livre"). */
  headline: string
  /** O que a régua mede (ex.: "Espaço usado"). */
  measure: string
}

/** "18,5" → 18.5. Os valores da API vêm em pt-BR. */
export function parseNumber(text: string): number | null {
  const n = Number(text.replace(/\./g, '').replace(',', '.'))
  return Number.isFinite(n) ? n : null
}

const clamp = (v: number) => Math.min(Math.max(v, 0), 100)
const fmt = (v: number) => v.toLocaleString('pt-BR', { maximumFractionDigits: 1 })

export function gaugeOf(alert: HealthAlert, t: HealthThresholds): Gauge | null {
  const observed = alert.observedValue
  if (!observed) return null

  if (alert.id === 'temperature:cpu') {
    const m = observed.match(/^([\d.,]+)\s*°C$/)
    const v = m && parseNumber(m[1])
    if (v === null || !m || t.cpuTempCriticalC <= 0) return null
    const scale = t.cpuTempCriticalC
    return {
      fill: clamp((v / scale) * 100),
      marks: [
        { at: clamp((t.cpuTempWarningC / scale) * 100), label: `Atenção ${fmt(t.cpuTempWarningC)} °C` },
        { at: 100, label: `Crítico ${fmt(t.cpuTempCriticalC)} °C` },
      ],
      headline: observed,
      measure: `Temperatura até o limite crítico (${fmt(scale)} °C)`,
    }
  }

  if (alert.id.startsWith('disk-space:')) {
    const m = observed.match(/\(([\d.,]+)% livre\)/)
    const free = m && parseNumber(m[1])
    if (free === null || !m) return null
    return {
      fill: clamp(100 - free),
      marks: [
        { at: clamp(100 - t.diskFreeWarningPercent), label: `Atenção com ${fmt(t.diskFreeWarningPercent)}% livre` },
        { at: clamp(100 - t.diskFreeCriticalPercent), label: `Crítico com ${fmt(t.diskFreeCriticalPercent)}% livre` },
      ],
      headline: `${m[1]}% livre`,
      measure: 'Espaço usado',
    }
  }

  if (alert.id === 'memory:usage') {
    const m = observed.match(/^([\d.,]+)%/)
    const used = m && parseNumber(m[1])
    if (used === null || !m) return null
    return {
      fill: clamp(used),
      marks: [{ at: clamp(t.ramUsedWarningPercent), label: `Aviso ${fmt(t.ramUsedWarningPercent)}%` }],
      headline: `${m[1]}% em uso`,
      measure: 'Memória em uso',
    }
  }

  if (alert.id === 'memory:commit') {
    const m = observed.match(/\(([\d.,]+)%\)/)
    const used = m && parseNumber(m[1])
    const limit = alert.threshold?.match(/([\d.,]+)%/)
    const at = limit ? parseNumber(limit[1]) : null
    if (used === null || !m) return null
    return {
      fill: clamp(used),
      marks: at === null ? [] : [{ at: clamp(at), label: `Aviso ${fmt(at)}%` }],
      headline: `${m[1]}% do limite`,
      measure: 'Memória comprometida',
    }
  }

  return null
}

/** Indicadores separados por " · " (ex.: saúde SMART) viram uma lista curta. */
export function factsOf(alert: HealthAlert): string[] {
  const parts = alert.observedValue?.split(' · ') ?? []
  return parts.length > 1 ? parts : []
}
