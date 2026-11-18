import { ArrowIcon } from '../../components/Icons'
import { Sparkline } from '../../components/Sparkline'
import { hrefOf } from '../../navigation'
import { celsius, livePercent, timeOfDay, NO_READING } from '../live/format'
import { CONNECTION_LABEL, connectionState, cpuTemperature, cpuTotal, gpuTemperature, gpuUtilization, memoryPercent } from '../live/series'
import type { LiveSample } from '../live/types'
import { useLiveMonitor } from '../live/useLiveMonitor'

/** Quantas amostras os minigráficos mostram (2 minutos com o intervalo padrão de 2 s). */
const SPARK_POINTS = 60

const TILES: { label: string; read: (s: LiveSample) => number | null; format: (v: number | null) => string }[] = [
  { label: 'Uso da CPU', read: cpuTotal, format: livePercent },
  { label: 'Uso da RAM', read: memoryPercent, format: livePercent },
  { label: 'Uso da GPU', read: gpuUtilization, format: livePercent },
  { label: 'Temperatura da CPU', read: cpuTemperature, format: celsius },
  { label: 'Temperatura da GPU', read: gpuTemperature, format: celsius },
]

/** Painel resumido do Live Monitor: leituras reais deste computador, atualizadas sozinhas. */
export function LiveGlance() {
  const { samples, status, everConnected, clientFailures, now } = useLiveMonitor()
  const latest = samples.length > 0 ? samples[samples.length - 1] : null
  const state = connectionState({ everConnected, clientFailures, status, latest, now })
  const recent = samples.slice(-SPARK_POINTS)

  return (
    <section className="section" aria-labelledby="live-title">
      <div className="section-head">
        <h2 id="live-title" className="section-title display">Agora</h2>
        <p className={`connection connection-${state}`} role="status" aria-live="polite">
          <span className="connection-dot" aria-hidden="true" />
          {CONNECTION_LABEL[state]}
        </p>
      </div>
      <p className="section-lead">
        Leituras deste computador nos últimos minutos, atualizadas sozinhas. Sem sensor ou permissão, o painel mostra
        "{NO_READING}" em vez de um valor.
      </p>
      <ul className="tiles">
        {TILES.map((t) => {
          const current = latest ? t.read(latest) : null
          return (
            <li key={t.label} className="tile">
              <span className="tile-label">{t.label}</span>
              <strong className={current === null ? 'tile-value tile-missing' : 'tile-value'}>{t.format(current)}</strong>
              <Sparkline values={recent.map(t.read)} />
            </li>
          )
        })}
      </ul>
      <div className="section-foot">
        <span className="muted">Última leitura: {latest ? timeOfDay(latest.collectedAt) : NO_READING}</span>
        <a className="link-arrow" href={hrefOf('live')}>Abrir o monitor completo <ArrowIcon /></a>
      </div>
    </section>
  )
}
