// Espelha GET /api/history (veja docs/api/api-historico.md). Em qualquer campo, null significa "sem leitura".

export interface HistoryVolume {
  mountPoint: string | null
  totalBytes: number | null
  freeBytes: number | null
}

export interface HistoryRecord {
  collectedAt: string
  cpuPercent: number | null
  ramUsedPercent: number | null
  commitPercent: number | null
  swapUsedPercent: number | null
  uptimeSeconds: number | null
  volumes: HistoryVolume[]
}

export interface SchedulerStatus {
  state: 'INSTALLED' | 'NOT_INSTALLED' | 'UNKNOWN'
  details: string | null
}

export interface HistoryResponse {
  directory: string
  days: number
  count: number
  lastCollectedAt: string | null
  scheduler: SchedulerStatus
  records: HistoryRecord[]
  problems: string[]
}
