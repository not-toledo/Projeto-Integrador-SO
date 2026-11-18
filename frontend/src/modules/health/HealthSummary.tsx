import { SEVERITY_LABEL, overallMessage } from './labels'
import type { HealthReport, Severity } from './types'

/** Palavra em destaque do estado geral. "Dentro dos limites" não afirma que o hardware está perfeito. */
const HEADLINE: Record<Severity, string> = {
  INFO: 'Dentro dos limites',
  WARNING: 'Pede atenção',
  CRITICAL: 'Nível crítico',
}

/** Resumo de saúde: estado geral em destaque e contagem por nível. */
export function HealthSummary({ report }: { report: HealthReport }) {
  const level = report.overall.toLowerCase()
  const counts: { severity: Severity; value: number }[] = [
    { severity: 'CRITICAL', value: report.criticalCount },
    { severity: 'WARNING', value: report.warningCount },
    { severity: 'INFO', value: report.infoCount },
  ]
  return (
    <section className={`summary summary-${level}`} aria-label="Resumo de saúde" data-overall={report.overall}>
      <div className="summary-main">
        <p className="summary-headline display">{HEADLINE[report.overall]}</p>
        <p className="summary-status">
          <span className={`badge badge-${level}`}>{SEVERITY_LABEL[report.overall]}</span>
          <span>{overallMessage(report.overall, report.criticalCount, report.warningCount)}</span>
        </p>
      </div>
      <ul className="summary-counts">
        {counts.map(({ severity, value }) => (
          <li key={severity} className={value > 0 ? `count count-${severity.toLowerCase()}` : 'count count-zero'}>
            <strong>{value}</strong> {SEVERITY_LABEL[severity]}
          </li>
        ))}
      </ul>
    </section>
  )
}
