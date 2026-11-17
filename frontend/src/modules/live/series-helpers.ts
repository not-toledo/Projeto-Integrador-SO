// Validação de leituras, separada para ser reutilizada por format.ts e series.ts sem dependência circular.
export const MIB_PER_BYTE = 1 / (1024 * 1024)

export function isReading(value: number | null | undefined): value is number {
  return typeof value === 'number' && Number.isFinite(value) && value >= 0
}

export function isPercentReading(value: number | null | undefined): value is number {
  return isReading(value) && value <= 100
}
