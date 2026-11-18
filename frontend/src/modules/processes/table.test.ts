import { describe, expect, it } from 'vitest'
import { elapsedLabel, filterRows, groupByName, priorityLabel, rowsOf, sortRows, stateLabel, summarize } from './table'
import type { ProcessInfo } from './types'

function proc(pid: number, name: string | null, overrides: Partial<ProcessInfo> = {}): ProcessInfo {
  return {
    pid, parentPid: 1, name, user: 'aluno', state: 'RUNNING', priority: 8, threads: 2, cpuPercent: 1,
    memoryBytes: 100, virtualBytes: 1000, startTime: '2026-01-01T10:00:00Z', self: false, ...overrides,
  }
}

const LIST = [
  proc(10, 'brave', { cpuPercent: 5, memoryBytes: 300, threads: 10, startTime: '2026-01-01T09:00:00Z' }),
  proc(11, 'Brave', { cpuPercent: 3, memoryBytes: 200, threads: 5, priority: 10 }),
  proc(20, 'java', { cpuPercent: 2, memoryBytes: 900, self: true }),
  proc(30, 'novo', { cpuPercent: null }),
]

describe('processos', () => {
  it('agrupa por nome somando CPU, memória e threads', () => {
    const groups = groupByName(LIST, true)
    const brave = groups.find((g) => g.name === 'brave')!
    expect(brave.pids).toEqual([10, 11])
    expect(brave.cpuPercent).toBe(8)
    expect(brave.memoryBytes).toBe(500)
    expect(brave.threads).toBe(15)
    expect(brave.priority).toBeNull() // prioridades diferentes
    expect(brave.startTime).toBe('2026-01-01T09:00:00Z') // o mais antigo
    expect(groupByName(LIST, false).filter((g) => g.name.toLowerCase() === 'brave')).toHaveLength(2)
  })

  it('processo sem medição de CPU continua sem valor (não vira zero)', () => {
    expect(groupByName(LIST, true).find((g) => g.name === 'novo')!.cpuPercent).toBeNull()
  })

  it('filtra por nome ou PID exato e ordena', () => {
    const rows = rowsOf(LIST)
    expect(filterRows(rows, 'BRA').map((r) => r.pids[0])).toEqual([10, 11])
    expect(filterRows(rows, '20').map((r) => r.name)).toEqual(['java'])
    expect(sortRows(rows, 'memory', true)[0].name).toBe('java')
    expect(sortRows(rows, 'cpu', true).at(-1)!.name).toBe('novo')
    expect(sortRows(rows, 'name', false)[0].name).toBe('brave')
  })

  it('nomeia prioridade e estado conforme o sistema', () => {
    expect(priorityLabel(8, 'windows')).toBe('Normal (8)')
    expect(priorityLabel(13, 'windows')).toBe('Alta (13)')
    expect(priorityLabel(9, 'windows')).toBe('9')
    expect(priorityLabel(20, 'linux')).toBe('nice 0')
    expect(priorityLabel(25, 'linux')).toBe('nice +5')
    expect(priorityLabel(null, 'linux')).toBe('Várias')
    expect(stateLabel('SLEEPING')).toBe('Dormindo')
    expect(stateLabel('ZOMBIE')).toBe('Zumbi')
  })

  it('escreve o tempo em execução de forma curta', () => {
    expect(elapsedLabel(30)).toBe('menos de 1 min')
    expect(elapsedLabel(24 * 60)).toBe('24 min')
    expect(elapsedLabel(2 * 3600 + 24 * 60)).toBe('2 h 24 min')
    expect(elapsedLabel(6 * 86400 + 3 * 3600 + 59)).toBe('6 d 3 h')
  })

  it('resume o maior consumo de CPU e de memória', () => {
    const s = summarize(groupByName(LIST, true))
    expect(s.topCpu!.name).toBe('brave')
    expect(s.topMemory!.name).toBe('java')
    expect(s.cpuTotal).toBe(10) // 5 + 3 + 2; o processo sem medição não entra
  })
})
