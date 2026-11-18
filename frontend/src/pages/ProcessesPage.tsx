import { useMemo, useState } from 'react'
import { PageHeader } from '../components/PageHeader'
import { NOT_AVAILABLE, bytes } from '../modules/hardware/format'
import { livePercent, timeOfDay } from '../modules/live/format'
import {
  elapsedLabel, filterRows, groupByName, priorityLabel, rowsOf, sortRows, stateLabel, summarize, type ProcessRow,
  type SortKey,
} from '../modules/processes/table'
import type { ProcessesResponse } from '../modules/processes/types'
import { AUTO_REFRESH_MS, useProcesses } from '../modules/processes/useProcesses'

/** Linhas mostradas antes de "Mostrar todos" (a lista completa costuma ter centenas de processos). */
const PAGE = 50

const COLUMNS: { key: SortKey; label: string; numeric?: boolean }[] = [
  { key: 'name', label: 'Nome' },
  { key: 'pid', label: 'PID', numeric: true },
  { key: 'cpu', label: 'CPU', numeric: true },
  { key: 'memory', label: 'Memória', numeric: true },
  { key: 'threads', label: 'Threads', numeric: true },
  { key: 'priority', label: 'Prioridade' },
  { key: 'start', label: 'Em execução há' },
]

const CONCEPTS: [string, string][] = [
  ['Processo', 'Um programa em execução, com memória e recursos próprios. Um mesmo aplicativo pode abrir vários.'],
  ['PID', 'Número que o sistema operacional dá a cada processo enquanto ele existe.'],
  ['Thread', 'Linha de execução dentro de um processo. As threads dividem a memória do processo e são o que o escalonador coloca na CPU.'],
  ['Prioridade', 'Peso que o escalonador usa para decidir quem roda primeiro. No Windows, 8 é a prioridade normal; no Linux, nice 0.'],
  ['Memória', 'Memória física em uso pelo processo (conjunto de trabalho no Windows, RSS no Linux).'],
  ['CPU', 'Parte da CPU inteira usada no último segundo medido, somando todos os núcleos.'],
]

function Meter({ value, max }: { value: number; max: number }) {
  const width = max > 0 ? Math.min(100, (value / max) * 100) : 0
  return (
    <span className="meter" aria-hidden="true">
      <span className="meter-fill" style={{ width: `${width}%` }} />
    </span>
  )
}

function TopList({ title, rows, value, max, format }: {
  title: string
  rows: ProcessRow[]
  value: (r: ProcessRow) => number
  max: number
  format: (r: ProcessRow) => string
}) {
  return (
    <section className="card top-list" aria-label={title}>
      <h2>{title}</h2>
      <ol>
        {rows.map((r) => (
          <li key={r.key}>
            <span className="top-name">
              {r.name}
              {r.pids.length > 1 && <small> · {r.pids.length} processos</small>}
            </span>
            <span className="top-value">{format(r)}</span>
            <Meter value={value(r)} max={max} />
          </li>
        ))}
      </ol>
    </section>
  )
}

