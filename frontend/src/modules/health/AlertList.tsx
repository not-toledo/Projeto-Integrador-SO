import { KIND_LABEL, SEVERITY_LABEL, sortAlerts } from './labels'
import type { HealthAlert } from './types'

function AlertItem({ alert }: { alert: HealthAlert }) {
  const level = alert.severity.toLowerCase()
  return (
    <li className={`alert alert-${level}`} data-severity={alert.severity}>
      <div className="alert-head">
        {/* O texto da severidade garante que a informação não dependa só da cor. */}
        <span className={`badge badge-${level}`}>{SEVERITY_LABEL[alert.severity]}</span>
        <span className="alert-component">{alert.component}</span>
      </div>
      <h3 className="alert-title">{alert.title}</h3>
      <p className="alert-description">{alert.description}</p>
      <dl className="fields">
        <div className="field">
          <dt>Valor observado</dt>
          <dd>{alert.observedValue ?? 'Sem leitura'}</dd>
        </div>
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

export function AlertList({ alerts }: { alerts: HealthAlert[] }) {
  if (alerts.length === 0) {
    return <p className="muted">Nenhum alerta foi gerado nesta avaliação.</p>
  }
  return (
    <ul className="alert-list" aria-label="Alertas do Health Check">
      {sortAlerts(alerts).map((alert) => (
        <AlertItem key={alert.id} alert={alert} />
      ))}
    </ul>
  )
}
