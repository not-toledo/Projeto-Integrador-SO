// Tipos que espelham as respostas de GET /api/live e GET /api/live/history (veja docs/api-live.md).
// Em qualquer campo de `data`, null significa "sem leitura".
import type { ComponentResult } from '../hardware/types'

export interface CpuLive {
  totalPercent: number | null
  perCorePercent: (number | null)[] | null
}

export interface MemoryLive {
  totalBytes: number | null
  availableBytes: number | null
  usedBytes: number | null
  usedPercent: number | null
}

export interface GpuLive {
  utilizationPercent: number | null
  vramUsedBytes: number | null
  vramTotalBytes?: number | null
  temperatureCelsius?: number | null
  name?: string | null
}

export interface DiskActivity {
  name: string | null
  readBytesPerSecond: number | null
  writeBytesPerSecond: number | null
  busyPercent: number | null
}

export interface LiveVolume {
  mountPoint: string | null
  label: string | null
  fileSystem: string | null
  totalBytes: number | null
  freeBytes: number | null
  usedBytes: number | null
  usedPercent: number | null
}

export interface DiskLive {
  drives: DiskActivity[] | null
  volumes: LiveVolume[] | null
}

export interface SensorLive {
  cpuTemperatureCelsius: number | null
  fanSpeedsRpm: number[] | null
  gpuTemperatureCelsius?: number | null
}

export interface LiveSample {
  collectedAt: string
  durationMs: number
  cpu: ComponentResult<CpuLive>
  memory: ComponentResult<MemoryLive>
  gpu: ComponentResult<GpuLive>
  disks: ComponentResult<DiskLive>
  sensors: ComponentResult<SensorLive>
}

export interface MonitorStatus {
  running: boolean
  collecting: boolean
  stalled: boolean
  intervalMs: number
  historyCapacity: number
  sampleCount: number
  lastSuccessAt: string | null
  consecutiveFailures: number
  lastError: string | null
}

export interface LiveResponse {
  source: string
  status: MonitorStatus
  latest: LiveSample | null
}

export interface LiveHistoryResponse {
  source: string
  status: MonitorStatus
  samples: LiveSample[]
}
