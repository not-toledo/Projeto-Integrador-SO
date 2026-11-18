import { factsOf, gaugeOf, type Gauge } from './gauge'
import { KIND_LABEL, SEVERITY_LABEL, sortAlerts } from './labels'
import type { HealthAlert, HealthThresholds } from './types'

function GaugeBar({ gauge, level }: { gauge: Gauge; level: string }) {
  return (
    <div className="gauge">
      <div className="gauge-track" role="img" aria-label={`${gauge.measure}: ${Math.round(gauge.fill)}%`}>
        <div className={`gauge-fill gauge-fill-${level}`} style={{ width: `${gauge.fill}%` }} />
        {gauge.marks.map((m) => (
          <span key={m.label} className="gauge-mark" style={{ left: `${m.at}%` }} title={m.label} />
        ))}
      </div>
      <div className="gauge-legend">
        <span>{gauge.measure}</span>
        <span>{gauge.marks.map((m) => m.label).join(' · ')}</span>
      </div>
    </div>
  )
}

function AlertItem({ alert, thresholds }: { alert: HealthAlert; thresholds: HealthThresholds }) {
  const level = alert.severity.toLowerCase()
  const gauge = gaugeOf(alert, thresholds)
  const facts = gauge ? [] : factsOf(alert)
  // O valor completo só se repete nos detalhes quando o destaque mostra uma versão curta.
  const showObserved = gauge ? gauge.headline !== alert.observedValue : facts.length > 0

  return (
    <li className={`alert alert-${level} alert-kind-${alert.kind.toLowerCase()}`} data-severity={alert.severity}>
      <div className="alert-head">
        {/* O texto da severidade garante que a informação não dependa só da cor. */}
        <span className={`badge badge-${level}`}>{SEVERITY_LABEL[alert.severity]}</span>
        <span className="alert-component">{alert.component}</span>
      </div>
      <h3 className="alert-title">{alert.title}</h3>

      {gauge && <p className={`alert-value alert-value-${level}`}>{gauge.headline}</p>}
      {!gauge && facts.length > 0 && (
        <ul className="alert-facts">
          {facts.map((f) => <li key={f}>{f}</li>)}
        </ul>
      )}
      {!gauge && facts.length === 0 && (
        <p className={alert.observedValue ? 'alert-value alert-value-text' : 'alert-value alert-value-missing'}>
          {alert.observedValue ?? 'Sem leitura'}
        </p>
      )}
      {gauge && <GaugeBar gauge={gauge} level={level} />}

      <p className="alert-description">{alert.description}</p>
      <dl className="fields alert-fields">
        {showObserved && (
          <div className="field">
            <dt>Valor observado</dt>
            <dd>{alert.observedValue}</dd>
          </div>
        )}
        {alert.threshold && (
          <div className="field">
            <dt>Limite</dt>
            <dd>{alert.threshold}</dd>
          </div>
        )}
        <div className="field">
          <dt>Natureza</dt>
          <dd>{KIND_LABEL[alert.kind]}</dd>
        </div>
      </dl>
    </li>
  )
}

export function AlertList({ alerts, thresholds }: { alerts: HealthAlert[]; thresholds: HealthThresholds }) {
  if (alerts.length === 0) {
    return <p className="muted">Nenhum alerta foi gerado nesta avaliação.</p>
  }
  return (
    <ul className="alert-list" aria-label="Alertas do Health Check">
      {sortAlerts(alerts).map((alert) => (
        <AlertItem key={alert.id} alert={alert} thresholds={thresholds} />
      ))}
    </ul>
  )
}
