import type { HistoryRecord } from './types'

/** Percentual válido (0 a 100) ou null. A memória comprometida pode passar de 100 no Linux. */
function percent(value: number | null | undefined, max = 100): number | null {
  return typeof value === 'number' && Number.isFinite(value) && value >= 0 && value <= max ? value : null
}

export function historyTimes(records: HistoryRecord[]): number[] {
  return records.map((r) => Date.parse(r.collectedAt))
}

export const cpuSeries = (records: HistoryRecord[]) => records.map((r) => percent(r.cpuPercent))
export const ramSeries = (records: HistoryRecord[]) => records.map((r) => percent(r.ramUsedPercent))
export const swapSeries = (records: HistoryRecord[]) => records.map((r) => percent(r.swapUsedPercent))
export const commitSeries = (records: HistoryRecord[]) => records.map((r) => percent(r.commitPercent, 1000))

/** Pontos de montagem presentes no período, na ordem em que aparecem. */
export function volumeMounts(records: HistoryRecord[]): string[] {
  const mounts: string[] = []
  for (const r of records) {
    for (const v of r.volumes) {
      if (v.mountPoint && !mounts.includes(v.mountPoint)) mounts.push(v.mountPoint)
    }
  }
  return mounts
}

/** Espaço livre (%) de um volume em cada coleta; null quando o volume não aparece ou não tem leitura. */
export function freePercentSeries(records: HistoryRecord[], mount: string): (number | null)[] {
  return records.map((r) => {
    const v = r.volumes.find((x) => x.mountPoint === mount)
    if (!v || v.totalBytes == null || v.freeBytes == null || v.totalBytes <= 0) return null
    return percent((v.freeBytes / v.totalBytes) * 100)
  })
}

/**
 * Lacuna máxima entre coletas para ligar os pontos do gráfico: 3 vezes o intervalo mais comum (mediana).
 * Assim, horas com o computador desligado aparecem como lacuna, e não como uma reta inventada.
 */
export function historyMaxGapMs(times: number[]): number {
  const deltas: number[] = []
  for (let i = 1; i < times.length; i++) {
    const d = times[i] - times[i - 1]
    if (d > 0) deltas.push(d)
  }
  if (deltas.length === 0) return 60 * 60 * 1000
  deltas.sort((a, b) => a - b)
  return deltas[Math.floor(deltas.length / 2)] * 3
}
