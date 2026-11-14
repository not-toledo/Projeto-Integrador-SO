// Lógica pura do Live Monitor: histórico limitado, extração das séries e geometria dos gráficos.
// Valores sem leitura viram null e produzem LACUNAS no gráfico (nunca zero nem interpolação).
import { MIB_PER_BYTE, isPercentReading, isReading } from './series-helpers'
import type { LiveSample, MonitorStatus } from './types'

/** Quantidade máxima de amostras mantidas no navegador (igual ao padrão do backend). */
export const MAX_POINTS = 300

/**
 * Acrescenta uma amostra ao histórico, mantendo no máximo `max` pontos.
 * - mesma amostra (mesmo horário): ignorada, sem duplicar;
 * - amostra mais antiga que a última (o backend foi reiniciado e o histórico recomeçou): descarta o histórico antigo.
 */
export function appendSample(samples: LiveSample[], sample: LiveSample, max: number = MAX_POINTS): LiveSample[] {
  const time = Date.parse(sample.collectedAt)
  if (Number.isNaN(time)) {
    return samples
  }
  const last = samples.length > 0 ? Date.parse(samples[samples.length - 1].collectedAt) : null
  if (last !== null && time === last) {
    return samples
  }
  const base = last !== null && time < last ? [] : samples
  const next = [...base, sample]
  return next.length > max ? next.slice(next.length - max) : next
}

/** Limita um histórico recebido da API (da mais antiga para a mais recente). */
export function limitSamples(samples: LiveSample[], max: number = MAX_POINTS): LiveSample[] {
  return samples.length > max ? samples.slice(samples.length - max) : samples
}

export function sampleTimes(samples: LiveSample[]): number[] {
  return samples.map((s) => Date.parse(s.collectedAt))
}

// --- Extratores: uma leitura por amostra; null = sem leitura. ---

export function cpuTotal(sample: LiveSample): number | null {
  const v = sample.cpu.status === 'OK' ? sample.cpu.data?.totalPercent : null
  return isPercentReading(v) ? v : null
}

export function memoryPercent(sample: LiveSample): number | null {
  const v = sample.memory.status === 'OK' ? sample.memory.data?.usedPercent : null
  return isPercentReading(v) ? v : null
}

export function cpuTemperature(sample: LiveSample): number | null {
  const v = sample.sensors.status === 'OK' ? sample.sensors.data?.cpuTemperatureCelsius : null
  return isReading(v) ? v : null
}

function driveOf(sample: LiveSample, name: string) {
  if (sample.disks.status !== 'OK') {
    return null
  }
  return sample.disks.data?.drives?.find((d) => d.name === name) ?? null
}

/** Leitura em MiB/s da unidade `name`; null se a unidade ou a taxa não estiverem disponíveis na amostra. */
export function driveRateMiB(sample: LiveSample, name: string, kind: 'read' | 'write'): number | null {
  const drive = driveOf(sample, name)
  const v = kind === 'read' ? drive?.readBytesPerSecond : drive?.writeBytesPerSecond
  return isReading(v) ? v * MIB_PER_BYTE : null
}

export function driveBusy(sample: LiveSample, name: string): number | null {
  const v = driveOf(sample, name)?.busyPercent
  return isPercentReading(v) ? v : null
}

/** True se há pelo menos uma leitura válida. */
export function hasReadings(values: (number | null)[]): boolean {
  return values.some(isReading)
}

/** Nomes das unidades físicas da amostra mais recente que informou unidades (ordem preservada). */
export function driveNames(samples: LiveSample[]): string[] {
  for (let i = samples.length - 1; i >= 0; i--) {
    const s = samples[i]
    const drives = s.disks.status === 'OK' ? s.disks.data?.drives : null
    if (drives && drives.length > 0) {
      return drives.map((d) => d.name).filter((n): n is string => typeof n === 'string' && n !== '')
    }
  }
  return []
}

// --- Geometria do gráfico ---

