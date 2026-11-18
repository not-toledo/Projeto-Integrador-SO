// Formatação dos valores do Live Monitor. Sem leitura válida, o texto é "Sem leitura";
// nada é estimado ou preenchido por conta própria.

import { isPercentReading, isReading } from './series-helpers'

export { isPercentReading, isReading }

export const NO_READING = 'Sem leitura'

const KIB = 1024
const MIB = KIB * 1024

function decimal(value: number, digits: number): string {
  return value.toLocaleString('pt-BR', { minimumFractionDigits: digits, maximumFractionDigits: digits })
}

export function livePercent(value: number | null | undefined): string {
  return isPercentReading(value) ? `${decimal(value, 1)}%` : NO_READING
}

export function celsius(value: number | null | undefined): string {
  return isReading(value) ? `${decimal(value, 1)} °C` : NO_READING
}

export function rpm(value: number | null | undefined): string {
  return isReading(value) ? `${decimal(value, 0)} RPM` : NO_READING
}

export function liveBytes(value: number | null | undefined): string {
  if (!isReading(value)) {
    return NO_READING
  }
  const gib = MIB * 1024
  return value >= gib ? `${decimal(value / gib, 2)} GiB` : `${decimal(value / MIB, 2)} MiB`
}

/** Taxa de transferência em KiB/s ou MiB/s. */
export function rate(bytesPerSecond: number | null | undefined): string {
  if (!isReading(bytesPerSecond)) {
    return NO_READING
  }
  return bytesPerSecond >= MIB
    ? `${decimal(bytesPerSecond / MIB, 2)} MiB/s`
    : `${decimal(bytesPerSecond / KIB, 1)} KiB/s`
}

export function timeOfDay(isoOrMillis: string | number | null | undefined): string {
  if (isoOrMillis === null || isoOrMillis === undefined) {
    return NO_READING
  }
  const date = new Date(isoOrMillis)
  return Number.isNaN(date.getTime()) ? NO_READING : date.toLocaleTimeString('pt-BR')
}

/** Intervalo de coleta legível: 2000 → "2 s", 500 → "0,5 s". */
export function intervalLabel(ms: number): string {
  return `${decimal(ms / 1000, ms % 1000 === 0 ? 0 : 1)} s`
}
