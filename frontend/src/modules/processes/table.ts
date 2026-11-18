import type { ProcessInfo, ProcessesResponse } from './types'

/** Uma linha da tabela: um processo ou um grupo de processos com o mesmo nome. */
export interface ProcessRow {
  key: string
  name: string
  pids: number[]
  /** null: nenhum processo da linha tinha medição de CPU no intervalo. */
  cpuPercent: number | null
  memoryBytes: number
  threads: number
  /** Só para linha de um processo; num grupo as prioridades podem diferir. */
  priority: number | null
  user: string | null
  state: string | null
  startTime: string | null
  self: boolean
}

export type SortKey = 'name' | 'pid' | 'cpu' | 'memory' | 'threads' | 'priority' | 'start'

const UNNAMED = '(sem nome)'

function single(p: ProcessInfo): ProcessRow {
  return {
    key: String(p.pid), name: p.name ?? UNNAMED, pids: [p.pid], cpuPercent: p.cpuPercent, memoryBytes: p.memoryBytes,
    threads: p.threads, priority: p.priority, user: p.user, state: p.state, startTime: p.startTime, self: p.self,
  }
}

/** Uma linha por processo. */
export function rowsOf(processes: ProcessInfo[]): ProcessRow[] {
  return processes.map(single)
}

/**
 * Agrupa por nome (sem diferenciar maiúsculas no Windows), como o Gerenciador de Tarefas: navegadores e editores
 * abrem vários processos. Soma CPU, memória e threads; o início é o do processo mais antigo.
 */
export function groupByName(processes: ProcessInfo[], windows: boolean): ProcessRow[] {
  const groups = new Map<string, ProcessRow>()
  for (const p of processes) {
    const name = p.name ?? UNNAMED
    const key = windows ? name.toLowerCase() : name
    const g = groups.get(key)
    if (!g) {
      groups.set(key, { ...single(p), key: `name:${key}` })
      continue
    }
    g.pids.push(p.pid)
    g.cpuPercent = g.cpuPercent === null && p.cpuPercent === null ? null : (g.cpuPercent ?? 0) + (p.cpuPercent ?? 0)
    g.memoryBytes += p.memoryBytes
    g.threads += p.threads
    if (g.priority !== p.priority) g.priority = null
    if (g.user !== p.user) g.user = null
    if (g.state !== p.state) g.state = null
    if (p.startTime && (!g.startTime || p.startTime < g.startTime)) g.startTime = p.startTime
    g.self ||= p.self
  }
  return [...groups.values()]
}

export function filterRows(rows: ProcessRow[], query: string): ProcessRow[] {
  const q = query.trim().toLowerCase()
  if (!q) return rows
  return rows.filter((r) => r.name.toLowerCase().includes(q) || r.pids.some((pid) => String(pid) === q))
}

const VALUE: Record<SortKey, (r: ProcessRow) => number | string> = {
  name: (r) => r.name.toLowerCase(),
  pid: (r) => r.pids[0],
  cpu: (r) => r.cpuPercent ?? -1,
  memory: (r) => r.memoryBytes,
  threads: (r) => r.threads,
  priority: (r) => r.priority ?? -1,
  start: (r) => r.startTime ?? '',
}

export function sortRows(rows: ProcessRow[], key: SortKey, descending: boolean): ProcessRow[] {
  const get = VALUE[key]
  return [...rows].sort((a, b) => {
    const va = get(a)
    const vb = get(b)
    const cmp = va < vb ? -1 : va > vb ? 1 : 0
    return descending ? -cmp : cmp
  })
}

/** Classes de prioridade do Windows (prioridade base). */
const WINDOWS_PRIORITY: Record<number, string> = {
  4: 'Baixa', 6: 'Abaixo do normal', 8: 'Normal', 10: 'Acima do normal', 13: 'Alta', 24: 'Tempo real',
}

/**
 * Nome da prioridade. Windows: classe pela prioridade base. Linux: o OSHI informa a prioridade do kernel
 * (20 + nice para processos comuns), então mostra o nice. Fora dessas escalas, só o número.
 */
export function priorityLabel(priority: number | null, osFamily: ProcessesResponse['osFamily']): string {
  if (priority === null) return 'Várias'
  if (osFamily === 'windows') {
    const name = WINDOWS_PRIORITY[priority]
    return name ? `${name} (${priority})` : String(priority)
  }
  if (priority >= 0 && priority <= 39) {
    const nice = priority - 20
    return `nice ${nice > 0 ? '+' : ''}${nice}`
  }
  return String(priority)
}

const STATE: Record<string, string> = {
  RUNNING: 'Em execução', SLEEPING: 'Dormindo', WAITING: 'Aguardando', ZOMBIE: 'Zumbi', STOPPED: 'Parado',
  SUSPENDED: 'Suspenso', NEW: 'Iniciando', INVALID: 'Encerrado', OTHER: 'Outro',
}

export function stateLabel(state: string | null): string {
  if (state === null) return 'Vários'
  return STATE[state] ?? state
}

/** Tempo em execução em linguagem curta: "menos de 1 min", "24 min", "2 h 24 min", "6 d 3 h". */
export function elapsedLabel(seconds: number): string {
  const minutes = Math.floor(Math.max(0, seconds) / 60)
  if (minutes < 1) return 'menos de 1 min'
  const days = Math.floor(minutes / 1440)
  const hours = Math.floor((minutes % 1440) / 60)
  const mins = minutes % 60
  if (days > 0) return `${days} d ${hours} h`
  if (hours > 0) return `${hours} h ${mins} min`
  return `${mins} min`
}

export interface Summary {
  topCpu: ProcessRow | null
  topMemory: ProcessRow | null
  /** Soma do uso de CPU de todos os processos no intervalo (% da CPU inteira). */
  cpuTotal: number | null
}

export function summarize(groups: ProcessRow[]): Summary {
  const measured = groups.filter((g) => g.cpuPercent !== null)
  const topCpu = sortRows(measured, 'cpu', true)[0] ?? null
  const topMemory = sortRows(groups, 'memory', true)[0] ?? null
  const cpuTotal = measured.length > 0 ? Math.min(100, measured.reduce((s, g) => s + (g.cpuPercent ?? 0), 0)) : null
  return { topCpu, topMemory, cpuTotal }
}