/** Maior valor "redondo" (1, 2, 5 × 10^n) que comporta `max`; usado para a escala automática. */
export function niceMax(max: number): number {
  if (!Number.isFinite(max) || max <= 0) {
    return 1
  }
  const exp = Math.floor(Math.log10(max))
  const base = Math.pow(10, exp)
  const fraction = max / base
  const step = fraction <= 1 ? 1 : fraction <= 2 ? 2 : fraction <= 5 ? 5 : 10
  return step * base
}

export interface Point {
  x: number
  y: number
}

/**
 * Converte uma série em segmentos contínuos de pontos. Um valor sem leitura (null/inválido) ou um
 * intervalo de tempo maior que `maxGapMs` entre dois pontos (por exemplo, o backend ficou fora do ar)
 * ENCERRA o segmento atual: o gráfico mostra uma lacuna, sem ligar pontos separados por falta de leitura.
 */
export function buildSegments(
  times: number[],
  values: (number | null)[],
  toX: (time: number) => number,
  toY: (value: number) => number,
  maxGapMs: number = Number.POSITIVE_INFINITY,
): Point[][] {
  const segments: Point[][] = []
  let current: Point[] = []
  let previousTime = Number.NaN
  for (let i = 0; i < values.length; i++) {
    const v = values[i]
    const t = times[i]
    if (isReading(v) && Number.isFinite(t)) {
      if (current.length > 0 && t - previousTime > maxGapMs) {
        segments.push(current)
        current = []
      }
      current.push({ x: toX(t), y: toY(v) })
      previousTime = t
    } else if (current.length > 0) {
      segments.push(current)
      current = []
    }
  }
  if (current.length > 0) {
    segments.push(current)
  }
  return segments
}

/** Converte um segmento em atributo `d` de um <path>. Segmento de um só ponto vira um ponto visível. */
export function segmentPath(segment: Point[]): string {
  if (segment.length === 0) {
    return ''
  }
  const f = (n: number) => n.toFixed(2)
  if (segment.length === 1) {
    const p = segment[0]
    return `M${f(p.x)} ${f(p.y)}h0.01`
  }
  return segment.map((p, i) => `${i === 0 ? 'M' : 'L'}${f(p.x)} ${f(p.y)}`).join(' ')
}

// --- Estado da conexão ---

export type ConnectionState = 'connecting' | 'waiting' | 'connected' | 'stale' | 'degraded' | 'disconnected'

export interface ConnectionInput {
  /** Houve ao menos uma resposta do backend. */
  everConnected: boolean
  /** Requisições seguidas que falharam (backend inacessível ou resposta inválida). */
  clientFailures: number
  status: MonitorStatus | null
  latest: LiveSample | null
  /** Horário atual em milissegundos (injetado para facilitar testes). */
  now: number
}

/**
 * Estado da coleta/conexão exibido ao usuário:
 * disconnected — o navegador não consegue falar com o backend;
 * degraded — o backend responde, mas a coleta falha ou travou;
 * stale — o backend responde, mas não chegam amostras novas;
 * waiting — o backend responde, mas ainda não terminou a primeira coleta.
 */
export function connectionState({ everConnected, clientFailures, status, latest, now }: ConnectionInput): ConnectionState {
  if (clientFailures > 0) {
    return 'disconnected'
  }
  if (!everConnected || status === null) {
    return 'connecting'
  }
  if (status.stalled || status.consecutiveFailures > 0) {
    return 'degraded'
  }
  if (latest === null) {
    return 'waiting'
  }
  const age = now - Date.parse(latest.collectedAt)
  const limit = Math.max(status.intervalMs * 5, 10_000)
  if (Number.isFinite(age) && age > limit) {
    return 'stale'
  }
  return 'connected'
}

export const CONNECTION_LABEL: Record<ConnectionState, string> = {
  connecting: 'Conectando…',
  waiting: 'Aguardando a primeira coleta',
  connected: 'Conectado',
  stale: 'Sem amostras novas',
  degraded: 'Coleta com problemas',
  disconnected: 'Sem conexão com o backend',
}
