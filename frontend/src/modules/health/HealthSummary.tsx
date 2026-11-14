import { SEVERITY_LABEL, overallMessage } from './labels'
import type { HealthReport } from './types'

/** Resumo de saúde: severidade geral e contagem por nível. */
export function HealthSummary({ report }: { report: HealthReport }) {
  const level = report.overall.toLowerCase()
  return (
    <section className={`summary summary-${level}`} aria-label="Resumo de saúde" data-overall={report.overall}>
      <p className="summary-status">
        <span className={`badge badge-${level}`}>{SEVERITY_LABEL[report.overall]}</span>
        <span>{overallMessage(report.overall, report.criticalCount, report.warningCount)}</span>
      </p>
      <ul className="summary-counts">
        <li>
          <strong>{report.criticalCount}</strong> Crítico
        </li>
        <li>
          <strong>{report.warningCount}</strong> Atenção
        </li>
        <li>
          <strong>{report.infoCount}</strong> Informação
        </li>
      </ul>
    </section>
  )
}
