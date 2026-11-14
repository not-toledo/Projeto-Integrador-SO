// Formatação de valores para exibição. Valores ausentes ou inválidos viram "Não disponível";
// nada é estimado ou preenchido por conta própria.

export const NOT_AVAILABLE = 'Não disponível'

const KIB = 1024
const MIB = KIB * 1024
const GIB = MIB * 1024
const TIB = GIB * 1024

function isValidNumber(value: number | null | undefined): value is number {
  return typeof value === 'number' && Number.isFinite(value) && value >= 0
}

function decimal(value: number, digits: number): string {
  return value.toLocaleString('pt-BR', { minimumFractionDigits: digits, maximumFractionDigits: digits })
}

export function text(value: string | null | undefined): string {
  if (value === null || value === undefined || value.trim() === '') {
    return NOT_AVAILABLE
  }
  return value
}

export function count(value: number | null | undefined): string {
  return isValidNumber(value) ? value.toLocaleString('pt-BR') : NOT_AVAILABLE
}

export function bytes(value: number | null | undefined): string {
  if (!isValidNumber(value)) {
    return NOT_AVAILABLE
  }
  if (value >= TIB) {
    return `${decimal(value / TIB, 2)} TiB`
  }
  if (value >= GIB) {
    return `${decimal(value / GIB, 2)} GiB`
  }
  return `${decimal(value / MIB, 2)} MiB`
}

export function frequency(hz: number | null | undefined): string {
  if (!isValidNumber(hz) || hz === 0) {
    return NOT_AVAILABLE
  }
  return hz >= 1_000_000_000 ? `${decimal(hz / 1_000_000_000, 2)} GHz` : `${decimal(hz / 1_000_000, 0)} MHz`
}

export function percent(value: number | null | undefined): string {
  return isValidNumber(value) && value <= 100 ? `${decimal(value, 1)}%` : NOT_AVAILABLE
}

export function duration(seconds: number | null | undefined): string {
  if (!isValidNumber(seconds)) {
    return NOT_AVAILABLE
  }
  const total = Math.floor(seconds)
  const days = Math.floor(total / 86400)
  const hours = Math.floor((total % 86400) / 3600)
  const minutes = Math.floor((total % 3600) / 60)
  return `${days}d ${hours}h ${minutes}min`
}

export function dateTime(iso: string | null | undefined): string {
  if (!iso) {
    return NOT_AVAILABLE
  }
  const date = new Date(iso)
  return Number.isNaN(date.getTime()) ? NOT_AVAILABLE : date.toLocaleString('pt-BR')
}
