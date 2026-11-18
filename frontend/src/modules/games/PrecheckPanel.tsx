import { useState } from 'react'
import { fetchPrecheck } from '../../services/gameService'
import { bytes, dateTime } from '../hardware/format'
import type { Precheck, PrecheckItem, ProcessGroup } from './types'

const STATUS_LABEL: Record<PrecheckItem['status'], string> = {
  OK: 'Ok',
  ATTENTION: 'Atenção',
  INFO: 'Informação',
  UNAVAILABLE: 'Não verificado',
}
const STATUS_CLASS: Record<PrecheckItem['status'], string> = {
  OK: 'game-status-ok',
  ATTENTION: 'game-status-bad',
  INFO: 'game-status-unknown',
  UNAVAILABLE: 'game-status-unknown',
}

function percent(value: number): string {
  return `${value.toLocaleString('pt-BR', { minimumFractionDigits: 1, maximumFractionDigits: 1 })}%`
}

function ProcessTable({ title, rows, value }: { title: string; rows: ProcessGroup[]; value: (p: ProcessGroup) => string }) {
  return (
    <div>
      <h4>{title}</h4>
      {rows.length === 0 ? <p className="muted">Nenhum processo em uso relevante.</p> : (
        <ol className="precheck-processes">
          {rows.map((p) => (
            <li key={p.name}>{p.name}{p.processCount > 1 ? ` (${p.processCount} processos)` : ''}: <strong>{value(p)}</strong></li>
          ))}
        </ol>
      )}
    </div>
  )
}

/**
 * Pré-checagem antes de jogar: o estado do PC agora (memória livre, CPU, processos, disco do jogo, energia).
 * Complementa a compatibilidade, que compara o hardware instalado com os requisitos.
 */
export function PrecheckPanel({ appId }: { appId: number }) {
  const [result, setResult] = useState<Precheck | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  async function run() {
    setLoading(true)
    setError('')
    try {
      setResult(await fetchPrecheck(appId))
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Não foi possível fazer a pré-checagem.')
    } finally {
      setLoading(false)
    }
  }

  const current = result?.appId === appId ? result : null
  return (
    <section className="card precheck" aria-label="Pré-checagem antes de jogar">
      <div className="precheck-header">
        <div>
          <h2>Pré-checagem antes de jogar</h2>
          <p className="muted">Avalia o estado do PC agora: memória livre, programas abertos, disco do jogo e energia.</p>
        </div>
        <button type="button" onClick={() => void run()} disabled={loading}>
          {loading ? 'Verificando…' : current ? 'Verificar de novo' : 'Verificar agora'}
        </button>
      </div>
      {error && <div className="banner banner-error" role="alert">{error}</div>}
      {current && (
        <>
          <p className={`game-verdict ${current.overall === 'READY' ? 'game-verdict-ok' : 'game-verdict-limited'}`}>
            <strong>{current.overallLabel}</strong>
          </p>
          <div className="table-scroll"><table className="game-table">
            <thead><tr><th>Verificação</th><th>Agora</th><th>Resultado</th></tr></thead>
            <tbody>{current.items.map((item) => (
              <tr key={item.id}>
                <td>{item.label}</td>
                <td>{item.observed ?? '—'}</td>
                <td>
                  <span className={`game-status ${STATUS_CLASS[item.status]}`}>{STATUS_LABEL[item.status]}</span>
                  <small className="game-explanation">{item.detail}</small>
                </td>
              </tr>
            ))}</tbody>
          </table></div>
          {current.processError ? <p className="muted">{current.processError}</p> : (
            <div className="precheck-columns">
              <ProcessTable title="Mais CPU agora" rows={current.topCpu} value={(p) => percent(p.cpuPercent)} />
              <ProcessTable title="Mais memória agora" rows={current.topMemory} value={(p) => bytes(p.memoryBytes)} />
            </div>
          )}
          <p className="muted">
            Verificado em {dateTime(current.checkedAt)}. CPU em % do total (todos os núcleos), medida em cerca de 1 s;
            memória é a soma do conjunto de trabalho de cada aplicativo. O PCCheck não encerra processos. Pré-checagem
            do estado atual: não garante desempenho.
          </p>
        </>
      )}
    </section>
  )
}
