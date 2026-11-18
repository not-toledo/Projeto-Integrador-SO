import { PageHeader } from '../components/PageHeader'
import { useHealthCheck } from '../modules/health/useHealthCheck'
import { AlertList } from '../modules/health/AlertList'
import { HealthSummary } from '../modules/health/HealthSummary'
import { dateTime } from '../modules/hardware/format'

/** Módulo B — Health Check. Apenas informa; não altera o sistema. */
export function HealthCheckPage() {
  const { report, loading, error, refresh } = useHealthCheck()

  return (
    <main className="page">
      <PageHeader
        eyebrow="Módulo B · Health Check"
        title="Saúde do computador"
        actions={
          <button type="button" onClick={refresh} disabled={loading}>
            {loading ? 'Avaliando…' : 'Reavaliar'}
          </button>
        }
      >
        {report && (
          <p className="muted">
            Última avaliação: <strong>{dateTime(report.collectedAt)}</strong>
            <br />
            Origem: {report.source}
          </p>
        )}
      </PageHeader>

      {error && (
        <div className="banner banner-error" role="alert">
          <strong>Não foi possível obter o Health Check.</strong>
          <p>{error}</p>
          {report && <p>Exibindo abaixo a última avaliação bem-sucedida ({dateTime(report.collectedAt)}).</p>}
        </div>
      )}

      {!report && loading && (
        <p className="loading" role="status" aria-live="polite">
          Avaliando os indicadores do computador…
        </p>
      )}

      {report && (
        <>
          <HealthSummary report={report} />
          <AlertList alerts={report.alerts} thresholds={report.thresholds} />
          <p className="muted">
            Limites: {report.thresholdsSource} O Health Check apenas informa; alertas preventivos e dados indisponíveis
            não comprovam defeito.
          </p>
        </>
      )}
    </main>
  )
}
