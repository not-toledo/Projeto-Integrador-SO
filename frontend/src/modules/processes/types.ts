// Tipos que espelham GET /api/processes (veja docs/api/api-processos.md).

export interface ProcessInfo {
  pid: number
  parentPid: number
  name: string | null
  /** null: o SO não informou (comum sem permissão de administrador). */
  user: string | null
  state: string | null
  priority: number
  threads: number
  /** % da CPU inteira no intervalo de medição; null para processos que surgiram durante a medição. */
  cpuPercent: number | null
  memoryBytes: number
  virtualBytes: number
  startTime: string | null
  self: boolean
}

export interface ProcessesResponse {
  collectedAt: string
  sampleMillis: number
  logicalProcessors: number
  totalMemoryBytes: number | null
  processCount: number | null
  threadCount: number | null
  processes: ProcessInfo[]
  error: string | null
  osFamily: 'windows' | 'linux'
}