function ProcessTable({ data, now }: { data: ProcessesResponse; now: number }) {
  const [query, setQuery] = useState('')
  const [grouped, setGrouped] = useState(true)
  const [sort, setSort] = useState<{ key: SortKey; desc: boolean }>({ key: 'cpu', desc: true })
  const [showAll, setShowAll] = useState(false)
  const windows = data.osFamily === 'windows'

  const rows = useMemo(() => {
    const base = grouped ? groupByName(data.processes, windows) : rowsOf(data.processes)
    return sortRows(filterRows(base, query), sort.key, sort.desc)
  }, [data, grouped, windows, query, sort])
  const visible = showAll ? rows : rows.slice(0, PAGE)

  const toggleSort = (key: SortKey) =>
    setSort((s) => (s.key === key ? { key, desc: !s.desc } : { key, desc: key !== 'name' }))

  return (
    <section className="card process-table" aria-label="Lista de processos">
      <h2>Todos os processos</h2>
      <div className="installed-toolbar">
        <label className="sr-only" htmlFor="process-query">Filtrar processos</label>
        <input id="process-query" type="search" value={query} onChange={(e) => setQuery(e.target.value)}
          placeholder="Filtrar por nome ou PID" autoComplete="off" />
        <label className="check">
          <input type="checkbox" checked={grouped} onChange={(e) => setGrouped(e.target.checked)} />
          Agrupar por nome
        </label>
        <span className="muted">{rows.length} {grouped ? 'aplicativos' : 'processos'}</span>
      </div>
      <div className="table-scroll">
        <table className="data-table">
          <thead>
            <tr>
              {COLUMNS.map((c) => (
                <th key={c.key} scope="col" className={c.numeric ? 'num' : undefined}
                  aria-sort={sort.key === c.key ? (sort.desc ? 'descending' : 'ascending') : 'none'}>
                  <button type="button" className="sort" onClick={() => toggleSort(c.key)}>
                    {c.label}
                    <span aria-hidden="true">{sort.key === c.key ? (sort.desc ? ' ▼' : ' ▲') : ''}</span>
                  </button>
                </th>
              ))}
              {!windows && <th scope="col">Estado</th>}
              <th scope="col">Usuário</th>
            </tr>
          </thead>
          <tbody>
            {visible.map((r) => (
              <tr key={r.key} className={r.self ? 'row-self' : undefined}>
                <td>
                  {r.name}
                  {r.self && <span className="chip">PCCheck</span>}
                </td>
                <td className="num">{r.pids.length > 1 ? `${r.pids.length} processos` : r.pids[0]}</td>
                <td className="num">
                  {r.cpuPercent === null ? <span className="muted">Medindo…</span> : livePercent(r.cpuPercent)}
                </td>
                <td className="num">{bytes(r.memoryBytes)}</td>
                <td className="num">{r.threads}</td>
                <td>{priorityLabel(r.priority, data.osFamily)}</td>
                <td>{r.startTime ? elapsedLabel((now - Date.parse(r.startTime)) / 1000) : NOT_AVAILABLE}</td>
                {!windows && <td>{stateLabel(r.state)}</td>}
                <td>{r.user ?? <span className="muted">Não informado</span>}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {rows.length === 0 && <p className="muted">Nenhum processo corresponde ao filtro.</p>}
      {!showAll && rows.length > PAGE && (
        <button type="button" className="show-all" onClick={() => setShowAll(true)}>
          Mostrar todos ({rows.length})
        </button>
      )}
    </section>
  )
}

/** Processos em execução: o que o sistema operacional está rodando agora. Somente leitura. */
export function ProcessesPage() {
  const { data, loading, error, auto, setAuto, refresh } = useProcesses()
  // Referência de "agora" para o tempo em execução: o instante da própria leitura.
  const now = data ? Date.parse(data.collectedAt) : 0
  const groups = useMemo(() => (data ? groupByName(data.processes, data.osFamily === 'windows') : []), [data])
  const summary = summarize(groups)
  const topCpu = sortRows(groups.filter((g) => g.cpuPercent !== null), 'cpu', true).slice(0, 5)
  const topMemory = sortRows(groups, 'memory', true).slice(0, 5)

  return (
    <main className="page">
      <PageHeader
        eyebrow="Sistema operacional · Processos"
        title="Processos"
        actions={
          <>
            <label className="check">
              <input type="checkbox" checked={auto} onChange={(e) => setAuto(e.target.checked)} />
              Atualizar a cada {AUTO_REFRESH_MS / 1000} s
            </label>
            <button type="button" onClick={refresh} disabled={loading}>{loading ? 'Lendo…' : 'Atualizar'}</button>
          </>
        }
      >
        <p className="muted">
          {data
            ? <>Leitura: <strong>{timeOfDay(data.collectedAt)}</strong> · CPU medida em {data.sampleMillis / 1000} s · {data.logicalProcessors} processadores lógicos</>
            : 'Programas em execução neste computador, lidos do sistema operacional.'}
        </p>
      </PageHeader>

      {error && (
        <div className="banner banner-error" role="alert">
          <strong>Não foi possível ler os processos.</strong>
          <p>{error}</p>
          {data && data.processes.length > 0 && <p>Exibindo a última leitura bem-sucedida.</p>}
        </div>
      )}
      {!data && loading && <p className="loading" role="status">Lendo os processos (cerca de 1 s)…</p>}

      {data && data.processes.length > 0 && (
        <>
          <ul className="tiles process-tiles" aria-label="Resumo dos processos">
            <li className="tile">
              <span className="tile-label">Processos</span>
              <strong className="tile-value">{data.processCount ?? data.processes.length}</strong>
            </li>
            <li className="tile">
              <span className="tile-label">Threads</span>
              <strong className="tile-value">{data.threadCount?.toLocaleString('pt-BR') ?? NOT_AVAILABLE}</strong>
            </li>
            <li className="tile">
              <span className="tile-label">CPU usada pelos processos</span>
              <strong className="tile-value">{livePercent(summary.cpuTotal)}</strong>
            </li>
            <li className="tile">
              <span className="tile-label">Mais CPU</span>
              <strong className="tile-value tile-value-text">{summary.topCpu?.name ?? NOT_AVAILABLE}</strong>
              <span className="muted">{summary.topCpu ? livePercent(summary.topCpu.cpuPercent) : ''}</span>
            </li>
            <li className="tile">
              <span className="tile-label">Mais memória</span>
              <strong className="tile-value tile-value-text">{summary.topMemory?.name ?? NOT_AVAILABLE}</strong>
              <span className="muted">{summary.topMemory ? bytes(summary.topMemory.memoryBytes) : ''}</span>
            </li>
          </ul>

          <div className="top-lists">
            {/* Barras relativas ao primeiro da lista; o valor exato fica ao lado. */}
            <TopList title="Mais CPU agora" rows={topCpu} value={(r) => r.cpuPercent ?? 0}
              max={topCpu[0]?.cpuPercent ?? 0} format={(r) => livePercent(r.cpuPercent)} />
            <TopList title="Mais memória" rows={topMemory} value={(r) => r.memoryBytes}
              max={topMemory[0]?.memoryBytes ?? 0} format={(r) => bytes(r.memoryBytes)} />
          </div>

          <ProcessTable data={data} now={now} />

          <section className="card concepts" aria-label="O que estes dados significam">
            <h2>O que estes dados significam</h2>
            <dl>
              {CONCEPTS.map(([term, text]) => (
                <div key={term}>
                  <dt>{term}</dt>
                  <dd>{text}</dd>
                </div>
              ))}
            </dl>
          </section>

          <p className="muted">
            O PCCheck só lê a lista de processos: não encerra nem altera nenhum.
            {data.osFamily === 'windows'
              ? ' No Windows, o sistema informa todos os processos como "em execução" (o estado detalhado não é exposto), por isso a coluna Estado não aparece. O dono de processos do sistema só é informado com permissão de administrador.'
              : ' Sem permissão de administrador, alguns dados de processos de outros usuários podem não ser informados.'}
          </p>
        </>
      )}
    </main>
  )
}
