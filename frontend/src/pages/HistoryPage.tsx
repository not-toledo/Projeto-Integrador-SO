import { PageHeader } from '../components/PageHeader'
import { useState } from 'react'
import { dateTime } from '../modules/hardware/format'
import {
  commitSeries, cpuSeries, freePercentSeries, historyMaxGapMs, historyTimes, ramSeries, swapSeries, volumeMounts,
} from '../modules/history/series'
import type { SchedulerStatus } from '../modules/history/types'
import { useHistory } from '../modules/history/useHistory'
import { LineChart } from '../modules/live/LineChart'

const PERIODS = [1, 7, 30]

function dayAndTime(ms: number): string {
  const d = new Date(ms)
  return Number.isNaN(d.getTime()) ? '—'
    : d.toLocaleString('pt-BR', { day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit' })
}

function SchedulerBanner({ scheduler }: { scheduler: SchedulerStatus }) {
  if (scheduler.state === 'INSTALLED') {
    return (
      <div className="banner banner-ok" role="status">
        <strong>Coleta agendada ativa.</strong> {scheduler.details}
      </div>
    )
  }
  if (scheduler.state === 'NOT_INSTALLED') {
    return (
      <div className="banner game-warning" role="status">
        <strong>Coleta agendada não instalada.</strong> Para registrar o histórico mesmo com o NEXCORE fechado, execute
        na raiz do projeto <code>scripts\agendar-coleta.bat instalar</code> (Windows) ou{' '}
        <code>scripts/agendar-coleta.sh instalar</code> (Linux).
      </div>
    )
  }
  return (
    <div className="banner game-warning" role="status">
      <strong>Não foi possível consultar o agendador do sistema.</strong> {scheduler.details}
    </div>
  )
}

export function HistoryPage() {
  const [days, setDays] = useState(7)
  const { history, loading, error, refresh } = useHistory(days)

  const records = history?.records ?? []
  const times = historyTimes(records)
  const maxGapMs = historyMaxGapMs(times)
  const mounts = volumeMounts(records)

  return (
    <main className="page">
      <PageHeader
        eyebrow="Coletas agendadas"
        title="Histórico"
        actions={
          <div className="history-controls">
            <label htmlFor="history-days">Período</label>
            <select id="history-days" value={days} onChange={(e) => setDays(Number(e.target.value))}>
              {PERIODS.map((p) => <option key={p} value={p}>{p === 1 ? 'Último dia' : `Últimos ${p} dias`}</option>)}
            </select>
            <button type="button" onClick={refresh} disabled={loading}>Atualizar</button>
          </div>
        }
      >
        <p className="muted">
          Coletas feitas pela tarefa agendada do sistema operacional, inclusive com o NEXCORE fechado.
        </p>
      </PageHeader>

      {error && <div className="banner banner-error" role="alert">{error}</div>}
      {loading && !history && <p className="loading" role="status">Lendo o histórico…</p>}

      {history && (
        <>
          <SchedulerBanner scheduler={history.scheduler} />
          <p className="muted">
            {history.count} coleta(s) no período · Última: {history.lastCollectedAt ? dateTime(history.lastCollectedAt) : 'nenhuma'}
            {' '}· Pasta: <code>{history.directory}</code>
          </p>
          {history.problems.length > 0 && (
            <div className="banner game-warning" role="alert">
              <strong>Alguns arquivos não puderam ser lidos</strong>
              <ul>{history.problems.map((p) => <li key={p}>{p}</li>)}</ul>
            </div>
          )}
          {records.length === 0 ? (
            <p className="muted">Nenhuma coleta neste período.</p>
          ) : (
            <div className="grid">
              <section className="card" aria-label="CPU e memória">
                <h2>CPU e memória</h2>
                <LineChart title="Uso da CPU" unit="%" times={times} series={[{ name: 'CPU', values: cpuSeries(records) }]}
                  yMax={100} maxGapMs={maxGapMs} formatTime={dayAndTime} />
                <LineChart title="Uso da RAM e do arquivo de paginação" unit="%" times={times}
                  series={[{ name: 'RAM', values: ramSeries(records) }, { name: 'Paginação/swap', values: swapSeries(records), dashed: true }]}
                  yMax={100} maxGapMs={maxGapMs} formatTime={dayAndTime} />
                <LineChart title="Memória comprometida" unit="% do limite" times={times}
                  series={[{ name: 'Comprometida', values: commitSeries(records) }]} maxGapMs={maxGapMs} formatTime={dayAndTime} />
              </section>
              <section className="card" aria-label="Espaço livre nos volumes">
                <h2>Espaço livre nos volumes</h2>
                {mounts.map((mount) => (
                  <LineChart key={mount} title={`Espaço livre — ${mount}`} unit="%" times={times}
                    series={[{ name: 'Livre', values: freePercentSeries(records, mount) }]} yMax={100}
                    maxGapMs={maxGapMs} formatTime={dayAndTime} />
                ))}
              </section>
            </div>
          )}
          <p className="muted">
            Lacunas nos gráficos indicam períodos sem coleta (por exemplo, computador desligado); nenhum valor é preenchido.
          </p>
        </>
      )}
    </main>
  )
}
